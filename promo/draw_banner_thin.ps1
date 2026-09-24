Add-Type -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class PixelBanner {
    const int G = 160;      // grid cells wide
    const int H = 30;       // grid cells tall (thin banner)
    const int SC = 12;      // 160*12=1920, 30*12=360

    static Color[,] cell = new Color[G, H];

    static void Set(int x, int y, Color c) {
        if (x >= 0 && x < G && y >= 0 && y < H) cell[x, y] = c;
    }

    static void Disc(int cx, int cy, double r, Color c) {
        int ri = (int)Math.Ceiling(r);
        for (int y = cy - ri; y <= cy + ri; y++)
            for (int x = cx - ri; x <= cx + ri; x++)
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r + r * 0.5)
                    Set(x, y, c);
    }

    static void Blend(int x, int y, Color c, double a) {
        if (x < 0 || x >= G || y < 0 || y >= H) return;
        Color bg = cell[x, y];
        cell[x, y] = Color.FromArgb(
            (int)(c.R * a + bg.R * (1 - a)),
            (int)(c.G * a + bg.G * (1 - a)),
            (int)(c.B * a + bg.B * (1 - a)));
    }

    static void HLine(int x0, int x1, int y, Color c) {
        for (int x = x0; x <= x1; x++) Set(x, y, c);
    }

    public static void Generate(string path, int outW) {
        double SCx = (double)outW / G;
        double SCy = SCx;
        int outH = (int)Math.Round(H * SCy);
        double sunX = 26, sunY = 15;

        // ---- background gradient (horizontal dark->blue->dark for depth)
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < G; x++) {
                double tv = (double)y / (H - 1);
                double th = Math.Abs((double)x / (G - 1) - 0.5) * 2; // 0 center, 1 edges
                int br = (int)((22 - 12 * th) * (1 - tv * 0.55));
                int bg = (int)((30 - 16 * th) * (1 - tv * 0.55));
                int bb = (int)((64 - 34 * th) * (1 - tv * 0.55));
                cell[x, y] = Color.FromArgb(br + 6, bg + 7, bb + 12);
            }
        }

        // ---- stars
        Random rnd = new Random(555);
        for (int i = 0; i < 130; i++) {
            int x = rnd.Next(G), y = rnd.Next(H);
            double d = Math.Sqrt((x - sunX) * (x - sunX) + (y - sunY) * (y - sunY));
            if (d < 12) continue;
            int a = rnd.Next(80, 230);
            Blend(x, y, Color.FromArgb(205, 218, 255), a / 255.0);
        }
        // a few brighter cross-stars
        int[][] brights = { new int[]{12,5}, new int[]{148,8}, new int[]{95,3}, new int[]{60,26}, new int[]{130,24}, new int[]{8,24}, new int[]{110,22} };
        foreach (int[] bs in brights) {
            int x = bs[0], y = bs[1];
            Set(x, y, Color.FromArgb(255, 235, 242, 255));
            Blend(x - 1, y, Color.FromArgb(180, 200, 230), 0.5);
            Blend(x + 1, y, Color.FromArgb(180, 200, 230), 0.5);
            Blend(x, y - 1, Color.FromArgb(180, 200, 230), 0.5);
            Blend(x, y + 1, Color.FromArgb(180, 200, 230), 0.5);
        }

        // ---- orbit arcs (ellipses clipped by banner height) - subtle
        double[] oR = { 30, 46, 62, 78 };
        Color[] oC = {
            Color.FromArgb(120, 90, 140, 205),
            Color.FromArgb(120, 205, 135, 90),
            Color.FromArgb(120, 105, 180, 95),
            Color.FromArgb(120, 176, 110, 217)
        };
        for (int k = 0; k < 4; k++) {
            double rx = oR[k], ry = oR[k] * 0.52;
            for (int i = 0; i <= 360; i += 2) {
                double a = i * Math.PI / 180.0;
                int x = (int)Math.Round(sunX + rx * Math.Cos(a));
                int y = (int)Math.Round(sunY + ry * Math.Sin(a));
                // dashed look
                if ((i / 2) % 2 == 0)
                    Blend(x, y, oC[k], 0.75);
            }
        }

        // ---- sun (left anchor) with layered glow
        Disc((int)sunX, (int)sunY, 11.5, Color.FromArgb(255, 70, 50, 26));
        Disc((int)sunX, (int)sunY, 9.5, Color.FromArgb(255, 150, 90, 30));
        Disc((int)sunX, (int)sunY, 7.5, Color.FromArgb(255, 255, 150, 45));
        Disc((int)sunX, (int)sunY, 5.5, Color.FromArgb(255, 255, 200, 80));
        Disc((int)(sunX - 2), (int)(sunY - 2), 2.4, Color.FromArgb(255, 255, 240, 190));

        // ---- planets on the orbits (right side of the banner)
        // angle chosen so planets sit nicely across the visible strip
        // planets placed by angle on their own orbit ellipse (right side of the sun)
        double[] pAng = { -0.35, 0.42, -0.28, 0.25 };
        int[][] pCol = {
            new int[]{74,144,217}, new int[]{217,131,74}, new int[]{106,176,74}, new int[]{176,74,217}
        };
        double[] pSize = { 3.2, 2.6, 3.0, 2.2 };
        for (int k = 0; k < 4; k++) {
            double rx = oR[k], ry = oR[k] * 0.52;
            int px = (int)Math.Round(sunX + rx * Math.Cos(pAng[k]));
            int py = (int)Math.Round(sunY + ry * Math.Sin(pAng[k]));
            // atmosphere
            Disc(px, py, pSize[k] + 1.6, Color.FromArgb(pCol[k][0], pCol[k][1], pCol[k][2]));
            // dark limb
            Disc(px, py, pSize[k], Color.FromArgb((int)(pCol[k][0]*0.45), (int)(pCol[k][1]*0.45), (int)(pCol[k][2]*0.45)));
            // lit face toward the sun (sun is to the left -> lit side left)
            Disc(px - 1, py - 1, pSize[k] * 0.82, Color.FromArgb(pCol[k][0], pCol[k][1], pCol[k][2]));
            // specular
            Set(px - 2, py - 2, Color.FromArgb(230, 255, 255, 255));
        }

        // ---- small trailing "comet" detail on the far right for flavor
        int comX = 146, comY = 7;
        Set(comX, comY, Color.FromArgb(255, 230, 240, 255));
        for (int t = 1; t <= 5; t++)
            Blend(comX + t, comY + (t / 2), Color.FromArgb(200, 220, 255), 0.6 - t * 0.09);

        // ---- render (exact outW width; cells scaled with float math, +0.5 to avoid seams)
        Bitmap bmp = new Bitmap(outW, outH, PixelFormat.Format32bppArgb);
        Graphics g = Graphics.FromImage(bmp);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < G; x++)
                using (SolidBrush b = new SolidBrush(cell[x, y]))
                    g.FillRectangle(b, (float)(x * SCx), (float)(y * SCy), (float)(SCx + 0.5), (float)(SCy + 0.5));
        g.Dispose();
        bmp.Save(path, ImageFormat.Png);
        bmp.Dispose();
    }
}
'@ -ReferencedAssemblies System.Drawing

[PixelBanner]::Generate('D:\OpenCode\Mod\promo\banner_thin.png', 850)
Write-Output 'thin banner done'
