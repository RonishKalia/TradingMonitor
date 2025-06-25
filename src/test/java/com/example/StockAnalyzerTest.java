package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

public class StockAnalyzerTest {

    private StockAnalyzer analyzer;

    @BeforeEach
    public void setUp() {
        analyzer = new StockAnalyzer();
    }

    @Test
    public void testGetSupportedExchanges() {
        Set<String> exchanges = analyzer.getSupportedExchanges();
        assertNotNull(exchanges);
        assertTrue(exchanges.contains("NYSE"));
        assertTrue(exchanges.contains("NASDAQ"));
        assertTrue(exchanges.contains("SP500"));
    }

    @Test
    public void testFormatBigNumber() {
        assertEquals("1.00K", analyzer.formatBigNumber(java.math.BigDecimal.valueOf(1000)));
        assertEquals("1.50M", analyzer.formatBigNumber(java.math.BigDecimal.valueOf(1500000)));
        assertEquals("2.00B", analyzer.formatBigNumber(java.math.BigDecimal.valueOf(2000000000)));
        assertEquals("N/A", analyzer.formatBigNumber(null));
    }

    @Test
    public void testInvalidExchange() {
        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.analyzeExchange("INVALID_EXCHANGE");
        });
    }

    @Test
    public void testStockDataCreation() {
        StockAnalyzer.StockData stockData = new StockAnalyzer.StockData(
            "AAPL", "Apple Inc.", 
            java.math.BigDecimal.valueOf(150.0),
            java.math.BigDecimal.valueOf(25.5),
            java.math.BigDecimal.valueOf(2500000000000L),
            java.math.BigDecimal.valueOf(394328000000L),
            java.math.BigDecimal.valueOf(170782000000L),
            java.math.BigDecimal.valueOf(1000000),
            "NASDAQ"
        );

        assertEquals("AAPL", stockData.getSymbol());
        assertEquals("Apple Inc.", stockData.getName());
        assertEquals(java.math.BigDecimal.valueOf(25.5), stockData.getPeRatio());
        assertEquals("NASDAQ", stockData.getExchange());
    }
} 