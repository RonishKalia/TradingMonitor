package com.tradingmonitor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private <T extends Comparable<T>> void printAnnualDataWithGrowth(
        String title,
        Map<T, BigDecimal> data,
        StockAnalyzer analyzer
    ) {
        if (data != null && !data.isEmpty()) {
            System.out.println("  " + title + ":");
            List<Map.Entry<T, BigDecimal>> sortedData = data.entrySet().stream()
                .sorted(Map.Entry.<T, BigDecimal>comparingByKey().reversed())
                .collect(Collectors.toList());

            int currentYear = LocalDate.now().getYear();

            for (int i = 0; i < sortedData.size(); i++) {
                Map.Entry<T, BigDecimal> currentEntry = sortedData.get(i);
                String formattedValue = analyzer.formatBigNumber(currentEntry.getValue());
                String growthString = "";

                if (i < sortedData.size() - 1) {
                    // Don't show growth for the current, incomplete year
                    if (currentEntry.getKey() instanceof Integer && (Integer) currentEntry.getKey() == currentYear) {
                        growthString = " (incomplete year)";
                    } else {
                        Map.Entry<T, BigDecimal> previousEntry = sortedData.get(i + 1);
                        BigDecimal growth = analyzer.calculateGrowth(currentEntry.getValue(), previousEntry.getValue());
                        if (growth != null) {
                            growthString = String.format(" (%s%.2f%%)", growth.signum() > 0 ? "+" : "", growth);
                        }
                    }
                }
                System.out.printf("    %s: $%s%s%n", currentEntry.getKey(), formattedValue, growthString);
            }
        }
    }

    private void printQuarterlyDataWithYoyGrowth(
        String title,
        Map<String, BigDecimal> data,
        StockAnalyzer analyzer
    ) {
        if (data != null && !data.isEmpty()) {
            System.out.println("  " + title + ":");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            List<Map.Entry<String, BigDecimal>> sortedData = data.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .collect(Collectors.toList());

            for (Map.Entry<String, BigDecimal> currentEntry : sortedData) {
                String formattedValue = analyzer.formatBigNumber(currentEntry.getValue());
                String growthString = "";

                LocalDate currentDate = LocalDate.parse(currentEntry.getKey(), formatter);
                LocalDate previousYearDate = currentDate.minusYears(1);
                String previousYearKey = previousYearDate.format(formatter);

                if (data.containsKey(previousYearKey)) {
                    BigDecimal previousYearValue = data.get(previousYearKey);
                    BigDecimal growth = analyzer.calculateGrowth(currentEntry.getValue(), previousYearValue);
                    if (growth != null) {
                        growthString = String.format(" (YoY: %s%.2f%%)", growth.signum() > 0 ? "+" : "", growth);
                    }
                }
                System.out.printf("    %s: $%s%s%n", currentEntry.getKey(), formattedValue, growthString);
            }
        }
    }

    public void printAllData(StockAnalyzer analyzer) {
        System.out.println(String.format("%-6s | %-20s | P/E: %-8s",
            getSymbol(),
            getName() != null ? getName().substring(0, Math.min(20, getName().length())) : "N/A",
            getPeRatio() != null ? getPeRatio().toString() : "N/A"
        ));

        printAnnualDataWithGrowth("Historical Revenue", getHistoricalRevenue(), analyzer);
        printAnnualDataWithGrowth("Historical Net Income", getHistoricalNetIncome(), analyzer);
        printAnnualDataWithGrowth("Historical Gross Profit", getHistoricalGrossProfit(), analyzer);
        printQuarterlyDataWithYoyGrowth("Quarterly Revenue", getQuarterlyRevenue(), analyzer);
        printQuarterlyDataWithYoyGrowth("Quarterly Net Income", getQuarterlyNetIncome(), analyzer);
        printQuarterlyDataWithYoyGrowth("Quarterly Gross Profit", getQuarterlyGrossProfit(), analyzer);
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
