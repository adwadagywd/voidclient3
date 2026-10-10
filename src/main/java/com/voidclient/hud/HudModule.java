package com.voidclient.hud;

import com.voidclient.VoidConfig;
import com.voidclient.gui.Ui;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;

public abstract class HudModule {
    public final String id, name;
    public final VoidConfig.ModuleSettings s;
    /** Ostatnie granice na ekranie (po skalowaniu) - uzywane przez edytor HUD. */
    public float bx, by, bw, bh;
    private final int index;

    protected HudModule(String id, String name, Consumer<VoidConfig.ModuleSettings> defaults) {
        this.id = id;
        this.name = name;
        this.s = VoidConfig.settingsFor(id, defaults);
        this.index = HudManager.MODULES.size();
    }

    /** Rozmiar zawartosci {szerokosc, wysokosc} bez skalowania. */
    public abstract int[] size(TextRenderer tr);

    protected abstract void draw(DrawContext c, TextRenderer tr, int w, int h);

    protected void placeDefault(int sw, int sh, float w, float h) {
        s.x = 4;
        s.y = 4 + index * 18;
    }

    public boolean contains(double x, double y) {
        return x >= bx && x <= bx + bw && y >= by && y <= by + bh;
    }

    public void render(DrawContext ctx, TextRenderer tr, int sw, int sh) {
        int[] size = size(tr);
        float sc = s.scale;
        float w = size[0] * sc, h = size[1] * sc;
        if (s.x < 0 || s.y < 0) placeDefault(sw, sh, w, h);
        s.x = Math.max(0f, Math.min(s.x, Math.max(0f, sw - w)));
        s.y = Math.max(0f, Math.min(s.y, Math.max(0f, sh - h)));
        bx = s.x;
        by = s.y;
        bw = w;
        bh = h;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(s.x, s.y, 0f);
        m.scale(sc, sc, 1f);
        draw(ctx, tr, size[0], size[1]);
        m.pop();
    }

    protected int textColor() {
        if (s.chroma) return Ui.hsv((System.currentTimeMillis() % 3000L) / 3000f, 0.55f, 1f);
        return 0xFF000000 | (s.color & 0xFFFFFF);
    }

    protected void drawBackground(DrawContext c, int w, int h) {
        if (s.background) {
            Ui.round(c, 0, 0, w, h, ((int) (s.bgOpacity * 255f) << 24) | 0x0A0614);
        }
        if (s.accent) {
            c.fillGradient(0, 1, 2, h - 1, Ui.a(Ui.ACCENT), Ui.a(Ui.ACCENT2));
        }
    }

    protected void drawString(DrawContext c, TextRenderer tr, String t, int x, int y) {
        if (!s.chroma) {
            Ui.text(c, t, x, y, textColor(), s.shadow);
            return;
        }
        float hue = (System.currentTimeMillis() % 3000L) / 3000f;
        int cx = x;
        for (int i = 0; i < t.length(); i++) {
            String ch = t.substring(i, i + 1);
            Ui.text(c, ch, cx, y, Ui.hsv(hue - i * 0.04f, 0.55f, 1f), s.shadow);
            cx += tr.getWidth(ch);
        }
    }
}
