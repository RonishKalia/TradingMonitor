package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class StockAnalyzerTest {

    private StockAnalyzer analyzer;

    // Stub for StockApiClient for testing purposes
    class StockApiClientStub extends StockApiClient {
        @Override
        public Stock fetchStockData(String symbol, String exchange) throws IOException {
            // Return some mock data for testing
            return new Stock(
                symbol,
                "Mock Stock",
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(20.0),
                BigDecimal.valueOf(1000000000),
                BigDecimal.valueOf(50000000),
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(1000000),
                exchange
            );
        }
    }

    @BeforeEach
    public void setUp() {
        analyzer = new StockAnalyzer(new StockApiClientStub());
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
} 