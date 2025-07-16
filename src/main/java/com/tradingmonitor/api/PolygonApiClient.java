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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PolygonApiClient implements ApiProvider {

    private static final String API_URL = "https://api.polygon.io/vX/reference/financials?ticker=%s&period_of_report=quarterly&limit=100&apiKey=%s";
    private final String apiKey;

    public PolygonApiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Stock fetchStockData(String symbol, String region) throws IOException {
        int maxRetries = 3;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                String urlString = String.format(API_URL, symbol, apiKey);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() != 200) {
                    if (conn.getResponseCode() == 429) { // Too Many Requests
                        System.out.println("Rate limit hit for Polygon.io. Retrying in 60 seconds...");
                        Thread.sleep(60000);
                        retryCount++;
                        continue;
                    }
                    throw new IOException("Failed to fetch data from Polygon.io: " + conn.getResponseMessage());
                }

                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);
                JsonArray results = jsonResponse.getAsJsonArray("results");

                Map<String, BigDecimal> quarterlyRevenue = new TreeMap<>(Collections.reverseOrder());
                Map<String, BigDecimal> quarterlyNetIncome = new TreeMap<>(Collections.reverseOrder());
                Map<String, BigDecimal> quarterlyGrossProfit = new TreeMap<>(Collections.reverseOrder());

                for (JsonElement resultElement : results) {
                    JsonObject result = resultElement.getAsJsonObject();
                    String endDate = result.get("end_date").getAsString();
                    JsonObject financials = result.getAsJsonObject("financials");
                    JsonObject incomeStatement = financials.getAsJsonObject("income_statement");

                    if (incomeStatement != null) {
                        if (incomeStatement.has("revenues")) {
                            quarterlyRevenue.put(endDate, incomeStatement.get("revenues").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("net_income_loss")) {
                            quarterlyNetIncome.put(endDate, incomeStatement.get("net_income_loss").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                        if (incomeStatement.has("gross_profit")) {
                            quarterlyGrossProfit.put(endDate, incomeStatement.get("gross_profit").getAsJsonObject().get("value").getAsBigDecimal());
                        }
                    }
                }

                // Filter quarterly data to the last 2 years (8 quarters)
                Map<String, BigDecimal> filteredQuarterlyRevenue = filterLastNQuarters(quarterlyRevenue, 8);
                Map<String, BigDecimal> filteredQuarterlyNetIncome = filterLastNQuarters(quarterlyNetIncome, 8);
                Map<String, BigDecimal> filteredQuarterlyGrossProfit = filterLastNQuarters(quarterlyGrossProfit, 8);

                Map<Integer, BigDecimal> annualRevenue = deriveAnnualData(quarterlyRevenue); // derive from all quarterly data
                Map<Integer, BigDecimal> annualNetIncome = deriveAnnualData(quarterlyNetIncome);
                Map<Integer, BigDecimal> annualGrossProfit = deriveAnnualData(quarterlyGrossProfit);

                // Filter annual data to the last 4 years
                int currentYear = LocalDate.now().getYear();
                Map<Integer, BigDecimal> filteredAnnualRevenue = filterLastNYears(annualRevenue, 4, currentYear);
                Map<Integer, BigDecimal> filteredAnnualNetIncome = filterLastNYears(annualNetIncome, 4, currentYear);
                Map<Integer, BigDecimal> filteredAnnualGrossProfit = filterLastNYears(annualGrossProfit, 4, currentYear);

                return new Stock(symbol, null, null, null, null, null, null,
                    filteredAnnualRevenue, filteredAnnualNetIncome, filteredAnnualGrossProfit,
                    filteredQuarterlyRevenue, filteredQuarterlyNetIncome, filteredQuarterlyGrossProfit);
            } catch (IOException e) {
                if (e.getMessage().contains("Too Many Requests")) {
                    System.out.println("Rate limit hit for Polygon.io. Retrying in 60 seconds...");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during rate limit wait", ie);
                    }
                    retryCount++;
                } else {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during fetch", e);
            }
        }
        throw new IOException("Failed to fetch data from Polygon.io after " + maxRetries + " retries.");
    }

    private Map<String, BigDecimal> filterLastNQuarters(Map<String, BigDecimal> quarterlyData, int quarters) {
        return quarterlyData.entrySet().stream()
                .limit(quarters)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<Integer, BigDecimal> filterLastNYears(Map<Integer, BigDecimal> annualData, int years, int currentYear) {
        return annualData.entrySet().stream()
                .filter(entry -> entry.getKey() >= currentYear - years)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Integer, BigDecimal> deriveAnnualData(Map<String, BigDecimal> quarterlyData) {
        Map<Integer, BigDecimal> annualData = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, BigDecimal> entry : quarterlyData.entrySet()) {
            int year = Integer.parseInt(entry.getKey().substring(0, 4));
            annualData.put(year, annualData.getOrDefault(year, BigDecimal.ZERO).add(entry.getValue()));
        }
        return annualData;
    }
}
