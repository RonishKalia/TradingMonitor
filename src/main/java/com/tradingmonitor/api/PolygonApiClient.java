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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PolygonApiClient implements ApiProvider {

    private static final String API_URL = "https://api.polygon.io/vX/reference/financials?ticker=%s&period_of_report=quarterly&limit=100&apiKey=%s";
    private final String apiKey;

    public PolygonApiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Stock fetchStockData(String symbol, String region) throws IOException {
        String urlString = String.format(API_URL, symbol, apiKey);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to fetch data from Polygon.io: " + conn.getResponseMessage());
        }

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);
        JsonArray results = jsonResponse.getAsJsonArray("results");

        Map<String, BigDecimal> quarterlyRevenue = new TreeMap<>();
        Map<String, BigDecimal> quarterlyNetIncome = new TreeMap<>();
        Map<String, BigDecimal> quarterlyGrossProfit = new TreeMap<>();

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

        Map<Integer, BigDecimal> annualRevenue = deriveAnnualData(quarterlyRevenue);
        Map<Integer, BigDecimal> annualNetIncome = deriveAnnualData(quarterlyNetIncome);
        Map<Integer, BigDecimal> annualGrossProfit = deriveAnnualData(quarterlyGrossProfit);

        return new Stock(symbol, null, null, null, null, null, null,
            annualRevenue, annualNetIncome, annualGrossProfit,
            quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit);
    }

    private Map<Integer, BigDecimal> deriveAnnualData(Map<String, BigDecimal> quarterlyData) {
        Map<Integer, BigDecimal> annualData = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : quarterlyData.entrySet()) {
            int year = Integer.parseInt(entry.getKey().substring(0, 4));
            annualData.put(year, annualData.getOrDefault(year, BigDecimal.ZERO).add(entry.getValue()));
        }
        return annualData;
    }
}