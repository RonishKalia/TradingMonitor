package com.example;

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
        String apiKey = System.getenv("FINNHUB_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Error: FINNHUB_API_KEY environment variable not set.");
            System.err.println("Please set the environment variable to your Finnhub API key.");
            return;
        }

        StockApiClient apiClient = new StockApiClient(apiKey);
        StockAnalyzer analyzer = new StockAnalyzer(apiClient);
        
        System.out.println("\n=== TRADING MONITOR - STOCK ANALYSIS ===");
        System.out.println("Supported exchanges: " + analyzer.getSupportedExchanges());
        
        // Analyze NASDAQ stocks (you can change this to NYSE, SP500, etc.)
        String exchangeToAnalyze = "NASDAQ";
        
        try {
            System.out.println("\nStarting analysis of " + exchangeToAnalyze + " stocks...");
            List<Stock> stockList = analyzer.analyzeExchange(exchangeToAnalyze);
            
            // Print detailed analysis summary
            analyzer.printAnalysisSummary(stockList);
            
            // Print individual stock details
            System.out.println("\n=== DETAILED STOCK DATA ===");
            for (Stock stock : stockList) {
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
