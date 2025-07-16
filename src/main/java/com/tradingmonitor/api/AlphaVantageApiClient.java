package com.tradingmonitor.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tradingmonitor.Stock;

public class AlphaVantageApiClient implements ApiProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageApiClient.class);
    private static final String ALPHA_VANTAGE_URL_FORMAT = "https://www.alphavantage.co/query?function=INCOME_STATEMENT&symbol=%s&apikey=%s";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AlphaVantageApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    private HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public Map<String, Map<String, BigDecimal>> fetchQuarterlyFinancials(String symbol) throws IOException {
        Map<String, Map<String, BigDecimal>> quarterlyFinancials = new HashMap<>();
        try {
            String url = String.format(ALPHA_VANTAGE_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JsonNode quarterlyFinancialsNode = objectMapper.readTree(response.body());
                if (quarterlyFinancialsNode.has("quarterlyReports") && quarterlyFinancialsNode.get("quarterlyReports").isArray()) {
                    int quartersProcessed = 0;
                    for (JsonNode quarterlyReport : quarterlyFinancialsNode.get("quarterlyReports")) {
                        if (quartersProcessed >= 8) break;
                        String quarter = quarterlyReport.get("fiscalDateEnding").asText();
                        Map<String, BigDecimal> quarterlyReportData = new HashMap<>();
                        quarterlyReportData.put("totalRevenue", toBigDecimal(quarterlyReport.get("totalRevenue")));
                        quarterlyReportData.put("netIncome", toBigDecimal(quarterlyReport.get("netIncome")));
                        quarterlyReportData.put("grossProfit", toBigDecimal(quarterlyReport.get("grossProfit")));
                        quarterlyFinancials.put(quarter, quarterlyReportData);
                        quartersProcessed++;
                    }
                }
            } else {
                logger.warn("Failed to fetch quarterly financials data for {}: Status {}", symbol, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching quarterly financials for {}", symbol, e);
            throw new IOException("Interrupted while fetching quarterly financials for " + symbol, e);
        }
        return quarterlyFinancials;
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
        Map<String, BigDecimal> quarterlyRevenue = new HashMap<>();
        Map<String, BigDecimal> quarterlyNetIncome = new HashMap<>();
        Map<String, BigDecimal> quarterlyGrossProfit = new HashMap<>();

        Map<String, Map<String, BigDecimal>> quarterlyFinancials = fetchQuarterlyFinancials(symbol);
        for (Map.Entry<String, Map<String, BigDecimal>> entry : quarterlyFinancials.entrySet()) {
            quarterlyRevenue.put(entry.getKey(), entry.getValue().get("totalRevenue"));
            quarterlyNetIncome.put(entry.getKey(), entry.getValue().get("netIncome"));
            quarterlyGrossProfit.put(entry.getKey(), entry.getValue().get("grossProfit"));
        }

        return new Stock(symbol, null, null, null, null, null, exchange, null, null, null, quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit);
    }
}
