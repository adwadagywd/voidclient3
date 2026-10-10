package com.voidclient.gui;

import com.voidclient.VoidClientMod;
import com.voidclient.VoidConfig;
import com.voidclient.hud.HudManager;
import com.voidclient.hud.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {
    private HudModule drag, selected;
    private double offX, offY;
    private float guideX = -1f, guideY = -1f;
    private final Starfield stars = new Starfield();
    private final Anim fade = new Anim(0);
    private final Widgets.Button menuBtn = new Widgets.Button("Menu", false,
            () -> MinecraftClient.getInstance().setScreen(new VoidMenuScreen()));
    private final Widgets.Button resetBtn = new Widgets.Button("Reset all", false, HudEditorScreen::resetAll);
    private final Widgets.Button doneBtn = new Widgets.Button("Done", true,
            () -> MinecraftClient.getInstance().setScreen(null));

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    private static void resetAll() {
        for (HudModule m : HudManager.MODULES) {
            m.s.x = -1f;
            m.s.y = -1f;
            m.s.scale = 1f;
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        Ui.applyTheme(VoidConfig.data.theme.hue);
        float o = fade.to(1f, 10f);
        Ui.alpha = o;
        if (VoidConfig.data.theme.animatedBg) {
            stars.draw(ctx, width, height, 0.55f);
        } else {
            ctx.fillGradient(0, 0, width, height, Ui.a(0xD0070312), Ui.a(0xE0140828));
        }
        for (int x = 0; x < width; x += 20) Ui.rect(ctx, x, 0, 1, height, 0x10FFFFFF);
        for (int y = 0; y < height; y += 20) Ui.rect(ctx, 0, y, width, 1, 0x10FFFFFF);
        Ui.rect(ctx, width / 2, 0, 1, height, 0x338727F5);
        Ui.rect(ctx, 0, height / 2, width, 1, 0x338727F5);
        Ui.textGrad(ctx, "HUD EDITOR", width / 2 - Ui.tr().getWidth("HUD EDITOR") / 2, 8,
                Ui.ACCENT2, Ui.ACCENT, true);

        Ui.alpha = 1f;
        boolean any = false;
        for (HudModule m : HudManager.MODULES) {
            if (!m.s.enabled) continue;
            any = true;
            m.render(ctx, Ui.tr(), width, height);
            boolean hov = m.contains(mx, my);
            int col = (m == selected || m == drag) ? 0xFF8727F5 : (hov ? 0x99FFFFFF : 0x44FFFFFF);
            Ui.roundStroke(ctx, (int) m.bx - 2, (int) m.by - 2, (int) m.bw + 4, (int) m.bh + 4, 1, col);
        }
        if (drag != null) {
            if (guideX >= 0) Ui.rect(ctx, (int) guideX, 0, 1, height, 0xAA22D3EE);
            if (guideY >= 0) Ui.rect(ctx, 0, (int) guideY, width, 1, 0xAA22D3EE);
        }

        Ui.alpha = o;
        if (!any) {
            Ui.textC(ctx, "No modules enabled - open Menu and turn some on", width / 2, height / 2 - 4,
                    Ui.DIM, true);
        }
        Ui.textC(ctx, "Drag to move  |  Scroll to resize  |  Arrows nudge  |  R reset module",
                width / 2, height - 44, Ui.DIM, true);
        int bw = 70, total = bw * 3 + 12;
        int bx = (width - total) / 2, by = height - 30;
        menuBtn.draw(ctx, bx, by, bw, mx, my);
        resetBtn.draw(ctx, bx + bw + 6, by, bw, mx, my);
        doneBtn.draw(ctx, bx + 2 * (bw + 6), by, bw, mx, my);
        Ui.alpha = 1f;
    }

    // ---------------------------------------------------------- przyciaganie
    private float snap(float v, float size, boolean horizontal) {
        int screen = horizontal ? width : height;
        List<float[]> cands = new ArrayList<>();
        cands.add(new float[]{0f, 0f});
        cands.add(new float[]{(screen - size) / 2f, screen / 2f});
        cands.add(new float[]{screen - size, screen});
        for (HudModule o : HudManager.MODULES) {
            if (o == drag || !o.s.enabled) continue;
            float p = horizontal ? o.bx : o.by;
            float s = horizontal ? o.bw : o.bh;
            cands.add(new float[]{p, p});
            cands.add(new float[]{p + s - size, p + s});
            cands.add(new float[]{p + s, p + s});
            cands.add(new float[]{p - size, p});
        }
        float best = v, bd = 5f, line = -1f;
        for (float[] c : cands) {
            float d = Math.abs(v - c[0]);
            if (d < bd) {
                bd = d;
                best = c[0];
                line = c[1];
            }
        }
        if (horizontal) guideX = line; else guideY = line;
        return best;
    }

    // ---------------------------------------------------------- mysz
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (menuBtn.click(mx, my, btn) || resetBtn.click(mx, my, btn) || doneBtn.click(mx, my, btn)) {
            return true;
        }
        if (btn == 0) {
            for (int i = HudManager.MODULES.size() - 1; i >= 0; i--) {
                HudModule m = HudManager.MODULES.get(i);
                if (m.s.enabled && m.contains(mx, my)) {
                    drag = m;
                    selected = m;
                    offX = mx - m.bx;
                    offY = my - m.by;
                    return true;
                }
            }
            selected = null;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (drag != null && btn == 0) {
            float nx = (float) (mx - offX), ny = (float) (my - offY);
            nx = snap(nx, drag.bw, true);
            ny = snap(ny, drag.bh, false);
            drag.s.x = Math.max(0f, Math.min(nx, width - drag.bw));
            drag.s.y = Math.max(0f, Math.min(ny, height - drag.bh));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        drag = null;
        guideX = -1f;
        guideY = -1f;
        VoidConfig.save();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        for (int i = HudManager.MODULES.size() - 1; i >= 0; i--) {
            HudModule m = HudManager.MODULES.get(i);
            if (m.s.enabled && m.contains(mx, my)) {
                m.s.scale = Ui.clamp(m.s.scale + (float) vAmt * 0.1f, 0.5f, 3f);
                selected = m;
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------- klawisze
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (VoidClientMod.openKey.matchesKey(keyCode, scanCode)) {
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }
        if (selected != null) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT: selected.s.x -= 1f; return true;
                case GLFW.GLFW_KEY_RIGHT: selected.s.x += 1f; return true;
                case GLFW.GLFW_KEY_UP: selected.s.y -= 1f; return true;
                case GLFW.GLFW_KEY_DOWN: selected.s.y += 1f; return true;
                case GLFW.GLFW_KEY_R:
                    selected.s.x = -1f;
                    selected.s.y = -1f;
                    return true;
                default: break;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Esc wraca do menu. */
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(new VoidMenuScreen());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        VoidConfig.save();
    }
}
