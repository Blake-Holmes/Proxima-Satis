
import javax.swing.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

public class HomeGui extends JFrame {

    
    static final Color BG_DEEP   = new Color(0x02020F);
    static final Color CYAN      = new Color(0x00F5FF);
    static final Color MAGENTA   = new Color(0xFF00C8);
    static final Color GOLD      = new Color(0xFFD700);
    static final Color DIM_CYAN  = new Color(0x003F44);
    static final Color TEXT_MAIN = new Color(0xE0E8FF);
    static final Color TEXT_DIM  = new Color(0x5A6080);

    static final int STAR_COUNT = 200;
    float[] starX=new float[STAR_COUNT],starY=new float[STAR_COUNT];
    float[] starZ=new float[STAR_COUNT],starB=new float[STAR_COUNT];
    Random rng = new Random(42);

    BufferedImage nebulaTex;
    float scanY=0f, pulse=0f;
    int tick=0;


    StarCanvas   canvas;
    CosmicField  searchField;
    CosmicButton searchBtn;
    JLabel       statusLabel;

    public HomeGui() {
        super("◈  Proxima-Satis  ◈  CORPUS SEARCH ENGINE");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 720);
        setMinimumSize(new Dimension(780, 540));
        setLocationRelativeTo(null);
        for (int i=0;i<STAR_COUNT;i++) {
            starX[i]=rng.nextFloat(); starY[i]=rng.nextFloat();
            starZ[i]=rng.nextFloat(); starB[i]=0.3f+rng.nextFloat()*0.7f;
        }
        buildNebula(); buildUI();
    }



    void buildNebula() {
        nebulaTex = new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<512;y++) for (int x=0;x<512;x++) {
            float n=noise(x*0.012f,y*0.012f)+0.5f*noise(x*0.025f,y*0.025f)+0.25f*noise(x*0.05f,y*0.05f);
            n=Math.max(0,Math.min(1,(n-0.2f)*1.4f));
            nebulaTex.setRGB(x,y,((int)(n*110)<<24)|((int)(n*16)<<16)|((int)(n*6)<<8)|(int)(n*50));
        }
    }

     float noise(float x, float y) {
        int xi=(int)x,yi=(int)y; float xf=x-xi,yf=y-yi;
        float a=pr(xi,yi),b=pr(xi+1,yi),c=pr(xi,yi+1),d=pr(xi+1,yi+1);
        float u=xf*xf*(3-2*xf),v=yf*yf*(3-2*yf);
        return a+u*(b-a)+v*(c-a)+u*v*(a-b-c+d);
    }


    float pr(int x,int y){int n=x+y*57;n=(n<<13)^n;return(1f-((n*(n*n*15731+789221)+1376312589)&0x7fffffff)/1073741824f)*0.5f+0.5f;}



    
    JPanel buildHeader() {
        JPanel p=new JPanel(new BorderLayout()){
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g; g2.setColor(BG_DEEP); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setPaint(new GradientPaint(0,getHeight()-1,MAGENTA,getWidth()/2,getHeight()-1,CYAN,true));
                g2.setStroke(new BasicStroke(1.5f)); g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
            }
        };
        p.setOpaque(false); p.setBorder(BorderFactory.createEmptyBorder(18,36,14,36));
        JLabel t=new JLabel("◈  P R O X I M A - S A T I S" );
        t.setFont(new Font("Courier New",Font.BOLD,26)); t.setForeground(CYAN);
        JLabel s=new JLabel("DEEP CORPUS RETRIEVAL  //  OKAPI ENGINE");
        s.setFont(new Font("Courier New",Font.PLAIN,11)); s.setForeground(TEXT_DIM);
        JPanel r=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); r.setOpaque(false); r.add(s);
        p.add(t,BorderLayout.WEST); p.add(r,BorderLayout.EAST); return p;
    }


    class StarCanvas extends JPanel {
        StarCanvas(){setOpaque(true);setBackground(BG_DEEP);}
        protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            g2.setColor(BG_DEEP); g2.fillRect(0,0,W,H);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));
            for(int tx=0;tx<W;tx+=512) for(int ty=0;ty<H;ty+=512) g2.drawImage(nebulaTex,tx,ty,null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
            g2.setColor(new Color(0x0A0A30)); g2.setStroke(new BasicStroke(0.5f));
            for(int x=0;x<W;x+=60) g2.drawLine(x,0,x,H);
            for(int y=0;y<H;y+=60) g2.drawLine(0,y,W,y);
            for(int i=0;i<STAR_COUNT;i++){
                float z=starZ[i],sx=(starX[i]-0.5f)/z+0.5f,sy=(starY[i]-0.5f)/z+0.5f;
                if(sx<0||sx>1||sy<0||sy>1) continue;
                float sz=(1-z)*3.5f,al=starB[i]*(1-z)*0.9f;
                Color sc=(i%5==0)?CYAN:(i%7==0)?MAGENTA:TEXT_MAIN;
                g2.setColor(new Color(sc.getRed(),sc.getGreen(),sc.getBlue(),(int)(Math.min(1f,al)*220)));
                g2.fill(new Ellipse2D.Float(sx*W-sz/2,sy*H-sz/2,sz,sz));
            }
            g2.setColor(new Color(0,245,255,(int)((0.06f+0.05f*pulse)*255)));
            g2.fillRect(0,(int)scanY,W,2);
            int L=22; int[][] cs={{0,0,1,1},{W,0,-1,1},{0,H,1,-1},{W,H,-1,-1}};
            g2.setColor(CYAN); g2.setStroke(new BasicStroke(1.5f));
            for(int[] c:cs){g2.drawLine(c[0],c[1],c[0]+c[2]*L,c[1]);g2.drawLine(c[0],c[1],c[0],c[1]+c[3]*L);}
            g2.dispose(); super.paintChildren(g);
        }
    }

    public class CosmicField extends JTextField {
        boolean hov=false; final String ph;
        public CosmicField(String ph){
            super(ph); this.ph=ph; setOpaque(false); setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
            setFont(new Font("Courier New",Font.PLAIN,15)); setForeground(TEXT_DIM); setCaretColor(CYAN);
            setPreferredSize(new Dimension(0,46));
            addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){hov=true;repaint();}public void mouseExited(MouseEvent e){hov=false;repaint();}});
            addFocusListener(new FocusAdapter(){
                public void focusGained(FocusEvent e){if(getText().equals(ph)){setText("");setForeground(TEXT_MAIN);}}
                public void focusLost(FocusEvent e){if(getText().isEmpty()){setText(ph);setForeground(TEXT_DIM);}}
            });
        }
        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            g2.setColor(new Color(0x07071A)); g2.fill(new RoundRectangle2D.Float(0,0,W,H,8,8));
            g2.setColor(hov||isFocusOwner()?CYAN:DIM_CYAN); g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.5f,0.5f,W-1,H-1,8,8));
            if(isFocusOwner()){g2.setColor(new Color(0,245,255,18));g2.fill(new RoundRectangle2D.Float(0,0,W,H,8,8));}
            g2.dispose(); super.paintComponent(g);
        }
    }

     class CosmicButton extends JButton {
        boolean hov=false,prs=false;
        CosmicButton(String t){

            super(t); setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setFont(new Font("Courier New",Font.BOLD,13)); setForeground(BG_DEEP); setPreferredSize(new Dimension(200,60));
            addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){hov=true;repaint();}public void mouseExited(MouseEvent e){hov=false;repaint();}
                public void mousePressed(MouseEvent e){prs=true;repaint();}public void mouseReleased(MouseEvent e){prs=false;repaint();}
            });
        }
        protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int W=getWidth(),H=getHeight();
            Color c1=prs?MAGENTA:hov?new Color(0x00CCDD):CYAN;
            Color c2=prs?new Color(0xAA0088):hov?CYAN:new Color(0x0099BB);
            g2.setPaint(new GradientPaint(0,0,c1,W,H,c2));
            g2.fill(new RoundRectangle2D.Float(0,0,W,H,8,8));
            g2.setColor(new Color(255,255,255,28));
            g2.fill(new RoundRectangle2D.Float(2,2,W-4,(H-4)/2,6,6));
            g2.dispose(); super.paintComponent(g);
        }
    }

    void buildUI() {
        canvas=new StarCanvas(); setContentPane(canvas); canvas.setLayout(new BorderLayout());
        canvas.add(buildHeader(),BorderLayout.NORTH);
        JPanel center=new JPanel(new GridBagLayout());
        center.setOpaque(false); center.setBorder(BorderFactory.createEmptyBorder(10,36,10,36));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS)); // Stack buttons vertically
        buttonPanel.setOpaque(false);

        buttonPanel.add(new CosmicButton("Search DataBase"));
        buttonPanel.add(new CosmicButton("View DataBase"));
        buttonPanel.add(new CosmicButton("Add To DataBase"));

        buttonPanel.add(Box.createVerticalStrut(50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;

        center.add(buttonPanel, gbc);  // Add the button panel



        canvas.add(center, BorderLayout.CENTER);
        }


    public static void main(String[] args) {
        try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ignored){}
        SwingUtilities.invokeLater(()->new HomeGui().setVisible(true));
    }


}
