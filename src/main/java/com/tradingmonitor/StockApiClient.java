package com.tradingmonitor;

import com.tradingmonitor.api.ApiProvider;
import com.tradingmonitor.api.FinancialModelingPrepApiClient;
import com.tradingmonitor.api.FinnhubApiClient;
import com.tradingmonitor.api.PolygonApiClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockApiClient {

    private static final Logger logger = LoggerFactory.getLogger(StockApiClient.class);

    private final FinnhubApiClient finnhubClient;
    private final PolygonApiClient polygonClient;
    private final FinancialModelingPrepApiClient fmpClient;
    private final List<ApiProvider> apiProviders;

    public StockApiClient(String finnhubApiKey, String fmpApiKey, String alphaVantageApiKey, String polygonApiKey) {
        this.apiProviders = new ArrayList<>();
        this.finnhubClient = new FinnhubApiClient(finnhubApiKey);
        this.apiProviders.add(this.finnhubClient);
        this.polygonClient = new PolygonApiClient(polygonApiKey);
        this.apiProviders.add(this.polygonClient);
        this.fmpClient = new FinancialModelingPrepApiClient(fmpApiKey);
    }

    public List<String> fetchUsStockSymbols(boolean isTesting) throws IOException {
        try {
            if (isTesting) {
                return polygonClient.fetchStockSymbols("XNAS", 10);
            } else {
                List<String> symbols = new ArrayList<>();
                symbols.addAll(polygonClient.fetchStockSymbols("XNAS"));
                symbols.addAll(polygonClient.fetchStockSymbols("XNYS"));
                return symbols;
            }
        } catch (IOException e) {
            logger.error("Failed to fetch stock symbols from Polygon.io", e);
            throw e;
        }
    }

    public Stock fetchStockData(String symbol, String exchange) throws IOException {
        String name = null;
        BigDecimal price = null;
        BigDecimal peRatio = null;
        BigDecimal marketCap = null;
        BigDecimal volume = null;
        Map<Integer, BigDecimal> historicalRevenue = new HashMap<>();
        Map<Integer, BigDecimal> historicalNetIncome = new HashMap<>();
        Map<Integer, BigDecimal> historicalGrossProfit = new HashMap<>();
        Map<String, BigDecimal> quarterlyRevenue = new HashMap<>();
        Map<String, BigDecimal> quarterlyNetIncome = new HashMap<>();
        Map<String, BigDecimal> quarterlyGrossProfit = new HashMap<>();
        Map<String, BigDecimal> quarterlyEps = new HashMap<>();

        for (ApiProvider provider : apiProviders) {
            try {
                Stock stock = provider.fetchStockData(symbol, exchange);
                if (stock != null) {
                    if (provider instanceof FinnhubApiClient) {
                        name = stock.getName();
                        price = stock.getPrice();
                        marketCap = stock.getMarketCap();
                    } else if (provider instanceof PolygonApiClient) {
                        historicalRevenue.putAll(stock.getHistoricalRevenue());
                        historicalNetIncome.putAll(stock.getHistoricalNetIncome());
                        historicalGrossProfit.putAll(stock.getHistoricalGrossProfit());
                        quarterlyRevenue.putAll(stock.getQuarterlyRevenue());
                        quarterlyNetIncome.putAll(stock.getQuarterlyNetIncome());
                        quarterlyGrossProfit.putAll(stock.getQuarterlyGrossProfit());
                        quarterlyEps.putAll(stock.getQuarterlyEps());
                    }
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while fetching data for {}", symbol, e);
            }
        }

        if (price != null && !quarterlyEps.isEmpty()) {
            BigDecimal ttmEps = quarterlyEps.values().stream()
                                        .limit(4)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (ttmEps.compareTo(BigDecimal.ZERO) > 0) {
                peRatio = price.divide(ttmEps, 2, RoundingMode.HALF_UP);
            }
        }

        return new Stock(symbol, name, price, peRatio, marketCap, volume, exchange, historicalRevenue, historicalNetIncome, historicalGrossProfit, quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit, quarterlyEps);
    }
}