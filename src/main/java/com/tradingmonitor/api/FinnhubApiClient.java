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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinnhubApiClient implements ApiProvider {

    private static final Logger logger = LoggerFactory.getLogger(FinnhubApiClient.class);
    private static final String BASE_URL = "https://finnhub.io/api/v1";
    private static final String QUOTE_URL_FORMAT = BASE_URL + "/quote?symbol=%s&token=%s";
    private static final String PROFILE_URL_FORMAT = BASE_URL + "/stock/profile2?symbol=%s&token=%s";
    private static final String SYMBOL_URL_FORMAT = BASE_URL + "/stock/symbol?exchange=%s&token=%s";
    private static final String METRIC_URL_FORMAT = BASE_URL + "/stock/metric?symbol=%s&metric=all&token=%s";
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long TIME_WINDOW_MS = 60000;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<Long> requestTimestamps;

    public FinnhubApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestTimestamps = new ArrayBlockingQueue<>(MAX_REQUESTS_PER_MINUTE);
    }

    private void rateLimit() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        if (requestTimestamps.size() == MAX_REQUESTS_PER_MINUTE) {
            long oldestTimestamp = requestTimestamps.peek();
            long elapsedTime = currentTime - oldestTimestamp;
            if (elapsedTime < TIME_WINDOW_MS) {
                long waitTime = TIME_WINDOW_MS - elapsedTime;
                logger.warn("Rate limit reached for Finnhub. Waiting for {} ms.", waitTime);
                Thread.sleep(waitTime);
            }
            requestTimestamps.poll();
        }
        requestTimestamps.offer(currentTime);
    }

    private HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
        rateLimit();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public Map<String, BigDecimal> fetchBasicFinancials(String symbol) throws IOException {
        Map<String, BigDecimal> metrics = new HashMap<>();
        try {
            String url = String.format(METRIC_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode metricNode = rootNode.get("metric");
                if (metricNode != null) {
                    metrics.put("peTTM", toBigDecimal(metricNode.get("peTTM")));
                }
            } else {
                logger.warn("Failed to fetch basic financials for {}: Status {}", symbol, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching basic financials for {}", symbol, e);
            throw new IOException("Interrupted while fetching basic financials for " + symbol, e);
        }
        return metrics;
    }

    public Map<String, BigDecimal> fetchQuote(String symbol) throws IOException {
        Map<String, BigDecimal> quote = new HashMap<>();
        try {
            String url = String.format(QUOTE_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JsonNode quoteNode = objectMapper.readTree(response.body());
                quote.put("price", toBigDecimal(quoteNode.get("c")));
            } else {
                logger.warn("Failed to fetch quote data for {}: Status {}", symbol, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching quote for {}", symbol, e);
            throw new IOException("Interrupted while fetching quote for " + symbol, e);
        }
        return quote;
    }

    public Map<String, Object> fetchProfile(String symbol) throws IOException {
        Map<String, Object> profile = new HashMap<>();
        try {
            String url = String.format(PROFILE_URL_FORMAT, symbol, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JsonNode profileNode = objectMapper.readTree(response.body());
                profile.put("name", profileNode.has("name") ? profileNode.get("name").asText() : null);
                profile.put("marketCapitalization", toBigDecimal(profileNode.get("marketCapitalization")));
            } else {
                logger.warn("Failed to fetch profile data for {}: Status {}", symbol, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching profile for {}", symbol, e);
            throw new IOException("Interrupted while fetching profile for " + symbol, e);
        }
        return profile;
    }

    public List<String> fetchStockSymbols(String exchange) throws IOException {
        List<String> symbols = new ArrayList<>();
        try {
            String url = String.format(SYMBOL_URL_FORMAT, exchange, apiKey);
            HttpResponse<String> response = sendRequest(url);
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.isArray()) {
                    for (JsonNode node : rootNode) {
                        if (node.has("type") && node.get("type").asText().equalsIgnoreCase("Common Stock")) {
                            symbols.add(node.get("symbol").asText());
                        }
                    }
                }
            } else {
                logger.warn("Failed to fetch stock symbols for {}: Status {}", exchange, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching stock symbols for {}", exchange, e);
            throw new IOException("Interrupted while fetching stock symbols for " + exchange, e);
        }
        return symbols;
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
        String name = null;
        BigDecimal price = null;
        BigDecimal marketCap = null;

        Map<String, BigDecimal> quote = fetchQuote(symbol);
        price = quote.get("price");

        Map<String, Object> profile = fetchProfile(symbol);
        name = (String) profile.get("name");
        marketCap = (BigDecimal) profile.get("marketCapitalization");

        return new Stock(symbol, name, price, null, marketCap, null, exchange, null, null, null, null, null, null, null, null, null);
    }
}
