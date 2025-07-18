package com.tradingmonitor;

import java.io.PrintWriter;
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
    private final Map<String, BigDecimal> quarterlyEps;
    private final Map<String, BigDecimal> quarterlyDilutedEps;
    private final Map<String, BigDecimal> weightedAverageSharesOutstanding;

    public Stock(String symbol, String name, BigDecimal price, BigDecimal peRatio,
                     BigDecimal marketCap,
                     BigDecimal volume, String exchange,
                     Map<Integer, BigDecimal> historicalRevenue, Map<Integer, BigDecimal> historicalNetIncome,
                     Map<Integer, BigDecimal> historicalGrossProfit, Map<String, BigDecimal> quarterlyRevenue,
                     Map<String, BigDecimal> quarterlyNetIncome, Map<String, BigDecimal> quarterlyGrossProfit,
                     Map<String, BigDecimal> quarterlyEps, Map<String, BigDecimal> quarterlyDilutedEps, Map<String, BigDecimal> weightedAverageSharesOutstanding) {
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
        this.quarterlyEps = quarterlyEps;
        this.quarterlyDilutedEps = quarterlyDilutedEps;
        this.weightedAverageSharesOutstanding = weightedAverageSharesOutstanding;
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
    public Map<String, BigDecimal> getQuarterlyEps() { return quarterlyEps; }
    public Map<String, BigDecimal> getQuarterlyDilutedEps() { return quarterlyDilutedEps; }
    public Map<String, BigDecimal> getWeightedAverageSharesOutstanding() { return weightedAverageSharesOutstanding; }

    private <T extends Comparable<T>> void printAnnualDataWithGrowth(
        String title,
        Map<T, BigDecimal> data,
        StockAnalyzer analyzer,
        PrintWriter writer
    ) {
        if (data != null && !data.isEmpty()) {
            String header = "  " + title + ":";
            System.out.println(header);
            writer.println(header);

            List<Map.Entry<T, BigDecimal>> sortedData = data.entrySet().stream()
                .sorted(Map.Entry.<T, BigDecimal>comparingByKey().reversed())
                .collect(Collectors.toList());

            int currentYear = LocalDate.now().getYear();

            for (int i = 0; i < sortedData.size(); i++) {
                Map.Entry<T, BigDecimal> currentEntry = sortedData.get(i);
                String formattedValue = analyzer.formatBigNumber(currentEntry.getValue());
                String growthString = "";

                if (i < sortedData.size() - 1) {
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
                String line = String.format("    %s: $%s%s", currentEntry.getKey(), formattedValue, growthString);
                System.out.println(line);
                writer.println(line);
            }
        }
    }

    private void printQuarterlyDataWithYoyGrowth(
        String title,
        Map<String, BigDecimal> data,
        StockAnalyzer analyzer,
        PrintWriter writer
    ) {
        if (data != null && !data.isEmpty()) {
            String header = "  " + title + ":";
            System.out.println(header);
            writer.println(header);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            List<Map.Entry<String, BigDecimal>> sortedData = data.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByKey().reversed())
                .limit(9)
                .collect(Collectors.toList());

            for (int i = 0; i < sortedData.size(); i++) {
                Map.Entry<String, BigDecimal> currentEntry = sortedData.get(i);
                String formattedValue = analyzer.formatBigNumber(currentEntry.getValue());
                final String[] yoyGrowthString = {""};
                final String[] qoqGrowthString = {""};

                // YoY Growth
                LocalDate currentDate = LocalDate.parse(currentEntry.getKey(), formatter);
                int currentMonth = currentDate.getMonthValue();
                int previousYear = currentDate.getYear() - 1;

                data.entrySet().stream()
                    .filter(e -> {
                        LocalDate d = LocalDate.parse(e.getKey(), formatter);
                        return d.getYear() == previousYear && d.getMonthValue() == currentMonth;
                    })
                    .findFirst()
                    .ifPresent(previousEntry -> {
                        BigDecimal growth = analyzer.calculateGrowth(currentEntry.getValue(), previousEntry.getValue());
                        if (growth != null) {
                            yoyGrowthString[0] = String.format(" (YoY: %s%.2f%%)", growth.signum() > 0 ? "+" : "", growth);
                        }
                    });

                // QoQ Growth
                if (i < sortedData.size() - 1) {
                    Map.Entry<String, BigDecimal> previousQuarterEntry = sortedData.get(i + 1);
                    BigDecimal growth = analyzer.calculateGrowth(currentEntry.getValue(), previousQuarterEntry.getValue());
                    if (growth != null) {
                        qoqGrowthString[0] = String.format(" (QoQ: %s%.2f%%)", growth.signum() > 0 ? "+" : "", growth);
                    }
                }

                String line = String.format("    %s: $%s%s%s", currentEntry.getKey(), formattedValue, yoyGrowthString[0], qoqGrowthString[0]);
                System.out.println(line);
                writer.println(line);
            }
        }
    }

    public void printAllData(StockAnalyzer analyzer, PrintWriter writer) {
        String header = String.format("%-6s | %-20s | P/E: %-8s",
            getSymbol(),
            getName() != null ? getName().substring(0, Math.min(20, getName().length())) : "N/A",
            getPeRatio() != null ? getPeRatio().toString() : "N/A"
        );
        System.out.println(header);
        writer.println(header);

        printAnnualDataWithGrowth("Historical Revenue", getHistoricalRevenue(), analyzer, writer);
        printAnnualDataWithGrowth("Historical Net Income", getHistoricalNetIncome(), analyzer, writer);
        printAnnualDataWithGrowth("Historical Gross Profit", getHistoricalGrossProfit(), analyzer, writer);
        printQuarterlyDataWithYoyGrowth("Quarterly Revenue", getQuarterlyRevenue(), analyzer, writer);
        printQuarterlyDataWithYoyGrowth("Quarterly Net Income", getQuarterlyNetIncome(), analyzer, writer);
        printQuarterlyDataWithYoyGrowth("Quarterly Gross Profit", getQuarterlyGrossProfit(), analyzer, writer);
        printQuarterlyDataWithYoyGrowth("Quarterly EPS (Basic)", getQuarterlyEps(), analyzer, writer);
        printQuarterlyDataWithYoyGrowth("Quarterly EPS (Diluted)", getQuarterlyDilutedEps(), analyzer, writer);
        writer.println(); // Add a blank line for separation
    }
    
    @Override
    public String toString() {
        return String.format("Stock{symbol='%s', name='%s', price=%s, peRatio=%s, " +
                "marketCap=%s, volume=%s, exchange='%s', " +
                "historicalRevenue=%s, historicalNetIncome=%s, historicalGrossProfit=%s, " +
                "quarterlyRevenue=%s, quarterlyNetIncome=%s, quarterlyGrossProfit=%s, quarterlyEps=%s}",
            symbol, name, price, peRatio, marketCap, volume, exchange,
            historicalRevenue, historicalNetIncome, historicalGrossProfit,
            quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit, quarterlyEps);
    }
}
