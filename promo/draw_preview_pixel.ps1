Add-Type -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Text;

public static class PixelPreview {
    const int G = 128;      // grid cells wide
    const int H = 72;       // grid cells tall
    const int SC = 15;      // pixel size (128*15=1920, 72*15=1080)

    static Color[,] cell = new Color[G, H];

    static void Set(int x, int y, Color c) {
        if (x < 0 || x >= G || y < 0 || y >= H) return;
        if (c.A < 255) {
            // pre-blend against the current cell so the final PNG stays fully opaque
            Color bg = cell[x, y];
            double a = c.A / 255.0;
            cell[x, y] = Color.FromArgb(
                (int)(c.R * a + bg.R * (1 - a)),
                (int)(c.G * a + bg.G * (1 - a)),
                (int)(c.B * a + bg.B * (1 - a)));
        } else {
            cell[x, y] = c;
        }
    }

    static void Disc(double cx, double cy, double r, Color c) {
        for (int y = (int)(cy - r - 1); y <= (int)(cy + r + 1); y++)
            for (int x = (int)(cx - r - 1); x <= (int)(cx + r + 1); x++) {
                double dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy <= r * r) Set(x, y, c);
            }
    }

    static void Line(int x0, int y0, int x1, int y1, Color c) {
        int dx = Math.Abs(x1 - x0), dy = Math.Abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            Set(x0, y0, c);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    static void Ellipse(double cx, double cy, double rx, double ry, Color c) {
        for (int y = (int)(cy - ry - 1); y <= (int)(cy + ry + 1); y++)
            for (int x = (int)(cx - rx - 1); x <= (int)(cx + rx + 1); x++) {
                double dx = (x - cx) / rx, dy = (y - cy) / ry;
                double d = Math.Sqrt(dx * dx + dy * dy);
                if (Math.Abs(d - 1.0) < 0.085) Set(x, y, c);
            }
    }

    static void Node(int x, int y, Color rim, Color icon) {
        // 3x3 medallion: rim border + dark face + icon center
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                Set(x + dx, y + dy, (dx == 0 && dy == 0) ? icon :
                    (Math.Abs(dx) == 1 && Math.Abs(dy) == 1 ? rim : Color.FromArgb(255, 22, 28, 48)));
    }

    public static void Generate(string path) {
        double sunX = 64, sunY = 38;

        // ---- background: row gradient + vignette
        for (int y = 0; y < H; y++) {
            double tv = (double)y / (H - 1);
            int br = (int)(20 + (7 - 20) * tv);
            int bg = (int)(26 + (10 - 26) * tv);
            int bb = (int)(56 + (21 - 56) * tv);
            for (int x = 0; x < G; x++) {
                double dx = (x - sunX) / 70.0, dy = (y - sunY) / 40.0;
                double v = 1.0 - 0.35 * Math.Min(1.0, Math.Sqrt(dx * dx + dy * dy));
                cell[x, y] = Color.FromArgb((int)(br * v), (int)(bg * v), (int)(bb * v));
            }
        }

        // ---- stars
        Random rnd = new Random(77);
        for (int i = 0; i < 150; i++) {
            int x = rnd.Next(G), y = rnd.Next(H);
            double dx = x - sunX, dy = y - sunY;
            if (dx * dx + dy * dy < 400) continue; // keep clear around the sun
            int a = rnd.Next(90, 255);
            Set(x, y, Color.FromArgb(a, 205, 218, 255));
        }

        // ---- planet orbits (own colored track each, elliptical)
        double[] oR = { 22, 28, 34 };
        Color[] oC = { Color.FromArgb(120, 90, 140, 205), Color.FromArgb(120, 205, 135, 90), Color.FromArgb(120, 105, 180, 95) };
        for (int k = 0; k < 3; k++)
            Ellipse(sunX, sunY, oR[k], oR[k] * 0.62, oC[k]);

        // ---- sun rings (create branch): two rings of nodes
        double[] ringR = { 11, 16 };
        int[] ringN = { 6, 12 };
        int[][] nodeX = new int[2][];
        int[][] nodeY = new int[2][];
        for (int r = 0; r < 2; r++) {
            nodeX[r] = new int[ringN[r]];
            nodeY[r] = new int[ringN[r]];
            Ellipse(sunX, sunY, ringR[r], ringR[r], Color.FromArgb(55, 70, 95, 145));
            for (int j = 0; j < ringN[r]; j++) {
                double a = 0.35 * (r + 1) + 2 * Math.PI * j / ringN[r];
                nodeX[r][j] = (int)Math.Round(sunX + ringR[r] * Math.Cos(a));
                nodeY[r][j] = (int)Math.Round(sunY + ringR[r] * Math.Sin(a));
            }
        }
        // links: sun -> ring0
        for (int j = 0; j < ringN[0]; j++) {
            Color lc = j < 4 ? Color.FromArgb(235, 200, 100) : Color.FromArgb(75, 85, 115);
            Line((int)sunX, (int)sunY, nodeX[0][j], nodeY[0][j], lc);
        }
        // links: ring0 -> ring1 (each ring1 node to two nearest ring0)
        for (int j = 0; j < ringN[1]; j++) {
            int best = 0; double bd = 1e9;
            for (int q = 0; q < ringN[0]; q++) {
                double dx = nodeX[0][q] - nodeX[1][j], dy = nodeY[0][q] - nodeY[1][j];
                if (dx * dx + dy * dy < bd) { bd = dx * dx + dy * dy; best = q; }
            }
            int second = (best + 1) % ringN[0];
            Line(nodeX[0][best], nodeY[0][best], nodeX[1][j], nodeY[1][j], Color.FromArgb(75, 85, 115));
            Line(nodeX[0][second], nodeY[0][second], nodeX[1][j], nodeY[1][j], Color.FromArgb(60, 68, 95));
        }
        // nodes ring0 / ring1 (unlocked=green rim, locked=dim)
        for (int j = 0; j < ringN[0]; j++)
            Node(nodeX[0][j], nodeY[0][j], j < 4 ? Color.FromArgb(95, 185, 95) : Color.FromArgb(150, 120, 60), Color.FromArgb(130, 205, 130));
        for (int j = 0; j < ringN[1]; j++)
            Node(nodeX[1][j], nodeY[1][j], j < 3 ? Color.FromArgb(95, 185, 95) : Color.FromArgb(95, 100, 115), j < 3 ? Color.FromArgb(130, 205, 130) : Color.FromArgb(125, 130, 145));

        // ---- sun body
        Disc(sunX, sunY, 8.6, Color.FromArgb(255, 92, 66, 34));
        Disc(sunX, sunY, 7.0, Color.FromArgb(255, 255, 140, 40));
        Disc(sunX, sunY, 5.6, Color.FromArgb(255, 195, 70));
        Disc(sunX - 1.2, sunY - 1.2, 2.6, Color.FromArgb(255, 240, 190));

        // ---- planets with local nodes
        double[] pAng = { -1.75, -0.15, 2.55 };
        Color[][] pCol = new Color[][] {
            new Color[] { Color.FromArgb(60, 105, 175), Color.FromArgb(120, 175, 235) },
            new Color[] { Color.FromArgb(175, 105, 60), Color.FromArgb(240, 170, 115) },
            new Color[] { Color.FromArgb(80, 145, 70), Color.FromArgb(140, 205, 125) }
        };
        string[] pName = { "FLUIDS", "LOGISTICS", "CONTRAPTIONS" };
        double[] px = new double[3], py = new double[3];
        for (int k = 0; k < 3; k++) {
            px[k] = sunX + oR[k] * Math.Cos(pAng[k]);
            py[k] = sunY + oR[k] * 0.62 * Math.Sin(pAng[k]);
            // local nodes (4) with dim links
            for (int j = 0; j < 4; j++) {
                double a = 0.5 + 2 * Math.PI * j / 4;
                int nx = (int)Math.Round(px[k] + 5 * Math.Cos(a));
                int ny = (int)Math.Round(py[k] + 5 * Math.Sin(a));
                Line((int)px[k], (int)py[k], nx, ny, Color.FromArgb(70, 80, 110));
                Node(nx, ny, Color.FromArgb(95, 100, 118), Color.FromArgb(125, 130, 148));
            }
            // planet body: dark limb, lit side toward sun, highlight
            Disc(px[k], py[k], 3.2, Color.FromArgb(30, pCol[k][0].R / 2, pCol[k][0].G / 2, pCol[k][0].B / 2));
            Disc(px[k], py[k], 2.6, pCol[k][0]);
            double lx = sunX - px[k], ly = sunY - py[k];
            double ll = Math.Sqrt(lx * lx + ly * ly);
            Disc(px[k] + lx / ll * 0.9, py[k] + ly / ll * 0.9, 1.9, pCol[k][1]);
            Set((int)(px[k] + lx / ll * 1.6), (int)(py[k] + ly / ll * 1.6), Color.FromArgb(235, 245, 255));
        }

        // ---- render cells
        Bitmap bmp = new Bitmap(G * SC, H * SC, PixelFormat.Format32bppArgb);
        Graphics g = Graphics.FromImage(bmp);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < G; x++)
                using (SolidBrush b = new SolidBrush(cell[x, y]))
                    g.FillRectangle(b, x * SC, y * SC, SC, SC);

        // ---- crisp pixel-style text on top
        g.TextRenderingHint = TextRenderingHint.SingleBitPerPixelGridFit;
        StringFormat cf = new StringFormat(); cf.Alignment = StringAlignment.Center;

        using (Font f = new Font("Segoe UI", 76, FontStyle.Bold))
        using (SolidBrush b = new SolidBrush(Color.FromArgb(255, 240, 200, 130)))
            g.DrawString("CREATE: CONSTELLATION", f, b, G * SC / 2, 26, cf);
        using (Font f = new Font("Segoe UI", 30))
        using (SolidBrush b = new SolidBrush(Color.FromArgb(190, 165, 178, 208)))
            g.DrawString("A skill tree that gates Create recipes", f, b, G * SC / 2, 150, cf);

        for (int k = 0; k < 3; k++) {
            using (Font f = new Font("Segoe UI", 19, FontStyle.Bold))
            using (SolidBrush b = new SolidBrush(Color.FromArgb(230, 225, 232, 250)))
                g.DrawString(pName[k], f, b, (float)px[k] * SC + SC / 2, (float)(py[k] + 5.5) * SC, cf);
        }

        using (Font f = new Font("Segoe UI", 27))
        using (SolidBrush b = new SolidBrush(Color.FromArgb(210, 150, 162, 192)))
            g.DrawString("craft  >  earn EXP  >  get points  >  unlock   |   open with [I]", f, b, G * SC / 2, H * SC - 64, cf);

        g.Dispose();
        bmp.Save(path, ImageFormat.Png);
        bmp.Dispose();
    }
}
'@ -ReferencedAssemblies System.Drawing

[PixelPreview]::Generate('D:\OpenCode\Mod\promo\preview1_system.png')
Write-Output 'pixel preview1 done'
