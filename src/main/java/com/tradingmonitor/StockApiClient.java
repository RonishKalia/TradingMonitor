package com.tradingmonitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        String name = null;
        BigDecimal price = null;
        BigDecimal peRatio = null;
        BigDecimal marketCap = null;
        BigDecimal revenue = null;
        BigDecimal grossProfit = null;
        BigDecimal volume = null;
        Map<Integer, BigDecimal> historicalRevenue = new HashMap<>();
        Map<Integer, BigDecimal> historicalNetIncome = new HashMap<>();

        try {
            // Fetch quote data
            String quoteUrl = String.format(QUOTE_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> quoteResponse = sendRequest(quoteUrl);
            if (quoteResponse.statusCode() == 200) {
                JsonNode quoteNode = objectMapper.readTree(quoteResponse.body());
                price = toBigDecimal(quoteNode.get("c"));
            } else {
                logger.warn("Failed to fetch quote data for {}: Status {}", symbol, quoteResponse.statusCode());
            }

            // Fetch profile data
            String profileUrl = String.format(PROFILE_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> profileResponse = sendRequest(profileUrl);
            if (profileResponse.statusCode() == 200) {
                JsonNode profileNode = objectMapper.readTree(profileResponse.body());
                name = profileNode.has("name") ? profileNode.get("name").asText() : null;
                marketCap = toBigDecimal(profileNode.get("marketCapitalization"));
            } else {
                logger.warn("Failed to fetch profile data for {}: Status {}", symbol, profileResponse.statusCode());
            }

            // Fetch metric data
            String metricUrl = String.format(METRIC_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> metricResponse = sendRequest(metricUrl);
            if (metricResponse.statusCode() == 200) {
                JsonNode metricNode = objectMapper.readTree(metricResponse.body());
                if (metricNode.has("metric") && metricNode.get("metric").has("peNormalizedAnnual")) {
                    peRatio = toBigDecimal(metricNode.get("metric").get("peNormalizedAnnual"));
                }
            } else {
                logger.warn("Failed to fetch metric data for {}: Status {}", symbol, metricResponse.statusCode());
            }

            // Fetch financials data
            String financialsUrl = String.format(FINANCIALS_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> financialsResponse = sendRequest(financialsUrl);
            if (financialsResponse.statusCode() == 200) {
                JsonNode financialsNode = objectMapper.readTree(financialsResponse.body());
                if (financialsNode.has("data") && financialsNode.get("data").isArray()) {
                    int yearsProcessed = 0;
                    for (JsonNode annualReport : financialsNode.get("data")) {
                        if (yearsProcessed >= 5) break;

                        int year = annualReport.get("year").asInt();
                        if (annualReport.has("report") && annualReport.get("report").has("ic")) {
                            for (JsonNode item : annualReport.get("report").get("ic")) {
                                String concept = item.get("concept").asText();
                                if (concept.equals("us-gaap_RevenueFromContractWithCustomerExcludingAssessedTax") || concept.equals("ifrs-full_Revenue") || concept.equals("Revenues")) {
                                    historicalRevenue.put(year, toBigDecimal(item.get("value")));
                                }
                                if (concept.equals("us-gaap_NetIncomeLoss") || concept.equals("ifrs-full_ProfitLoss")) {
                                    historicalNetIncome.put(year, toBigDecimal(item.get("value")));
                                }
                            }
                        }
                        yearsProcessed++;
                    }
                    // Also get the most recent revenue and gross profit
                    if (financialsNode.get("data").size() > 0) {
                        JsonNode latestFinancials = financialsNode.get("data").get(0);
                        if (latestFinancials.has("report") && latestFinancials.get("report").has("ic")) {
                            for (JsonNode item : latestFinancials.get("report").get("ic")) {
                                String concept = item.get("concept").asText();
                                if (concept.equals("us-gaap_RevenueFromContractWithCustomerExcludingAssessedTax") || concept.equals("ifrs-full_Revenue") || concept.equals("Revenues")) {
                                    revenue = toBigDecimal(item.get("value"));
                                }
                                if (concept.equals("us-gaap_GrossProfit")) {
                                    grossProfit = toBigDecimal(item.get("value"));
                                }
                            }
                        }
                    }
                }
            } else {
                logger.warn("Failed to fetch financials data for {}: Status {}", symbol, financialsResponse.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching data for {}", symbol, e);
            throw new IOException("Interrupted while fetching data for " + symbol, e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching data for {}", symbol, e);
        }

        return new Stock(symbol, name, price, peRatio, marketCap, revenue, grossProfit, volume, exchange, historicalRevenue, historicalNetIncome);
    }
}
