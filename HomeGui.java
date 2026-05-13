import javax.swing.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class HomeGui extends JFrame {

    static final Color BG_DEEP      = new Color(0x02020F);
    static final Color CYAN         = new Color(0x00FF41);
    static final Color CYAN_DIM     = new Color(0x00882A);
    static final Color CYAN_DIMMER  = new Color(0x003F44);
    static final Color TEXT_DIM     = new Color(0x5A6080);

    static final int    CELL    = 18;
    static final char[] CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%&*<>{}[]|/\\!?".toCharArray();

    static final File PA_DOCS = new File("PADocs");

    javax.swing.Timer   animTimer;
    BackgroundPanel     background;

    PlaceholderField    searchField;
    ResultsPanel        resultsPanel;
    JLabel              statusLabel;

    DataBase            db;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new HomeGui().setVisible(true));
    }

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
        // Load real database in background so UI stays responsive
        new SwingWorker<DataBase, Void>() {
            protected DataBase doInBackground() { return new DataBase(); }
            protected void done() {
                try {
                    db = get();
                    statusLabel.setForeground(CYAN);
                    statusLabel.setText("DATABASE LOADED  //  " + DataBase.numDoc + " DOCUMENTS INDEXED  //  AWAITING QUERY");
                } catch (Exception ex) {
                    statusLabel.setText("DB LOAD ERROR: " + ex.getMessage());
                }
            }
        }.execute();
    }

    //UI construction

    void buildUI() {
        background = new BackgroundPanel();
        setContentPane(background);
        background.setLayout(new BorderLayout());
        background.add(buildHeader(), BorderLayout.NORTH);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 36, 10, 10));

        for (Object[] spec : new Object[][] {
            { "View DataBase",   (Runnable) this::viewDatabase    },
            { "Add To DataBase", (Runnable) this::importDocuments },
            { "Clear DataBase",  (Runnable) this::clearDatabase    },
        }) {
            GradientButton btn = new GradientButton((String) spec[0]);
            btn.setFont(new Font("Courier New", Font.BOLD, 14));
            btn.setPreferredSize(new Dimension(180, 46));
            btn.setMaximumSize(new Dimension(180, 46));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.addActionListener(e -> ((Runnable) spec[1]).run());
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(16));
        }

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 36));
        center.add(buildSearchRow(), BorderLayout.NORTH);

        resultsPanel = new ResultsPanel();
        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(CYAN_DIMMER, 1));
        JScrollBar vsb = scroll.getVerticalScrollBar();
        vsb.setUI(new ThemedScrollBarUI());
        vsb.setBackground(new Color(0x05050F));
        center.add(scroll, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(sidebar, BorderLayout.WEST);
        body.add(center,  BorderLayout.CENTER);

        background.add(body,             BorderLayout.CENTER);
        background.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    JPanel buildSearchRow() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        searchField = new PlaceholderField("ENTER QUERY VECTOR...");
        searchField.addActionListener(e -> doSearch());
        GradientButton searchBtn = new GradientButton("◈ SEARCH");
        searchBtn.setPreferredSize(new Dimension(140, 46));
        searchBtn.setFont(new Font("Courier New", Font.BOLD, 13));
        searchBtn.addActionListener(e -> doSearch());
        panel.add(searchField, BorderLayout.CENTER);
        panel.add(searchBtn,   BorderLayout.EAST);
        return panel;
    }

    //Actions

    void doSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty() || query.equals("ENTER QUERY...")) return;
        if (db == null) {
            statusLabel.setForeground(CYAN_DIM);
            statusLabel.setText("DATABASE STILL LOADING — PLEASE WAIT...");
            return;
        }
        statusLabel.setForeground(CYAN_DIM);
        statusLabel.setText("SCANNING CORPUS...  COMPUTING BM25 SCORES...");
        new SwingWorker<List<ResultRow>, Void>() {
            protected List<ResultRow> doInBackground() throws Exception {
                db.getQuery(query);
                java.util.HashMap<String, Double> scores = db.conductSearch();
                List<ResultRow> rows = new ArrayList<>();
                for (java.util.Map.Entry<String, Double> e : scores.entrySet()) {
                    if (e.getValue() > 0.0) rows.add(new ResultRow(e.getKey(), e.getValue().floatValue()));
                }
                rows.sort((a, b) -> Float.compare(b.score, a.score));
                return rows;
            }
            protected void done() {
                try {
                    List<ResultRow> rows = get();
                    resultsPanel.setResults(rows);
                    statusLabel.setForeground(CYAN);
                    statusLabel.setText("RETRIEVED " + rows.size() + " DOCUMENTS  //  QUERY: [" + query.toUpperCase() + "]");
                } catch (Exception ex) {
                    statusLabel.setText("SEARCH ERROR: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    void clearDatabase(){
       File dir = new File("myDirectory");
       
        int response = JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to delete this folder and all its contents?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (response == JOptionPane.YES_OPTION) {
            if (deleteFolder(dir)) {
            System.out.println("Directory deleted successfully.");
            db = new DataBase();
            }
        } else {
            System.out.println("User clicked No or closed the dialog. Cancel deletion.");
        }
    }

    private boolean deleteFolder(File dir) {
        File[] files = PA_DOCS.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        return PA_DOCS.delete();
    }

    void viewDatabase() {
        if (db == null) {
            JOptionPane.showMessageDialog(this, "Database is still loading, please wait.", "Not Ready", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INDEXED DOCUMENTS (").append(DataBase.numDoc).append(" total)\n");
        for (int i = 0; i < 60; i++) {
            sb.append("─");
        }
        sb.append("\n\n");
        int i = 1;
        for (String docName : DataBase.tokenMap.keySet()) {
            int termCount = DataBase.tokenMap.get(docName).size();
            sb.append(String.format("%2d.  %-45s  [%d terms]\n", i++, docName, termCount));
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Courier New", Font.PLAIN, 13));
        ta.setEditable(false);
        ta.setBackground(new Color(0x07071A));
        ta.setForeground(CYAN);
        ta.setCaretColor(CYAN);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(620, 360));
        JOptionPane.showMessageDialog(this, sp, "◈  DATABASE CONTENTS", JOptionPane.PLAIN_MESSAGE);
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
        db = new DataBase();
    }

    File resolveDestination(File src) {
        File   dest = new File(PA_DOCS, src.getName());
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
        if (db == null) return;
        try {
            Tokenizer tok = new Tokenizer();
            tok.parse(file.getPath());
            DataBase.tokenMap.put(file.getName(), tok.getLex());
            DataBase.numDoc = DataBase.tokenMap.size();
            DataBase.getTotalTokens();
            db.buildInvertedIndex();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //Matrix rain

    int      cols, rows;
    int[]    headRow;
    int[]    trailLen;
    char[][] glyphs;
    float[]  speed;
    float[]  speedAccum;
    Random   rng = new Random();

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
                float t     = (float)(r - (head - trailLen[c])) / trailLen[c];
                Color color = (r == head)
                    ? new Color(220, 255, 220, 255)
                    : new Color(0, Math.min(255, (int)(40 + t * 180)), 0, Math.min(255, (int)(60 + t * 195)));
                g2.setColor(color);
                g2.drawString(String.valueOf(glyphs[c][r]), c * CELL + (CELL - cw) / 2, r * CELL + ch);
            }
        }
    }

    //Widgets

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

    JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0x03031A));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(CYAN_DIMMER);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 36, 6, 36));

        statusLabel = new JLabel("SYSTEM READY  //  AWAITING QUERY INPUT");
        statusLabel.setFont(new Font("Courier New", Font.PLAIN, 10));
        statusLabel.setForeground(TEXT_DIM);

        JLabel version = new JLabel("BM25-OKAPI  K1=1.2  B=0.75");
        version.setFont(new Font("Courier New", Font.PLAIN, 10));
        version.setForeground(new Color(0x222248));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(version,     BorderLayout.EAST);
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

    class PlaceholderField extends JTextField {
        boolean hovered = false;
        final String placeholder;

        PlaceholderField(String placeholder) {
            super(placeholder);
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            setFont(new Font("Courier New", Font.PLAIN, 15));
            setForeground(TEXT_DIM);
            setCaretColor(CYAN);
            setPreferredSize(new Dimension(0, 46));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (getText().equals(placeholder)) { setText(""); setForeground(CYAN); }
                }
                public void focusLost(FocusEvent e) {
                    if (getText().isEmpty()) { setText(placeholder); setForeground(TEXT_DIM); }
                }
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0x07071A));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
            g2.setColor(hovered || isFocusOwner() ? CYAN : CYAN_DIMMER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8));
            if (isFocusOwner()) {
                g2.setColor(new Color(0, 245, 255, 18));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
            }
            g2.dispose();
            super.paintComponent(g);
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
            setPreferredSize(new Dimension(220, 80));
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

    static class ResultRow {
        String title;
        float  score;
        ResultRow(String title, float score) { this.title = title; this.score = score; }
    }

    class ResultsPanel extends JPanel {
        ResultsPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        void setResults(List<ResultRow> rows) {
            removeAll();
            float max = rows.stream().map(r -> r.score).max(Float::compare).orElse(1f);
            for (int i = 0; i < rows.size(); i++) {
                add(new ResultCard(rows.get(i), i, max));
                add(Box.createVerticalStrut(6));
            }
            if (rows.isEmpty()) {
                JLabel empty = new JLabel("NO MATCHING DOCUMENTS IN CORPUS");
                empty.setFont(new Font("Courier New", Font.PLAIN, 13));
                empty.setForeground(TEXT_DIM);
                empty.setAlignmentX(CENTER_ALIGNMENT);
                add(Box.createVerticalStrut(40));
                add(empty);
            }
            revalidate();
            repaint();
        }
    }

    class ResultCard extends JPanel {
        boolean   hovered = false;
        ResultRow row;
        int       rank;
        float     maxScore;
        File      file;

        ResultCard(ResultRow row, int rank, float maxScore) {
            this.file = new File(row.title);
            this.row = row; this.rank = rank; this.maxScore = maxScore;
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            setPreferredSize(new Dimension(100, 72));
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { 
                    hovered = true;  
                    repaint(); 
                }
                public void mouseExited(MouseEvent e)  { 
                    hovered = false; 
                    repaint(); 
                }
                public void mouseClicked(MouseEvent e) {
                   try {
                        File file = new File(PA_DOCS, row.title);

                        if (file.exists()) {
                        Desktop.getDesktop().open(file);
                        } else {
                        JOptionPane.showMessageDialog(
                        ResultCard.this,
                        "File not found:\n" + file.getAbsolutePath(),
                        "Open Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }

                } catch (IOException ex) {
                    ex.printStackTrace();

                    JOptionPane.showMessageDialog(
                    ResultCard.this,
                    "Could not open file:\n" + ex.getMessage(),
                    "Open Error",
                    JOptionPane.ERROR_MESSAGE
                    );
                }
                }
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setColor(hovered ? new Color(0x0F0F30) : new Color(0x07071A));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 6, 6));

            Color accent = rank == 0 ? CYAN : rank <= 2 ? CYAN_DIM : CYAN_DIMMER;
            g2.setColor(accent);
            g2.fillRect(0, 6, 3, h - 12);

            g2.setColor(hovered ? CYAN_DIMMER : new Color(0x07120A));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 6, 6));

            g2.setFont(new Font("Courier New", Font.PLAIN, 10));
            g2.setColor(accent);
            g2.drawString(String.format("#%02d", rank + 1), 12, 22);

            g2.setFont(new Font("Courier New", Font.BOLD, 14));
            g2.setColor(CYAN);
            g2.drawString(row.title.toUpperCase(), 12, 40);

            int barW = (int)((w - 80) * (maxScore > 0 ? row.score / maxScore : 0));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
            g2.fillRect(12, 50, w - 80, 6);
            g2.setPaint(new GradientPaint(
                12, 0, accent,
                12 + Math.max(1, barW), 0,
                new Color(accent.getRed() / 3, accent.getGreen() / 3, accent.getBlue() / 3 + 20)
            ));
            g2.fillRect(12, 50, barW, 6);

            String scoreText = String.format("%.2f", row.score);
            g2.setFont(new Font("Courier New", Font.BOLD, 11));
            g2.setColor(accent);
            g2.drawString(scoreText, w - g2.getFontMetrics().stringWidth(scoreText) - 14, 57);

            g2.dispose();
        }
    }

    static class ThemedScrollBarUI extends BasicScrollBarUI {
        protected void configureScrollBarColors() {
            thumbColor = CYAN_DIMMER;
            trackColor = new Color(0x05050F);
        }
        protected JButton createDecreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        protected JButton createIncreaseButton(int o) { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            ((Graphics2D) g).setColor(CYAN);
            ((Graphics2D) g).fill(new RoundRectangle2D.Float(r.x + 2, r.y, r.width - 4, r.height, 4, 4));
        }
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(new Color(0x05050F));
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}