package com.tradingmonitor;

import com.tradingmonitor.api.AlphaVantageApiClient;
import com.tradingmonitor.api.ApiProvider;
import com.tradingmonitor.api.FinancialModelingPrepApiClient;
import com.tradingmonitor.api.FinnhubApiClient;
import com.tradingmonitor.api.PolygonApiClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockApiClient {

    private static final Logger logger = LoggerFactory.getLogger(StockApiClient.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long TIME_WINDOW_MS = 65 * 1000; // 65 seconds

    private final List<ApiProvider> apiProviders;
    private final FinancialModelingPrepApiClient fmpClient;
    private final BlockingQueue<Long> requestTimestamps;

    public StockApiClient(String finnhubApiKey, String fmpApiKey, String alphaVantageApiKey, String polygonApiKey) {
        this.apiProviders = new ArrayList<>();
        this.apiProviders.add(new FinnhubApiClient(finnhubApiKey));
        this.fmpClient = new FinancialModelingPrepApiClient(fmpApiKey);
        this.apiProviders.add(this.fmpClient);
        this.apiProviders.add(new AlphaVantageApiClient(alphaVantageApiKey));
        this.apiProviders.add(new PolygonApiClient(polygonApiKey));
        this.requestTimestamps = new ArrayBlockingQueue<>(MAX_REQUESTS_PER_MINUTE);
    }

    private void rateLimit() throws InterruptedException {
        long currentTime = System.currentTimeMillis();

        // If the queue is full, we must wait.
        if (requestTimestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            long oldestTimestamp = requestTimestamps.peek(); // Time of the first request in the window
            long elapsedTime = currentTime - oldestTimestamp;

            if (elapsedTime < TIME_WINDOW_MS) {
                long waitTime = TIME_WINDOW_MS - elapsedTime;
                logger.warn("Rate limit reached. Waiting for {} ms to respect the 65-second window.", waitTime);
                Thread.sleep(waitTime);
            }
            // After waiting, the time window for the oldest request has passed. Remove it to make space.
            requestTimestamps.poll();
        }
        // Add the timestamp for the new request.
        requestTimestamps.offer(System.currentTimeMillis());
    }

    public List<String> fetchUsStockSymbols() throws IOException {
        return fmpClient.fetchUsStockSymbols();
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

        for (ApiProvider provider : apiProviders) {
            try {
                rateLimit();
                Stock stock = provider.fetchStockData(symbol, exchange);
                if (stock != null) {
                    if (provider instanceof FinnhubApiClient) {
                        name = stock.getName();
                        price = stock.getPrice();
                        marketCap = stock.getMarketCap();
                    } else if (provider instanceof FinancialModelingPrepApiClient) {
                        peRatio = stock.getPeRatio();
                    } else if (provider instanceof PolygonApiClient) {
                        historicalRevenue.putAll(stock.getHistoricalRevenue());
                        historicalNetIncome.putAll(stock.getHistoricalNetIncome());
                        historicalGrossProfit.putAll(stock.getHistoricalGrossProfit());
                        quarterlyRevenue.putAll(stock.getQuarterlyRevenue());
                        quarterlyNetIncome.putAll(stock.getQuarterlyNetIncome());
                        quarterlyGrossProfit.putAll(stock.getQuarterlyGrossProfit());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while fetching data for {}", symbol, e);
                throw new IOException("Interrupted while fetching data for " + symbol, e);
            } catch (Exception e) {
                logger.error("An unexpected error occurred while fetching data for {}", symbol, e);
            }
        }

        return new Stock(symbol, name, price, peRatio, marketCap, volume, exchange, historicalRevenue, historicalNetIncome, historicalGrossProfit, quarterlyRevenue, quarterlyNetIncome, quarterlyGrossProfit);
    }
}
