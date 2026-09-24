Add-Type -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class TitlePreview {
    const int G = 192, H = 108, SC = 10; // 1920x1080
    static Color[,] cell = new Color[G, H];

    static void Set(int x, int y, Color c) { if (x >= 0 && x < G && y >= 0 && y < H) cell[x, y] = c; }
    static void Blend(int x, int y, Color c, double a) {
        if (x < 0 || x >= G || y < 0 || y >= H) return;
        Color bg = cell[x, y];
        cell[x, y] = Color.FromArgb((int)(c.R * a + bg.R * (1 - a)), (int)(c.G * a + bg.G * (1 - a)), (int)(c.B * a + bg.B * (1 - a)));
    }
    static void Disc(int cx, int cy, double r, Color c) {
        int ri = (int)Math.Ceiling(r);
        for (int y = cy - ri; y <= cy + ri; y++)
            for (int x = cx - ri; x <= cx + ri; x++)
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r + r * 0.5) Set(x, y, c);
    }
    static void DiscBlend(int cx, int cy, double r, Color c, double a) {
        int ri = (int)Math.Ceiling(r);
        for (int y = cy - ri; y <= cy + ri; y++)
            for (int x = cx - ri; x <= cx + ri; x++)
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r + r * 0.5) Blend(x, y, c, a);
    }
    static void Ring(int cx, int cy, double r, Color c, int dotEvery) {
        int ri = (int)Math.Ceiling(r);
        double step = Math.PI / (ri * 2);
        int i = 0;
        for (double a = 0; a < Math.PI * 2; a += step) {
            if (i % dotEvery == 0)
                Set(cx + (int)Math.Round(r * Math.Cos(a)), cy + (int)Math.Round(r * Math.Sin(a)), c);
            i++;
        }
    }

    // a small golden medallion node (rim + dark face + icon square), as in the GUI
    static void Node(int x, int y, Color rim, Color icon) {
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++) {
                bool edge = Math.Abs(dx) == 2 || Math.Abs(dy) == 2;
                Set(x + dx, y + dy, edge ? rim : Color.FromArgb(255, 20, 26, 46));
            }
        Set(x - 1, y - 1, icon); Set(x, y - 1, icon); Set(x + 1, y - 1, icon);
        Set(x - 1, y, icon); Set(x, y, icon); Set(x + 1, y, icon);
        Set(x - 1, y + 1, icon); Set(x, y + 1, icon); Set(x + 1, y + 1, icon);
    }

    static void StarLine(double x0, double y0, double x1, double y1, Color c) {
        int steps = (int)Math.Max(Math.Abs(x1 - x0), Math.Abs(y1 - y0));
        for (int s = 0; s <= steps; s++) {
            double t = s / (double)steps;
            Blend((int)Math.Round(x0 + (x1 - x0) * t), (int)Math.Round(y0 + (y1 - y0) * t), c, 1.0);
        }
    }

    public static void Generate(string path) {
        // ---- background: vertical gradient + vignette
        for (int y = 0; y < H; y++) {
            double tv = (double)y / (H - 1);
            for (int x = 0; x < G; x++) {
                double th = Math.Abs((double)x / (G - 1) - 0.5) * 2;
                int r = (int)((14 - 6 * th) * (1 - tv * 0.45));
                int gc = (int)((20 - 9 * th) * (1 - tv * 0.45));
                int b = (int)((44 - 22 * th) * (1 - tv * 0.45));
                cell[x, y] = Color.FromArgb(r + 5, gc + 6, b + 10);
            }
        }

        // ---- stars
        Random rnd = new Random(20260912);
        for (int i = 0; i < 240; i++) {
            int x = rnd.Next(G), y = rnd.Next(H);
            double d = Math.Sqrt((x - 96) * (x - 96) + (y - 54) * (y - 54));
            if (d < 42) continue; // keep the system area clean
            Blend(x, y, Color.FromArgb(210, 222, 255), 0.3 + rnd.NextDouble() * 0.6);
        }
        int[][] brights = { new int[]{16,18}, new int[]{176,20}, new int[]{120,12}, new int[]{40,96}, new int[]{150,98}, new int[]{8,54}, new int[]{184,60}, new int[]{64,102}, new int[]{136,14}, new int[]{24,72}, new int[]{168,86}, new int[]{100,8} };
        foreach (int[] bs in brights) {
            int x = bs[0], y = bs[1];
            Set(x, y, Color.FromArgb(255, 240, 246, 255));
            Blend(x - 1, y, Color.FromArgb(190, 205, 235), 0.55);
            Blend(x + 1, y, Color.FromArgb(190, 205, 235), 0.55);
            Blend(x, y - 1, Color.FromArgb(190, 205, 235), 0.55);
            Blend(x, y + 1, Color.FromArgb(190, 205, 235), 0.55);
        }

        // ---- comet top-right
        int comX = 174, comY = 13;
        Set(comX, comY, Color.FromArgb(255, 240, 248, 255));
        for (int t = 1; t <= 8; t++)
            Blend(comX - t, comY + t / 2, Color.FromArgb(200, 220, 255), 0.7 - t * 0.08);

        // ---- solar system, centered
        int sunX = 96, sunY = 54;

        // orbits (dashed, colored per planet)
        double[] oR = { 26, 37, 48 };
        Color[] oC = { Color.FromArgb(150, 90, 140, 205), Color.FromArgb(150, 205, 135, 90), Color.FromArgb(150, 105, 180, 95) };
        for (int k = 0; k < 3; k++)
            Ring(sunX, sunY, oR[k], oC[k], 3);

        // main-branch links FIRST (under the sun body), six golden lines + medallions
        Color rimGold = Color.FromArgb(255, 231, 195, 106);
        Color rimDim = Color.FromArgb(255, 95, 100, 115);
        for (int j = 0; j < 6; j++) {
            double a = 0.35 + Math.PI * 2 * j / 6;
            int nx = (int)Math.Round(sunX + 16 * Math.Cos(a));
            int ny = (int)Math.Round(sunY + 16 * Math.Sin(a));
            StarLine(sunX, sunY, nx, ny, Color.FromArgb(255, 170, 140, 70));
        }
        for (int j = 0; j < 6; j++) {
            double a = 0.35 + Math.PI * 2 * j / 6;
            int nx = (int)Math.Round(sunX + 16 * Math.Cos(a));
            int ny = (int)Math.Round(sunY + 16 * Math.Sin(a));
            Node(nx, ny, j < 4 ? rimGold : rimDim, j < 4 ? Color.FromArgb(255, 255, 214, 110) : Color.FromArgb(255, 125, 130, 145));
        }

        // sun: layered glow + body + highlight (drawn over the inner link ends)
        DiscBlend(sunX, sunY, 17, Color.FromArgb(255, 176, 96), 0.10);
        DiscBlend(sunX, sunY, 13, Color.FromArgb(255, 192, 80), 0.14);
        Disc(sunX, sunY, 8.5, Color.FromArgb(255, 110, 60, 20));
        Disc(sunX, sunY, 7.5, Color.FromArgb(255, 255, 150, 48));
        Disc(sunX, sunY, 5.8, Color.FromArgb(255, 255, 192, 72));
        Disc(sunX, sunY, 3.6, Color.FromArgb(255, 255, 216, 110));
        Disc(sunX - 2, sunY - 2, 2.0, Color.FromArgb(255, 255, 244, 196));

        // wrench hint inside the sun (dark notch + handle), tiny
        Set(sunX - 3, sunY + 3, Color.FromArgb(255, 120, 66, 28));
        Set(sunX - 2, sunY + 2, Color.FromArgb(255, 140, 76, 32));
        Set(sunX + 2, sunY - 2, Color.FromArgb(255, 140, 76, 32));
        Set(sunX + 3, sunY - 3, Color.FromArgb(255, 120, 66, 28));

        // planets on their orbits + node medallions orbiting each planet
        double[] pAng = { -0.7, 0.45, 2.3 };
        int[][] pCol = { new int[]{74,144,217}, new int[]{217,131,74}, new int[]{106,176,74} };
        double[] pSize = { 4.6, 4.0, 3.4 };
        for (int k = 0; k < 3; k++) {
            int px = (int)Math.Round(sunX + oR[k] * Math.Cos(pAng[k]));
            int py = (int)Math.Round(sunY + oR[k] * Math.Sin(pAng[k]));

            // planet body: atmosphere, dark limb, lit face toward the sun, specular
            DiscBlend(px, py, pSize[k] + 2.2, Color.FromArgb(pCol[k][0], pCol[k][1], pCol[k][2]), 0.28);
            Disc(px, py, pSize[k], Color.FromArgb((int)(pCol[k][0] * 0.45), (int)(pCol[k][1] * 0.45), (int)(pCol[k][2] * 0.45)));
            double lx = sunX - px, ly = sunY - py;
            double ll = Math.Sqrt(lx * lx + ly * ly);
            Disc(px + (int)(lx / ll * 1.2), py + (int)(ly / ll * 1.2), pSize[k] * 0.78, Color.FromArgb(pCol[k][0], pCol[k][1], pCol[k][2]));
            Set(px + (int)(lx / ll * 2.4), py + (int)(ly / ll * 2.4), Color.FromArgb(235, 255, 255, 255));

            // 3-4 medallion nodes around the planet, linked with golden lines (the branch tree)
            int nCount = 4 - k / 2;
            double nodeOrbit = pSize[k] + 5.5;
            for (int j = 0; j < nCount; j++) {
                double a = pAng[k] + Math.PI * 2 * j / nCount + 0.4;
                int nx = (int)Math.Round(px + nodeOrbit * Math.Cos(a));
                int ny = (int)Math.Round(py + nodeOrbit * Math.Sin(a));
                StarLine(px, py, nx, ny, Color.FromArgb(255, 150, 128, 70));
                Node(nx, ny, j < nCount - 1 ? rimGold : rimDim,
                     j < nCount - 1 ? Color.FromArgb(255, 255, 214, 110) : Color.FromArgb(255, 125, 130, 145));
            }
        }

        // ---- render (integer scale, no seams)
        Bitmap bmp = new Bitmap(G * SC, H * SC, PixelFormat.Format32bppArgb);
        Graphics g = Graphics.FromImage(bmp);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < G; x++)
                using (SolidBrush b = new SolidBrush(cell[x, y]))
                    g.FillRectangle(b, x * SC, y * SC, SC, SC);
        g.Dispose();
        bmp.Save(path, ImageFormat.Png);
        bmp.Dispose();
    }
}
'@ -ReferencedAssemblies System.Drawing

[TitlePreview]::Generate('D:\OpenCode\Mod\promo\preview_main.png')
Write-Output 'main preview (no text) done'
