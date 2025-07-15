package com.example;

import java.io.IOException;
import java.util.List;

public class Main {
    public static final String GREETING = "Hello, World!";

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
        StockAnalyzer analyzer = new StockAnalyzer();
        
        System.out.println("\n=== TRADING MONITOR - STOCK ANALYSIS ===");
        System.out.println("Supported exchanges: " + analyzer.getSupportedExchanges());
        
        // Analyze NASDAQ stocks (you can change this to NYSE, SP500, etc.)
        String exchangeToAnalyze = "NASDAQ";
        
        try {
            System.out.println("\nStarting analysis of " + exchangeToAnalyze + " stocks...");
            List<StockAnalyzer.StockData> stockDataList = analyzer.analyzeExchange(exchangeToAnalyze);
            
            // Print detailed analysis summary
            analyzer.printAnalysisSummary(stockDataList);
            
            // Print individual stock details
            System.out.println("\n=== DETAILED STOCK DATA ===");
            for (StockAnalyzer.StockData stock : stockDataList) {
                System.out.println(String.format("%-6s | %-20s | P/E: %-8s | Revenue: %-12s",
                    stock.getSymbol(),
                    stock.getName() != null ? stock.getName().substring(0, Math.min(20, stock.getName().length())) : "N/A",
                    stock.getPeRatio() != null ? stock.getPeRatio().toString() : "N/A",
                    stock.getRevenue() != null ? "$" + analyzer.formatBigNumber(stock.getRevenue()) : "N/A"
                ));
            }
            
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid exchange: " + e.getMessage());
        }
    }
}
