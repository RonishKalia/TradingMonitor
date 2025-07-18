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
        sb.append("<link href=\"https://fonts.googleapis.com/css2?family=Roboto+Mono:wght@300;400;500&display=swap\" rel=\"stylesheet\">");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>");
        sb.append("<script src=\"https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.0.0\"></script>");
        sb.append("<style>");
        sb.append("body { background-color: #282a36; font-family: 'Roboto Mono', monospace; color: #f8f8f2; }");
        sb.append("nav { background-color: #21222c; box-shadow: none; }");
        sb.append(".container { max-width: 1200px; }");
        sb.append(".card { background-color: #44475a; border-radius: 16px; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4); border: 1px solid #6272a4; flex: 1 1 100%; margin: 10px; display: flex; flex-direction: column; }");
        sb.append(".card .card-content { padding: 32px; flex-grow: 1; }");
        sb.append(".card .card-title { font-size: 1.5rem; font-weight: 500; color: #ff79c6; }");
        sb.append(".stat-value { font-size: 2.2rem; font-weight: 500; color: #8be9fd; }");
        sb.append(".stat-label { font-size: 0.9rem; color: #6272a4; text-transform: uppercase; }");
        sb.append(".tabs { background-color: transparent; }");
        sb.append(".tabs .tab a { color: #bd93f9; transition: background-color 0.3s ease; border-radius: 8px; }");
        sb.append(".tabs .tab a:hover { background-color: rgba(98, 114, 164, 0.2); }");
        sb.append(".tabs .tab a.active { background-color: #6272a4; color: #f8f8f2; font-weight: 500; }");
        sb.append(".tabs .indicator { display: none; }");
        sb.append(".chart-wrapper { background: #282a36; border-radius: 12px; padding: 24px; margin-top: 20px; height: 40vh; max-height: 600px; border: 1px solid #6272a4; flex-grow: 1; }");
        sb.append(".chart-title { text-align: center; font-size: 1.1rem; font-weight: 500; color: #bd93f9; margin-bottom: 15px; }");
        sb.append(".tab-content { display: none; } .tab-content.active { display: block; }");
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
            sb.append("<ul id=\"tabs-").append(stock.getSymbol()).append("\" class=\"tabs\">");
            sb.append("<li class=\"tab col s6\"><a class=\"active\" href=\"#historical-").append(stock.getSymbol()).append("\">Historical</a></li>");
            sb.append("<li class=\"tab col s6\"><a href=\"#quarterly-").append(stock.getSymbol()).append("\">Quarterly</a></li>");
            sb.append("</ul>");
            sb.append("</div>");

            sb.append("<div class=\"card-content grey darken-3\" style=\"padding-top: 0;\">");
            sb.append("<div id=\"historical-").append(stock.getSymbol()).append("\" class=\"tab-content active\">").append(createChartTabs(stock, "historical")).append("</div>");
            sb.append("<div id=\"quarterly-").append(stock.getSymbol()).append("\" class=\"tab-content\">").append(createChartTabs(stock, "quarterly")).append("</div>");
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
            sb.append("var tabsInstance_").append(symbol).append(" = M.Tabs.init(tabs_").append(symbol).append(", {});");
            sb.append("tabs_").append(symbol).append(".addEventListener('click', function(e) {");
            sb.append("if (e.target.tagName === 'A') {");
            sb.append("var tabId = e.target.getAttribute('href');");
            sb.append("document.querySelectorAll('#' + '").append(symbol).append("' + ' .tab-content').forEach(function(tc) { tc.style.display = 'none'; });");
            sb.append("document.querySelector(tabId).style.display = 'block';");
            sb.append("if (tabId.includes('quarterly')) { initQuarterlyCharts_").append(symbol).append("(); }");
            sb.append("}");
            sb.append("});");
            sb.append("initHistoricalCharts_").append(symbol).append("();");
            sb.append("document.querySelector('#historical-").append(symbol).append("').style.display = 'block';");
        }
        sb.append("});");

        for (Stock stock : stocks) {
            String symbol = stock.getSymbol();
            sb.append("function initHistoricalCharts_").append(symbol).append("() {");
            sb.append("if (Chart.getChart('historical-revenue-chart-").append(symbol).append("')) return;");
            sb.append(createChartScript(symbol, "historical-revenue-chart", "Historical Revenue", stock.getHistoricalRevenue(), analyzer, "'#ff79c6'", "'#ff79c6'"));
            sb.append(createChartScript(symbol, "historical-income-chart", "Historical Net Income", stock.getHistoricalNetIncome(), analyzer, "'#82b184'", "'#82b184'"));
            sb.append(createChartScript(symbol, "historical-gross-profit-chart", "Historical Gross Profit", stock.getHistoricalGrossProfit(), analyzer, "'#bd93f9'", "'#bd93f9'"));
            sb.append("}");

            sb.append("function initQuarterlyCharts_").append(symbol).append("() {");
            sb.append("if (Chart.getChart('quarterly-revenue-chart-").append(symbol).append("')) return;");
            sb.append(createChartScript(symbol, "quarterly-revenue-chart", "Quarterly Revenue", stock.getQuarterlyRevenue(), analyzer, "'#ff79c6'", "'#ff79c6'"));
            sb.append(createChartScript(symbol, "quarterly-income-chart", "Quarterly Net Income", stock.getQuarterlyNetIncome(), analyzer, "'#82b184'", "'#82b184'"));
            sb.append(createChartScript(symbol, "quarterly-gross-profit-chart", "Quarterly Gross Profit", stock.getQuarterlyGrossProfit(), analyzer, "'#bd93f9'", "'#bd93f9'"));
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
        sb.append("<div class=\"col s12 l6\"><div class=\"chart-wrapper\"><p class=\"chart-title\">Revenue</p><canvas id=\"").append(type).append("-revenue-chart-").append(symbol).append("\"></canvas></div></div>");
        sb.append("<div class=\"col s12 l6\"><div class=\"chart-wrapper\"><p class=\"chart-title\">Net Income</p><canvas id=\"").append(type).append("-income-chart-").append(symbol).append("\"></canvas></div></div>");
        sb.append("</div>");
        sb.append("<div class=\"row\">");
        sb.append("<div class=\"col s12 l6 offset-l3\"><div class=\"chart-wrapper\"><p class=\"chart-title\">Gross Profit</p><canvas id=\"").append(type).append("-gross-profit-chart-").append(symbol).append("\"></canvas></div></div>");
        sb.append("</div>");
        return sb.toString();
    }

    private String createChartScript(String symbol, String chartId, String label, Map<?, ?> data, StockAnalyzer analyzer, String bgColor, String borderColor) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        final List<Map.Entry<?, ?>> entries = new ArrayList<>(data.entrySet());
        List<Map.Entry<?, ?>> filteredEntries = new ArrayList<>();

        if (label.contains("Quarterly")) {
            Map<String, List<Map.Entry<?, ?>>> groupedByQuarter = entries.stream()
                .collect(Collectors.groupingBy(e -> ((String) e.getKey()).split(":")[1]));

            List<Map.Entry<?, ?>> lastEntries = new ArrayList<>();
            for (List<Map.Entry<?, ?>> quarterEntries : groupedByQuarter.values()) {
                quarterEntries.sort(Comparator.comparing(e -> (String) e.getKey(), Comparator.reverseOrder()));
                int size = quarterEntries.size();
                if (size > 3) {
                    lastEntries.addAll(quarterEntries.subList(0, 3));
                } else {
                    lastEntries.addAll(quarterEntries);
                }
            }

            Comparator<Map.Entry<?, ?>> finalComparator = (e1, e2) -> {
                String[] parts1 = ((String) e1.getKey()).split(":");
                String[] parts2 = ((String) e2.getKey()).split(":");
                String fiscalPeriod1 = parts1[1];
                String fiscalPeriod2 = parts2[1];
                String fiscalYear1 = parts1[0];
                String fiscalYear2 = parts2[0];

                int quarter1 = Integer.parseInt(fiscalPeriod1.substring(1));
                int quarter2 = Integer.parseInt(fiscalPeriod2.substring(1));

                if (quarter1 != quarter2) {
                    return Integer.compare(quarter1, quarter2);
                } else {
                    return fiscalYear1.compareTo(fiscalYear2);
                }
            };
            lastEntries.sort(finalComparator);
            filteredEntries.addAll(lastEntries);
        } else {
            entries.sort((e1, e2) -> ((Comparable) e1.getKey()).compareTo(e2.getKey()));
            filteredEntries.addAll(entries);
        }

        String labels;
        if (label.contains("Quarterly")) {
            labels = filteredEntries.stream().map(e -> {
                String[] parts = ((String) e.getKey()).split(":");
                String fiscalYear = parts[0];
                String fiscalPeriod = parts[1];
                return String.format("\"%s %s\"", fiscalPeriod, fiscalYear);
            }).collect(Collectors.joining(", "));
        } else {
            labels = filteredEntries.stream().map(e -> String.format("\"%s\"", e.getKey())).collect(Collectors.joining(", "));
        }

        String values = filteredEntries.stream()
                .map(e -> ((BigDecimal) e.getValue()).divide(BigDecimal.valueOf(1000000)))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        
        List<String> growthPercentages = new ArrayList<>();
        for (int i = 0; i < filteredEntries.size(); i++) {
            BigDecimal growth = null;
            if (i > 0) {
                if (label.contains("Quarterly")) {
                    String[] currentKeyParts = ((String) filteredEntries.get(i).getKey()).split(":");
                    String[] prevKeyParts = ((String) filteredEntries.get(i - 1).getKey()).split(":");
                    String currentFiscalPeriod = currentKeyParts[1];
                    String prevFiscalPeriod = prevKeyParts[1];

                    if (currentFiscalPeriod.equals(prevFiscalPeriod)) {
                        growth = analyzer.calculateGrowth((BigDecimal) filteredEntries.get(i).getValue(), (BigDecimal) filteredEntries.get(i - 1).getValue());
                    }
                } else {
                    growth = analyzer.calculateGrowth((BigDecimal) filteredEntries.get(i).getValue(), (BigDecimal) filteredEntries.get(i - 1).getValue());
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
                    "tooltip: { " +
                        "backgroundColor: '#21222c', titleFont: { size: 14 }, bodyFont: { size: 12 }, footerFont: { size: 10 }, padding: 12, boxPadding: 6 " +
                    "}," +
                    "datalabels: {" +
                        "anchor: 'end'," +
                        "align: 'top'," +
                        "color: '#f8f8f2'," +
                        "font: { weight: '500' }," +
                        "formatter: function(value, context) {" +
                            "var growth = [%s][context.dataIndex];" +
                            "return growth !== 'null' ? growth : '';" +
                        "}" +
                    "}" +
                "}," +
                "scales: { " +
                    "y: { beginAtZero: true, grid: { color: '#6272a4', drawBorder: false }, ticks: { color: '#f8f8f2', callback: function(value) { return value + 'M'; } } }, " +
                    "x: { grid: { display: false }, ticks: { color: '#f8f8f2' } } " +
                "}" +
            "}" +
            "});",
            chartId, symbol, labels, label, values, bgColor, borderColor, growthData
        );
    }
}
