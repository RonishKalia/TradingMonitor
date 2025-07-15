package com.example;

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
    
    private final StockApiClient stockApiClient;

    static {
        EXCHANGE_SYMBOLS.put("NYSE", Arrays.asList(
            "JPM", "V", "JNJ", "WMT", "PG", "UNH", "HD", "BAC", "MA", "XOM",
            "DIS", "KO", "PFE", "CVX", "PEP", "T", "MRK", "ABT", "MCD", "CSCO",
            "DOW", "IBM", "NKE", "ORCL", "GE", "GS", "AXP", "BA", "CAT", "MMM",
            "TRV", "WBA", "INTC", "VZ", "MSFT"
        ));
        EXCHANGE_SYMBOLS.put("NASDAQ", Arrays.asList(
            "AAPL", "MSFT", "AMZN", "GOOGL", "GOOG", "META", "TSLA", "NVDA"
        ));
        EXCHANGE_SYMBOLS.put("SP500", Arrays.asList(
            "AAPL", "MSFT", "AMZN", "GOOGL", "GOOG", "META", "TSLA", "NVDA", "JPM", "V",
            "JNJ", "WMT", "PG", "UNH", "HD", "BAC", "MA", "XOM", "DIS", "KO",
            "PFE", "CVX", "PEP", "T", "MRK", "ABT", "MCD"
        ));
    }

    public StockAnalyzer(StockApiClient stockApiClient) {
        this.stockApiClient = stockApiClient;
    }

    public List<Stock> analyzeExchange(String exchange) {
        List<String> symbols = EXCHANGE_SYMBOLS.get(exchange.toUpperCase());
        if (symbols == null) {
            throw new IllegalArgumentException("Exchange not supported: " + exchange +
                ". Supported exchanges: " + getSupportedExchanges());
        }

        System.out.println("Analyzing " + symbols.size() + " stocks from " + exchange + "...");

        return symbols.stream()
            .map(symbol -> {
                try {
                    return stockApiClient.fetchStockData(symbol, exchange);
                } catch (IOException e) {
                    System.err.println("Error analyzing " + symbol + ": " + e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .peek(data -> System.out.println("✓ " + data.getSymbol() + " - P/E: " + data.getPeRatio() +
                ", Revenue: $" + formatBigNumber(data.getRevenue())))
            .collect(Collectors.toList());
    }

    public void printAnalysisSummary(List<Stock> stockDataList) {
        if (stockDataList.isEmpty()) {
            System.out.println("No stock data available for analysis.");
            return;
        }

        System.out.println("\n=== STOCK ANALYSIS SUMMARY ===");
        System.out.println("Total stocks analyzed: " + stockDataList.size());

        printMetricAnalysis("P/E Ratio", stockDataList, Stock::getPeRatio, true);
        printMetricAnalysis("Revenue", stockDataList, Stock::getRevenue, false);
        printMetricAnalysis("Gross Profit", stockDataList, Stock::getGrossProfit, false);

        printTop5ByMarketCap(stockDataList);
    }

    private void printMetricAnalysis(String metricName, List<Stock> stockData, Function<Stock, BigDecimal> metricExtractor, boolean isRatio) {
        List<Stock> validData = stockData.stream()
            .filter(s -> metricExtractor.apply(s) != null && metricExtractor.apply(s).compareTo(BigDecimal.ZERO) > 0)
            .toList();

        if (validData.isEmpty()) {
            System.out.println("\n" + metricName + " Analysis:");
            System.out.println("  Note: Data not available for this metric.");
            return;
        }

        BigDecimal total = validData.stream()
            .map(metricExtractor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Stock min = validData.stream().min(Comparator.comparing(metricExtractor)).orElse(null);
        Stock max = validData.stream().max(Comparator.comparing(metricExtractor)).orElse(null);

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

    private void printTop5ByMarketCap(List<Stock> stockDataList) {
        List<Stock> validMarketCap = stockDataList.stream()
            .filter(s -> s.getMarketCap() != null && s.getMarketCap().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(Stock::getMarketCap).reversed())
            .limit(5)
            .toList();

        if (!validMarketCap.isEmpty()) {
            System.out.println("\nTop 5 Stocks by Market Cap:");
            for (int i = 0; i < validMarketCap.size(); i++) {
                Stock stock = validMarketCap.get(i);
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
}
