package com.tradingmonitor.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingmonitor.Stock;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinancialModelingPrepApiClient implements ApiProvider {

    private static final Logger logger = LoggerFactory.getLogger(FinancialModelingPrepApiClient.class);
    private static final String FMP_SYMBOL_LIST_URL_FORMAT = "https://financialmodelingprep.com/api/v3/stock/list?apikey=%s";
    private static final String FMP_KEY_METRICS_URL_FORMAT = "https://financialmodelingprep.com/api/v3/key-metrics-ttm/%s?apikey=%s";
    private static final String FMP_FINANCIALS_URL_FORMAT = "https://financialmodelingprep.com/api/v3/income-statement/%s?limit=5&apikey=%s";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FinancialModelingPrepApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    private HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public List<String> fetchUsStockSymbols() throws IOException {
        try {
            String url = String.format(FMP_SYMBOL_LIST_URL_FORMAT, apiKey);
            HttpResponse<String> response = sendRequest(url);

            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch stock symbols from FMP: " + response.statusCode() + " " + response.body());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            List<String> symbols = new ArrayList<>();
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    if (node.has("symbol") && node.has("exchange")) {
                        String exchange = node.get("exchange").asText();
                        if (exchange.contains("New York Stock Exchange") || exchange.contains("Nasdaq")) {
                            symbols.add(node.get("symbol").asText());
                        }
                    }
                }
            }
            return symbols;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Failed to fetch stock symbols", e);
        }
    }

    public Map<String, BigDecimal> fetchKeyMetrics(String symbol) throws IOException {
        Map<String, BigDecimal> metrics = new HashMap<>();
        try {
            String url = String.format(FMP_KEY_METRICS_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JsonNode metricNode = objectMapper.readTree(response.body());
                if (metricNode.isArray() && metricNode.size() > 0) {
                    metrics.put("peRatioTTM", toBigDecimal(metricNode.get(0).get("peRatioTTM")));
                }
            } else {
                logger.warn("Failed to fetch metric data for {}: Status {}", symbol, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching key metrics for {}", symbol, e);
            throw new IOException("Interrupted while fetching key metrics for " + symbol, e);
        }
        return metrics;
    }

    public Map<Integer, Map<String, BigDecimal>> fetchFinancials(String symbol) throws IOException {
        Map<Integer, Map<String, BigDecimal>> financials = new HashMap<>();
        try {
            String url = String.format(FMP_FINANCIALS_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JsonNode financialsNode = objectMapper.readTree(response.body());
                if (financialsNode.isArray()) {
                    for (JsonNode annualReport : financialsNode) {
                        int year = annualReport.get("calendarYear").asInt();
                        Map<String, BigDecimal> yearlyFinancials = new HashMap<>();
                        yearlyFinancials.put("revenue", toBigDecimal(annualReport.get("revenue")));
                        yearlyFinancials.put("netIncome", toBigDecimal(annualReport.get("netIncome")));
                        yearlyFinancials.put("grossProfit", toBigDecimal(annualReport.get("grossProfit")));
                        financials.put(year, yearlyFinancials);
                    }
                }
            } else {
                logger.warn("Failed to fetch financials data for {}: Status {}", symbol, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching financials for {}", symbol, e);
            throw new IOException("Interrupted while fetching financials for " + symbol, e);
        }
        return financials;
    }

    private BigDecimal toBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        if (text.isEmpty() || text.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse '{}' as BigDecimal", text);
            return null;
        }
    }

    @Override
    public Stock fetchStockData(String symbol, String exchange) throws IOException {
        BigDecimal peRatio = null;

        Map<String, BigDecimal> metrics = fetchKeyMetrics(symbol);
        peRatio = metrics.get("peRatioTTM");

        return new Stock(symbol, null, null, peRatio, null, null, exchange, null, null, null, null, null, null);
    }
}
