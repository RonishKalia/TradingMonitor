package com.example;

import io.finnhub.api.apis.DefaultApi;
import io.finnhub.api.infrastructure.ApiClient;
import io.finnhub.api.models.CompanyProfile2;
import io.finnhub.api.models.Quote;
import java.io.IOException;
import java.math.BigDecimal;

public class StockApiClient {

    private final DefaultApi finnhubClient;

    public StockApiClient(String apiKey) {
        this.finnhubClient = new DefaultApi(apiKey);
    }

    public Stock fetchStockData(String symbol, String exchange) throws IOException {
        try {
            Quote quote = finnhubClient.quote(symbol);
            CompanyProfile2 profile = finnhubClient.companyProfile2(symbol, null, null);

            if (quote == null || profile == null) {
                return null;
            }

            return new Stock(
                symbol,
                profile.getName(),
                quote.getC() != null ? new BigDecimal(quote.getC()) : null,
                null, // P/E ratio not directly available in these endpoints
                profile.getMarketCapitalization() != null ? new BigDecimal(profile.getMarketCapitalization()) : null,
                null, // Revenue not directly available in these endpoints
                null, // Gross profit not available
                null,
                exchange
            );
        } catch (Exception e) {
            throw new IOException("Failed to fetch data for symbol: " + symbol, e);
        }
    }
}
