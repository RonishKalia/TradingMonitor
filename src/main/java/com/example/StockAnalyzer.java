package com.example;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;
import yahoofinance.quotes.stock.StockStats;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StockAnalyzer {
    
    private static final Map<String, List<String>> EXCHANGE_SYMBOLS = new HashMap<>();
    
    static {
        // Common stock symbols for major exchanges
        // NYSE (New York Stock Exchange) - reduced to 1 stock for testing
        EXCHANGE_SYMBOLS.put("NYSE", Arrays.asList(
            "AAPL"
        ));
        
        // NASDAQ - reduced to 1 stock for testing
        EXCHANGE_SYMBOLS.put("NASDAQ", Arrays.asList(
            "AAPL"
        ));
        
        // S&P 500 (subset) - reduced to 1 stock for testing
        EXCHANGE_SYMBOLS.put("SP500", Arrays.asList(
            "AAPL"
        ));
    }
    
    public static class StockData {
        private String symbol;
        private String name;
        private BigDecimal price;
        private BigDecimal peRatio;
        private BigDecimal marketCap;
        private BigDecimal revenue;
        private BigDecimal grossProfit;
        private BigDecimal volume;
        private String exchange;
        
        public StockData(String symbol, String name, BigDecimal price, BigDecimal peRatio,
                        BigDecimal marketCap, BigDecimal revenue, BigDecimal grossProfit,
                        BigDecimal volume, String exchange) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.peRatio = peRatio;
            this.marketCap = marketCap;
            this.revenue = revenue;
            this.grossProfit = grossProfit;
            this.volume = volume;
            this.exchange = exchange;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getPeRatio() { return peRatio; }
        public BigDecimal getMarketCap() { return marketCap; }
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getGrossProfit() { return grossProfit; }
        public BigDecimal getVolume() { return volume; }
        public String getExchange() { return exchange; }
        
        @Override
        public String toString() {
            return String.format("StockData{symbol='%s', name='%s', price=%s, peRatio=%s, " +
                               "marketCap=%s, revenue=%s, grossProfit=%s, volume=%s, exchange='%s'}",
                               symbol, name, price, peRatio, marketCap, revenue, grossProfit, volume, exchange);
        }
    }
    
    public List<StockData> analyzeExchange(String exchange) throws IOException {
        List<String> symbols = EXCHANGE_SYMBOLS.get(exchange.toUpperCase());
        if (symbols == null) {
            throw new IllegalArgumentException("Exchange not supported: " + exchange + 
                ". Supported exchanges: " + EXCHANGE_SYMBOLS.keySet());
        }
        
        List<StockData> stockDataList = new ArrayList<>();
        
        System.out.println("Analyzing " + symbols.size() + " stocks from " + exchange + "...");
        
        for (String symbol : symbols) {
            try {
                StockData data = analyzeStock(symbol, exchange);
                if (data != null) {
                    stockDataList.add(data);
                    System.out.println("✓ " + symbol + " - P/E: " + data.getPeRatio() + 
                                     ", Revenue: $" + formatBigNumber(data.getRevenue()));
                }
                
                // Rate limiting to avoid API restrictions
                Thread.sleep(5000);
                
            } catch (Exception e) {
                System.err.println("Error analyzing " + symbol + ": " + e.getMessage());
            }
        }
        
        return stockDataList;
    }
    
    private StockData analyzeStock(String symbol, String exchange) throws IOException {
        Stock stock = YahooFinance.get(symbol);
        if (stock == null) {
            return null;
        }
        
        StockQuote quote = stock.getQuote();
        StockStats stats = stock.getStats();
        
        if (quote == null || stats == null) {
            return null;
        }
        
        return new StockData(
            symbol,
            stock.getName(),
            quote.getPrice(),
            stats.getPe(),
            stats.getMarketCap(),
            stats.getRevenue(),
            null, // Gross profit not available in Yahoo Finance API
            quote.getVolume() != null ? java.math.BigDecimal.valueOf(quote.getVolume()) : null,
            exchange
        );
    }
    
    public void printAnalysisSummary(List<StockData> stockDataList) {
        if (stockDataList.isEmpty()) {
            System.out.println("No stock data available for analysis.");
            return;
        }
        
        System.out.println("\n=== STOCK ANALYSIS SUMMARY ===");
        System.out.println("Total stocks analyzed: " + stockDataList.size());
        
        // P/E Ratio Analysis
        List<StockData> validPE = stockDataList.stream()
            .filter(s -> s.getPeRatio() != null && s.getPeRatio().compareTo(BigDecimal.ZERO) > 0)
            .toList();
        
        if (!validPE.isEmpty()) {
            BigDecimal avgPE = validPE.stream()
                .map(StockData::getPeRatio)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf((long) validPE.size()), 2, BigDecimal.ROUND_HALF_UP);
            
            StockData lowestPE = validPE.stream()
                .min(Comparator.comparing(StockData::getPeRatio))
                .orElse(null);
            
            StockData highestPE = validPE.stream()
                .max(Comparator.comparing(StockData::getPeRatio))
                .orElse(null);
            
            System.out.println("\nP/E Ratio Analysis:");
            System.out.println("  Average P/E: " + avgPE);
            System.out.println("  Lowest P/E: " + lowestPE.getSymbol() + " (" + lowestPE.getPeRatio() + ")");
            System.out.println("  Highest P/E: " + highestPE.getSymbol() + " (" + highestPE.getPeRatio() + ")");
        }
        
        // Revenue Analysis
        List<StockData> validRevenue = stockDataList.stream()
            .filter(s -> s.getRevenue() != null && s.getRevenue().compareTo(BigDecimal.ZERO) > 0)
            .toList();
        
        if (!validRevenue.isEmpty()) {
            BigDecimal totalRevenue = validRevenue.stream()
                .map(StockData::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            StockData highestRevenue = validRevenue.stream()
                .max(Comparator.comparing(StockData::getRevenue))
                .orElse(null);
            
            System.out.println("\nRevenue Analysis:");
            System.out.println("  Total Revenue (all stocks): $" + formatBigNumber(totalRevenue));
            System.out.println("  Highest Revenue: " + highestRevenue.getSymbol() + 
                             " ($" + formatBigNumber(highestRevenue.getRevenue()) + ")");
        }
        
        // Gross Profit Analysis
        List<StockData> validGrossProfit = stockDataList.stream()
            .filter(s -> s.getGrossProfit() != null && s.getGrossProfit().compareTo(BigDecimal.ZERO) > 0)
            .toList();
        
        if (!validGrossProfit.isEmpty()) {
            BigDecimal totalGrossProfit = validGrossProfit.stream()
                .map(StockData::getGrossProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            StockData highestGrossProfit = validGrossProfit.stream()
                .max(Comparator.comparing(StockData::getGrossProfit))
                .orElse(null);
            
            System.out.println("\nGross Profit Analysis:");
            System.out.println("  Total Gross Profit (all stocks): $" + formatBigNumber(totalGrossProfit));
            System.out.println("  Highest Gross Profit: " + highestGrossProfit.getSymbol() + 
                             " ($" + formatBigNumber(highestGrossProfit.getGrossProfit()) + ")");
        } else {
            System.out.println("\nGross Profit Analysis:");
            System.out.println("  Note: Gross profit data not available from Yahoo Finance API");
        }
        
        // Top 5 stocks by market cap
        List<StockData> validMarketCap = stockDataList.stream()
            .filter(s -> s.getMarketCap() != null && s.getMarketCap().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(StockData::getMarketCap).reversed())
            .limit(5)
            .toList();
        
        if (!validMarketCap.isEmpty()) {
            System.out.println("\nTop 5 Stocks by Market Cap:");
            for (int i = 0; i < validMarketCap.size(); i++) {
                StockData stock = validMarketCap.get(i);
                System.out.println("  " + (i + 1) + ". " + stock.getSymbol() + " - $" +
                                 formatBigNumber(stock.getMarketCap()));
            }
        }
    }
    
    public String formatBigNumber(BigDecimal number) {
        if (number == null) return "N/A";
        
        if (number.compareTo(BigDecimal.valueOf(1_000_000_000)) >= 0) {
            return number.divide(BigDecimal.valueOf(1_000_000_000), 2, BigDecimal.ROUND_HALF_UP) + "B";
        } else if (number.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            return number.divide(BigDecimal.valueOf(1_000_000), 2, BigDecimal.ROUND_HALF_UP) + "M";
        } else if (number.compareTo(BigDecimal.valueOf(1_000)) >= 0) {
            return number.divide(BigDecimal.valueOf(1_000), 2, BigDecimal.ROUND_HALF_UP) + "K";
        } else {
            return number.toString();
        }
    }
    
    public Set<String> getSupportedExchanges() {
        return EXCHANGE_SYMBOLS.keySet();
    }
} 