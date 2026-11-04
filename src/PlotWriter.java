import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public final class PlotWriter {
    private static final Color[] COLORS = {
        new Color(35, 92, 190), new Color(205, 100, 20), new Color(25, 135, 75)
    };

    private PlotWriter() { }

    public static void write(List<Experiment.Row> rows) throws IOException {
        Path directory = Path.of("results", "plots");
        Files.createDirectories(directory);
        List<Experiment.Row> dp = rows.stream().filter(row -> row.inputCase().equals("random-pair")).toList();
        BufferedImage dpImage = image(1_200, 800);
        Graphics2D graphics = graphics(dpImage);
        title(graphics, "Dynamic programming: average execution time", 100);
        drawPanel(graphics, dp, new String[]{"LCS", "Edit Distance"},
                115, 130, 1_000, 485, "Equal string lengths", "Length of each string (n)",
                new int[]{100, 500, 1_000}, bounds(dp));
        graphics.dispose();
        ImageIO.write(dpImage, "png", directory.resolve("dp_time_vs_size.png").toFile());

        List<Experiment.Row> matching = rows.stream()
                .filter(row -> row.inputCase().equals("present") || row.inputCase().equals("absent")).toList();
        BufferedImage matchingImage = image(1_400, 800);
        graphics = graphics(matchingImage);
        title(graphics, "String matching: average execution time", 85);
        double[] range = bounds(matching);
        for (int panel = 0; panel < 2; panel++) {
            String inputCase = panel == 0 ? "present" : "absent";
            List<Experiment.Row> selected = matching.stream()
                    .filter(row -> row.inputCase().equals(inputCase)).toList();
            drawPanel(graphics, selected, new String[]{"Naive", "KMP", "Rabin-Karp"},
                    105 + panel * 690, 130, 525, 485, "Pattern " + inputCase + " (m = 32)",
                    "Text length (n)", new int[]{10_000, 50_000, 100_000}, range);
        }
        graphics.dispose();
        ImageIO.write(matchingImage, "png", directory.resolve("matching_time_vs_length.png").toFile());
    }

    private static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        return graphics;
    }

    private static void title(Graphics2D graphics, String title, int left) {
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        graphics.drawString(title, left, 45);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        graphics.drawString("Mean of 5 full algorithm calls; both axes logarithmic.", left, 77);
    }

    private static double[] bounds(List<Experiment.Row> rows) {
        double minimum = rows.stream().mapToDouble(row -> row.averageNs() / 1_000.0).min().orElseThrow();
        double maximum = rows.stream().mapToDouble(row -> row.averageNs() / 1_000.0).max().orElseThrow();
        double low = Math.floor(Math.log10(Math.max(minimum, 0.001)));
        double high = Math.max(low + 1, Math.ceil(Math.log10(maximum)));
        return new double[]{low, high};
    }

    private static void drawPanel(Graphics2D graphics, List<Experiment.Row> rows, String[] algorithms,
                                  int left, int top, int width, int height, String title, String xLabel,
                                  int[] ticks, double[] range) {
        double lowX = Math.log10(ticks[0]);
        double highX = Math.log10(ticks[ticks.length - 1]);
        graphics.setStroke(new BasicStroke(1));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        graphics.setColor(Color.BLACK);
        centered(graphics, title, left + width / 2, top - 22);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        for (int tick : ticks) {
            int x = coordinate(Math.log10(tick), lowX, highX, left, width);
            graphics.setColor(new Color(225, 228, 232));
            graphics.drawLine(x, top, x, top + height);
            graphics.setColor(Color.DARK_GRAY);
            centered(graphics, Integer.toString(tick), x, top + height + 29);
        }
        for (double exponent = range[0]; exponent <= range[1]; exponent++) {
            int y = top + height - coordinate(exponent, range[0], range[1], 0, height);
            graphics.setColor(new Color(225, 228, 232));
            graphics.drawLine(left, y, left + width, y);
            graphics.setColor(Color.DARK_GRAY);
            String label = String.format(Locale.US, exponent < 0 ? "%.2f" : "%.0f", Math.pow(10, exponent));
            graphics.drawString(label, left - 13 - graphics.getFontMetrics().stringWidth(label), y + 6);
        }
        graphics.setColor(Color.DARK_GRAY);
        graphics.drawRect(left, top, width, height);
        centered(graphics, xLabel, left + width / 2, top + height + 65);
        graphics.rotate(-Math.PI / 2);
        centered(graphics, "Average time (microseconds)", -(top + height / 2), left - 70);
        graphics.rotate(Math.PI / 2);
        for (int series = 0; series < algorithms.length; series++) {
            graphics.setColor(COLORS[series]);
            graphics.setStroke(new BasicStroke(2.5f));
            int previousX = -1;
            int previousY = -1;
            for (Experiment.Row row : rows) {
                if (!row.algorithm().equals(algorithms[series])) continue;
                int x = coordinate(Math.log10(row.n()), lowX, highX, left, width);
                int y = top + height - coordinate(Math.log10(row.averageNs() / 1_000.0),
                        range[0], range[1], 0, height);
                if (previousX != -1) graphics.drawLine(previousX, previousY, x, y);
                graphics.fillOval(x - 5, y - 5, 10, 10);
                previousX = x;
                previousY = y;
            }
            int legendX = left + series * (width / algorithms.length);
            graphics.drawLine(legendX, top + height + 110, legendX + 28, top + height + 110);
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawString(algorithms[series], legendX + 37, top + height + 116);
        }
    }

    private static void centered(Graphics2D graphics, String text, int x, int y) {
        graphics.drawString(text, x - graphics.getFontMetrics().stringWidth(text) / 2, y);
    }

    private static int coordinate(double value, double minimum, double maximum, int start, int length) {
        return start + (int) Math.round((value - minimum) / (maximum - minimum) * length);
    }
}
