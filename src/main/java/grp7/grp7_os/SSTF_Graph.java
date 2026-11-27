package grp7.grp7_os;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;


public class SSTF_Graph extends JPanel {

    // Empty space around the drawing area so the labels fit
    private static final int MARGIN_LEFT = 40;
    private static final int MARGIN_RIGHT = 40;
    private static final int MARGIN_TOP = 55;
    private static final int MARGIN_BOTTOM = 30;

    // path[0] = starting head position, path[1..n] = each served request.
    // It stays null until the user clicks Calculate.
    private int[] path;
    private int maxCylinder = 200;
    // Example Cyclinder Above
    public SSTF_Graph() {
        setBackground(Color.WHITE);
    }

    // Called by SSTF_GUI after the algorithm runs.
    public void setData(int initialHead, int[] executionOrder, int maxCylinder) {
        this.maxCylinder = maxCylinder;

        // Build the path: the start position followed by the execution order.
        path = new int[executionOrder.length + 1];
        path[0] = initialHead;
        for (int i = 0; i < executionOrder.length; i++) {
            path[i + 1] = executionOrder[i];
        }
        repaint();
    }

    // Called by the Clear button.
    public void clear() {
        path = null;
        repaint();
    }

    // ---- converting numbers to pixels ------------------------------------

    // Cylinder number -> x pixel.
    // Cylinder 0 = left edge of the plot, maxCylinder = right edge,
    // every other cylinder is placed proportionally in between.
    private int xFor(int cylinder) {
        int plotWidth = getWidth() - MARGIN_LEFT - MARGIN_RIGHT;
        return MARGIN_LEFT + (int) Math.round((double) cylinder / maxCylinder * plotWidth);
    }

    // Step number -> y pixel.
    // Step 0 (the start) = top, the last step = bottom, evenly spaced.
    private int yFor(int step, int totalSteps) {
        int plotHeight = getHeight() - MARGIN_TOP - MARGIN_BOTTOM;
        return MARGIN_TOP + (int) Math.round((double) step / totalSteps * plotHeight);
    }

    // ---- drawing -----------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // clears the panel first
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (path == null) {
            g2.setColor(Color.GRAY);
            g2.drawString("Enter values and click Calculate to see the graph.", 20, 30);
            return;
        }

        int totalSteps = path.length - 1;

        drawAxis(g2);

        // STEP 1: draw a line between every pair of neighbouring points.
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(30, 90, 200));
        for (int i = 0; i < totalSteps; i++) {
            g2.drawLine(xFor(path[i]), yFor(i, totalSteps),
                        xFor(path[i + 1]), yFor(i + 1, totalSteps));
        }

        // STEP 2: draw a dot and a number on every point.
        for (int i = 0; i < path.length; i++) {
            int x = xFor(path[i]);
            int y = yFor(i, totalSteps);

            // Start = green dot, every served request = red dot
            g2.setColor(i == 0 ? new Color(0, 150, 60) : new Color(210, 40, 40));
            g2.fillOval(x - 5, y - 5, 10, 10);

            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf(path[i]), x + 8, y + 4);
        }
    }

    // Draws the cylinder scale (0 ... maxCylinder) across the top.
    private void drawAxis(Graphics2D g2) {
        int bottomY = getHeight() - MARGIN_BOTTOM;

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Cylinder number  (green = start, red = served request)", MARGIN_LEFT, 15);
        g2.drawLine(MARGIN_LEFT, MARGIN_TOP - 10, getWidth() - MARGIN_RIGHT, MARGIN_TOP - 10);

        // 10 evenly spaced tick marks, each with a faint vertical grid line
        int ticks = 10;
        for (int t = 0; t <= ticks; t++) {
            int cylinder = (int) Math.round((double) t / ticks * maxCylinder);
            int x = xFor(cylinder);

            g2.setColor(new Color(225, 225, 225));
            g2.drawLine(x, MARGIN_TOP - 10, x, bottomY);

            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(x, MARGIN_TOP - 14, x, MARGIN_TOP - 6);

            String label = String.valueOf(cylinder);
            int labelWidth = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x - labelWidth / 2, MARGIN_TOP - 20);
        }
    }
}
