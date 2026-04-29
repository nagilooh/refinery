package tools.refinery.generator.gui;

import com.github.weisj.jsvg.SVGDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class RefineryGuiView implements RefineryGuiModel.Listener {
    private final JFrame frame;
    private final DefaultListModel<Activation> listModel;
    private final JList<Activation> list;
    private final JPanel imagePanel;
    private boolean autoFit = true;
    private double zoomFactor = 1.0;

    private final RefineryGuiModel appModel;
    private RefineryGuiController controller;

    public RefineryGuiView(RefineryGuiModel appModel) {
        this.appModel = appModel;
        this.appModel.addListener(this);

        frame = new JFrame("Refinery GUI");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane listScrollPane = new JScrollPane(list);
        listScrollPane.setPreferredSize(new Dimension(300, 0));

        imagePanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                SVGDocument currentSvg = appModel.getSvg();
                if (currentSvg != null && !autoFit) {
                    com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
                    return new Dimension((int) (size.width * zoomFactor), (int) (size.height * zoomFactor));
                }
                return super.getPreferredSize();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                SVGDocument currentSvg = appModel.getSvg();
                if (currentSvg != null) {
                    com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
                    float iw = size.width;
                    float ih = size.height;
                    if (iw > 0 && ih > 0) {
                        double scale;
                        int x = 0, y = 0;
                        if (autoFit) {
                            Container parent = getParent();
                            int w = parent != null ? parent.getWidth() : getWidth();
                            int h = parent != null ? parent.getHeight() : getHeight();
                            scale = Math.min((double) w / iw, (double) h / ih);
                            x = (int) (w - iw * scale) / 2;
                            y = (int) (h - ih * scale) / 2;
                        } else {
                            scale = zoomFactor;
                        }
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.translate(x, y);
                        g2d.scale(scale, scale);
                        currentSvg.render(null, g2d);
                        g2d.dispose();
                    }
                }
            }
        };

        JScrollPane imageScrollPane = new JScrollPane(imagePanel);
        imageScrollPane.setWheelScrollingEnabled(false);

        imagePanel.addMouseWheelListener(e -> {
            SVGDocument currentSvg = appModel.getSvg();
            if (currentSvg == null) return;

            Point mousePoint = e.getPoint();
            double oldZoom = zoomFactor;

            if (autoFit) {
                autoFit = false;
                com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
                Container parent = imagePanel.getParent();
                int w = parent != null ? parent.getWidth() : imagePanel.getWidth();
                int h = parent != null ? parent.getHeight() : imagePanel.getHeight();
                zoomFactor = Math.min((double) w / size.width, (double) h / size.height);
                oldZoom = zoomFactor;
            }

            if (e.getWheelRotation() < 0) {
                zoomFactor *= 1.1;
            } else {
                zoomFactor /= 1.1;
            }

            JViewport viewport = imageScrollPane.getViewport();
            Point viewPosition = viewport.getViewPosition();

            double scaleChange = zoomFactor / oldZoom;
            int newX = (int)((viewPosition.x + mousePoint.x) * scaleChange - mousePoint.x);
            int newY = (int)((viewPosition.y + mousePoint.y) * scaleChange - mousePoint.y);

            imagePanel.revalidate();
            imagePanel.repaint();

            SwingUtilities.invokeLater(() -> {
                viewport.setViewPosition(new Point(Math.max(0, newX), Math.max(0, newY)));
            });
        });

        MouseAdapter dragAdapter = new MouseAdapter() {
            private Point origin;

            @Override
            public void mousePressed(MouseEvent e) {
                origin = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin != null && !autoFit) {
                    JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, imagePanel);
                    if (viewPort != null) {
                        int deltaX = origin.x - e.getX();
                        int deltaY = origin.y - e.getY();
                        Rectangle view = viewPort.getViewRect();
                        view.x += deltaX;
                        view.y += deltaY;
                        imagePanel.scrollRectToVisible(view);
                    }
                }
            }
        };
        imagePanel.addMouseListener(dragAdapter);
        imagePanel.addMouseMotionListener(dragAdapter);

        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel toolbar = getJPanel();

        rightPanel.add(toolbar, BorderLayout.NORTH);
        rightPanel.add(imageScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, rightPanel);
        splitPane.setDividerLocation(300);

        JButton sendButton = new JButton("Send Selected");
        sendButton.addActionListener(e -> sendSelected());

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    selectItem();
                }
                if (e.getClickCount() == 2) {
                    sendSelected();
                }
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(sendButton);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.getContentPane().add(bottom, BorderLayout.SOUTH);
    }

    public void setController(RefineryGuiController controller) {
        this.controller = controller;
    }

    private JPanel getJPanel() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton zoomInBtn = new JButton("+");
        JButton zoomOutBtn = new JButton("-");
        JButton fitBtn = new JButton(String.valueOf((char)0x26F6));

        zoomInBtn.addActionListener(e -> {
            baseZoomFactor();
            zoomFactor *= 1.2;
            imagePanel.revalidate();
            imagePanel.repaint();
        });

        zoomOutBtn.addActionListener(e -> {
            baseZoomFactor();
            zoomFactor /= 1.2;
            imagePanel.revalidate();
            imagePanel.repaint();
        });

        fitBtn.addActionListener(e -> {
            autoFit = true;
            imagePanel.revalidate();
            imagePanel.repaint();
        });

        toolbar.add(zoomInBtn);
        toolbar.add(zoomOutBtn);
        toolbar.add(fitBtn);
        return toolbar;
    }

    private void baseZoomFactor() {
        SVGDocument currentSvg = appModel.getSvg();
        if (autoFit && currentSvg != null) {
            autoFit = false;
            com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
            Container parent = imagePanel.getParent();
            int w = parent != null ? parent.getWidth() : imagePanel.getWidth();
            int h = parent != null ? parent.getHeight() : imagePanel.getHeight();
            zoomFactor = Math.min((double) w / size.width, (double) h / size.height);
        }
    }

    private void selectItem() {
        Activation sel = list.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(frame, "No item selected", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (controller != null) {
            controller.onActivationSelected(sel);
        }
    }

    private void sendSelected() {
        Activation sel = list.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(frame, "No item selected", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (controller != null) {
            controller.onActivationSent(sel);
        }
    }

    public void show() {
        frame.setVisible(true);
    }

    @Override
    public void onActivationsChanged(List<Activation> activations) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (Activation a : activations) {
                listModel.addElement(a);
            }
        });
    }

    @Override
    public void onSvgChanged(SVGDocument svg) {
        SwingUtilities.invokeLater(() -> {
            imagePanel.revalidate();
            imagePanel.repaint();
        });
    }
}

