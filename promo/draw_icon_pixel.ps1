Add-Type -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class PixelIcon {
    public static void Generate(string path, int grid, int scale) {
        int S = grid * scale;
        Bitmap bmp = new Bitmap(S, S, PixelFormat.Format32bppArgb);
        Graphics g = Graphics.FromImage(bmp);

        double cx = (grid - 1) / 2.0;
        double cy = (grid - 1) / 2.0;
        double orbitR = grid * 0.375;
        double sunCore = grid * 0.14;

        double[] pAng = { -Math.PI / 2, Math.PI * 0.18, Math.PI * 0.82 };
        Color[,] pCol = {
            { Color.FromArgb(240, 205, 130), Color.FromArgb(245, 230, 185) },  // top planet (gold-ish)
            { Color.FromArgb(75, 135, 215),  Color.FromArgb(145, 195, 245) },  // right planet (blue)
            { Color.FromArgb(225, 130, 75),  Color.FromArgb(250, 185, 130) }   // left planet (orange)
        };

        for (int py = 0; py < grid; py++) {
            for (int px = 0; px < grid; px++) {
                double dx = px - cx, dy = py - cy;
                double dist = Math.Sqrt(dx * dx + dy * dy);

                // 1) background gradient by row
                double tv = (double)py / (grid - 1);
                Color c = Color.FromArgb(
                    (int)(22 + (8 - 22) * tv),
                    (int)(28 + (11 - 28) * tv),
                    (int)(58 + (24 - 58) * tv));

                // 2) fixed star pixels
                bool star = (px == 3 && py == 4) || (px == 27 && py == 3) || (px == 5 && py == 26)
                         || (px == 28 && py == 27) || (px == 15 && py == 2) || (px == 2 && py == 15)
                         || (px == 29 && py == 14) || (px == 10 && py == 30) || (px == 22 && py == 29)
                         || (px == 6 && py == 8) || (px == 25 && py == 22) || (px == 19 && py == 6);
                if (star) c = Color.FromArgb(205, 218, 255);

                // 3) orbit ring
                if (Math.Abs(dist - orbitR) < 0.62)
                    c = Color.FromArgb(95, 115, 168);

                // 4) gold links from sun to each planet
                for (int p = 0; p < 3; p++) {
                    double qx = cx + orbitR * Math.Cos(pAng[p]);
                    double qy = cy + orbitR * Math.Sin(pAng[p]);
                    double lx = qx - cx, ly = qy - cy;
                    double len2 = lx * lx + ly * ly;
                    double t = ((dx) * lx + (dy) * ly) / len2;
                    if (t > 0.08 && t < 0.92) {
                        double projX = cx + lx * t - px, projY = cy + ly * t - py;
                        if (Math.Sqrt(projX * projX + projY * projY) < 0.55)
                            c = Color.FromArgb(235, 195, 95);
                    }
                }

                // 5) planets
                for (int p = 0; p < 3; p++) {
                    double qx = cx + orbitR * Math.Cos(pAng[p]);
                    double qy = cy + orbitR * Math.Sin(pAng[p]);
                    double pd = Math.Sqrt((px - qx) * (px - qx) + (py - qy) * (py - qy));
                    if (pd < 2.3) {
                        // lit side faces the sun
                        double towardX = (cx - qx), towardY = (cy - qy);
                        double tl = Math.Sqrt(towardX * towardX + towardY * towardY);
                        double lit = ((px - qx) * towardX / tl + (py - qy) * towardY / tl) / Math.Max(0.001, pd);
                        c = lit > 0.35 ? pCol[p, 1] : pCol[p, 0];
                    }
                }

                // 6) sun glow + core
                if (dist < sunCore + 1.6 && dist >= sunCore - 0.2)
                    c = Color.FromArgb(255, 150, 45);        // rim
                if (dist < sunCore - 0.2)
                    c = Color.FromArgb(255, 200, 75);        // body
                if (dist < sunCore * 0.5)
                    c = Color.FromArgb(255, 240, 190);       // hot center
                if (dist >= sunCore + 1.6 && dist < sunCore + 2.6 && !star)
                    c = Color.FromArgb(70, 55, 30);          // faint warm halo pixels

                using (SolidBrush b = new SolidBrush(c))
                    g.FillRectangle(b, px * scale, py * scale, scale, scale);
            }
        }

        g.Dispose();
        bmp.Save(path, ImageFormat.Png);
        bmp.Dispose();
    }
}
'@ -ReferencedAssemblies System.Drawing

[PixelIcon]::Generate('D:\OpenCode\Mod\promo\icon_pixel.png', 32, 16)
Write-Output 'pixel icon done'
