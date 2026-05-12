import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class HomeGui extends JFrame {

    static final Color BG_DEEP  = new Color(0x02020F);
    static final Color CYAN     = new Color(0x00FF41);
    static final Color TEXT_DIM = new Color(0x5A6080);

    static final int    CELL    = 18;
    static final char[] CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%&*<>{}[]|/\\!?".toCharArray();

    static final File PA_DOCS = new File("PADocs");

    int      cols, rows;
    int[]    headRow;
    int[]    trailLen;
    char[][] glyphs;
    float[]  speed;
    float[]  speedAccum;
    Random   rng = new Random();

    javax.swing.Timer animTimer;
    BackgroundPanel   background;

    public HomeGui() {
        super("◈  Proxima-Satis  ◈  CORPUS SEARCH ENGINE");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 720);
        setMinimumSize(new Dimension(780, 540));
        setLocationRelativeTo(null);
        buildUI();
        initRain(980, 720);
        animTimer = new javax.swing.Timer(50, e -> { stepRain(); background.repaint(); });
        animTimer.start();
    }

    void initRain(int w, int h) {
        cols       = w / CELL + 2;
        rows       = h / CELL + 2;
        headRow    = new int[cols];
        trailLen   = new int[cols];
        speed      = new float[cols];
        speedAccum = new float[cols];
        glyphs     = new char[cols][rows];
        for (int c = 0; c < cols; c++) {
            headRow[c]  = -rng.nextInt(rows);
            trailLen[c] = 6 + rng.nextInt(20);
            speed[c]    = 0.6f + rng.nextFloat() * 0.8f;
            for (int r = 0; r < rows; r++)
                glyphs[c][r] = randomGlyph();
        }
    }

    char randomGlyph() {
        return CHARSET[rng.nextInt(CHARSET.length)];
    }

    void stepRain() {
        for (int c = 0; c < cols; c++) {
            speedAccum[c] += speed[c];
            if (speedAccum[c] < 1f) continue;
            speedAccum[c] -= 1f;
            headRow[c]++;
            int shimmerRow = headRow[c] - rng.nextInt(Math.max(1, trailLen[c]));
            if (shimmerRow >= 0 && shimmerRow < rows)
                glyphs[c][shimmerRow] = randomGlyph();
            if (headRow[c] - trailLen[c] > rows) {
                headRow[c]  = -rng.nextInt(rows / 2);
                trailLen[c] = 6 + rng.nextInt(20);
                speed[c]    = 0.2f + rng.nextFloat() * 0.6f;
            }
        }
    }

    void paintRain(Graphics2D g2) {
        g2.setFont(new Font("Courier New", Font.BOLD, CELL - 2));
        FontMetrics fm = g2.getFontMetrics();
        int cw = fm.charWidth('M');
        int ch = fm.getAscent();

        for (int c = 0; c < cols; c++) {
            int head = headRow[c];
            for (int r = head - trailLen[c]; r <= head; r++) {
                if (r < 0 || r >= rows) continue;
                float t = (float)(r - (head - trailLen[c])) / trailLen[c];
                Color color = (r == head)
                    ? new Color(220, 255, 220, 255)
                    : new Color(0, Math.min(255, (int)(40 + t * 180)), 0, Math.min(255, (int)(60 + t * 195)));
                g2.setColor(color);
                g2.drawString(
                    String.valueOf(glyphs[c][r]),
                    c * CELL + (CELL - cw) / 2,
                    r * CELL + ch
                );
            }
        }
    }

    JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_DEEP);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(new GradientPaint(
                    0, getHeight() - 1, new Color(0x008F11),
                    getWidth() / 2, getHeight() - 1, CYAN, true
                ));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 36, 14, 36));

        JLabel title = new JLabel("◈  P R O X I M A - S A T I S");
        title.setFont(new Font("Courier New", Font.BOLD, 26));
        title.setForeground(CYAN);

        JLabel subtitle = new JLabel("DEEP CORPUS RETRIEVAL  //  OKAPI ENGINE");
        subtitle.setFont(new Font("Courier New", Font.PLAIN, 11));
        subtitle.setForeground(TEXT_DIM);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(subtitle);

        panel.add(title, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    class BackgroundPanel extends JPanel {
        BackgroundPanel() { setOpaque(true); setBackground(BG_DEEP); }

        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (cols != w / CELL + 2 || rows != h / CELL + 2) initRain(w, h);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(BG_DEEP);
            g2.fillRect(0, 0, w, h);
            paintRain(g2);
            paintCornerBrackets(g2, w, h);

            g2.dispose();
            super.paintChildren(g);
        }

        private void paintCornerBrackets(Graphics2D g2, int w, int h) {
            int[][] corners = { {0, 0, 1, 1}, {w, 0, -1, 1}, {0, h, 1, -1}, {w, h, -1, -1} };
            int arm = 22;
            g2.setColor(CYAN);
            g2.setStroke(new BasicStroke(1.5f));
            for (int[] c : corners) {
                g2.drawLine(c[0], c[1], c[0] + c[2] * arm, c[1]);
                g2.drawLine(c[0], c[1], c[0],              c[1] + c[3] * arm);
            }
        }
    }

    class GradientButton extends JButton {
        boolean hovered = false, pressed = false;

        GradientButton(String label) {
            super(label);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("Courier New", Font.BOLD, 32));
            setForeground(BG_DEEP);
            setPreferredSize(new Dimension(640, 160));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e)  { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)   { hovered = false; repaint(); }
                public void mousePressed(MouseEvent e)  { pressed = true;  repaint(); }
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color from = pressed ? new Color(0x008F11) : hovered ? new Color(0x006400) : CYAN;
            Color to   = pressed ? new Color(0x006400) : hovered ? CYAN               : new Color(0x006400);
            g2.setPaint(new GradientPaint(0, 0, from, w, h, to));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
            g2.setColor(new Color(255, 255, 255, 28));
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, (h - 4) / 2f, 6, 6));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    void searchDatabase() {
        System.out.println("[placeholder] Search DataBase clicked");
    }

    void viewDatabase() {
        System.out.println("[placeholder] View DataBase clicked");
    }

    void importDocuments() {
        if (!PA_DOCS.exists()) PA_DOCS.mkdirs();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Documents to Add  →  PADocs");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        int           copied = 0, failed = 0;
        StringBuilder log    = new StringBuilder();

        for (File src : chooser.getSelectedFiles()) {
            File dest = resolveDestination(src);
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                ingestFile(dest);
                log.append("  ✔  ").append(dest.getName()).append("\n");
                copied++;
            } catch (IOException ex) {
                log.append("  ✘  ").append(src.getName()).append("  (").append(ex.getMessage()).append(")\n");
                failed++;
            }
        }

        String summary = copied + " file(s) added to PADocs"
            + (failed > 0 ? ", " + failed + " failed" : "") + ":\n\n" + log;
        JOptionPane.showMessageDialog(this, summary, "PADocs — Import Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    File resolveDestination(File src) {
        File   dest  = new File(PA_DOCS, src.getName());
        if (!dest.exists()) return dest;
        String name  = src.getName();
        int    dot   = name.lastIndexOf('.');
        String base  = dot >= 0 ? name.substring(0, dot) : name;
        String ext   = dot >= 0 ? name.substring(dot)    : "";
        int    count = 1;
        while (dest.exists()) dest = new File(PA_DOCS, base + "_" + count++ + ext);
        return dest;
    }

    void ingestFile(File file) {
        System.out.println("[placeholder] Ingesting into database: " + file.getAbsolutePath());
    }

    void buildUI() {
        background = new BackgroundPanel();
        setContentPane(background);
        background.setLayout(new BorderLayout());
        background.add(buildHeader(), BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setOpaque(false);

        for (Object[] spec : new Object[][] {
            { "Search DataBase", (Runnable) this::searchDatabase  },
            { "View DataBase",   (Runnable) this::viewDatabase    },
            { "Add To DataBase", (Runnable) this::importDocuments },
        }) {
            GradientButton btn = new GradientButton((String) spec[0]);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.addActionListener(e -> ((Runnable) spec[1]).run());
            buttons.add(btn);
            buttons.add(Box.createVerticalStrut(16));
        }
        buttons.add(Box.createVerticalStrut(34));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 36, 10, 36));
        center.add(buttons, new GridBagConstraints());
        background.add(center, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new HomeGui().setVisible(true));
    }
}