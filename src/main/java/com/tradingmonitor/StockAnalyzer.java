package com.tradingmonitor;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StockAnalyzer {

    private final StockApiClient stockApiClient;

    public StockAnalyzer(StockApiClient stockApiClient) {
        this.stockApiClient = stockApiClient;
    }

    public List<Stock> analyzeUsStocks(boolean isTesting) throws IOException {
        List<String> symbols = stockApiClient.fetchUsStockSymbols(isTesting);
        if (symbols == null || symbols.isEmpty()) {
            throw new IOException("No symbols found for US stocks.");
        }

        if (isTesting) {
            System.out.println("--- Analyzing 10 stocks in test mode ---");
        }

        System.out.println("Analyzing " + symbols.size() + " US stocks...");

        return symbols.parallelStream()
            .map(symbol -> {
                try {
                    return stockApiClient.fetchStockData(symbol, "US");
                } catch (IOException e) {
                    System.err.println("Error analyzing " + symbol + ": " + e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .peek(data -> System.out.println("✓ " + data.getSymbol() + " - P/E: " + data.getPeRatio()))
            .collect(Collectors.toList());
    }

    public List<Stock> analyzeSingleStock(String symbol) throws IOException {
        System.out.println("Analyzing single stock: " + symbol);
        Stock stock = stockApiClient.fetchStockData(symbol, "US");
        if (stock == null) {
            throw new IOException("Could not fetch data for symbol: " + symbol);
        }
        System.out.println("✓ " + stock.getSymbol() + " - P/E: " + stock.getPeRatio());
        return List.of(stock);
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

    public BigDecimal calculateGrowth(BigDecimal currentValue, BigDecimal previousValue) {
        if (currentValue == null || previousValue == null || previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentValue.subtract(previousValue)
            .divide(previousValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
}
