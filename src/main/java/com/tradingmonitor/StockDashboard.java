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
        sb.append("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/css/materialize.min.css\">");
        sb.append("<link href=\"https://fonts.googleapis.com/icon?family=Material+Icons\" rel=\"stylesheet\">");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.0.0\"></script>");
        sb.append("<style>");
        sb.append("body { background-color: #eef1f5; }");
        sb.append("nav { background-color: #26a69a; }");
        sb.append(".container { margin-top: 30px; }");
        sb.append(".card { border-radius: 15px; box-shadow: 0 5px 15px rgba(0,0,0,0.08); }");
        sb.append(".card .card-content { padding: 30px; }");
        sb.append(".card .card-title { font-weight: 400; }");
        sb.append(".stat-value { font-size: 2.5rem; font-weight: 500; }");
        sb.append(".stat-label { font-size: 1rem; color: #9e9e9e; }");
        sb.append(".tabs .tab a { color: #26a69a; }");
        sb.append(".tabs .tab a.active { font-weight: 500; }");
        sb.append(".tabs .indicator { background-color: #26a69a; }");
        sb.append(".chart-wrapper { background: #fff; border-radius: 10px; padding: 20px; margin-top: 15px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }");
        sb.append("</style>");
        sb.append("</head><body>");

        sb.append("<nav><div class=\"nav-wrapper\"><a href=\"#\" class=\"brand-logo center\">Stock Analysis</a></div></nav>");

        sb.append("<div class=\"container\">");

        for (Stock stock : stocks) {
            sb.append("<div class=\"card\">");
            sb.append("<div class=\"card-content\">");
            sb.append("<span class=\"card-title\">").append(stock.getName()).append(" (").append(stock.getSymbol()).append(")</span>");
            sb.append("<div class=\"row\" style=\"margin-top: 20px;\">");
            sb.append("<div class=\"col s6 center-align\"><p class=\"stat-value\">").append(stock.getPeRatio()).append("</p><p class=\"stat-label\">P/E Ratio</p></div>");
            sb.append("<div class=\"col s6 center-align\"><p class=\"stat-value\">").append(analyzer.formatBigNumber(stock.getMarketCap())).append("</p><p class=\"stat-label\">Market Cap</p></div>");
            sb.append("</div>");
            sb.append("</div>");

            sb.append("<div class=\"card-tabs\">");
            sb.append("<ul id=\"tabs-").append(stock.getSymbol()).append("\" class=\"tabs tabs-fixed-width\">");
            sb.append("<li class=\"tab\"><a class=\"active\" href=\"#historical-").append(stock.getSymbol()).append("\">Historical</a></li>");
            sb.append("<li class=\"tab\"><a href=\"#quarterly-").append(stock.getSymbol()).append("\">Quarterly</a></li>");
            sb.append("</ul>");
            sb.append("</div>");

            sb.append("<div class=\"card-content grey lighten-4\">");
            sb.append("<div id=\"historical-").append(stock.getSymbol()).append("\">").append(createChartTabs(stock, "historical")).append("</div>");
            sb.append("<div id=\"quarterly-").append(stock.getSymbol()).append("\">").append(createChartTabs(stock, "quarterly")).append("</div>");
            sb.append("</div>");
            sb.append("</div>");
        }

        sb.append("</div>");

        sb.append("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/js/materialize.min.js\"></script>");
        sb.append("<script>");
        sb.append("Chart.register(ChartDataLabels);");

        sb.append("document.addEventListener('DOMContentLoaded', function() {");
        for (Stock stock : stocks) {
            String symbol = stock.getSymbol();
            sb.append("var tabs_").append(symbol).append(" = document.getElementById('tabs-").append(symbol).append("');");
            sb.append("M.Tabs.init(tabs_").append(symbol).append(", { onShow: function(tab) {");
            sb.append("if (tab.id.startsWith('quarterly-')) { initQuarterlyCharts_").append(symbol).append("(); }");
            sb.append("else if (tab.id.startsWith('historical-')) { initHistoricalCharts_").append(symbol).append("(); }");
            sb.append("} });");
            sb.append("initHistoricalCharts_").append(symbol).append("();");
        }
        sb.append("});");

        for (Stock stock : stocks) {
            String symbol = stock.getSymbol();
            sb.append("function initHistoricalCharts_").append(symbol).append("() {");
            sb.append("if (Chart.getChart('historical-revenue-chart-").append(symbol).append("')) return;");
            sb.append(createChartScript(symbol, "historical-revenue-chart", "Historical Revenue", stock.getHistoricalRevenue(), analyzer, "'rgba(75, 192, 192, 0.6)'", "'rgba(75, 192, 192, 1)'"));
            sb.append(createChartScript(symbol, "historical-income-chart", "Historical Net Income", stock.getHistoricalNetIncome(), analyzer, "'rgba(255, 159, 64, 0.6)'", "'rgba(255, 159, 64, 1)'"));
            sb.append(createChartScript(symbol, "historical-gross-profit-chart", "Historical Gross Profit", stock.getHistoricalGrossProfit(), analyzer, "'rgba(153, 102, 255, 0.6)'", "'rgba(153, 102, 255, 1)'"));
            sb.append("}");

            sb.append("function initQuarterlyCharts_").append(symbol).append("() {");
            sb.append("if (Chart.getChart('quarterly-revenue-chart-").append(symbol).append("')) return;");
            sb.append(createChartScript(symbol, "quarterly-revenue-chart", "Quarterly Revenue", stock.getQuarterlyRevenue(), analyzer, "'rgba(75, 192, 192, 0.6)'", "'rgba(75, 192, 192, 1)'"));
            sb.append(createChartScript(symbol, "quarterly-income-chart", "Quarterly Net Income", stock.getQuarterlyNetIncome(), analyzer, "'rgba(255, 159, 64, 0.6)'", "'rgba(255, 159, 64, 1)'"));
            sb.append(createChartScript(symbol, "quarterly-gross-profit-chart", "Quarterly Gross Profit", stock.getQuarterlyGrossProfit(), analyzer, "'rgba(153, 102, 255, 0.6)'", "'rgba(153, 102, 255, 1)'"));
            sb.append("}");
        }

        sb.append("</script>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String createChartTabs(Stock stock, String type) {
        StringBuilder sb = new StringBuilder();
        String symbol = stock.getSymbol();
        sb.append("<div class=\"row\">");
        sb.append("<div class=\"col s12 l6\"><div class=\"chart-wrapper\"><canvas id=\"").append(type).append("-revenue-chart-").append(symbol).append("\"></canvas></div></div>");
        sb.append("<div class=\"col s12 l6\"><div class=\"chart-wrapper\"><canvas id=\"").append(type).append("-income-chart-").append(symbol).append("\"></canvas></div></div>");
        sb.append("</div>");
        sb.append("<div class=\"row\">");
        sb.append("<div class=\"col s12\"><div class=\"chart-wrapper\"><canvas id=\"").append(type).append("-gross-profit-chart-").append(symbol).append("\"></canvas></div></div>");
        sb.append("</div>");
        return sb.toString();
    }

    private String createChartScript(String symbol, String chartId, String label, Map<?, ?> data, StockAnalyzer analyzer, String bgColor, String borderColor) {
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
        int lookback = label.contains("Quarterly") ? 4 : 1;
        for (int i = 0; i < entries.size(); i++) {
            BigDecimal growth = null;
            if (i >= lookback) {
                growth = analyzer.calculateGrowth((BigDecimal) entries.get(i).getValue(), (BigDecimal) entries.get(i - lookback).getValue());
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
            "backgroundColor: %s," +
            "borderColor: %s," +
            "borderWidth: 1" +
            "}]" +
            "}," +
            "options: {" +
                "responsive: true," +
                "maintainAspectRatio: true," +
                "plugins: {" +
                    "datalabels: {" +
                        "anchor: 'end'," +
                        "align: 'top'," +
                        "color: '#555'," +
                        "font: { weight: 'bold' }," +
                        "formatter: function(value, context) {" +
                            "var growth = [%s][context.dataIndex];" +
                            "return growth !== 'null' ? growth : '';" +
                        "}" +
                    "}" +
                "}," +
                "scales: { y: { beginAtZero: true, ticks: { callback: function(value, index, values) { return value + 'M'; } } } } }" +
            "});",
            chartId, symbol, labels, label, values, bgColor, borderColor, growthData
        );
    }
}
