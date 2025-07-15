# TradingMonitor

A Java application that analyzes stock data from various exchanges, providing insights into P/E ratios, revenue, and gross profit metrics.

## Features

- **Multi-Exchange Support**: Analyze stocks from NYSE, NASDAQ, and S&P 500
- **Comprehensive Metrics**: P/E ratios, revenue, gross profit, market cap, and volume
- **Real-time Data**: Uses Finnhub API for live stock data
- **Detailed Analysis**: Provides summary statistics and individual stock breakdowns

## Supported Exchanges

- **NYSE**: New York Stock Exchange (35 major stocks)
- **NASDAQ**: NASDAQ Composite (8 major stocks)
- **SP500**: S&P 500 subset (27 major stocks)

## Installation

1. Clone the repository
2. Ensure you have Java 17+ installed
3. Build the project:
   ```bash
   ./gradlew build
   ```

## Usage

### API Key

This project uses the Finnhub API. You will need to get a free API key from [https://finnhub.io/](https://finnhub.io/) and set it in `src/main/java/com/example/Main.java`.

### Running the Application

```bash
./gradlew run
```

This will:
1. Analyze NASDAQ stocks by default
2. Display real-time P/E ratios, revenue, and gross profit
3. Show summary statistics
4. List top stocks by market cap

### Programmatic Usage

```java
StockApiClient apiClient = new StockApiClient("YOUR_API_KEY");
StockAnalyzer analyzer = new StockAnalyzer(apiClient);

// Analyze a specific exchange
try {
    List<Stock> stockData = analyzer.analyzeExchange("NYSE");

    // Print analysis summary
    analyzer.printAnalysisSummary(stockData);

    // Access individual stock data
    for (Stock stock : stockData) {
        System.out.println(stock.getSymbol() + " - P/E: " + stock.getPeRatio());
    }
} catch (IllegalArgumentException e) {
    System.err.println(e.getMessage());
}
```

### Available Exchanges

```java
Set<String> exchanges = analyzer.getSupportedExchanges();
// Returns: [NYSE, NASDAQ, SP500]
```

## Stock Data Metrics

Each stock provides the following metrics:

- **Symbol**: Stock ticker symbol
- **Name**: Company name
- **Price**: Current stock price
- **P/E Ratio**: Price-to-Earnings ratio
- **Market Cap**: Market capitalization
- **Revenue**: Annual revenue
- **Gross Profit**: Annual gross profit
- **Volume**: Trading volume
- **Exchange**: Stock exchange

## Dependencies

- **Finnhub API**: For real-time stock data
- **Jackson**: JSON processing
- **Apache HttpClient**: HTTP requests
- **SLF4J**: Logging

## Testing

Run the test suite:

```bash
./gradlew test
```

## Notes

- Some stocks may not have complete data available
- Internet connection is required for real-time data
- Data accuracy depends on Finnhub API availability

## Example Output

```
=== TRADING MONITOR - STOCK ANALYSIS ===
Supported exchanges: [NYSE, NASDAQ, SP500]

Starting analysis of NASDAQ stocks...
Analyzing 8 stocks from NASDAQ...
✓ AAPL - P/E: 28.98, Revenue: $394.33B
✓ MSFT - P/E: 33.38, Revenue: $198.27B
...

=== STOCK ANALYSIS SUMMARY ===
Total stocks analyzed: 8

P/E Ratio Analysis:
  Average P/E: 62.38
  Lowest P/E: GOOGL (23.35)
  Highest P/E: NVDA (229.64)

Revenue Analysis:
  Total Revenue (all stocks): $1897.24B
  Highest Revenue: AMZN ($513.98B)

Gross Profit Analysis:
  Note: Data not available for this metric.
```