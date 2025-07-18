package com.tradingmonitor;

import com.tradingmonitor.api.FinancialModelingPrepApiClient;
import com.tradingmonitor.api.PolygonApiClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockApiClient {

    private static final Logger logger = LoggerFactory.getLogger(StockApiClient.class);

    private final PolygonApiClient polygonClient;
    private final FinancialModelingPrepApiClient fmpClient;

    public StockApiClient(String finnhubApiKey, String fmpApiKey, String alphaVantageApiKey, String polygonApiKey) {
        this.polygonClient = new PolygonApiClient(polygonApiKey);
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
        try {
            Stock stock = polygonClient.fetchStockData(symbol, exchange);
            if (stock != null) {
                BigDecimal peRatio = fmpClient.fetchKeyMetrics(symbol).get("peRatioTTM");
                return new Stock(symbol, stock.getName(), stock.getPrice(), peRatio, stock.getMarketCap(), stock.getVolume(), exchange, stock.getHistoricalRevenue(), stock.getHistoricalNetIncome(), stock.getHistoricalGrossProfit(), stock.getQuarterlyRevenue(), stock.getQuarterlyNetIncome(), stock.getQuarterlyGrossProfit(), stock.getQuarterlyEps(), stock.getQuarterlyDilutedEps(), stock.getWeightedAverageSharesOutstanding());
            }
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching data for {}", symbol, e);
        }
        return null;
    }
}
