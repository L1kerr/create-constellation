Add-Type -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;

public static class IconGen {
    static double Sat(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    static double Smooth(double a, double b, double x) {
        double t = Sat((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    public static void Generate(string path, int S) {
        Bitmap bmp = new Bitmap(S, S, PixelFormat.Format32bppArgb);
        BitmapData d = bmp.LockBits(new Rectangle(0, 0, S, S), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        int stride = d.Stride;
        byte[] buf = new byte[stride * S];

        double cx = S * 0.5, cy = S * 0.50;
        double sunR = S * 0.155;
        double orbitR = S * 0.365;

        // planets: angle, radius, RGB
        double[] pAng = { -2.45, 0.30, 2.45 };
        double[] pRad = { S * 0.072, S * 0.060, S * 0.046 };
        double[,] pCol = { { 90, 158, 230 }, { 228, 142, 82 }, { 122, 190, 96 } };
        int nP = 3;

        // stars (seeded)
        Random rnd = new Random(1234);
        int nStars = 90;
        double[] sx = new double[nStars], sy = new double[nStars], sa = new double[nStars], ss = new double[nStars];
        for (int i = 0; i < nStars; i++) {
            sx[i] = rnd.NextDouble() * S; sy[i] = rnd.NextDouble() * S;
            sa[i] = 0.25 + rnd.NextDouble() * 0.75;
            ss[i] = S * (0.0025 + rnd.NextDouble() * 0.004);
            // keep stars away from the very center glow
        }

        // per-pixel float accumulators (ARGB, 0..1)
        double[] r = new double[S * S], g = new double[S * S], b = new double[S * S];

        // ---------- pass 1: background gradient + vignette ----------
        for (int y = 0; y < S; y++) {
            double tv = (double)y / S;
            // top #141B36 -> bottom #070A16
            double br = 20 + (7 - 20) * tv, bg = 27 + (10 - 27) * tv, bb = 54 + (22 - 54) * tv;
            for (int x = 0; x < S; x++) {
                double dx = (x - cx) / (S * 0.62), dy = (y - cy) / (S * 0.62);
                double vig = 1.0 - 0.42 * Sat(Math.Sqrt(dx * dx + dy * dy) - 0.35) / 0.65;
                int i = y * S + x;
                r[i] = br * vig / 255.0; g[i] = bg * vig / 255.0; b[i] = bb * vig / 255.0;
            }
        }

        // ---------- pass 2: stars (additive) ----------
        for (int si = 0; si < nStars; si++) {
            int x0 = Math.Max(0, (int)(sx[si] - ss[si] * 4)), x1 = Math.Min(S - 1, (int)(sx[si] + ss[si] * 4));
            int y0 = Math.Max(0, (int)(sy[si] - ss[si] * 4)), y1 = Math.Min(S - 1, (int)(sy[si] + ss[si] * 4));
            for (int y = y0; y <= y1; y++)
                for (int x = x0; x <= x1; x++) {
                    double dx = x - sx[si], dy = y - sy[si];
                    double a = sa[si] * Math.Exp(-(dx * dx + dy * dy) / (2 * ss[si] * ss[si]));
                    if (a < 0.004) continue;
                    int i = y * S + x;
                    r[i] += a * 0.86; g[i] += a * 0.90; b[i] += a * 1.0;
                }
        }

        // ---------- pass 3: sun glow (additive, two halos) ----------
        for (int y = 0; y < S; y++)
            for (int x = 0; x < S; x++) {
                double dx = x - cx, dy = y - cy;
                double dist = Math.Sqrt(dx * dx + dy * dy);
                int i = y * S + x;
                if (dist > sunR) {
                    double near = Math.Exp(-(dist - sunR) / (S * 0.055));   // tight corona
                    double wide = Math.Exp(-(dist - sunR) / (S * 0.30));    // big halo
                    double a = 0.85 * near * near + 0.22 * wide;
                    r[i] += a * 1.00; g[i] += a * 0.62; b[i] += a * 0.22;
                }
            }

        // ---------- pass 4: orbit ring + gold links (alpha blend) ----------
        for (int p = 0; p < nP; p++) {
            double px = cx + orbitR * Math.Cos(pAng[p]), py = cy + orbitR * Math.Sin(pAng[p]) * 0.96;
            // segment sun->planet
            double lx = px - cx, ly = py - cy;
            double len2 = lx * lx + ly * ly;
            for (int y = 0; y < S; y++)
                for (int x = 0; x < S; x++) {
                    int i = y * S + x;
                    // link
                    double t = Sat(((x - cx) * lx + (y - cy) * ly) / len2);
                    double qx = cx + lx * t - x, qy = cy + ly * t - y;
                    double dl = Math.Sqrt(qx * qx + qy * qy);
                    double core = Sat(S * 0.0038 + 0.75 - dl);
                    double halo = 0.30 * Math.Exp(-dl / (S * 0.012));
                    double a = Sat(core * 0.75 + halo);
                    if (a > 0.004) {
                        double gr = 1.00, gg = 0.80, gb = 0.43;
                        r[i] = r[i] + (gr - r[i]) * a * 0.8;
                        g[i] = g[i] + (gg - g[i]) * a * 0.8;
                        b[i] = b[i] + (gb - b[i]) * a * 0.8;
                    }
                }
        }
        // orbit ring on top of links
        for (int y = 0; y < S; y++)
            for (int x = 0; x < S; x++) {
                double dx = x - cx, dy = (y - cy) / 0.96; // very slight tilt
                double dist = Math.Sqrt(dx * dx + dy * dy);
                double dr = Math.Abs(dist - orbitR);
                double a = Sat(S * 0.0042 + 0.55 - dr) * 0.85;
                double halo = 0.16 * Math.Exp(-dr / (S * 0.010));
                double total = Sat(a + halo);
                if (total > 0.004) {
                    int i = y * S + x;
                    double rr = 0.55, gg2 = 0.66, bb = 0.92;
                    r[i] += (rr - r[i]) * total; g[i] += (gg2 - g[i]) * total; b[i] += (bb - b[i]) * total;
                }
            }

        // ---------- pass 5: sun disc ----------
        for (int y = 0; y < S; y++)
            for (int x = 0; x < S; x++) {
                double dx = x - cx, dy = y - cy;
                double dist = Math.Sqrt(dx * dx + dy * dy);
                if (dist > sunR + 1.5) continue;
                double edge = Sat(sunR + 0.75 - dist); // AA
                if (edge <= 0) continue;
                double t = Sat(dist / sunR);
                // core #FFF7D2 -> mid #FFC24A -> rim #FF8A1E
                double cr, cg, cb;
                if (t < 0.55) {
                    double u = t / 0.55;
                    cr = 1.00; cg = 0.97 + (0.76 - 0.97) * u; cb = 0.82 + (0.29 - 0.82) * u;
                } else {
                    double u = (t - 0.55) / 0.45;
                    cr = 1.00; cg = 0.76 + (0.54 - 0.76) * u; cb = 0.29 + (0.12 - 0.29) * u;
                }
                // granulation shimmer (deterministic)
                double gr2 = 0.03 * Math.Sin(dist * 0.9 + Math.Atan2(dy, dx) * 6.0);
                cr = Sat(cr + gr2); cg = Sat(cg + gr2 * 0.6);
                int i = y * S + x;
                r[i] = r[i] + (cr - r[i]) * edge;
                g[i] = g[i] + (cg - g[i]) * edge;
                b[i] = b[i] + (cb - b[i]) * edge;
            }

        // ---------- pass 6: planets (lit spheres) ----------
        for (int p = 0; p < nP; p++) {
            double px = cx + orbitR * Math.Cos(pAng[p]), py = cy + orbitR * Math.Sin(pAng[p]) * 0.96;
            double R = pRad[p];
            // light direction: from planet toward sun, lifted toward viewer
            double Lx = cx - px, Ly = cy - py;
            double Ll = Math.Sqrt(Lx * Lx + Ly * Ly);
            Lx /= Ll; Ly /= Ll;
            double Lz = 0.55;
            double Lnorm = Math.Sqrt(Lx * Lx + Ly * Ly + Lz * Lz);
            Lx /= Lnorm; Ly /= Lnorm; Lz /= Lnorm;

            int x0 = Math.Max(0, (int)(px - R * 2.2)), x1 = Math.Min(S - 1, (int)(px + R * 2.2));
            int y0 = Math.Max(0, (int)(py - R * 2.2)), y1 = Math.Min(S - 1, (int)(py + R * 2.2));
            for (int y = y0; y <= y1; y++)
                for (int x = x0; x <= x1; x++) {
                    int i = y * S + x;
                    double dx = x - px, dy = y - py;
                    double d2 = dx * dx + dy * dy;
                    // atmosphere halo (additive)
                    if (d2 > R * R && d2 < (R * 1.85) * (R * 1.85)) {
                        double dd = Math.Sqrt(d2);
                        double a = 0.34 * Math.Exp(-(dd - R) / (R * 0.30));
                        r[i] += a * pCol[p, 0] / 255.0;
                        g[i] += a * pCol[p, 1] / 255.0;
                        b[i] += a * pCol[p, 2] / 255.0;
                    }
                    if (d2 >= R * R) continue;
                    double dz = Math.Sqrt(R * R - d2);
                    double nx = dx / R, ny = dy / R, nz = dz / R;
                    double diff = Math.Max(0, nx * Lx + ny * Ly + nz * Lz);
                    double ambient = 0.16;
                    double lit = ambient + 0.92 * diff;
                    // fresnel rim light (atmosphere edge)
                    double fres = Math.Pow(1 - nz, 2.6);
                    // specular
                    double Hx = Lx, Hy = Ly, Hz = Lz + 1.0;
                    double Hl = Math.Sqrt(Hx * Hx + Hy * Hy + Hz * Hz);
                    double spec = Math.Pow(Math.Max(0, (nx * Hx + ny * Hy + nz * Hz) / Hl), 26) * 0.55;

                    double edgeAA = Sat(R + 0.5 - Math.Sqrt(d2));
                    double pr = pCol[p, 0] / 255.0 * lit + fres * pCol[p, 0] / 255.0 * 0.55 + spec;
                    double pg = pCol[p, 1] / 255.0 * lit + fres * pCol[p, 1] / 255.0 * 0.55 + spec;
                    double pb = pCol[p, 2] / 255.0 * lit + fres * pCol[p, 2] / 255.0 * 0.55 + spec;
                    r[i] = r[i] + (Sat(pr) - r[i]) * edgeAA;
                    g[i] = g[i] + (Sat(pg) - g[i]) * edgeAA;
                    b[i] = b[i] + (Sat(pb) - b[i]) * edgeAA;
                }
        }

        // ---------- write buffer ----------
        for (int y = 0; y < S; y++)
            for (int x = 0; x < S; x++) {
                int i = y * S + x;
                int o = y * stride + x * 4;
                buf[o + 0] = (byte)(Sat(b[i]) * 255);
                buf[o + 1] = (byte)(Sat(g[i]) * 255);
                buf[o + 2] = (byte)(Sat(r[i]) * 255);
                buf[o + 3] = 255;
            }
        System.Runtime.InteropServices.Marshal.Copy(buf, 0, d.Scan0, buf.Length);
        bmp.UnlockBits(d);
        bmp.Save(path, ImageFormat.Png);
        bmp.Dispose();
    }
}
'@ -ReferencedAssemblies System.Drawing

[IconGen]::Generate('D:\OpenCode\Mod\promo\icon.png', 512)
Write-Output 'icon v2 done'
