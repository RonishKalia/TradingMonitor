package com.tradingmonitor.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tradingmonitor.Stock;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolygonApiClient implements ApiProvider {

    private static final Logger logger = LoggerFactory.getLogger(PolygonApiClient.class);
    private static final String FINANCIALS_API_URL = "https://api.polygon.io/vX/reference/financials?ticker=%s&limit=100&apiKey=%s";
    private static final String TICKERS_API_URL = "https://api.polygon.io/v3/reference/tickers?exchange=%s&market=stocks&active=true&apiKey=%s";
    private static final String TICKER_DETAILS_URL = "https://api.polygon.io/v3/reference/tickers/%s?apiKey=%s";
    private static final String PREVIOUS_DAY_CLOSE_URL = "https://api.polygon.io/v2/aggs/ticker/%s/prev?apiKey=%s";
    private static final int RATE_LIMIT_WAIT_MS = 60000;
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long TIME_WINDOW_MS = 60000;

    private final String apiKey;
    private final BlockingQueue<Long> requestTimestamps;

    public PolygonApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.requestTimestamps = new ArrayBlockingQueue<>(MAX_REQUESTS_PER_MINUTE);
    }

    private void rateLimit() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        if (requestTimestamps.size() == MAX_REQUESTS_PER_MINUTE) {
            long oldestTimestamp = requestTimestamps.peek();
            long elapsedTime = currentTime - oldestTimestamp;
            if (elapsedTime < TIME_WINDOW_MS) {
                long waitTime = TIME_WINDOW_MS - elapsedTime;
                logger.warn("Rate limit reached for Polygon. Waiting for {} ms.", waitTime);
                Thread.sleep(waitTime);
            }
            requestTimestamps.poll();
        }
        requestTimestamps.offer(currentTime);
    }

    public List<String> fetchStockSymbols(String exchange, Integer limit) throws IOException {
        List<String> symbols = new ArrayList<>();
        String baseUrl = String.format(TICKERS_API_URL, exchange, apiKey);
        if (limit != null) {
            baseUrl += "&limit=" + limit;
        } else {
            baseUrl += "&limit=1000"; // Default limit if not specified
        }
        String nextUrl = baseUrl;

        while (nextUrl != null) {
            try {
                rateLimit();
                URL url = new URL(nextUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() != 200) {
                    if (conn.getResponseCode() == 429) { // Too Many Requests
                        logger.warn("Rate limit hit for Polygon.io tickers. Retrying in 60 seconds...");
                        Thread.sleep(RATE_LIMIT_WAIT_MS);
                        continue; // Retry the same URL
                    }
                    throw new IOException("Failed to fetch tickers from Polygon.io: " + conn.getResponseCode() + " " + conn.getResponseMessage());
                }

                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);
                JsonArray results = jsonResponse.getAsJsonArray("results");

                for (JsonElement resultElement : results) {
                    JsonObject result = resultElement.getAsJsonObject();
                    symbols.add(result.get("ticker").getAsString());
                }

                if (jsonResponse.has("next_url")) {
                    nextUrl = jsonResponse.get("next_url").getAsString() + "&apiKey=" + apiKey;
                } else {
                    nextUrl = null;
                }
                if (limit != null && symbols.size() >= limit) {
                    return symbols.subList(0, limit);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while fetching tickers from Polygon.io", e);
            }
        }
        return symbols;
    }

    public List<String> fetchStockSymbols(String exchange) throws IOException {
        return fetchStockSymbols(exchange, null);
    }

    @Override
    public Stock fetchStockData(String symbol, String region) throws IOException {
        try {
            rateLimit();
            String urlString = String.format(FINANCIALS_API_URL, symbol, apiKey);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                if (conn.getResponseCode() == 429) { // Too Many Requests
                    logger.warn("Rate limit hit for Polygon.io. Retrying in 60 seconds...");
                    Thread.sleep(RATE_LIMIT_WAIT_MS);
                    return fetchStockData(symbol, region); // Retry
                }
                throw new IOException("Failed to fetch data from Polygon.io: " + conn.getResponseMessage());
            }

            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);
            JsonArray results = jsonResponse.getAsJsonArray("results");

            Map<String, BigDecimal> quarterlyRevenue = new TreeMap<>(Collections.reverseOrder());
            Map<String, BigDecimal> quarterlyNetIncome = new TreeMap<>(Collections.reverseOrder());
            Map<String, BigDecimal> quarterlyGrossProfit = new TreeMap<>(Collections.reverseOrder());
            Map<String, BigDecimal> quarterlyEps = new TreeMap<>(Collections.reverseOrder());
            Map<String, BigDecimal> quarterlyDilutedEps = new TreeMap<>(Collections.reverseOrder());
            Map<String, BigDecimal> weightedAverageSharesOutstanding = new TreeMap<>(Collections.reverseOrder());

            Map<Integer, BigDecimal> annualRevenue = new TreeMap<>(Collections.reverseOrder());
            Map<Integer, BigDecimal> annualNetIncome = new TreeMap<>(Collections.reverseOrder());
            Map<Integer, BigDecimal> annualGrossProfit = new TreeMap<>(Collections.reverseOrder());

            for (JsonElement resultElement : results) {
                JsonObject result = resultElement.getAsJsonObject();
                String timeframe = result.get("timeframe").getAsString();
                String endDate = result.get("end_date").getAsString();
                JsonObject financials = result.getAsJsonObject("financials");
                JsonObject incomeStatement = financials.getAsJsonObject("income_statement");

                if (incomeStatement != null) {
                    if ("quarterly".equals(timeframe)) {
                        String fiscalPeriod = result.get("fiscal_period").getAsString();
                        String fiscalYear = result.get("fiscal_year").getAsString();
                        String key = fiscalYear + ":" + fiscalPeriod + ":" + endDate;
                        if (incomeStatement.has("revenues")) {
                            quarterlyRevenue.put(key, incomeStatement.get("revenues").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("net_income_loss")) {
                            quarterlyNetIncome.put(key, incomeStatement.get("net_income_loss").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("gross_profit")) {
                            quarterlyGrossProfit.put(key, incomeStatement.get("gross_profit").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("basic_earnings_per_share")) {
                            quarterlyEps.put(key, incomeStatement.get("basic_earnings_per_share").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("diluted_earnings_per_share")) {
                            quarterlyDilutedEps.put(key, incomeStatement.get("diluted_earnings_per_share").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                    } else if ("annual".equals(timeframe)) {
                        int year = Integer.parseInt(endDate.substring(0, 4));
                        if (incomeStatement.has("revenues")) {
                            annualRevenue.put(year, incomeStatement.get("revenues").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("net_income_loss")) {
                            annualNetIncome.put(year, incomeStatement.get("net_income_loss").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("gross_profit")) {
                            annualGrossProfit.put(year, incomeStatement.get("gross_profit").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                    }
                }
                if (financials.has("shares")) {
                    weightedAverageSharesOutstanding.put(endDate, financials.get("shares").getAsJsonObject().get("value").getAsBigDecimal());
                }
            }

            int currentYear = LocalDate.now().getYear();
            Map<Integer, BigDecimal> filteredAnnualRevenue = filterLastNYears(annualRevenue, 4, currentYear);
            Map<Integer, BigDecimal> filteredAnnualNetIncome = filterLastNYears(annualNetIncome, 4, currentYear);
            Map<Integer, BigDecimal> filteredAnnualGrossProfit = filterLastNYears(annualGrossProfit, 4, currentYear);

            // Fetch ticker details
            rateLimit();
            urlString = String.format(TICKER_DETAILS_URL, symbol, apiKey);
            url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            String name = null;
            BigDecimal marketCap = null;
            if (conn.getResponseCode() == 200) {
                jsonResponse = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);
                if (jsonResponse.has("results")) {
                    JsonObject tickerDetails = jsonResponse.getAsJsonObject("results");
                    name = tickerDetails.get("name").getAsString();
                    marketCap = tickerDetails.get("market_cap").getAsBigDecimal();
                }
            }

            // Fetch previous day's close
            rateLimit();
            urlString = String.format(PREVIOUS_DAY_CLOSE_URL, symbol, apiKey);
            url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BigDecimal price = null;
            if (conn.getResponseCode() == 200) {
                jsonResponse = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);
                if (jsonResponse.has("results") && jsonResponse.get("results").getAsJsonArray().size() > 0) {
                    price = jsonResponse.get("results").getAsJsonArray().get(0).getAsJsonObject().get("c").getAsBigDecimal();
                }
            }

            BigDecimal ttmNetIncome = quarterlyNetIncome.values().stream()
                .limit(4)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal latestSharesOutstanding = weightedAverageSharesOutstanding.values().stream()
                .findFirst()
                .orElse(BigDecimal.ZERO);

            BigDecimal ttmEps = BigDecimal.ZERO;
            if (latestSharesOutstanding.compareTo(BigDecimal.ZERO) != 0) {
                ttmEps = ttmNetIncome.divide(latestSharesOutstanding, 4, java.math.RoundingMode.HALF_UP);
            }

            BigDecimal peRatio = null;
            if (price != null && ttmEps.compareTo(BigDecimal.ZERO) > 0) {
                peRatio = price.divide(ttmEps, 2, java.math.RoundingMode.HALF_UP);
            }

            return new Stock(symbol, name, price, peRatio, marketCap, null, null,
                filteredAnnualRevenue, filteredAnnualNetIncome, filteredAnnualGrossProfit,
                quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit, quarterlyEps, quarterlyDilutedEps, weightedAverageSharesOutstanding);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during fetch", e);
        }
    }

    

    private Map<Integer, BigDecimal> filterLastNYears(Map<Integer, BigDecimal> annualData, int years, int currentYear) {
        return annualData.entrySet().stream()
                .filter(entry -> entry.getKey() >= currentYear - years)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    
}
