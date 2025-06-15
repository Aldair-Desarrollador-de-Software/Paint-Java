package com.mycompany.paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Paint {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new PaintFrame().setVisible(true);
            }
        });
    }
}

class PaintFrame extends JFrame {
    private DrawArea drawArea;
    private Color currentColor = Color.BLACK;
    private Tool currentTool = Tool.PENCIL;
    private int brushSize = 2;
    private boolean darkMode = false;

    enum Tool { PENCIL, LINE, RECTANGLE, OVAL, SELECT, IMAGE }

    public PaintFrame() {
        setTitle("ModernPaint");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton btnSave = createButton("💾", "Guardar");
        JButton btnInsert = createButton("🖼", "Insertar Imagen");
        JButton btnPencil = createButton("✏️", "Lápiz");
        JButton btnLine = createButton("📏", "Línea");
        JButton btnRect = createButton("▭", "Rectángulo");
        JButton btnOval = createButton("◯", "Óvalo");
        JButton btnClear = createButton("🧹", "Limpiar");
        JButton btnColor = createButton("🎨", "Color");
        JButton btnCopy = createButton("📋", "Copiar");
        JButton btnPaste = createButton("📥", "Pegar");
        JButton btnCrop = createButton("✂️", "Recortar");
        JButton btnBrushSize = createButton("🔧", "Grosor");
        JButton btnDarkMode = createButton("🌙", "Modo Claro/Oscuro");

        toolbar.add(btnSave);
        toolbar.add(btnInsert);
        toolbar.add(btnCopy);
        toolbar.add(btnPaste);
        toolbar.add(btnCrop);
        toolbar.addSeparator();
        toolbar.add(btnPencil);
        toolbar.add(btnLine);
        toolbar.add(btnRect);
        toolbar.add(btnOval);
        toolbar.add(btnBrushSize);
        toolbar.add(btnColor);
        toolbar.add(btnClear);
        toolbar.add(btnDarkMode);

        add(toolbar, BorderLayout.NORTH);

        drawArea = new DrawArea();
        drawArea.setToolSupplier(new Supplier() {
            public Object get() { return currentTool; }
        });
        drawArea.setColorSupplier(new Supplier() {
            public Object get() { return currentColor; }
        });
        drawArea.setBrushSizeSupplier(new Supplier() {
            public Object get() { return new Integer(brushSize); }
        });

        add(drawArea, BorderLayout.CENTER);

        btnPencil.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { currentTool = Tool.PENCIL; }
        });
        btnLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { currentTool = Tool.LINE; }
        });
        btnRect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { currentTool = Tool.RECTANGLE; }
        });
        btnOval.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { currentTool = Tool.OVAL; }
        });
        btnCrop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { drawArea.crop(); }
        });
        btnClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { drawArea.clear(); }
        });
        btnColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color chosen = JColorChooser.showDialog(null, "Selecciona un color", currentColor);
                if (chosen != null) currentColor = chosen;
            }
        });
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        ImageIO.write(drawArea.getImage(), "png", chooser.getSelectedFile());
                        JOptionPane.showMessageDialog(null, "Imagen guardada exitosamente.");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "Error al guardar.");
                    }
                }
            }
        });
        btnInsert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    drawArea.insertImage(chooser.getSelectedFile());
                }
            }
        });
        btnCopy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { drawArea.copyToClipboard(); }
        });
        btnPaste.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { drawArea.pasteFromClipboard(); }
        });
        btnBrushSize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String[] options = {"2", "5", "10"};
                String s = (String) JOptionPane.showInputDialog(null, "Tamaño del pincel:", "Grosor",
                        JOptionPane.PLAIN_MESSAGE, null, options, "2");
                if (s != null) brushSize = Integer.parseInt(s);
            }
        });
        btnDarkMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                darkMode = !darkMode;
                drawArea.setDarkMode(darkMode);
                getContentPane().setBackground(darkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                toolbar.setBackground(darkMode ? Color.GRAY : null);
            }
        });
    }

    private JButton createButton(String icon, String tooltip) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Dialog", Font.PLAIN, 18));
        return btn;
    }
}

interface Supplier { Object get(); }

class DrawArea extends JPanel {
    private BufferedImage canvas;
    private Graphics2D g2;
    private Point start, end;
    private PaintFrame.Tool tool;
    private Color color;
    private int brushSize = 2;

    private Supplier toolSupplier, colorSupplier, brushSizeSupplier;

    public DrawArea() {
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                start = e.getPoint();
                tool = (PaintFrame.Tool) toolSupplier.get();
                color = (Color) colorSupplier.get();
                brushSize = ((Integer) brushSizeSupplier.get()).intValue();
                if (tool == PaintFrame.Tool.PENCIL) drawLine(start, start);
            }

            public void mouseReleased(MouseEvent e) {
                end = e.getPoint();
                drawShape(start, end);
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (tool == PaintFrame.Tool.PENCIL) {
                    end = e.getPoint();
                    drawLine(start, end);
                    start = end;
                    repaint();
                }
            }
        });
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (canvas == null) {
            canvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            g2 = canvas.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g.drawImage(canvas, 0, 0, null);
    }

    private void drawLine(Point p1, Point p2) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(brushSize));
        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
    }

    private void drawShape(Point p1, Point p2) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(brushSize));
        int x = Math.min(p1.x, p2.x);
        int y = Math.min(p1.y, p2.y);
        int w = Math.abs(p1.x - p2.x);
        int h = Math.abs(p1.y - p2.y);

        switch (tool) {
            case LINE: g2.drawLine(p1.x, p1.y, p2.x, p2.y); break;
            case RECTANGLE: g2.drawRect(x, y, w, h); break;
            case OVAL: g2.drawOval(x, y, w, h); break;
        }
    }

    public void clear() {
        g2.setPaint(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        repaint();
    }

    public void crop() {
        // Para Java 7: no implementado visualmente, pero puede hacerse con un área fija si se desea
        JOptionPane.showMessageDialog(null, "Función de recorte en desarrollo.");
    }

    public void insertImage(File file) {
        try {
            Image img = ImageIO.read(file);
            g2.drawImage(img, 50, 50, this);
            repaint();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "No se pudo insertar imagen.");
        }
    }

    public void copyToClipboard() {
        TransferableImage trans = new TransferableImage(canvas);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans, null);
    }

    public void pasteFromClipboard() {
        try {
            Image img = (Image) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.imageFlavor);
            g2.drawImage(img, 100, 100, this);
            repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No se pudo pegar la imagen del portapapeles.");
        }
    }

    public void setDarkMode(boolean dark) {
        setBackground(dark ? Color.DARK_GRAY : Color.WHITE);
        repaint();
    }

    public BufferedImage getImage() {
        return canvas;
    }

    public void setToolSupplier(Supplier s) { toolSupplier = s; }
    public void setColorSupplier(Supplier s) { colorSupplier = s; }
    public void setBrushSizeSupplier(Supplier s) { brushSizeSupplier = s; }
}

class TransferableImage implements Transferable {
    private Image image;

    public TransferableImage(Image image) {
        this.image = image;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor.equals(DataFlavor.imageFlavor) && image != null) return image;
        else throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { DataFlavor.imageFlavor };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.imageFlavor);
    }
}