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
    private final Map<Integer, BigDecimal> historicalRevenueChange;
    private final Map<Integer, BigDecimal> historicalNetIncomeChange;
    private final Map<Integer, BigDecimal> historicalGrossProfitChange;
    private final Map<String, BigDecimal> quarterlyRevenueChange;
    private final Map<String, BigDecimal> quarterlyNetIncomeChange;
    private final Map<String, BigDecimal> quarterlyGrossProfitChange;

    public Stock(String symbol, String name, BigDecimal price, BigDecimal peRatio,
                     BigDecimal marketCap,
                     BigDecimal volume, String exchange,
                     Map<Integer, BigDecimal> historicalRevenue, Map<Integer, BigDecimal> historicalNetIncome,
                     Map<Integer, BigDecimal> historicalGrossProfit, Map<String, BigDecimal> quarterlyRevenue,
                     Map<String, BigDecimal> quarterlyNetIncome, Map<String, BigDecimal> quarterlyGrossProfit,
                     Map<Integer, BigDecimal> historicalRevenueChange, Map<Integer, BigDecimal> historicalNetIncomeChange,
                     Map<Integer, BigDecimal> historicalGrossProfitChange, Map<String, BigDecimal> quarterlyRevenueChange,
                     Map<String, BigDecimal> quarterlyNetIncomeChange, Map<String, BigDecimal> quarterlyGrossProfitChange) {
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
        this.historicalRevenueChange = historicalRevenueChange;
        this.historicalNetIncomeChange = historicalNetIncomeChange;
        this.historicalGrossProfitChange = historicalGrossProfitChange;
        this.quarterlyRevenueChange = quarterlyRevenueChange;
        this.quarterlyNetIncomeChange = quarterlyNetIncomeChange;
        this.quarterlyGrossProfitChange = quarterlyGrossProfitChange;
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
    public Map<Integer, BigDecimal> getHistoricalRevenueChange() { return historicalRevenueChange; }
    public Map<Integer, BigDecimal> getHistoricalNetIncomeChange() { return historicalNetIncomeChange; }
    public Map<Integer, BigDecimal> getHistoricalGrossProfitChange() { return historicalGrossProfitChange; }
    public Map<String, BigDecimal> getQuarterlyRevenueChange() { return quarterlyRevenueChange; }
    public Map<String, BigDecimal> getQuarterlyNetIncomeChange() { return quarterlyNetIncomeChange; }
    public Map<String, BigDecimal> getQuarterlyGrossProfitChange() { return quarterlyGrossProfitChange; }

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
                    BigDecimal change = getHistoricalRevenueChange().get(entry.getKey());
                    String changeStr = (change != null) ? String.format(" (%+.2f%%)", change) : "";
                    System.out.println(String.format("    %d: $%s%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue()), changeStr));
                });
        }

        if (getHistoricalNetIncome() != null && !getHistoricalNetIncome().isEmpty()) {
            System.out.println("  Historical Net Income:");
            getHistoricalNetIncome().entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    BigDecimal change = getHistoricalNetIncomeChange().get(entry.getKey());
                    String changeStr = (change != null) ? String.format(" (%+.2f%%)", change) : "";
                    System.out.println(String.format("    %d: $%s%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue()), changeStr));
                });
        }

        if (getHistoricalGrossProfit() != null && !getHistoricalGrossProfit().isEmpty()) {
            System.out.println("  Historical Gross Profit:");
            getHistoricalGrossProfit().entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    BigDecimal change = getHistoricalGrossProfitChange().get(entry.getKey());
                    String changeStr = (change != null) ? String.format(" (%+.2f%%)", change) : "";
                    System.out.println(String.format("    %d: $%s%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue()), changeStr));
                });
        }

        if (getQuarterlyRevenue() != null && !getQuarterlyRevenue().isEmpty()) {
            System.out.println("  Quarterly Revenue:");
            getQuarterlyRevenue().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    BigDecimal change = getQuarterlyRevenueChange().get(entry.getKey());
                    String changeStr = (change != null) ? String.format(" (%+.2f%%)", change) : "";
                    System.out.println(String.format("    %s: $%s%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue()), changeStr));
                });
        }

        if (getQuarterlyNetIncome() != null && !getQuarterlyNetIncome().isEmpty()) {
            System.out.println("  Quarterly Net Income:");
            getQuarterlyNetIncome().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    BigDecimal change = getQuarterlyNetIncomeChange().get(entry.getKey());
                    String changeStr = (change != null) ? String.format(" (%+.2f%%)", change) : "";
                    System.out.println(String.format("    %s: $%s%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue()), changeStr));
                });
        }

        if (getQuarterlyGrossProfit() != null && !getQuarterlyGrossProfit().isEmpty()) {
            System.out.println("  Quarterly Gross Profit:");
            getQuarterlyGrossProfit().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .forEach(entry -> {
                    BigDecimal change = getQuarterlyGrossProfitChange().get(entry.getKey());
                    String changeStr = (change != null) ? String.format(" (%+.2f%%)", change) : "";
                    System.out.println(String.format("    %s: $%s%s", entry.getKey(), analyzer.formatBigNumber(entry.getValue()), changeStr));
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
