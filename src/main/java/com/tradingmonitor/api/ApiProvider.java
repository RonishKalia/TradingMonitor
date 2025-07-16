package com.tradingmonitor.api;

import com.tradingmonitor.Stock;
import java.io.IOException;

public interface ApiProvider {
    Stock fetchStockData(String symbol, String exchange) throws IOException;
}
