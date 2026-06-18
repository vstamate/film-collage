package org.example;

import static java.lang.IO.println;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class Main {

    private static final int SCALE_FACTOR = 4;

    private static final int TOTAL_WIDTH = 2480 * SCALE_FACTOR;
    private static final int TOTAL_HEIGHT = 3508 * SCALE_FACTOR;

    private static final int GAP = 42 * SCALE_FACTOR;
    private static final int MARGIN = 128 * SCALE_FACTOR;
    private static final int FONT_SIZE = 32 * SCALE_FACTOR;

    private static final int TEXT_HEADER = 128 * SCALE_FACTOR + (int) (1d * GAP / 2d);

    static void main() {
        try {
            run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void run() throws IOException {

        println("Reading data");

        var film = "";
        var filmType = "";
        var ratioString = "";
        var ratio = 0d;
        var date = "";
        var roll = "";

        try (Scanner scanner = new Scanner(Paths.get("data.txt"))) {
            film = scanner.nextLine();
            filmType = scanner.nextLine();
            ratioString = scanner.nextLine();
            ratio = parseRatio(ratioString); // 3:2 -> 3 / 2
            date = scanner.nextLine();
            roll =  scanner.nextLine();
        } catch (Exception e) {
            println("Exception while reading data: " + e.getMessage());
            return;
        }

        var cols = filmType.equals("35mm") ? 5 : 3;
        var headerHeight = switch (ratioString) {
            case "3:2" -> 375 * SCALE_FACTOR;
            case "6:6" -> 250 * SCALE_FACTOR;
            case "6:4.5" -> 375 * SCALE_FACTOR;
            case "7:6" -> 375 * SCALE_FACTOR;
            default -> 375 * SCALE_FACTOR;
        };

        int thumbWidth = (TOTAL_WIDTH - 2 * MARGIN) / cols - GAP;
        int thumbHeight = (int) (1.0 * thumbWidth * (1 / ratio));

        int thumbFullWidth = thumbWidth + GAP;
        int thumbFullHeight = thumbHeight + GAP;

        var root = Path.of(System.getProperty("user.dir"));
        var photoPaths = Files.list(root)
                .filter(p -> !p.toString().contains("sheet"))
                .filter(p -> p.toString().matches("(?i).*\\.(jpg|jpeg|png|tiff)$"))
                .sorted()
                .map(Path::toFile)
                .toList();

        println(String.format("Found %d photos", photoPaths.size()));

        var photos = photoPaths.stream()
                .map(path -> createImage(path.toString(), thumbWidth, thumbHeight))
                .toList();

        var sheet = new BufferedImage(TOTAL_WIDTH, TOTAL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, TOTAL_WIDTH, TOTAL_HEIGHT);

        for (int i = 0; i < photos.size(); i++) {
            int col = i % cols;
            int row = i / cols;

            int x = MARGIN + col * thumbFullWidth + (int) (1.0 * GAP / 2.0);
            int y = headerHeight + MARGIN + row * thumbFullHeight + (int) (1.0 * GAP / 2.0);
            g.drawImage(photos.get(i), x, y, thumbWidth, thumbHeight, null);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Monospaced", Font.BOLD, FONT_SIZE));
        var textX = MARGIN + (int) (1d * GAP / 2d);
        g.drawString(String.format("Film: %s %s", film, filmType), textX, TEXT_HEADER);
        g.drawString(String.format("Date: %s", date), textX, TEXT_HEADER + FONT_SIZE);
        g.drawString(String.format("Roll: %s", roll), textX, TEXT_HEADER + FONT_SIZE * 2);

        var fileDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        ImageIO.write(sheet, "jpg", new File(String.format("%s_%s_sheet.jpg", fileDate, roll)));

        println("Done");
    }

    private static BufferedImage createImage(String path, int width, int height) {

        BufferedImage image;
        try {
            image = ImageIO.read(new File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BufferedImage scaled;
        int scaledW;
        int scaledH;

        if (image.getWidth() + 300 >= image.getHeight()) {
            double scale = Math.max((double) width / image.getWidth(), (double) height / image.getHeight());

            scaledW = (int) Math.round(image.getWidth()  * scale);
            scaledH = (int) Math.round(image.getHeight() * scale);

            scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(image, 0, 0, scaledW, scaledH, null);
            g.dispose();
        } else {
            double scale = Math.max((double) width / image.getHeight(), (double) height / image.getWidth());

            scaledW = (int) Math.round(image.getHeight() * scale);
            scaledH = (int) Math.round(image.getWidth() * scale);

            scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.translate(scaledW, 0);
            g.rotate(Math.PI / 2);
            g.drawImage(image, 0, 0, scaledH, scaledW, null);
            g.dispose();
        }

        int cropX = (scaledW - width) / 2;
        int cropY = (scaledH - height) / 2;
        return scaled.getSubimage(cropX, cropY, width, height);
    }

    private static double parseRatio(String ratioString) {
        var split = ratioString.split(":");
        return Double.parseDouble(split[0]) / Double.parseDouble(split[1]);
    }
}
