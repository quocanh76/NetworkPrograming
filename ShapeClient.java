import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class ShapeClient extends JFrame {
    private JTextField xField, yField, sizeField, widthField, heightField, sidesField, rotationField;
    private JTextField startXField, startYField, endXField, endYField; // For line
    private JComboBox<String> shapeSelector;
    private JButton colorButton, drawButton;
    private ImagePanel imagePanel;
    private Color selectedColor = Color.BLACK;

    public ShapeClient() {
        setTitle("Shape Client");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Control Panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(5, 4, 5, 5));

        // Shape selection
        shapeSelector = new JComboBox<>(new String[]{"circle", "square", "rectangle", "polygon", "line"});
        controlPanel.add(new JLabel("Shape:"));
        controlPanel.add(shapeSelector);

        // Position fields
        xField = new JTextField("100");
        yField = new JTextField("100");
        controlPanel.add(new JLabel("X:"));
        controlPanel.add(xField);
        controlPanel.add(new JLabel("Y:"));
        controlPanel.add(yField);

        // Size fields
        sizeField = new JTextField("50");
        widthField = new JTextField("100");
        heightField = new JTextField("50");
        sidesField = new JTextField("6");

        controlPanel.add(new JLabel("Size:"));
        controlPanel.add(sizeField);
        controlPanel.add(new JLabel("Width:"));
        controlPanel.add(widthField);
        controlPanel.add(new JLabel("Height:"));
        controlPanel.add(heightField);
        controlPanel.add(new JLabel("Sides (Polygon):"));
        controlPanel.add(sidesField);

        // Line fields
        startXField = new JTextField("50");
        startYField = new JTextField("50");
        endXField = new JTextField("200");
        endYField = new JTextField("200");

        controlPanel.add(new JLabel("Start X (Line):"));
        controlPanel.add(startXField);
        controlPanel.add(new JLabel("Start Y (Line):"));
        controlPanel.add(startYField);
        controlPanel.add(new JLabel("End X (Line):"));
        controlPanel.add(endXField);
        controlPanel.add(new JLabel("End Y (Line):"));
        controlPanel.add(endYField);

        // Rotation field
        rotationField = new JTextField("0");
        controlPanel.add(new JLabel("Rotation (°):"));
        controlPanel.add(rotationField);

        // Color selection button
        colorButton = new JButton("Select Color");
        colorButton.addActionListener(e -> {
            selectedColor = JColorChooser.showDialog(this, "Choose Color", selectedColor);
        });
        controlPanel.add(new JLabel("Color:"));
        controlPanel.add(colorButton);

        // Draw button
        drawButton = new JButton("Draw Shape");
        drawButton.addActionListener(new DrawAction());
        controlPanel.add(drawButton);

        // Image panel
        imagePanel = new ImagePanel();
        imagePanel.setPreferredSize(new Dimension(400, 300));

        add(controlPanel, BorderLayout.NORTH);
        add(imagePanel, BorderLayout.CENTER);
    }

    private class DrawAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String serverAddress = "localhost";
            int port = 8080;

            try (Socket socket = new Socket(serverAddress, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 InputStream in = socket.getInputStream()) {

                // Get user input
                String shape = (String) shapeSelector.getSelectedItem();
                int rotation = Integer.parseInt(rotationField.getText());
                String colorHex = String.format("#%02x%02x%02x", selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());

                String request = "";
                if ("polygon".equalsIgnoreCase(shape)) {
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    int sides = Integer.parseInt(sidesField.getText());
                    int radius = Integer.parseInt(sizeField.getText());
                    request = String.format("polygon,%d,%d,%s,%d,%d,%d", x, y, colorHex, sides, radius, rotation);
                } else if ("rectangle".equalsIgnoreCase(shape)) {
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    request = String.format("rectangle,%d,%d,%s,%d,%d,%d", x, y, colorHex, width, height, rotation);
                } else if ("line".equalsIgnoreCase(shape)) { // Line handling
                    int startX = Integer.parseInt(startXField.getText());
                    int startY = Integer.parseInt(startYField.getText());
                    int endX = Integer.parseInt(endXField.getText());
                    int endY = Integer.parseInt(endYField.getText());
                    request = String.format("line,%d,%d,%d,%d,%s", startX, startY, endX, endY, colorHex);
                } else { // Circle and Square
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    int size = Integer.parseInt(sizeField.getText());
                    request = String.format("%s,%d,%d,%s,%d,%d", shape, x, y, colorHex, size, rotation);
                }

                // Send request
                out.println(request);
                System.out.println("Sent request: " + request);

                // Receive and display image
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    System.out.println("Image received.");
                    if (!"line".equalsIgnoreCase(shape)) {
                        image = rotateImage(image, rotation); // Rotate before displaying (except for lines)
                    }
                    imagePanel.setImage(image);
                } else {
                    System.out.println("No image received.");
                }

            } catch (IOException | NumberFormatException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error: Invalid input or server issue.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Custom JPanel to display images
    class ImagePanel extends JPanel {
        private BufferedImage image;

        public void setImage(BufferedImage image) {
            this.image = image;
            repaint(); // Refresh the panel when a new image is set
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image != null) {
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imgWidth = image.getWidth();
                int imgHeight = image.getHeight();

                // Calculate aspect ratio preserving scaling
                double scaleX = (double) panelWidth / imgWidth;
                double scaleY = (double) panelHeight / imgHeight;
                double scale = Math.min(scaleX, scaleY); // Maintain aspect ratio

                int newWidth = (int) (imgWidth * scale);
                int newHeight = (int) (imgHeight * scale);

                // Center image
                int x = (panelWidth - newWidth) / 2;
                int y = (panelHeight - newHeight) / 2;

                g.drawImage(image, x, y, newWidth, newHeight, this);
            }
        }
    }

    // Method to rotate an image
    private static BufferedImage rotateImage(BufferedImage img, double angle) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage rotated = new BufferedImage(w, h, img.getType());
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angle), w / 2.0, h / 2.0);
        g2d.setTransform(transform);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return rotated;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ShapeClient().setVisible(true);
        });
    }
}