package com.tradingmonitor;

import java.math.BigDecimal;

public class Stock {
    private final String symbol;
    private final String name;
    private final BigDecimal price;
    private final BigDecimal peRatio;
    private final BigDecimal marketCap;
    private final BigDecimal revenue;
    private final BigDecimal grossProfit;
    private final BigDecimal volume;
    private final String exchange;

    public Stock(String symbol, String name, BigDecimal price, BigDecimal peRatio,
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
        return String.format("Stock{symbol='%s', name='%s', price=%s, peRatio=%s, " +
                "marketCap=%s, revenue=%s, grossProfit=%s, volume=%s, exchange='%s'}",
            symbol, name, price, peRatio, marketCap, revenue, grossProfit, volume, exchange);
    }
}
