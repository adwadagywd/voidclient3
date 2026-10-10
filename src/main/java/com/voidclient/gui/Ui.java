package com.voidclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class Ui {
    /** Globalny mnoznik przezroczystosci (animacja fade). */
    public static float alpha = 1f;

    public static final int PANEL = 0xCC090514;
    public static final int PANEL_GLASS = 0x7A090514;
    public static int ACCENT = 0xFF8727F5;
    public static int ACCENT2 = 0xFF22D3EE;
    public static final int TEXT = 0xFFEDE9FE;
    public static final int DIM = 0xFF8E84B8;
    public static final int TRACK = 0xFF2A2145;

    private static final int RADIUS = 12;
    private static final int SS = 10; // supersampling na os przy antyaliasingu lukow (10x10 probek/px)

    private Ui() {}

    /** Przelicza kolory akcentu na podstawie motywu (hue shift). */
    public static void applyTheme(float hueShift) {
        if (Math.abs(hueShift) < 0.001f) {
            ACCENT = 0xFF8727F5;
            ACCENT2 = 0xFF22D3EE;
            return;
        }
        ACCENT = hsv(0.744f + hueShift, 0.78f, 0.96f);
        ACCENT2 = hsv(0.49f + hueShift, 0.62f, 0.93f);
    }

    public static TextRenderer tr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static int mulA(int c, float f) {
        float ff = Math.max(0f, Math.min(1f, f));
        int al = (int) (((c >>> 24) & 0xFF) * ff);
        return (al << 24) | (c & 0xFFFFFF);
    }

    public static int a(int c) {
        return mulA(c, alpha);
    }

    public static int lerp(int a, int b, float t) {
        float tt = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            int cv = (int) (ca + (cb - ca) * tt);
            out |= (cv & 0xFF) << shift;
        }
        return out;
    }

    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static float easeOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        float f = t - 1f;
        return f * f * f + 1f;
    }

    /** Easing z lekkim "przestrzeleniem" - daje efektowne, sprezyste wejscie elementu. */
    public static float easeOutBack(float t) {
        t = clamp(t, 0f, 1f);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float tm1 = t - 1f;
        return 1f + c3 * tm1 * tm1 * tm1 + c1 * tm1 * tm1;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float f6 = h * 6f;
        int i = (int) f6;
        float f = f6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static void rect(DrawContext c, int x, int y, int w, int h, int col) {
        if (w <= 0 || h <= 0) return;
        c.fill(x, y, x + w, y + h, a(col));
    }

    /** Prawdziwie zaokraglony prostokat (promien RADIUS) z antyaliasingiem na krawedzi. */
    public static void round(DrawContext c, int x, int y, int w, int h, int col) {
        roundR(c, x, y, w, h, Math.min(RADIUS, Math.min(w, h) / 2), col);
    }

    /** Jeden piksel z dodatkowym mnoznikiem krycia (uzywany do wygladzania krawedzi lukow). */
    private static void pix(DrawContext c, int x, int y, int col, float coverage) {
        if (coverage <= 0.004f) return;
        int cc = coverage >= 0.999f ? a(col) : mulA(a(col), coverage);
        if (((cc >>> 24) & 0xFF) == 0) return;
        c.fill(x, y, x + 1, y + 1, cc);
    }

    public static void roundR(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        if (r <= 0 || w < r * 2 || h < r * 2) {
            rect(c, x, y, w, h, col);
            return;
        }
        // srodek: pelna szerokosc pomiedzy gornym i dolnym pasem
        rect(c, x, y + r, w, h - 2 * r, col);
        // gorny/dolny pas, bez naroznikow
        rect(c, x + r, y, w - 2 * r, r, col);
        rect(c, x + r, y + h - r, w - 2 * r, r, col);
        // cztery narozniki: kazdy piksel liczony z supersamplingiem SSxSS -> bardzo gladki luk
        float rr = r;
        float cx = rr, cy = rr;
        float step = 1f / SS;
        float half = step / 2f;
        for (int j = 0; j < r; j++) {
            for (int i = 0; i < r; i++) {
                int hits = 0;
                for (int sy = 0; sy < SS; sy++) {
                    float py = j + half + sy * step;
                    float dy = cy - py;
                    for (int sx = 0; sx < SS; sx++) {
                        float px = i + half + sx * step;
                        float dx = cx - px;
                        if (dx * dx + dy * dy <= rr * rr) hits++;
                    }
                }
                if (hits == 0) continue;
                float cov = (float) hits / (SS * SS);
                pix(c, x + i, y + j, col, cov);
                pix(c, x + w - 1 - i, y + j, col, cov);
                pix(c, x + i, y + h - 1 - j, col, cov);
                pix(c, x + w - 1 - i, y + h - 1 - j, col, cov);
            }
        }
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        rect(c, x + 1, y, w - 2, 1, col);
        rect(c, x + 1, y + h - 1, w - 2, 1, col);
        rect(c, x, y + 1, 1, h - 2, col);
        rect(c, x + w - 1, y + 1, 1, h - 2, col);
    }

    /** Zaokraglona powierzchnia z kolorowa "ramka": zewnetrzny zaokraglony prostokat w kolorze
     *  ramki, a na nim mniejszy (wciety o borderPx) zaokraglony prostokat w kolorze wypelnienia.
     *  Zamiast rysowac prosty, kwadratowy outline() nad zaokraglonym tlem (co tworzy widoczny
     *  "kwadratowy" rog), ramka tutaj sama jest zaokraglona i idealnie podaza za krzywizna. */
    public static void roundBordered(DrawContext c, int x, int y, int w, int h, int fillCol, int borderCol, int borderPx) {
        round(c, x, y, w, h, borderCol);
        int bp = Math.max(1, borderPx);
        if (w - 2 * bp > 0 && h - 2 * bp > 0) {
            round(c, x + bp, y + bp, w - 2 * bp, h - 2 * bp, fillCol);
        }
    }

    /** Prawdziwie zaokraglony, PUSTY w srodku obrys (pierscien) o zadanej grubosci - do zaznaczen
     *  rysowanych nad dowolna, nieznana tresc (gdzie nie mozna po prostu wypelnic tla). */
    public static void roundStroke(DrawContext c, int x, int y, int w, int h, int thickness, int col) {
        int r = Math.min(RADIUS, Math.min(w, h) / 2);
        int t = Math.max(1, Math.min(thickness, Math.max(1, Math.min(w, h) / 2)));
        if (r <= 0) {
            outline(c, x, y, w, h, col);
            return;
        }
        rect(c, x + r, y, w - 2 * r, t, col);
        rect(c, x + r, y + h - t, w - 2 * r, t, col);
        rect(c, x, y + r, t, h - 2 * r, col);
        rect(c, x + w - t, y + r, t, h - 2 * r, col);

        float rr = r;
        float ri = Math.max(0, r - t);
        float step = 1f / SS, half = step / 2f;
        for (int j = 0; j < r; j++) {
            for (int i = 0; i < r; i++) {
                int hits = 0;
                for (int sy = 0; sy < SS; sy++) {
                    float py = j + half + sy * step;
                    float dy = rr - py;
                    for (int sx = 0; sx < SS; sx++) {
                        float px = i + half + sx * step;
                        float dx = rr - px;
                        float d2 = dx * dx + dy * dy;
                        if (d2 <= rr * rr && d2 >= ri * ri) hits++;
                    }
                }
                if (hits == 0) continue;
                float cov = (float) hits / (SS * SS);
                pix(c, x + i, y + j, col, cov);
                pix(c, x + w - 1 - i, y + j, col, cov);
                pix(c, x + i, y + h - 1 - j, col, cov);
                pix(c, x + w - 1 - i, y + h - 1 - j, col, cov);
            }
        }
    }

    public static void gradH(DrawContext c, int x, int y, int w, int h, int c1, int c2) {
        for (int i = 0; i < w; i += 2) {
            int cw = Math.min(2, w - i);
            rect(c, x + i, y, cw, h, lerp(c1, c2, (float) i / Math.max(1, w - 1)));
        }
    }

    public static void text(DrawContext c, String s, int x, int y, int col, boolean shadow) {
        int cc = a(col);
        if (((cc >>> 24) & 0xFF) <= 3) return;
        c.drawText(tr(), s, x, y, cc, shadow);
    }

    public static void textC(DrawContext c, String s, int cx, int y, int col, boolean shadow) {
        text(c, s, cx - tr().getWidth(s) / 2, y, col, shadow);
    }

    public static void textGrad(DrawContext c, String s, int x, int y, int c1, int c2, boolean shadow) {
        int cx = x;
        int n = s.length();
        for (int i = 0; i < n; i++) {
            String ch = s.substring(i, i + 1);
            text(c, ch, cx, y, lerp(c1, c2, n <= 1 ? 0f : (float) i / (n - 1)), shadow);
            cx += tr().getWidth(ch);
        }
    }
}
