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
        sb.append("<link href=\"https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500&display=swap\" rel=\"stylesheet\">");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.0.0\"></script>");
        sb.append("<style>");
        sb.append("body { background-color: #f8f9fa; font-family: 'Roboto', sans-serif; }");
        sb.append("nav { background-color: #1e2a38; box-shadow: none; }");
        sb.append(".container { width: 90%; max-width: 1800px; margin-top: 40px; margin-bottom: 40px; }");
        sb.append(".card { border-radius: 16px; box-shadow: 0 8px 24px rgba(149, 157, 165, 0.2); border: none; }");
        sb.append(".card .card-content { padding: 32px; }");
        sb.append(".card .card-title { font-weight: 500; }");
        sb.append(".stat-value { font-size: 2.2rem; font-weight: 500; color: #2c3e50; }");
        sb.append(".stat-label { font-size: 0.9rem; color: #90a4ae; text-transform: uppercase; }");
        sb.append(".tabs .tab a { color: #1e2a38; }");
        sb.append(".tabs .tab a.active { font-weight: 500; }");
        sb.append(".tabs .indicator { background-color: #2962ff; }");
        sb.append(".chart-wrapper { background: #ffffff; border-radius: 12px; padding: 24px; margin-top: 20px; height: 450px; }");
        sb.append("</style>");
        sb.append("</head><body>");

        sb.append("<nav><div class=\"nav-wrapper\"><a href=\"#\" class=\"brand-logo center\">Stock Analysis</a></div></nav>");

        sb.append("<div class=\"container\">");

        for (Stock stock : stocks) {
            sb.append("<div class=\"card\" style=\"margin-bottom: 40px;\">");
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

            sb.append("<div class=\"card-content grey lighten-5\">");
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
            sb.append(createChartScript(symbol, "historical-revenue-chart", "Historical Revenue", stock.getHistoricalRevenue(), analyzer, "'#42a5f5'", "'#1e88e5'"));
            sb.append(createChartScript(symbol, "historical-income-chart", "Historical Net Income", stock.getHistoricalNetIncome(), analyzer, "'#66bb6a'", "'#43a047'"));
            sb.append(createChartScript(symbol, "historical-gross-profit-chart", "Historical Gross Profit", stock.getHistoricalGrossProfit(), analyzer, "'#ab47bc'", "'#8e24aa'"));
            sb.append("}");

            sb.append("function initQuarterlyCharts_").append(symbol).append("() {");
            sb.append("if (Chart.getChart('quarterly-revenue-chart-").append(symbol).append("')) return;");
            sb.append(createChartScript(symbol, "quarterly-revenue-chart", "Quarterly Revenue", stock.getQuarterlyRevenue(), analyzer, "'#42a5f5'", "'#1e88e5'"));
            sb.append(createChartScript(symbol, "quarterly-income-chart", "Quarterly Net Income", stock.getQuarterlyNetIncome(), analyzer, "'#66bb6a'", "'#43a047'"));
            sb.append(createChartScript(symbol, "quarterly-gross-profit-chart", "Quarterly Gross Profit", stock.getQuarterlyGrossProfit(), analyzer, "'#ab47bc'", "'#8e24aa'"));
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
        sb.append("<div class=\"col s12 l6 offset-l3\"><div class=\"chart-wrapper\"><canvas id=\"").append(type).append("-gross-profit-chart-").append(symbol).append("\"></canvas></div></div>");
        sb.append("</div>");
        return sb.toString();
    }

    private String createChartScript(String symbol, String chartId, String label, Map<?, ?> data, StockAnalyzer analyzer, String bgColor, String borderColor) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        List<Map.Entry<?, ?>> entries = new ArrayList<>(data.entrySet());
        
        if (label.contains("Quarterly")) {
            Comparator<Map.Entry<?, ?>> customComparator = (e1, e2) -> {
                String dateStr1 = (String) e1.getKey();
                String dateStr2 = (String) e2.getKey();
                int month1 = Integer.parseInt(dateStr1.substring(5, 7));
                int month2 = Integer.parseInt(dateStr2.substring(5, 7));
                int year1 = Integer.parseInt(dateStr1.substring(0, 4));
                int year2 = Integer.parseInt(dateStr2.substring(0, 4));
                int quarter1 = (month1 - 1) / 3 + 1;
                int quarter2 = (month2 - 1) / 3 + 1;

                if (quarter1 != quarter2) {
                    return Integer.compare(quarter1, quarter2);
                } else {
                    return Integer.compare(year1, year2);
                }
            };
            entries.sort(customComparator);
        } else {
            entries.sort((e1, e2) -> ((Comparable) e1.getKey()).compareTo(e2.getKey()));
        }

        String labels;
        if (label.contains("Quarterly")) {
            labels = entries.stream().map(e -> {
                String dateStr = (String) e.getKey();
                int month = Integer.parseInt(dateStr.substring(5, 7));
                int year = Integer.parseInt(dateStr.substring(0, 4));
                int quarter = (month - 1) / 3 + 1;
                return String.format("\"Q%d %d\"", quarter, year);
            }).collect(Collectors.joining(", "));
        } else {
            labels = entries.stream().map(e -> String.format("\"%s\"", e.getKey())).collect(Collectors.joining(", "));
        }

        String values = entries.stream()
                .map(e -> ((BigDecimal) e.getValue()).divide(BigDecimal.valueOf(1000000)))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        
        List<String> growthPercentages = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            BigDecimal growth = null;
            if (i > 0) {
                boolean shouldCalculate = true;
                if (label.contains("Quarterly")) {
                    String dateStr1 = (String) entries.get(i).getKey();
                    String dateStr2 = (String) entries.get(i - 1).getKey();
                    int month1 = Integer.parseInt(dateStr1.substring(5, 7));
                    int month2 = Integer.parseInt(dateStr2.substring(5, 7));
                    int quarter1 = (month1 - 1) / 3 + 1;
                    int quarter2 = (month2 - 1) / 3 + 1;
                    if (quarter1 != quarter2) {
                        shouldCalculate = false;
                    }
                }

                if (shouldCalculate) {
                    growth = analyzer.calculateGrowth((BigDecimal) entries.get(i).getValue(), (BigDecimal) entries.get(i - 1).getValue());
                }
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
            "borderWidth: 1," +
            "borderRadius: 8," +
            "barPercentage: 0.7," +
            "categoryPercentage: 0.8" +
            "}]" +
            "}," +
            "options: {" +
                "responsive: true," +
                "maintainAspectRatio: false," +
                "layout: { padding: { top: 30 } }," +
                "plugins: {" +
                    "legend: { display: false }," +
                    "datalabels: {" +
                        "anchor: 'end'," +
                        "align: 'top'," +
                        "color: '#555'," +
                        "font: { weight: '500' }," +
                        "formatter: function(value, context) {" +
                            "var growth = [%s][context.dataIndex];" +
                            "return growth !== 'null' ? growth : '';" +
                        "}" +
                    "}" +
                "}," +
                "scales: { " +
                    "y: { beginAtZero: true, grid: { drawBorder: false }, ticks: { callback: function(value) { return value + 'M'; } } }, " +
                    "x: { grid: { display: false } } " +
                "}" +
            "}" +
            "});",
            chartId, symbol, labels, label, values, bgColor, borderColor, growthData
        );
    }
}
