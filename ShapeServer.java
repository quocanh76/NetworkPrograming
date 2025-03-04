import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ShapeServer {
    public static void main(String[] args) {
        int port = 8080;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Shape Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            // Read request
            String request = in.readLine();
            System.out.println("Received request: " + request);

            if (request != null) {
                // Generate the image
                BufferedImage image = processRequest(request);

                // Send image to client
                if (image != null) {
                    ImageIO.write(image, "PNG", out);
                    System.out.println("Image sent to client.");
                } else {
                    System.out.println("Error: Could not create image.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage processRequest(String request) {
        String[] parts = request.split(",");
        String shape = parts[0];
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing for smooth drawing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Default color (black)
        Color color = Color.BLACK;
        try {
            color = Color.decode(parts[3]);
        } catch (Exception ignored) {}

        g2d.setColor(color);

        if ("circle".equalsIgnoreCase(shape) || "square".equalsIgnoreCase(shape)) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int size = Integer.parseInt(parts[4]);
            int rotation = Integer.parseInt(parts[5]);

            if ("square".equalsIgnoreCase(shape)) {
                drawRotatedRectangle(g2d, x, y, size, size, rotation);
            } else {
                g2d.fillOval(x, y, size, size);
            }

        } else if ("rectangle".equalsIgnoreCase(shape)) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int width = Integer.parseInt(parts[4]);
            int height = Integer.parseInt(parts[5]);
            int rotation = Integer.parseInt(parts[6]);
            drawRotatedRectangle(g2d, x, y, width, height, rotation);

        } else if ("polygon".equalsIgnoreCase(shape)) {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int sides = Integer.parseInt(parts[4]);
            int radius = Integer.parseInt(parts[5]);
            int rotation = Integer.parseInt(parts[6]);
            drawPolygon(g2d, x, y, sides, radius, rotation);

        } else if ("line".equalsIgnoreCase(shape)) {
            int startX = Integer.parseInt(parts[1]);
            int startY = Integer.parseInt(parts[2]);
            int endX = Integer.parseInt(parts[3]);
            int endY = Integer.parseInt(parts[4]);
            Color color2 = Color.BLACK; // Default

            try {
                color2 = Color.decode(parts[5]); // Extract color from request
            } catch (Exception ignored) {}

            g2d.setColor(color2); // Set the color before drawing
            g2d.setStroke(new BasicStroke(3)); // Line thickness
            g2d.drawLine(startX, startY, endX, endY);
        }


        g2d.dispose();
        return image;
    }

    // Draws a rotated rectangle (including squares)
    private static void drawRotatedRectangle(Graphics2D g2d, int x, int y, int width, int height, int angle) {
        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(Math.toRadians(angle), x + width / 2.0, y + height / 2.0);
        g2d.fillRect(x, y, width, height);
        g2d.setTransform(oldTransform);
    }

    // Draws a regular polygon
    private static void drawPolygon(Graphics2D g2d, int x, int y, int sides, int radius, int angle) {
        int[] xPoints = new int[sides];
        int[] yPoints = new int[sides];

        for (int i = 0; i < sides; i++) {
            double theta = 2 * Math.PI * i / sides + Math.toRadians(angle);
            xPoints[i] = x + (int) (radius * Math.cos(theta));
            yPoints[i] = y + (int) (radius * Math.sin(theta));
        }

        g2d.fillPolygon(xPoints, yPoints, sides);
    }
}