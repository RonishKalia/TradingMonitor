package com.tradingmonitor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Main {
    private static final String FINNHUB_API_KEY = "d1rapopr01qk8n65sjs0d1rapopr01qk8n65sjsg";
    private static final String FMP_API_KEY = "CgBHy11GWoHAYxfp3t5zoj3jzJTkEF5r";
    private static final String ALPHA_VANTAGE_API_KEY = "T03NLMTO0T4J9PC1";
    private static final boolean IS_TESTING_MODE = true;
    private static final boolean IS_SINGLE_STOCK_TEST_MODE = false;
    private static final String SINGLE_STOCK_SYMBOL = "GOOG";

    public static void main(String[] args) {
        Main main = new Main();
        main.runStockAnalysis();
    }

    public void runStockAnalysis() {
        StockApiClient apiClient = new StockApiClient(FINNHUB_API_KEY, FMP_API_KEY, ALPHA_VANTAGE_API_KEY);
        StockAnalyzer analyzer = new StockAnalyzer(apiClient);
        
        System.out.println("\n=== TRADING MONITOR - STOCK ANALYSIS ===");
        
        try {
            if (IS_SINGLE_STOCK_TEST_MODE) {
                System.out.println("\n--- RUNNING IN SINGLE STOCK TESTING MODE ---");
                List<Stock> stockList = analyzer.analyzeSingleStock(SINGLE_STOCK_SYMBOL);
                printStockDetails(stockList, analyzer);
            } else {
                if (IS_TESTING_MODE) {
                    System.out.println("\n--- RUNNING IN TESTING MODE ---");
                }
                System.out.println("\nStarting analysis of US stocks...");
                List<Stock> stockList = analyzer.analyzeUsStocks(IS_TESTING_MODE);
                
                // Print individual stock details
                printStockDetails(stockList, analyzer);
            }
        } catch (IOException e) {
            System.err.println("An error occurred during stock analysis: " + e.getMessage());
        }
    }


    private void printStockDetails(List<Stock> stockList, StockAnalyzer analyzer) {
        System.out.println("\n=== DETAILED STOCK DATA ===");
        for (Stock stock : stockList) {
            stock.printAllData(analyzer);
        }
    }
}

