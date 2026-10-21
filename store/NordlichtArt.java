import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Renders Nordlicht icons, TV banners and store graphics. Usage: java NordlichtArt <resDir> <storeDir> */
public class NordlichtArt {
    static final Color BG_TOP = new Color(0x0B1020);
    static final Color BG_BOTTOM = new Color(0x05060A);
    static final Color[] AURORA = {new Color(0x3DFFB0), new Color(0x22C7D9), new Color(0x7B6CFF)};

    public static void main(String[] a) throws Exception {
        String res = a[0], store = a[1];
        new File(store).mkdirs();
        int[] fg = {108, 162, 216, 324, 432};
        int[] legacy = {48, 72, 96, 144, 192};
        String[] dpi = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int i = 0; i < dpi.length; i++) {
            String d = res + "/mipmap-" + dpi[i] + "/";
            save(foreground(fg[i]), d + "ic_launcher_foreground.png");
            save(fullIcon(legacy[i], false), d + "ic_launcher.png");
            save(fullIcon(legacy[i], true), d + "ic_launcher_round.png");
        }
        save(banner(320, 180, true), res + "/mipmap-xhdpi/ic_banner.png");
        save(banner(1280, 720, true), store + "/tv-banner-1280x720.png");
        save(fullIcon(512, false), store + "/icon-512.png");
        save(feature(1024, 500), store + "/feature-graphic-1024x500.png");
    }

    static Graphics2D g(BufferedImage im) {
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static void background(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g.fillRect(0, 0, w, h);
    }

    /** Logo E: aurora curtain inside a dark circle, fitted to the box (x,y,w,h). */
    static void aurora(Graphics2D g, double x, double y, double w, double h) {
        double d = Math.min(w, h), bx = x + (w - d) / 2, by = y + (h - d) / 2;
        Ellipse2D ball = new Ellipse2D.Double(bx, by, d, d);
        g.setPaint(new Color(0x070A14));
        g.fill(ball);
        Shape old = g.getClip();
        g.clip(ball);
        double cxw = d * 1.086, cyh = d;
        curtain(g, bx - d * 0.043, by - d * 0.014, cxw, cyh, 0.07);
        g.setClip(old);
        g.setPaint(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke((float) Math.max(1, d * 0.006)));
        g.draw(ball);
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t), (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static void curtain(Graphics2D g, double x, double y, double w, double h, double amp) {
        int n = 90;
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            double px = x + t * w;
            double base = y + h * 0.78 + Math.sin(t * Math.PI * 2.2) * h * amp;
            double top = base - h * (0.45 + 0.25 * Math.sin(t * Math.PI * 3.1 + 1) * Math.sin(t * Math.PI));
            Color c = t < 0.5 ? mix(AURORA[0], AURORA[1], t * 2) : mix(AURORA[1], AURORA[2], (t - 0.5) * 2);
            g.setPaint(new GradientPaint((float) px, (float) base, withAlpha(c, 230), (float) px, (float) top, withAlpha(c, 0)));
            g.setStroke(new BasicStroke((float) (w / n * 1.3), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(px, base, px, top));
        }
        Path2D edge = new Path2D.Double();
        for (int i = 0; i <= 60; i++) {
            double t = i / 60.0;
            double px = x + t * w, py = y + h * 0.78 + Math.sin(t * Math.PI * 2.2) * h * amp;
            if (i == 0) edge.moveTo(px, py); else edge.lineTo(px, py);
        }
        g.setPaint(new GradientPaint((float) x, 0, withAlpha(AURORA[0], 255), (float) (x + w), 0, withAlpha(AURORA[2], 220)));
        g.setStroke(new BasicStroke((float) (h * 0.025), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(edge);
    }

    static Color withAlpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    /** Adaptive-icon foreground: transparent, motif inside the 66/108 safe zone. */
    static BufferedImage foreground(int s) {
        BufferedImage im = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(im);
        double inset = s * 21.0 / 108.0, box = s - 2 * inset;
        aurora(g, inset, inset, box, box);
        g.dispose();
        return im;
    }

    static BufferedImage fullIcon(int s, boolean round) {
        BufferedImage im = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(im);
        Shape clip = round ? new Ellipse2D.Double(0, 0, s, s) : new RoundRectangle2D.Double(0, 0, s, s, s * 0.22, s * 0.22);
        if (s == 512) clip = new Rectangle(0, 0, s, s); // Play adds its own mask
        g.setClip(clip);
        background(g, s, s);
        aurora(g, s * 0.15, s * 0.15, s * 0.70, s * 0.70);
        g.dispose();
        return im;
    }

    static void wordmark(Graphics2D g, String text, double x, double baseline, double size) {
        Font f = new Font("Avenir Next", Font.BOLD, (int) size);
        if (!f.getFamily().startsWith("Avenir")) f = new Font(Font.SANS_SERIF, Font.BOLD, (int) size);
        g.setFont(f);
        g.setPaint(new Color(0xF2F5F8));
        g.drawString(text, (float) x, (float) baseline);
    }

    static BufferedImage banner(int w, int h, boolean withText) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(im);
        background(g, w, h);
        double s = h * 0.56;
        double x0 = w * 0.08;
        aurora(g, x0, (h - s) / 2, s, s);
        if (withText) {
            double tx = x0 + s * 1.12, avail = w * 0.92 - tx, size = h * 0.20;
            Font f = new Font("Avenir Next", Font.BOLD, (int) size);
            double tw = g.getFontMetrics(f).stringWidth("nordlicht");
            if (tw > avail) size *= avail / tw;
            wordmark(g, "nordlicht", tx, h / 2 + size * 0.36, size);
        }
        g.dispose();
        return im;
    }

    static BufferedImage feature(int w, int h) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(im);
        background(g, w, h);
        aurora(g, w * 0.70, h * 0.18, h * 0.64, h * 0.64);
        g.setPaint(new GradientPaint(0, 0, withAlpha(BG_BOTTOM, 235), (float) (w * 0.62), 0, withAlpha(BG_BOTTOM, 0)));
        g.fillRect(0, 0, w, h);
        wordmark(g, "nordlicht", w * 0.07, h * 0.50, h * 0.19);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, (float) (h * 0.065)));
        g.setPaint(new Color(0xB8C2CF));
        g.drawString("Deine Jellyfin-Mediathek auf dem Fernseher", (float) (w * 0.07), (float) (h * 0.64));
        g.dispose();
        return im;
    }

    static void save(BufferedImage im, String path) throws Exception {
        new File(path).getParentFile().mkdirs();
        ImageIO.write(im, "png", new File(path));
        System.out.println("wrote " + path);
    }
}
