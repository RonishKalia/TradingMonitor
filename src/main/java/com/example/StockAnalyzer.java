package com.example;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;
import yahoofinance.quotes.stock.StockStats;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StockAnalyzer {

    private static final Map<String, List<String>> EXCHANGE_SYMBOLS = new HashMap<>();
    private static final int API_REQUEST_DELAY_MS = 100;

    static {
        EXCHANGE_SYMBOLS.put("NYSE", Arrays.asList(
            "JPM", "V", "JNJ", "WMT", "PG", "UNH", "HD", "BAC", "MA", "XOM",
            "DIS", "KO", "PFE", "CVX", "PEP", "T", "MRK", "ABT", "MCD", "CSCO",
            "DOW", "IBM", "NKE", "ORCL", "GE", "GS", "AXP", "BA", "CAT", "MMM",
            "TRV", "WBA", "INTC", "VZ", "MSFT"
        ));
        EXCHANGE_SYMBOLS.put("NASDAQ", Arrays.asList(
            "AAPL", "MSFT", "AMZN", "GOOGL", "GOOG", "META", "TSLA", "NVDA", "ASML", "AVGO",
            "PEP", "COST", "CMCSA", "ADBE", "INTC", "CSCO", "TMUS", "AMD", "TXN", "QCOM",
            "AMGN", "HON", "INTU", "AMAT", "ISRG", "SBUX", "MDLZ", "GILD", "BKNG", "ADP",
            "PYPL", "NFLX"
        ));
        EXCHANGE_SYMBOLS.put("SP500", Arrays.asList(
            "AAPL", "MSFT", "AMZN", "GOOGL", "GOOG", "META", "TSLA", "NVDA", "JPM", "V",
            "JNJ", "WMT", "PG", "UNH", "HD", "BAC", "MA", "XOM", "DIS", "KO",
            "PFE", "CVX", "PEP", "T", "MRK", "ABT", "MCD"
        ));
    }

    public List<StockData> analyzeExchange(String exchange) {
        List<String> symbols = EXCHANGE_SYMBOLS.get(exchange.toUpperCase());
        if (symbols == null) {
            throw new IllegalArgumentException("Exchange not supported: " + exchange +
                ". Supported exchanges: " + getSupportedExchanges());
        }

        System.out.println("Analyzing " + symbols.size() + " stocks from " + exchange + "...");

        return symbols.stream()
            .map(symbol -> {
                try {
                    // Respect API rate limits
                    Thread.sleep(API_REQUEST_DELAY_MS);
                    return analyzeStock(symbol, exchange);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error analyzing " + symbol + ": " + e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .peek(data -> System.out.println("✓ " + data.getSymbol() + " - P/E: " + data.getPeRatio() +
                ", Revenue: $" + formatBigNumber(data.getRevenue())))
            .collect(Collectors.toList());
    }

    private StockData analyzeStock(String symbol, String exchange) throws IOException {
        Stock stock = YahooFinance.get(symbol);
        if (stock == null || !stock.isValid()) {
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
            quote.getVolume() != null ? BigDecimal.valueOf(quote.getVolume()) : null,
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

        printMetricAnalysis("P/E Ratio", stockDataList, StockData::getPeRatio, true);
        printMetricAnalysis("Revenue", stockDataList, StockData::getRevenue, false);
        printMetricAnalysis("Gross Profit", stockDataList, StockData::getGrossProfit, false);

        printTop5ByMarketCap(stockDataList);
    }

    private void printMetricAnalysis(String metricName, List<StockData> stockData, Function<StockData, BigDecimal> metricExtractor, boolean isRatio) {
        List<StockData> validData = stockData.stream()
            .filter(s -> metricExtractor.apply(s) != null && metricExtractor.apply(s).compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());

        if (validData.isEmpty()) {
            System.out.println("\n" + metricName + " Analysis:");
            System.out.println("  Note: Data not available for this metric.");
            return;
        }

        BigDecimal total = validData.stream()
            .map(metricExtractor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        StockData min = validData.stream().min(Comparator.comparing(metricExtractor)).orElse(null);
        StockData max = validData.stream().max(Comparator.comparing(metricExtractor)).orElse(null);

        System.out.println("\n" + metricName + " Analysis:");
        if (isRatio) {
            BigDecimal average = total.divide(BigDecimal.valueOf(validData.size()), 2, RoundingMode.HALF_UP);
            System.out.println("  Average " + metricName + ": " + average);
            System.out.println("  Lowest " + metricName + ": " + min.getSymbol() + " (" + metricExtractor.apply(min) + ")");
            System.out.println("  Highest " + metricName + ": " + max.getSymbol() + " (" + metricExtractor.apply(max) + ")");
        } else {
            System.out.println("  Total " + metricName + " (all stocks): $" + formatBigNumber(total));
            System.out.println("  Highest " + metricName + ": " + max.getSymbol() + " ($" + formatBigNumber(metricExtractor.apply(max)) + ")");
        }
    }

    private void printTop5ByMarketCap(List<StockData> stockDataList) {
        List<StockData> validMarketCap = stockDataList.stream()
            .filter(s -> s.getMarketCap() != null && s.getMarketCap().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(StockData::getMarketCap).reversed())
            .limit(5)
            .collect(Collectors.toList());

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
            return number.divide(BigDecimal.valueOf(1_000_000_000), 2, RoundingMode.HALF_UP) + "B";
        } else if (number.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            return number.divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP) + "M";
        } else if (number.compareTo(BigDecimal.valueOf(1_000)) >= 0) {
            return number.divide(BigDecimal.valueOf(1_000), 2, RoundingMode.HALF_UP) + "K";
        } else {
            return number.toString();
        }
    }

    public Set<String> getSupportedExchanges() {
        return EXCHANGE_SYMBOLS.keySet();
    }

    public static class StockData {
        private final String symbol;
        private final String name;
        private final BigDecimal price;
        private final BigDecimal peRatio;
        private final BigDecimal marketCap;
        private final BigDecimal revenue;
        private final BigDecimal grossProfit;
        private final BigDecimal volume;
        private final String exchange;

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
}