package com.tradingmonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StockAnalyzerTest {

    private StockAnalyzer analyzer;

    // Stub for StockApiClient for testing purposes
    class StockApiClientStub extends StockApiClient {
        public StockApiClientStub() {
            super("test-api-key", "test-api-key", "test-api-key", "test-api-key");
        }

        @Override
        public List<String> fetchUsStockSymbols(boolean isTesting) throws IOException {
            List<String> symbols = new ArrayList<>();
            IntStream.range(0, 20).forEach(i -> symbols.add("TEST" + i));
            if (isTesting) {
                return symbols.stream().limit(10).collect(Collectors.toList());
            }
            return symbols;
        }

        @Override
        public Stock fetchStockData(String symbol, String exchange) throws IOException {
            // Return some mock data for testing
            return new Stock(
                symbol,
                "Mock Stock",
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(20.0),
                BigDecimal.valueOf(1000000000),
                BigDecimal.valueOf(1000000),
                exchange,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
            );
        }
    }

    @BeforeEach
    public void setUp() {
        analyzer = new StockAnalyzer(new StockApiClientStub());
    }

    @Test
    public void testAnalyzeUsStocks_noTesting() throws IOException {
        List<Stock> stocks = analyzer.analyzeUsStocks(false);
        assertNotNull(stocks);
        assertEquals(20, stocks.size());
    }

    @Test
    public void testAnalyzeUsStocks_withTesting() throws IOException {
        List<Stock> stocks = analyzer.analyzeUsStocks(true);
        assertNotNull(stocks);
        assertEquals(10, stocks.size());
    }

    @Test
    public void testFormatBigNumber() {
        assertEquals("1.00K", analyzer.formatBigNumber(java.math.BigDecimal.valueOf(1000)));
        assertEquals("1.50M", analyzer.formatBigNumber(java.math.BigDecimal.valueOf(1500000)));
        assertEquals("2.00B", analyzer.formatBigNumber(java.math.BigDecimal.valueOf(2000000000)));
        assertEquals("N/A", analyzer.formatBigNumber(null));
    }

    }
 