package com.voidclient.hud;

import com.voidclient.gui.Anim;
import com.voidclient.gui.Ui;
import com.voidclient.stats.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class KeystrokesModule extends HudModule {

    private static class Key {
        final String label;
        final int x, y, w, h, mouse;
        final BooleanSupplier down;
        final Anim anim = new Anim(0);

        Key(String label, int x, int y, int w, int h, BooleanSupplier down, int mouse) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.down = down;
            this.mouse = mouse;
        }
    }

    private final List<Key> keys = new ArrayList<>();

    private static GameOptions opt() {
        return MinecraftClient.getInstance().options;
    }

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", d -> d.accent = false);
        keys.add(new Key("W", 24, 0, 22, 22, () -> opt().forwardKey.isPressed(), 0));
        keys.add(new Key("A", 0, 24, 22, 22, () -> opt().leftKey.isPressed(), 0));
        keys.add(new Key("S", 24, 24, 22, 22, () -> opt().backKey.isPressed(), 0));
        keys.add(new Key("D", 48, 24, 22, 22, () -> opt().rightKey.isPressed(), 0));
        keys.add(new Key("LMB", 0, 48, 34, 22, () -> opt().attackKey.isPressed(), 1));
        keys.add(new Key("RMB", 36, 48, 34, 22, () -> opt().useKey.isPressed(), 2));
        keys.add(new Key("", 0, 72, 70, 14, () -> opt().jumpKey.isPressed(), 0));
    }

    @Override
    public int[] size(TextRenderer tr) {
        return new int[]{70, 86};
    }

    @Override
    protected void placeDefault(int sw, int sh, float w, float h) {
        s.x = sw - w - 8;
        s.y = sh - h - 40;
    }

    @Override
    protected void draw(DrawContext c, TextRenderer tr, int w, int h) {
        int baseA = s.background ? (int) (s.bgOpacity * 255f) : 30;
        int base = (baseA << 24) | 0x0A0614;
        int col = textColor();
        MatrixStack m = c.getMatrices();
        for (Key k : keys) {
            float t = k.anim.to(k.down.getAsBoolean() ? 1f : 0f, 18f);
            Ui.round(c, k.x, k.y, k.w, k.h, Ui.lerp(base, 0xD08727F5, t));
            if (k.label.isEmpty()) {
                Ui.rect(c, k.x + 22, k.y + 6, 26, 2, col);
            } else if (k.mouse != 0) {
                Ui.textC(c, k.label, k.x + k.w / 2, k.y + (s.label ? 4 : 7), col, s.shadow);
                if (s.label) {
                    int cps = k.mouse == 1 ? Stats.leftCps() : Stats.rightCps();
                    m.push();
                    m.translate(k.x + k.w / 2f, k.y + 13f, 0f);
                    m.scale(0.75f, 0.75f, 1f);
                    Ui.textC(c, cps + " CPS", 0, 0, Ui.DIM, false);
                    m.pop();
                }
            } else {
                Ui.textC(c, k.label, k.x + k.w / 2, k.y + (k.h - 8) / 2, col, s.shadow);
            }
        }
    }
}
