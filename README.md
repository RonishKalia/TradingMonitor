# TradingMonitor

A Java application that analyzes stock data from various exchanges, providing insights into P/E ratios, revenue, and gross profit metrics.

## Features

- **Multi-Exchange Support**: Analyze stocks from NYSE, NASDAQ, and S&P 500
- **Comprehensive Metrics**: P/E ratios, revenue, gross profit, market cap, and volume
- **Real-time Data**: Uses Yahoo Finance API for live stock data
- **Detailed Analysis**: Provides summary statistics and individual stock breakdowns
- **Rate Limiting**: Built-in protection against API rate limits

## Supported Exchanges

- **NYSE**: New York Stock Exchange (35 major stocks)
- **NASDAQ**: NASDAQ Composite (32 major stocks)
- **SP500**: S&P 500 subset (27 major stocks)

## Installation

1. Clone the repository
2. Ensure you have Java 8+ installed
3. Build the project:
   ```bash
   ./gradlew build
   ```

## Usage

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
StockAnalyzer analyzer = new StockAnalyzer();

// Analyze a specific exchange
try {
    List<StockAnalyzer.StockData> stockData = analyzer.analyzeExchange("NYSE");

    // Print analysis summary
    analyzer.printAnalysisSummary(stockData);

    // Access individual stock data
    for (StockAnalyzer.StockData stock : stockData) {
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

- **Yahoo Finance API**: For real-time stock data
- **Jackson**: JSON processing
- **Apache HttpClient**: HTTP requests
- **SLF4J**: Logging

## Testing

Run the test suite:

```bash
./gradlew test
```

## Notes

- The application includes rate limiting (100ms delay between requests) to respect API limits
- Some stocks may not have complete data available
- Internet connection is required for real-time data
- Data accuracy depends on Yahoo Finance API availability

## Example Output

```
=== TRADING MONITOR - STOCK ANALYSIS ===
Supported exchanges: [NYSE, NASDAQ, SP500]

Starting analysis of NASDAQ stocks...
Analyzing 32 stocks from NASDAQ...
✓ AAPL - P/E: 25.5, Revenue: $394.33B
✓ MSFT - P/E: 30.2, Revenue: $198.27B
...

=== STOCK ANALYSIS SUMMARY ===
Total stocks analyzed: 28

P/E Ratio Analysis:
  Average P/E: 28.45
  Lowest P/E: BRK-B (8.2)
  Highest P/E: TSLA (45.8)

Revenue Analysis:
  Total Revenue (all stocks): $2,847.56B
  Highest Revenue: AAPL ($394.33B)

Gross Profit Analysis:
  Note: Data not available for this metric.
```