package com.tradingmonitor;

import java.math.BigDecimal;
import java.util.Map;

public class Stock {
    private final String symbol;
    private final String name;
    private final BigDecimal price;
    private final BigDecimal peRatio;
    private final BigDecimal marketCap;
    private final BigDecimal volume;
    private final String exchange;
    private final Map<Integer, BigDecimal> historicalRevenue;
    private final Map<Integer, BigDecimal> historicalNetIncome;
    private final Map<Integer, BigDecimal> historicalGrossProfit;
    private final Map<String, BigDecimal> quarterlyRevenue;
    private final Map<String, BigDecimal> quarterlyNetIncome;
    private final Map<String, BigDecimal> quarterlyGrossProfit;

    public Stock(String symbol, String name, BigDecimal price, BigDecimal peRatio,
                     BigDecimal marketCap,
                     BigDecimal volume, String exchange,
                     Map<Integer, BigDecimal> historicalRevenue, Map<Integer, BigDecimal> historicalNetIncome,
                     Map<Integer, BigDecimal> historicalGrossProfit, Map<String, BigDecimal> quarterlyRevenue,
                     Map<String, BigDecimal> quarterlyNetIncome, Map<String, BigDecimal> quarterlyGrossProfit) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.peRatio = peRatio;
        this.marketCap = marketCap;
        this.volume = volume;
        this.exchange = exchange;
        this.historicalRevenue = historicalRevenue;
        this.historicalNetIncome = historicalNetIncome;
        this.historicalGrossProfit = historicalGrossProfit;
        this.quarterlyRevenue = quarterlyRevenue;
        this.quarterlyNetIncome = quarterlyNetIncome;
        this.quarterlyGrossProfit = quarterlyGrossProfit;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getPeRatio() { return peRatio; }
    public BigDecimal getMarketCap() { return marketCap; }
    public BigDecimal getVolume() { return volume; }
    public String getExchange() { return exchange; }
    public Map<Integer, BigDecimal> getHistoricalRevenue() { return historicalRevenue; }
    public Map<Integer, BigDecimal> getHistoricalNetIncome() { return historicalNetIncome; }
    public Map<Integer, BigDecimal> getHistoricalGrossProfit() { return historicalGrossProfit; }
    public Map<String, BigDecimal> getQuarterlyRevenue() { return quarterlyRevenue; }
    public Map<String, BigDecimal> getQuarterlyNetIncome() { return quarterlyNetIncome; }
    public Map<String, BigDecimal> getQuarterlyGrossProfit() { return quarterlyGrossProfit; }

    public void printAllData(StockAnalyzer analyzer) {
        System.out.println(String.format("%-6s | %-20s | P/E: %-8s",
            getSymbol(),
            getName() != null ? getName().substring(0, Math.min(20, getName().length())) : "N/A",
            getPeRatio() != null ? getPeRatio().toString() : "N/A"
        ));

        if (getHistoricalRevenue() != null && !getHistoricalRevenue().isEmpty()) {
            System.out.println("  Historical Revenue:");
            getHistoricalRevenue().entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    System.out.println(String.format("    %d: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                });
        }

        if (getHistoricalNetIncome() != null && !getHistoricalNetIncome().isEmpty()) {
            System.out.println("  Historical Net Income:");
            getHistoricalNetIncome().entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    System.out.println(String.format("    %d: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                });
        }

        if (getHistoricalGrossProfit() != null && !getHistoricalGrossProfit().isEmpty()) {
            System.out.println("  Historical Gross Profit:");
            getHistoricalGrossProfit().entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    System.out.println(String.format("    %d: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                });
        }

        if (getQuarterlyRevenue() != null && !getQuarterlyRevenue().isEmpty()) {
            System.out.println("  Quarterly Revenue:");
            getQuarterlyRevenue().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    System.out.println(String.format("    %s: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                });
        }

        if (getQuarterlyNetIncome() != null && !getQuarterlyNetIncome().isEmpty()) {
            System.out.println("  Quarterly Net Income:");
            getQuarterlyNetIncome().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    System.out.println(String.format("    %s: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                });
        }

        if (getQuarterlyGrossProfit() != null && !getQuarterlyGrossProfit().isEmpty()) {
            System.out.println("  Quarterly Gross Profit:");
            getQuarterlyGrossProfit().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    System.out.println(String.format("    %s: $%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue())));
                });
        }
    }
    
    @Override
    public String toString() {
        return String.format("Stock{symbol='%s', name='%s', price=%s, peRatio=%s, " +
                "marketCap=%s, volume=%s, exchange='%s', " +
                "historicalRevenue=%s, historicalNetIncome=%s, historicalGrossProfit=%s, " +
                "quarterlyRevenue=%s, quarterlyNetIncome=%s, quarterlyGrossProfit=%s}",
            symbol, name, price, peRatio, marketCap, volume, exchange,
            historicalRevenue, historicalNetIncome, historicalGrossProfit,
            quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit);
    }
}
