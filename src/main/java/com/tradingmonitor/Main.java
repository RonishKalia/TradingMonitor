package com.tradingmonitor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Main {
    public static final String GREETING = "Hello, World!";
    private static final String API_KEY = "d1rapopr01qk8n65sjs0d1rapopr01qk8n65sjsg";
    private static final boolean IS_TESTING_MODE = true;
    private static final boolean IS_SINGLE_STOCK_TEST_MODE = true;
    private static final String SINGLE_STOCK_SYMBOL = "GOOG";

    public static void main(String[] args) {
        Main main = new Main();
        System.out.println(main.getGreeting());
        
        // Demonstrate stock analysis
        main.runStockAnalysis();
    }

    public String getGreeting() {
        return GREETING;
    }
    
    public void runStockAnalysis() {
        StockApiClient apiClient = new StockApiClient(API_KEY);
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
            System.out.println(String.format("%-6s | %-20s | P/E: %-8s | Revenue: %-12s",
                stock.getSymbol(),
                stock.getName() != null ? stock.getName().substring(0, Math.min(20, stock.getName().length())) : "N/A",
                stock.getPeRatio() != null ? stock.getPeRatio().toString() : "N/A",
                stock.getRevenue() != null ? "$" + analyzer.formatBigNumber(stock.getRevenue()) : "N/A"
            ));

            if (stock.getHistoricalRevenue() != null && !stock.getHistoricalRevenue().isEmpty()) {
                System.out.println("  Historical Revenue:");
                stock.getHistoricalRevenue().entrySet().stream()
                    .sorted(Map.Entry.<Integer, BigDecimal>comparingByKey().reversed())
                    .forEach(entry -> {
                        System.out.println(String.format("    %d: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                    });
            }

            if (stock.getHistoricalNetIncome() != null && !stock.getHistoricalNetIncome().isEmpty()) {
                System.out.println("  Historical Net Income:");
                stock.getHistoricalNetIncome().entrySet().stream()
                    .sorted(Map.Entry.<Integer, BigDecimal>comparingByKey().reversed())
                    .forEach(entry -> {
                        System.out.println(String.format("    %d: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                    });
            }
        }
    }
}

