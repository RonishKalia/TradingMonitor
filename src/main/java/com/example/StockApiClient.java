package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockApiClient {

    private static final Logger logger = LoggerFactory.getLogger(StockApiClient.class);
    private static final String BASE_URL = "https://finnhub.io/api/v1";
    private static final String SYMBOL_URL_FORMAT = BASE_URL + "/stock/symbol?exchange=US&token=%s";
    private static final String QUOTE_URL_FORMAT = BASE_URL + "/quote?symbol=%s&token=%s";
    private static final String PROFILE_URL_FORMAT = BASE_URL + "/stock/profile2?symbol=%s&token=%s";
    private static final String METRIC_URL_FORMAT = BASE_URL + "/stock/metric?symbol=%s&metric=all&token=%s";
    private static final String FINANCIALS_URL_FORMAT = BASE_URL + "/stock/financials-reported?symbol=%s&freq=annual&token=%s";
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long TIME_WINDOW_MS = 65 * 1000; // 65 seconds

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<Long> requestTimestamps;

    public StockApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestTimestamps = new ArrayBlockingQueue<>(MAX_REQUESTS_PER_MINUTE);
    }

    private void rateLimit() throws InterruptedException {
        long currentTime = System.currentTimeMillis();

        // If the queue is full, we must wait.
        if (requestTimestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            long oldestTimestamp = requestTimestamps.peek(); // Time of the first request in the window
            long elapsedTime = currentTime - oldestTimestamp;

            if (elapsedTime < TIME_WINDOW_MS) {
                long waitTime = TIME_WINDOW_MS - elapsedTime;
                logger.warn("Rate limit reached. Waiting for {} ms to respect the 65-second window.", waitTime);
                Thread.sleep(waitTime);
            }
            // After waiting, the time window for the oldest request has passed. Remove it to make space.
            requestTimestamps.poll();
        }
        // Add the timestamp for the new request.
        requestTimestamps.offer(System.currentTimeMillis());
    }

    private HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
        rateLimit();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public List<String> fetchUsStockSymbols() throws IOException {
        try {
            String url = String.format(SYMBOL_URL_FORMAT, apiKey);
            HttpResponse<String> response = sendRequest(url);

            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch stock symbols: " + response.statusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            List<String> symbols = new ArrayList<>();
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    if (node.has("symbol")) {
                        symbols.add(node.get("symbol").asText());
                    }
                }
            }
            return symbols;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Failed to fetch stock symbols", e);
        }
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

    public Stock fetchStockData(String symbol, String exchange) throws IOException {
        try {
            String quoteUrl = String.format(QUOTE_URL_FORMAT, symbol, apiKey);
            String profileUrl = String.format(PROFILE_URL_FORMAT, symbol, apiKey);
            String metricUrl = String.format(METRIC_URL_FORMAT, symbol, apiKey);
            String financialsUrl = String.format(FINANCIALS_URL_FORMAT, symbol, apiKey);

            HttpResponse<String> quoteResponse = sendRequest(quoteUrl);
            HttpResponse<String> profileResponse = sendRequest(profileUrl);
            HttpResponse<String> metricResponse = sendRequest(metricUrl);
            HttpResponse<String> financialsResponse = sendRequest(financialsUrl);

            if (quoteResponse.statusCode() != 200 || profileResponse.statusCode() != 200 || metricResponse.statusCode() != 200 || financialsResponse.statusCode() != 200) {
                logger.error("Failed to fetch data for symbol: {}. Quote status: {}, Profile status: {}, Metric status: {}, Financials status: {}", symbol, quoteResponse.statusCode(), profileResponse.statusCode(), metricResponse.statusCode(), financialsResponse.statusCode());
                throw new IOException("Failed to fetch data for symbol: " + symbol);
            }

            JsonNode quoteNode = objectMapper.readTree(quoteResponse.body());
            JsonNode profileNode = objectMapper.readTree(profileResponse.body());
            JsonNode metricNode = objectMapper.readTree(metricResponse.body());
            JsonNode financialsNode = objectMapper.readTree(financialsResponse.body());

            BigDecimal peRatio = null;
            if (metricNode.has("metric") && metricNode.get("metric").has("peNormalizedAnnual")) {
                peRatio = toBigDecimal(metricNode.get("metric").get("peNormalizedAnnual"));
            }

            BigDecimal revenue = null;
            BigDecimal grossProfit = null;
            if (financialsNode.has("data") && financialsNode.get("data").isArray() && financialsNode.get("data").size() > 0) {
                JsonNode latestFinancials = financialsNode.get("data").get(0);
                if (latestFinancials.has("report") && latestFinancials.get("report").has("ic") && latestFinancials.get("report").get("ic").isArray()) {
                    for (JsonNode item : latestFinancials.get("report").get("ic")) {
                        if (item.has("concept") && item.get("concept").asText().equals("us-gaap_RevenueFromContractWithCustomerExcludingAssessedTax")) {
                            revenue = toBigDecimal(item.get("value"));
                        }
                        if (item.has("concept") && item.get("concept").asText().equals("us-gaap_GrossProfit")) {
                            grossProfit = toBigDecimal(item.get("value"));
                        }
                    }
                }
            }

            return new Stock(
                symbol,
                profileNode.has("name") ? profileNode.get("name").asText() : null,
                toBigDecimal(quoteNode.get("c")),
                peRatio,
                toBigDecimal(profileNode.get("marketCapitalization")),
                revenue,
                grossProfit,
                null,
                exchange
            );
        } catch (Exception e) {
            logger.error("Failed to fetch data for symbol: " + symbol, e);
            throw new IOException("Failed to fetch data for symbol: " + symbol, e);
        }
    }
}
