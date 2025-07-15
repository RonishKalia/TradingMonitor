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
    private static final String QUOTE_URL_FORMAT = BASE_URL + "/quote?symbol=%s&token=%s";
    private static final String PROFILE_URL_FORMAT = BASE_URL + "/stock/profile2?symbol=%s&token=%s";
    private static final String FMP_SYMBOL_LIST_URL_FORMAT = "https://financialmodelingprep.com/api/v3/stock/list?apikey=%s";
    private static final String FMP_KEY_METRICS_URL_FORMAT = "https://financialmodelingprep.com/api/v3/key-metrics-ttm/%s?apikey=%s";
    private static final String FMP_FINANCIALS_URL_FORMAT = "https://financialmodelingprep.com/api/v3/income-statement/%s?limit=5&apikey=%s";
    private static final String ALPHA_VANTAGE_URL_FORMAT = "https://www.alphavantage.co/query?function=INCOME_STATEMENT&symbol=%s&apikey=%s";
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long TIME_WINDOW_MS = 65 * 1000; // 65 seconds

    private final String finnhubApiKey;
    private final String fmpApiKey;
    private final String alphaVantageApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<Long> requestTimestamps;

    public StockApiClient(String finnhubApiKey, String fmpApiKey, String alphaVantageApiKey) {
        this.finnhubApiKey = finnhubApiKey;
        this.fmpApiKey = fmpApiKey;
        this.alphaVantageApiKey = alphaVantageApiKey;
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
            String url = String.format(FMP_SYMBOL_LIST_URL_FORMAT, fmpApiKey);
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
        BigDecimal volume = null;
        Map<Integer, BigDecimal> historicalRevenue = new HashMap<>();
        Map<Integer, BigDecimal> historicalNetIncome = new HashMap<>();
        Map<Integer, BigDecimal> historicalGrossProfit = new HashMap<>();
        Map<String, BigDecimal> quarterlyRevenue = new HashMap<>();
        Map<String, BigDecimal> quarterlyNetIncome = new HashMap<>();
        Map<String, BigDecimal> quarterlyGrossProfit = new HashMap<>();

        try {
            // Fetch quote data
            String quoteUrl = String.format(QUOTE_URL_FORMAT, symbol, finnhubApiKey);
            HttpResponse<String> quoteResponse = sendRequest(quoteUrl);
            if (quoteResponse.statusCode() == 200) {
                JsonNode quoteNode = objectMapper.readTree(quoteResponse.body());
                price = toBigDecimal(quoteNode.get("c"));
            } else {
                logger.warn("Failed to fetch quote data for {}: Status {}", symbol, quoteResponse.statusCode());
            }

            // Fetch profile data
            String profileUrl = String.format(PROFILE_URL_FORMAT, symbol, finnhubApiKey);
            HttpResponse<String> profileResponse = sendRequest(profileUrl);
            if (profileResponse.statusCode() == 200) {
                JsonNode profileNode = objectMapper.readTree(profileResponse.body());
                name = profileNode.has("name") ? profileNode.get("name").asText() : null;
                marketCap = toBigDecimal(profileNode.get("marketCapitalization"));
            } else {
                logger.warn("Failed to fetch profile data for {}: Status {}", symbol, profileResponse.statusCode());
            }

            // Fetch metric data
            String metricUrl = String.format(FMP_KEY_METRICS_URL_FORMAT, symbol, fmpApiKey);
            HttpResponse<String> metricResponse = sendRequest(metricUrl);
            if (metricResponse.statusCode() == 200) {
                JsonNode metricNode = objectMapper.readTree(metricResponse.body());
                if (metricNode.isArray() && metricNode.size() > 0) {
                    peRatio = toBigDecimal(metricNode.get(0).get("peRatioTTM"));
                }
            } else {
                logger.warn("Failed to fetch metric data for {}: Status {}", symbol, metricResponse.statusCode());
            }

            // Fetch financials data
            String financialsUrl = String.format(FMP_FINANCIALS_URL_FORMAT, symbol, fmpApiKey);
            HttpResponse<String> financialsResponse = sendRequest(financialsUrl);
            if (financialsResponse.statusCode() == 200) {
                JsonNode financialsNode = objectMapper.readTree(financialsResponse.body());
                if (financialsNode.isArray()) {
                    for (JsonNode annualReport : financialsNode) {
                        int year = annualReport.get("calendarYear").asInt();
                        historicalRevenue.put(year, toBigDecimal(annualReport.get("revenue")));
                        historicalNetIncome.put(year, toBigDecimal(annualReport.get("netIncome")));
                        historicalGrossProfit.put(year, toBigDecimal(annualReport.get("grossProfit")));
                    }
                }
            } else {
                logger.warn("Failed to fetch financials data for {}: Status {}", symbol, financialsResponse.statusCode());
            }

            // Fetch quarterly financials data
            String quarterlyFinancialsUrl = String.format(ALPHA_VANTAGE_URL_FORMAT, symbol, alphaVantageApiKey);
            HttpResponse<String> quarterlyFinancialsResponse = sendRequest(quarterlyFinancialsUrl);
            if (quarterlyFinancialsResponse.statusCode() == 200) {
                JsonNode quarterlyFinancialsNode = objectMapper.readTree(quarterlyFinancialsResponse.body());
                if (quarterlyFinancialsNode.has("quarterlyReports") && quarterlyFinancialsNode.get("quarterlyReports").isArray()) {
                    int quartersProcessed = 0;
                    for (JsonNode quarterlyReport : quarterlyFinancialsNode.get("quarterlyReports")) {
                        if (quartersProcessed >= 8) break;
                        String quarter = quarterlyReport.get("fiscalDateEnding").asText();
                        quarterlyRevenue.put(quarter, toBigDecimal(quarterlyReport.get("totalRevenue")));
                        quarterlyNetIncome.put(quarter, toBigDecimal(quarterlyReport.get("netIncome")));
                        quarterlyGrossProfit.put(quarter, toBigDecimal(quarterlyReport.get("grossProfit")));
                        quartersProcessed++;
                    }
                }
            } else {
                logger.warn("Failed to fetch quarterly financials data for {}: Status {}", symbol, quarterlyFinancialsResponse.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching data for {}", symbol, e);
            throw new IOException("Interrupted while fetching data for " + symbol, e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching data for {}", symbol, e);
        }

        Map<Integer, BigDecimal> historicalRevenueChange = calculateHistoricalPercentageChange(historicalRevenue);
        Map<Integer, BigDecimal> historicalNetIncomeChange = calculateHistoricalPercentageChange(historicalNetIncome);
        Map<Integer, BigDecimal> historicalGrossProfitChange = calculateHistoricalPercentageChange(historicalGrossProfit);
        Map<String, BigDecimal> quarterlyRevenueChange = calculateQuarterlyPercentageChange(quarterlyRevenue);
        Map<String, BigDecimal> quarterlyNetIncomeChange = calculateQuarterlyPercentageChange(quarterlyNetIncome);
        Map<String, BigDecimal> quarterlyGrossProfitChange = calculateQuarterlyPercentageChange(quarterlyGrossProfit);

        return new Stock(symbol, name, price, peRatio, marketCap, volume, exchange, historicalRevenue, historicalNetIncome, historicalGrossProfit, quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit, historicalRevenueChange, historicalNetIncomeChange, historicalGrossProfitChange, quarterlyRevenueChange, quarterlyNetIncomeChange, quarterlyGrossProfitChange);
    }

    private Map<Integer, BigDecimal> calculateHistoricalPercentageChange(Map<Integer, BigDecimal> historicalData) {
        Map<Integer, BigDecimal> percentageChanges = new HashMap<>();
        List<Integer> sortedYears = new ArrayList<>(historicalData.keySet());
        sortedYears.sort(Integer::compareTo);

        for (int i = 1; i < sortedYears.size(); i++) {
            Integer currentYear = sortedYears.get(i);
            Integer previousYear = sortedYears.get(i - 1);
            BigDecimal currentValue = historicalData.get(currentYear);
            BigDecimal previousValue = historicalData.get(previousYear);

            if (currentValue != null && previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal change = currentValue.subtract(previousValue);
                BigDecimal percentageChange = change.divide(previousValue, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
                percentageChanges.put(currentYear, percentageChange);
            }
        }
        return percentageChanges;
    }

    private Map<String, BigDecimal> calculateQuarterlyPercentageChange(Map<String, BigDecimal> quarterlyData) {
        Map<String, BigDecimal> percentageChanges = new HashMap<>();
        List<String> sortedQuarters = new ArrayList<>(quarterlyData.keySet());
        sortedQuarters.sort(String::compareTo);

        for (int i = 1; i < sortedQuarters.size(); i++) {
            String currentQuarter = sortedQuarters.get(i);
            String previousQuarter = sortedQuarters.get(i - 1);
            BigDecimal currentValue = quarterlyData.get(currentQuarter);
            BigDecimal previousValue = quarterlyData.get(previousQuarter);

            if (currentValue != null && previousValue != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal change = currentValue.subtract(previousValue);
                BigDecimal percentageChange = change.divide(previousValue, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
                percentageChanges.put(currentQuarter, percentageChange);
            }
        }
        return percentageChanges;
    }
}