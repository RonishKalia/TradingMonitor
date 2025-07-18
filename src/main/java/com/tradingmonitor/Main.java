package com.tradingmonitor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final String FINNHUB_API_KEY = "d1rapopr01qk8n65sjs0d1rapopr01qk8n65sjsg";
    private static final String FMP_API_KEY = "CgBHy11GWoHAYxfp3t5zoj3jzJTkEF5r";
    private static final String ALPHA_VANTAGE_API_KEY = "T03NLMTO0T4J9PC1";
    private static final String POLYGON_API_KEY = "7CrGcKbTwnPRRsksHHX2cLEfDun9BSt4";
    private static final boolean IS_TESTING_MODE = true;
    private static final boolean IS_SINGLE_STOCK_TEST_MODE = false;
    private static final String SINGLE_STOCK_SYMBOL = "AAPL";
    private static final String OUTPUT_FILE_PATH = "stock_analysis_output.txt";

    public static void main(String[] args) {
        Main main = new Main();
        main.runStockAnalysis();
    }

    public void runStockAnalysis() {
        StockApiClient apiClient = new StockApiClient(FINNHUB_API_KEY, FMP_API_KEY, ALPHA_VANTAGE_API_KEY, POLYGON_API_KEY);
        StockAnalyzer analyzer = new StockAnalyzer(apiClient);
        StockDashboard dashboard = new StockDashboard();

        System.out.println("\n=== TRADING MONITOR - STOCK ANALYSIS ===");

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH)))) {
            List<Stock> stockList;
            if (IS_SINGLE_STOCK_TEST_MODE) {
                System.out.println("\n--- RUNNING IN SINGLE STOCK TESTING MODE ---");
                stockList = analyzer.analyzeSingleStock(SINGLE_STOCK_SYMBOL);
                printStockDetails(stockList, analyzer, writer);
            } else {
                if (IS_TESTING_MODE) {
                    System.out.println("\n--- RUNNING IN TESTING MODE ---");
                    List<String> testStocks = Arrays.asList("NVDA", "MSFT", "AAPL", "AMZN", "GOOG");
                    stockList = new ArrayList<>();
                    for (String symbol : testStocks) {
                        stockList.addAll(analyzer.analyzeSingleStock(symbol));
                    }
                } else {
                    System.out.println("\nStarting analysis of US stocks...");
                    stockList = analyzer.analyzeUsStocks(IS_TESTING_MODE);
                }
                printStockDetails(stockList, analyzer, writer);
            }
            dashboard.generateDashboard(stockList, analyzer);
        } catch (IOException e) {
            System.err.println("An error occurred during stock analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printStockDetails(List<Stock> stockList, StockAnalyzer analyzer, PrintWriter writer) {
        System.out.println("\n=== DETAILED STOCK DATA ===");
        for (Stock stock : stockList) {
            stock.printAllData(analyzer, writer);
        }
    }
}
