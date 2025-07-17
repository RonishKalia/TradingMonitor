package com.tradingmonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.awt.Desktop;
import java.util.Map;
import java.util.stream.Collectors;

public class StockDashboard {

    public void generateDashboard(List<Stock> stocks, StockAnalyzer analyzer) throws IOException {
        String html = createHtml(stocks, analyzer);
        File file = new File("stock_dashboard.html");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(html);
        }
        Desktop.getDesktop().browse(file.toURI());
    }

    private String createHtml(List<Stock> stocks, StockAnalyzer analyzer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>Stock Dashboard</title>");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.0.0\"></script>");
        sb.append("<style>");
        sb.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; color: #333; }");
        sb.append("h1 { text-align: center; color: #2c3e50; }");
        sb.append(".stock-container { background-color: #fff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); margin: 20px; padding: 20px; }");
        sb.append(".stock-header { border-bottom: 2px solid #eaeaea; padding-bottom: 10px; margin-bottom: 20px; }");
        sb.append(".stock-header h2 { color: #3498db; }");
        sb.append(".stock-details { display: flex; justify-content: space-around; margin-bottom: 20px; }");
        sb.append(".detail { text-align: center; }");
        sb.append(".detail p { font-size: 1.2em; font-weight: bold; margin: 0; }");
        sb.append(".detail span { font-size: 0.9em; color: #7f8c8d; }");
        sb.append(".chart-container { display: flex; flex-wrap: wrap; justify-content: space-around; }");
        sb.append(".chart { width: 45%; margin-bottom: 20px; }");
        sb.append("</style>");
        sb.append("</head><body>");
        sb.append("<h1>Stock Analysis Dashboard</h1>");

        for (Stock stock : stocks) {
            sb.append("<div class='stock-container'>");
            sb.append("<div class='stock-header'>");
            sb.append("<h2>").append(stock.getName()).append(" (").append(stock.getSymbol()).append(")</h2>");
            sb.append("</div>");

            sb.append("<div class='stock-details'>");
            sb.append("<div class='detail'><p>").append(stock.getPeRatio()).append("</p><span>P/E Ratio</span></div>");
            sb.append("<div class='detail'><p>").append(analyzer.formatBigNumber(stock.getMarketCap())).append("</p><span>Market Cap</span></div>");
            sb.append("</div>");

            sb.append("<div class='chart-container'>");
            sb.append("<div class='chart'><canvas id='revenueChart-").append(stock.getSymbol()).append("'></canvas></div>");
            sb.append("<div class='chart'><canvas id='incomeChart-").append(stock.getSymbol()).append("'></canvas></div>");
            sb.append("<div class='chart'><canvas id='grossProfitChart-").append(stock.getSymbol()).append("'></canvas></div>");
            sb.append("<div class='chart'><canvas id='quarterlyRevenueChart-").append(stock.getSymbol()).append("'></canvas></div>");
            sb.append("<div class='chart'><canvas id='quarterlyIncomeChart-").append(stock.getSymbol()).append("'></canvas></div>");
            sb.append("<div class='chart'><canvas id='quarterlyGrossProfitChart-").append(stock.getSymbol()).append("'></canvas></div>");
            sb.append("</div>");
            sb.append("</div>");
        }

        sb.append("<script>");
        sb.append("Chart.register(ChartDataLabels);");
        for (Stock stock : stocks) {
            sb.append(createChartScript(stock.getSymbol(), "revenueChart", "Historical Revenue", stock.getHistoricalRevenue(), analyzer));
            sb.append(createChartScript(stock.getSymbol(), "incomeChart", "Historical Net Income", stock.getHistoricalNetIncome(), analyzer));
            sb.append(createChartScript(stock.getSymbol(), "grossProfitChart", "Historical Gross Profit", stock.getHistoricalGrossProfit(), analyzer));
            sb.append(createChartScript(stock.getSymbol(), "quarterlyRevenueChart", "Quarterly Revenue", stock.getQuarterlyRevenue(), analyzer));
            sb.append(createChartScript(stock.getSymbol(), "quarterlyIncomeChart", "Quarterly Net Income", stock.getQuarterlyNetIncome(), analyzer));
            sb.append(createChartScript(stock.getSymbol(), "quarterlyGrossProfitChart", "Quarterly Gross Profit", stock.getQuarterlyGrossProfit(), analyzer));
        }
        sb.append("</script>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String createChartScript(String symbol, String chartId, String label, Map<?, ?> data, StockAnalyzer analyzer) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        List<Map.Entry<?, ?>> entries = new ArrayList<>(data.entrySet());
        entries.sort((e1, e2) -> ((Comparable) e1.getKey()).compareTo(e2.getKey()));

        String labels = entries.stream().map(e -> String.valueOf(e.getKey())).collect(Collectors.joining("', '", "'", "'"));
        String values = entries.stream()
                .map(e -> ((BigDecimal) e.getValue()).divide(BigDecimal.valueOf(1000000)))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        
        List<String> growthPercentages = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            BigDecimal growth = null;
            if (i > 0) {
                growth = analyzer.calculateGrowth((BigDecimal) entries.get(i).getValue(), (BigDecimal) entries.get(i-1).getValue());
            }
            if (growth != null) {
                growthPercentages.add(String.format("'%.2f%%'", growth));
            } else {
                growthPercentages.add("null");
            }
        }
        String growthData = String.join(", ", growthPercentages);

        return String.format(
            "new Chart(document.getElementById('%s-%s'), {" +
            "type: 'bar'," +
            "data: {" +
            "labels: [%s]," +
            "datasets: [{" +
            "label: '%s (in millions)'," +
            "data: [%s]," +
            "backgroundColor: 'rgba(52, 152, 219, 0.5)'," +
            "borderColor: 'rgba(52, 152, 219, 1)'," +
            "borderWidth: 1" +
            "}]" +
            "}," +
            "options: {" +
                "plugins: {" +
                    "datalabels: {" +
                        "anchor: 'end'," +
                        "align: 'top'," +
                        "formatter: function(value, context) {" +
                            "var growth = [%s][context.dataIndex];" +
                            "return growth ? growth : '';" +
                        "}" +
                    "}" +
                "}," +
                "scales: { y: { beginAtZero: true, ticks: { callback: function(value, index, values) { return value + 'M'; } } } } }" +
            "});",
            chartId, symbol, labels, label, values, growthData
        );
    }
}
