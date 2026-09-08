Add-Type -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Text;

public static class PixelPreview2 {
    const int G = 128;      // grid cells wide
    const int H = 72;       // grid cells tall
    const int SC = 15;      // 128*15=1920, 72*15=1080

    static Color[,] cell = new Color[G, H];

    static void Set(int x, int y, Color c) {
        if (x >= 0 && x < G && y >= 0 && y < H) cell[x, y] = c;
    }

    static void Rect(int x, int y, int w, int h, Color c) {
        for (int j = y; j < y + h; j++)
            for (int i = x; i < x + w; i++)
                Set(i, j, c);
    }

    static void RectOutline(int x, int y, int w, int h, Color c) {
        for (int i = x; i < x + w; i++) { Set(i, y, c); Set(i, y + h - 1, c); }
        for (int j = y; j < y + h; j++) { Set(x, j, c); Set(x + w - 1, j, c); }
    }

    static void Disc(int cx, int cy, int r, Color c) {
        for (int y = cy - r; y <= cy + r; y++)
            for (int x = cx - r; x <= cx + r; x++)
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r + r / 2)
                    Set(x, y, c);
    }

    static void HLine(int x0, int x1, int y, Color c) {
        for (int x = x0; x <= x1; x++) Set(x, y, c);
    }

    static void Arrow(int x0, int x1, int y, Color c) {
        HLine(x0, x1 - 2, y, c);
        Set(x1 - 1, y, c);
        Set(x1 - 2, y - 1, c); Set(x1 - 2, y + 1, c);
        Set(x1 - 3, y - 2, c); Set(x1 - 3, y + 2, c);
        Set(x1, y, c);
    }

    public static void Generate(string path) {
        // ---- background: dark space, same palette as preview1
        for (int y = 0; y < H; y++) {
            double tv = (double)y / (H - 1);
            int br = (int)(20 + (7 - 20) * tv);
            int bg = (int)(26 + (10 - 26) * tv);
            int bb = (int)(56 + (21 - 56) * tv);
            for (int x = 0; x < G; x++)
                cell[x, y] = Color.FromArgb(br, bg, bb);
        }

        // stars
        Random rnd = new Random(123);
        for (int i = 0; i < 120; i++) {
            int x = rnd.Next(G), y = rnd.Next(H);
            if (y > 12 && y < 62 && x > 6 && x < 122) continue; // keep the flow row clean-ish
            int a = rnd.Next(90, 235);
            Set(x, y, Color.FromArgb(a, 205, 218, 255));
        }

        // ============ flow panels: CRAFT -> EXP -> POINTS -> UNLOCK ============
        int[] panelX = { 10, 40, 70, 100 };
        int panelY = 24;
        int panelW = 20, panelH = 22;

        // --- panel 1: mechanical crafter crafting (machine + item)
        int px = panelX[0];
        Rect(px + 3, panelY + 4, 14, 14, Color.FromArgb(255, 62, 68, 84));       // machine body
        RectOutline(px + 3, panelY + 4, 14, 14, Color.FromArgb(255, 120, 130, 155));
        Rect(px + 6, panelY + 7, 8, 8, Color.FromArgb(255, 30, 34, 48));           // window
        Rect(px + 8, panelY + 9, 4, 4, Color.FromArgb(255, 240, 200, 110));        // item inside (brass-ish)
        Rect(px + 5, panelY + 2, 3, 2, Color.FromArgb(255, 120, 130, 155));        // chute nub
        Rect(px + 12, panelY + 2, 3, 2, Color.FromArgb(255, 120, 130, 155));
        // motion specks
        Set(px + 2, panelY + 10, Color.FromArgb(255, 160, 170, 195));
        Set(px + 18, panelY + 12, Color.FromArgb(255, 160, 170, 195));

        // --- panel 2: EXP orbs flying
        px = panelX[1];
        Disc(px + 6, panelY + 8, 2, Color.FromArgb(255, 120, 220, 140));
        Disc(px + 12, panelY + 12, 3, Color.FromArgb(255, 140, 235, 160));
        Disc(px + 8, panelY + 16, 1, Color.FromArgb(255, 100, 200, 120));
        Disc(px + 15, panelY + 6, 1, Color.FromArgb(255, 100, 200, 120));
        // orb shine
        Set(px + 11, panelY + 11, Color.FromArgb(255, 220, 255, 225));

        // --- panel 3: skill point (gold star-ish diamond)
        px = panelX[2];
        int scx = px + 10, scy = panelY + 11;
        for (int d = 4; d >= 0; d--) {
            Color cc = d == 4 ? Color.FromArgb(255, 190, 140, 40) :
                       d >= 2 ? Color.FromArgb(255, 235, 195, 90) :
                                Color.FromArgb(255, 255, 240, 180);
            for (int i = -d; i <= d; i++) {
                Set(scx + i, scy - (d - Math.Abs(i)), cc);
                Set(scx + i, scy + (d - Math.Abs(i)), cc);
            }
        }
        // sparkle
        Set(scx - 6, scy - 5, Color.FromArgb(255, 255, 235, 160));
        Set(scx + 6, scy + 5, Color.FromArgb(255, 255, 235, 160));

        // --- panel 4: unlocked node (medallion with green check)
        px = panelX[3];
        Rect(px + 5, panelY + 6, 11, 11, Color.FromArgb(255, 22, 28, 48));          // node face
        RectOutline(px + 5, panelY + 6, 11, 11, Color.FromArgb(255, 95, 185, 95));   // green rim = unlocked
        Rect(px + 8, panelY + 9, 5, 5, Color.FromArgb(255, 130, 205, 130));          // item
        // check mark
        Set(px + 13, panelY + 15, Color.FromArgb(255, 70, 220, 70));
        Set(px + 14, panelY + 16, Color.FromArgb(255, 70, 220, 70));
        Set(px + 15, panelY + 15, Color.FromArgb(255, 70, 220, 70));
        Set(px + 16, panelY + 14, Color.FromArgb(255, 70, 220, 70));

        // --- panel frames
        for (int k = 0; k < 4; k++) {
            RectOutline(panelX[k] + 1, panelY, panelW - 2, panelH, Color.FromArgb(255, 44, 52, 78));
        }

        // --- arrows between panels
        Color ac = Color.FromArgb(255, 235, 195, 95);
        Arrow(panelX[0] + panelW - 1, panelX[1] + 1, panelY + panelH / 2, ac);
        Arrow(panelX[1] + panelW - 1, panelX[2] + 1, panelY + panelH / 2, ac);
        Arrow(panelX[2] + panelW - 1, panelX[3] + 1, panelY + panelH / 2, ac);

        // --- step captions under panels
        // (text drawn later with crisp font)

        // ============ bottom: locked vs unlocked comparison ============
        int rowY = 54;
        // locked node
        Rect(30, rowY, 9, 9, Color.FromArgb(255, 22, 28, 48));
        RectOutline(30, rowY, 9, 9, Color.FromArgb(255, 138, 74, 74));
        // padlock
        Rect(33, rowY + 2, 3, 1, Color.FromArgb(255, 176, 176, 184));
        Rect(32, rowY + 3, 5, 4, Color.FromArgb(255, 216, 216, 224));
        // machine refusing (red X)
        Rect(44, rowY + 1, 7, 7, Color.FromArgb(255, 62, 68, 84));
        RectOutline(44, rowY + 1, 7, 7, Color.FromArgb(255, 120, 130, 155));
        Set(46, rowY + 3, Color.FromArgb(255, 235, 90, 90)); Set(50 - 1, rowY + 3 + 3, Color.FromArgb(255, 235, 90, 90));
        Set(47, rowY + 4, Color.FromArgb(255, 235, 90, 90)); Set(49, rowY + 4, Color.FromArgb(255, 235, 90, 90));
        Set(48, rowY + 5, Color.FromArgb(255, 235, 90, 90));
        Set(47, rowY + 6, Color.FromArgb(255, 235, 90, 90)); Set(49, rowY + 6, Color.FromArgb(255, 235, 90, 90));
        Set(46, rowY + 7, Color.FromArgb(255, 235, 90, 90)); Set(50, rowY + 7, Color.FromArgb(255, 235, 90, 90));

        // unlocked node
        Rect(88, rowY, 9, 9, Color.FromArgb(255, 22, 28, 48));
        RectOutline(88, rowY, 9, 9, Color.FromArgb(255, 95, 185, 95));
        Set(93, rowY + 6, Color.FromArgb(255, 70, 220, 70));
        Set(94, rowY + 7, Color.FromArgb(255, 70, 220, 70));
        Set(95, rowY + 5, Color.FromArgb(255, 70, 220, 70));
        Set(96, rowY + 4, Color.FromArgb(255, 70, 220, 70));
        // machine working (green tick + item out)
        Rect(70, rowY + 1, 7, 7, Color.FromArgb(255, 62, 68, 84));
        RectOutline(70, rowY + 1, 7, 7, Color.FromArgb(255, 120, 130, 155));
        Set(72, rowY + 5, Color.FromArgb(255, 120, 230, 120));
        Set(73, rowY + 6, Color.FromArgb(255, 120, 230, 120));
        Set(74, rowY + 4, Color.FromArgb(255, 120, 230, 120));
        Set(75, rowY + 3, Color.FromArgb(255, 120, 230, 120));

        // ---- render cells
        Bitmap bmp = new Bitmap(G * SC, H * SC, PixelFormat.Format32bppArgb);
        Graphics g = Graphics.FromImage(bmp);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < G; x++)
                using (SolidBrush b = new SolidBrush(cell[x, y]))
                    g.FillRectangle(b, x * SC, y * SC, SC, SC);

        // ---- crisp text
        g.TextRenderingHint = TextRenderingHint.SingleBitPerPixelGridFit;
        StringFormat cf = new StringFormat(); cf.Alignment = StringAlignment.Center;

        using (Font f = new Font("Segoe UI", 64, FontStyle.Bold))
        using (SolidBrush b = new SolidBrush(Color.FromArgb(255, 240, 200, 130)))
            g.DrawString("CRAFT. EARN. UNLOCK.", f, b, G * SC / 2, 24, cf);
        using (Font f = new Font("Segoe UI", 26))
        using (SolidBrush b = new SolidBrush(Color.FromArgb(200, 165, 178, 208)))
            g.DrawString("Create recipes stay locked until you buy them in the tree", f, b, G * SC / 2, 132, cf);

        // step captions
        string[] caps = { "CRAFT IN CREATE", "EARN EXP", "GET SKILL POINTS", "UNLOCK RECIPES" };
        Color[] capc = { Color.FromArgb(255, 150, 195, 240), Color.FromArgb(255, 140, 235, 160), Color.FromArgb(255, 240, 210, 120), Color.FromArgb(255, 130, 220, 130) };
        using (Font f = new Font("Segoe UI", 20, FontStyle.Bold))
            for (int k = 0; k < 4; k++)
                using (SolidBrush b = new SolidBrush(capc[k]))
                    g.DrawString(caps[k], f, b, (panelX[k] + panelW / 2) * SC + SC / 2, (panelY + panelH + 1) * SC, cf);

        // bottom comparison captions
        using (Font f = new Font("Segoe UI", 19))
        using (SolidBrush b1 = new SolidBrush(Color.FromArgb(230, 235, 130, 130)))
        using (SolidBrush b2 = new SolidBrush(Color.FromArgb(230, 130, 235, 150))) {
            g.DrawString("LOCKED: machines refuse the recipe", f, b1, 42 * SC, (rowY + 10) * SC + 6, cf);
            g.DrawString("UNLOCKED: everything works again", f, b2, 84 * SC, (rowY + 10) * SC + 6, cf);
        }

        // footer
        using (Font f = new Font("Segoe UI", 24))
        using (SolidBrush b = new SolidBrush(Color.FromArgb(200, 150, 162, 192)))
            g.DrawString("items never destroyed  -  progress saved server-side  -  fully configurable via datapack", f, b, G * SC / 2, H * SC - 56, cf);

        g.Dispose();
        bmp.Save(path, ImageFormat.Png);
        bmp.Dispose();
    }
}
'@ -ReferencedAssemblies System.Drawing

[PixelPreview2]::Generate('D:\OpenCode\Mod\promo\preview2_flow.png')
Write-Output 'pixel preview2 done'
