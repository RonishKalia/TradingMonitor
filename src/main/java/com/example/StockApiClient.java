package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockApiClient {

    private static final Logger logger = LoggerFactory.getLogger(StockApiClient.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StockApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public Stock fetchStockData(String symbol, String exchange) throws IOException {
        try {
            String quoteUrl = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
            String profileUrl = String.format("https://finnhub.io/api/v1/stock/profile2?symbol=%s&token=%s", symbol, apiKey);
            String metricUrl = String.format("https://finnhub.io/api/v1/stock/metric?symbol=%s&metric=all&token=%s", symbol, apiKey);
            String financialsUrl = String.format("https://finnhub.io/api/v1/stock/financials-reported?symbol=%s&freq=annual&token=%s", symbol, apiKey);

            HttpRequest quoteRequest = HttpRequest.newBuilder().uri(URI.create(quoteUrl)).build();
            HttpRequest profileRequest = HttpRequest.newBuilder().uri(URI.create(profileUrl)).build();
            HttpRequest metricRequest = HttpRequest.newBuilder().uri(URI.create(metricUrl)).build();
            HttpRequest financialsRequest = HttpRequest.newBuilder().uri(URI.create(financialsUrl)).build();

            HttpResponse<String> quoteResponse = httpClient.send(quoteRequest, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> profileResponse = httpClient.send(profileRequest, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> metricResponse = httpClient.send(metricRequest, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> financialsResponse = httpClient.send(financialsRequest, HttpResponse.BodyHandlers.ofString());

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
                peRatio = new BigDecimal(metricNode.get("metric").get("peNormalizedAnnual").asText());
            }

            BigDecimal revenue = null;
            BigDecimal grossProfit = null;
            if (financialsNode.has("data") && financialsNode.get("data").isArray() && financialsNode.get("data").size() > 0) {
                JsonNode latestFinancials = financialsNode.get("data").get(0);
                if (latestFinancials.has("report") && latestFinancials.get("report").has("ic") && latestFinancials.get("report").get("ic").isArray()) {
                    for (JsonNode item : latestFinancials.get("report").get("ic")) {
                        if (item.has("concept") && item.get("concept").asText().equals("us-gaap_RevenueFromContractWithCustomerExcludingAssessedTax")) {
                            revenue = new BigDecimal(item.get("value").asText());
                        }
                        if (item.has("concept") && item.get("concept").asText().equals("us-gaap_GrossProfit")) {
                            grossProfit = new BigDecimal(item.get("value").asText());
                        }
                    }
                }
            }

            return new Stock(
                symbol,
                profileNode.has("name") ? profileNode.get("name").asText() : null,
                quoteNode.has("c") ? new BigDecimal(quoteNode.get("c").asText()) : null,
                peRatio,
                profileNode.has("marketCapitalization") ? new BigDecimal(profileNode.get("marketCapitalization").asText()) : null,
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
