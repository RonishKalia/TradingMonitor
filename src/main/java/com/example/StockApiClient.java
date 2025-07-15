package com.example;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;
import yahoofinance.quotes.stock.StockStats;

import java.io.IOException;
import java.math.BigDecimal;

public class StockApiClient {

    public com.example.Stock fetchStockData(String symbol, String exchange) throws IOException {
        Stock stock = YahooFinance.get(symbol);
        if (stock == null || !stock.isValid()) {
            return null;
        }

        StockQuote quote = stock.getQuote();
        StockStats stats = stock.getStats();

        if (quote == null || stats == null) {
            return null;
        }

        return new com.example.Stock(
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
}
