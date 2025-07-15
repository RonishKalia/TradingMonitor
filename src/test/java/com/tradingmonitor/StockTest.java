package com.tradingmonitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

public class StockTest {

    @Test
    public void testStockCreation() {
        Stock stock = new Stock(
            "AAPL", "Apple Inc.",
            BigDecimal.valueOf(150.0),
            BigDecimal.valueOf(25.5),
            BigDecimal.valueOf(2500000000000L),
            BigDecimal.valueOf(394328000000L),
            BigDecimal.valueOf(170782000000L),
            BigDecimal.valueOf(1000000),
            "NASDAQ"
        );

        assertEquals("AAPL", stock.getSymbol());
        assertEquals("Apple Inc.", stock.getName());
        assertEquals(BigDecimal.valueOf(25.5), stock.getPeRatio());
        assertEquals("NASDAQ", stock.getExchange());
    }
}
