package com.voidclient.gui;

import com.voidclient.VoidClientMod;
import com.voidclient.VoidConfig;
import com.voidclient.VoidConfig.ColorSettings;
import com.voidclient.VoidConfig.ModuleSettings;
import com.voidclient.VoidConfig.ViewModelSettings;
import com.voidclient.hud.HudManager;
import com.voidclient.hud.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoidMenuScreen extends Screen {
    private static final int HEADER = 28, SIDE = 86, LISTW = 112, ROWH = 20, GAP = 4;

    private enum Tab {
        MODULES("Modules"), COLOR("Color"), VIEWMODEL("View Model");
        final String label;

        Tab(String l) {
            label = l;
        }
    }

    private Tab tab = Tab.MODULES;
    private HudModule selected = HudManager.MODULES.get(0);
    private final List<Widgets.Widget> widgets = new ArrayList<>();
    private Widgets.Widget active;
    private float listScroll, setScroll;

    private final Anim open = new Anim(0);
    private final Anim tabY = new Anim(0);
    private boolean tabInit;
    private final Anim[] tabHover = {new Anim(0), new Anim(0), new Anim(0)};
    private final Anim[] rowHover, rowOn;
    private boolean closing, closeQueued;
    private final Starfield stars = new Starfield();
    private final Widgets.Button editorBtn;

    private int px, py, pw, ph;
    private int listX, listY, listH, setX, setY, setW, setH;
    private float sc = 1f;

    public VoidMenuScreen() {
        super(Text.literal("VoidClient"));
        int n = HudManager.MODULES.size();
        rowHover = new Anim[n];
        rowOn = new Anim[n];
        for (int i = 0; i < n; i++) {
            rowHover[i] = new Anim(0);
            rowOn[i] = new Anim(HudManager.MODULES.get(i).s.enabled ? 1f : 0f);
        }
        editorBtn = new Widgets.Button("HUD Editor", true,
                () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen()));
        rebuild();
    }

    // ------------------------------------------------------------ budowa widgetow
    private void rebuild() {
        widgets.clear();
        active = null;
        if (tab == Tab.MODULES) {
            ModuleSettings s = selected.s;
            widgets.add(new Widgets.Section(selected.name));
            widgets.add(new Widgets.Toggle("Enabled", () -> s.enabled, v -> s.enabled = v));
            widgets.add(new Widgets.Section("Text"));
            widgets.add(new Widgets.Toggle("Show label / CPS", () -> s.label, v -> s.label = v));
            widgets.add(new Widgets.Toggle("Text shadow", () -> s.shadow, v -> s.shadow = v));
            widgets.add(new Widgets.Toggle("Chroma (rainbow)", () -> s.chroma, v -> s.chroma = v));
            widgets.add(new Widgets.ColorPicker(s));
            widgets.add(new Widgets.Section("Layout"));
            widgets.add(new Widgets.Slider("Size", 0.5f, 3f, () -> s.scale, v -> s.scale = v,
                    v -> String.format(Locale.ROOT, "%.2fx", v)));
            widgets.add(new Widgets.Section("Background"));
            widgets.add(new Widgets.Toggle("Background", () -> s.background, v -> s.background = v));
            widgets.add(new Widgets.Slider("Opacity", 0f, 1f, () -> s.bgOpacity, v -> s.bgOpacity = v,
                    v -> Math.round(v * 100f) + "%"));
            widgets.add(new Widgets.Toggle("Accent bar", () -> s.accent, v -> s.accent = v));
        } else if (tab == Tab.COLOR) {
            ColorSettings c = VoidConfig.data.color;
            widgets.add(new Widgets.Section("Color grading"));
            widgets.add(new Widgets.Toggle("Enabled", () -> c.enabled, v -> c.enabled = v));
            widgets.add(new Widgets.Slider("Brightness", -1f, 1f, () -> c.brightness, v -> c.brightness = v,
                    v -> String.format(Locale.ROOT, "%.2f", v)));
            widgets.add(new Widgets.Slider("Contrast", 0f, 2f, () -> c.contrast, v -> c.contrast = v,
                    v -> String.format(Locale.ROOT, "%.2f", v)));
            widgets.add(new Widgets.Slider("Saturation", 0f, 2f, () -> c.saturation, v -> c.saturation = v,
                    v -> String.format(Locale.ROOT, "%.2f", v)));
            widgets.add(new Widgets.Slider("Hue", -180f, 180f, () -> c.hue, v -> c.hue = v,
                    v -> String.format(Locale.ROOT, "%.0f\u00B0", v)));
            widgets.add(new Widgets.Slider("Gamma", 0.5f, 2f, () -> c.gamma, v -> c.gamma = v,
                    v -> String.format(Locale.ROOT, "%.2f", v)));
            widgets.add(new Widgets.Button("Reset colors", false, () -> {
                c.reset();
                rebuild();
            }));
        } else {
            ViewModelSettings vm = VoidConfig.data.viewModel;
            widgets.add(new Widgets.Section("View Model"));
            widgets.add(new Widgets.Toggle("Enabled", () -> vm.enabled, v -> vm.enabled = v));
            widgets.add(new Widgets.Slider("Scale", 0.2f, 2f, () -> vm.scale, v -> vm.scale = v,
                    v -> String.format(Locale.ROOT, "%.2fx", v)));
            widgets.add(new Widgets.Section("Position"));
            widgets.add(new Widgets.Slider("Left / Right", -1f, 1f, () -> vm.offsetX, v -> vm.offsetX = v,
                    v -> String.format(Locale.ROOT, "%.2f", v)));
            widgets.add(new Widgets.Slider("Up / Down", -1f, 1f, () -> vm.offsetY, v -> vm.offsetY = v,
                    v -> String.format(Locale.ROOT, "%.2f", v)));
            widgets.add(new Widgets.Slider("Close / Far", -1f, 1f, () -> vm.offsetZ, v -> vm.offsetZ = v,
                    v -> String.format(Locale.ROOT, "%.2f", v)));
            widgets.add(new Widgets.Button("Reset view model", false, () -> {
                vm.reset();
                rebuild();
            }));
        }
    }

    private void layout() {
        pw = Math.min(width - 20, 480);
        ph = Math.min(height - 20, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listX = px + SIDE + 8;
        listY = py + HEADER + 8;
        listH = ph - HEADER - 16;
        if (tab == Tab.MODULES) {
            setX = listX + LISTW + 8;
        } else {
            setX = listX;
        }
        setW = px + pw - 8 - setX;
        setY = listY;
        setH = listH;
    }

    private int contentHeight() {
        int t = 0;
        for (Widgets.Widget w : widgets) t += w.h + GAP;
        return t;
    }

    // ------------------------------------------------------------ rysowanie
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        layout();
        float o = open.to(closing ? 0f : 1f, 12f);
        if (closing && o < 0.03f && !closeQueued) {
            closeQueued = true;
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(null));
        }
        Ui.alpha = o;
        sc = 0.94f + 0.06f * o;

        stars.draw(ctx, width, height, 1f);

        double mx = mapX(mouseX), my = mapY(mouseY);
        float cx = width / 2f, cy = height / 2f;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0f);
        m.scale(sc, sc, 1f);
        m.translate(-cx, -cy, 0f);
        drawPanel(ctx, (int) mx, (int) my);
        m.pop();
        Ui.alpha = 1f;
    }

    private void drawPanel(DrawContext ctx, int mx, int my) {
        Ui.round(ctx, px, py, pw, ph, Ui.PANEL);
        ctx.fillGradient(px + 1, py, px + pw - 1, py + HEADER, Ui.a(0x558B5CF6), Ui.a(0x008B5CF6));
        Ui.outline(ctx, px, py, pw, ph, 0x558B5CF6);

        // logo
        Ui.textGrad(ctx, "VOID", px + 12, py + 10, Ui.ACCENT2, Ui.ACCENT, true);
        int vw = Ui.tr().getWidth("VOID");
        Ui.text(ctx, "CLIENT", px + 12 + vw + 3, py + 10, Ui.TEXT, true);
        String ver = "v1.0  |  1.21.4";
        Ui.text(ctx, ver, px + pw - 12 - Ui.tr().getWidth(ver), py + 10, Ui.DIM, false);
        Ui.rect(ctx, px + 1, py + HEADER, pw - 2, 1, 0x22FFFFFF);

        // sidebar
        Ui.rect(ctx, px + 1, py + HEADER + 1, SIDE, ph - HEADER - 2, 0x40000000);
        Ui.rect(ctx, px + SIDE + 1, py + HEADER + 1, 1, ph - HEADER - 2, 0x18FFFFFF);
        Tab[] tabs = Tab.values();
        int activeY = 0;
        for (int i = 0; i < tabs.length; i++) {
            int tx = px + 6, ty = py + HEADER + 10 + i * 24, tw = SIDE - 10, th = 20;
            boolean act = tabs[i] == tab;
            if (act) activeY = ty;
            float hv = tabHover[i].to(Ui.inside(mx, my, tx, ty, tw, th) ? 1f : 0f, 14f);
            if (act) Ui.round(ctx, tx, ty, tw, th, 0x338B5CF6);
            else Ui.round(ctx, tx, ty, tw, th, Ui.lerp(0x00FFFFFF, 0x14FFFFFF, hv));
            Ui.text(ctx, tabs[i].label, tx + 10, ty + 6, act ? Ui.TEXT : Ui.lerp(Ui.DIM, Ui.TEXT, hv), false);
        }
        if (!tabInit) {
            tabY.v = activeY;
            tabInit = true;
        }
        float iy = tabY.to(activeY, 16f);
        Ui.rect(ctx, px + 4, (int) iy + 3, 2, 14, Ui.ACCENT);

        Ui.textC(ctx, "Y = close", px + 1 + SIDE / 2, py + ph - 44, Ui.DIM, false);
        editorBtn.draw(ctx, px + 6, py + ph - 30, SIDE - 10, mx, my);

        // lista modulow
        if (tab == Tab.MODULES) {
            drawModuleList(ctx, mx, my);
        }

        // panel ustawien
        int hmx = Ui.inside(mx, my, setX, setY, setW, setH) ? mx : -10000;
        int hmy = Ui.inside(mx, my, setX, setY, setW, setH) ? my : -10000;
        float maxS = Math.max(0, contentHeight() - setH);
        setScroll = Ui.clamp(setScroll, 0, maxS);
        ctx.enableScissor(setX, setY, setX + setW, setY + setH);
        int y = setY - (int) setScroll;
        for (Widgets.Widget w : widgets) {
            if (y + w.h >= setY && y <= setY + setH) w.draw(ctx, setX, y, setW, hmx, hmy);
            y += w.h + GAP;
        }
        ctx.disableScissor();
        drawScrollbar(ctx, setX + setW - 2, setY, setH, setScroll, maxS);
    }

    private void drawModuleList(DrawContext ctx, int mx, int my) {
        int n = HudManager.MODULES.size();
        float maxL = Math.max(0, n * ROWH - listH);
        listScroll = Ui.clamp(listScroll, 0, maxL);
        boolean mouseIn = Ui.inside(mx, my, listX, listY, LISTW, listH);
        ctx.enableScissor(listX, listY, listX + LISTW, listY + listH);
        int y = listY - (int) listScroll;
        for (int i = 0; i < n; i++) {
            HudModule hm = HudManager.MODULES.get(i);
            boolean sel = hm == selected;
            boolean over = mouseIn && Ui.inside(mx, my, listX, y, LISTW, ROWH - 2);
            float hv = rowHover[i].to(over ? 1f : 0f, 14f);
            if (sel) {
                Ui.round(ctx, listX, y, LISTW, ROWH - 2, 0x338B5CF6);
                Ui.rect(ctx, listX, y + 3, 2, ROWH - 8, Ui.ACCENT);
            } else {
                Ui.round(ctx, listX, y, LISTW, ROWH - 2, Ui.lerp(0x00FFFFFF, 0x14FFFFFF, hv));
            }
            Ui.text(ctx, hm.name, listX + 8, y + 5, hm.s.enabled ? Ui.TEXT : Ui.DIM, false);
            float t = rowOn[i].to(hm.s.enabled ? 1f : 0f, 16f);
            int sx = listX + LISTW - 24, sy = y + 4;
            Ui.round(ctx, sx, sy, 18, 10, Ui.lerp(Ui.TRACK, Ui.ACCENT, t));
            Ui.round(ctx, sx + 1 + (int) (t * 8), sy + 1, 8, 8, 0xFFFFFFFF);
            y += ROWH;
        }
        ctx.disableScissor();
        drawScrollbar(ctx, listX + LISTW - 1, listY, listH, listScroll, maxL);
    }

    private void drawScrollbar(DrawContext ctx, int x, int y, int h, float scroll, float max) {
        if (max <= 0) return;
        int bar = Math.max(12, (int) (h * h / (h + max)));
        int by = y + (int) ((h - bar) * (scroll / max));
        Ui.rect(ctx, x, by, 2, bar, 0x558B5CF6);
    }

    // ------------------------------------------------------------ mysz
    private double mapX(double x) {
        return width / 2.0 + (x - width / 2.0) / sc;
    }

    private double mapY(double y) {
        return height / 2.0 + (y - height / 2.0) / sc;
    }

    @Override
    public boolean mouseClicked(double rx, double ry, int btn) {
        if (closing) return true;
        layout();
        double mx = mapX(rx), my = mapY(ry);

        if (editorBtn.click(mx, my, btn)) return true;

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (Ui.inside(mx, my, px + 6, py + HEADER + 10 + i * 24, SIDE - 10, 20)) {
                if (tab != tabs[i]) {
                    tab = tabs[i];
                    setScroll = 0;
                    listScroll = 0;
                    layout();
                    rebuild();
                }
                return true;
            }
        }

        if (tab == Tab.MODULES && Ui.inside(mx, my, listX, listY, LISTW, listH)) {
            int idx = (int) ((my - listY + listScroll) / ROWH);
            if (idx >= 0 && idx < HudManager.MODULES.size()) {
                HudModule hm = HudManager.MODULES.get(idx);
                if (mx >= listX + LISTW - 28) {
                    hm.s.enabled = !hm.s.enabled;
                    if (hm == selected) rebuild();
                } else if (hm != selected) {
                    selected = hm;
                    setScroll = 0;
                    rebuild();
                }
            }
            return true;
        }

        if (Ui.inside(mx, my, setX, setY, setW, setH)) {
            int y = setY - (int) setScroll;
            for (int i = 0; i < widgets.size(); i++) {
                Widgets.Widget w = widgets.get(i);
                if (my >= y && my < y + w.h && w.click(mx, my, btn)) {
                    active = w;
                    return true;
                }
                y += w.h + GAP;
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double rx, double ry, int btn, double dx, double dy) {
        if (active != null) {
            active.drag(mapX(rx), mapY(ry));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double rx, double ry, int btn) {
        if (active != null) {
            active.release();
            active = null;
            VoidConfig.save();
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double rx, double ry, double hAmt, double vAmt) {
        layout();
        double mx = mapX(rx), my = mapY(ry);
        if (tab == Tab.MODULES && Ui.inside(mx, my, listX, listY, LISTW, listH)) {
            listScroll = Ui.clamp(listScroll - (float) vAmt * 20f, 0,
                    Math.max(0, HudManager.MODULES.size() * ROWH - listH));
            return true;
        }
        if (Ui.inside(mx, my, setX, setY, setW, setH)) {
            setScroll = Ui.clamp(setScroll - (float) vAmt * 20f, 0, Math.max(0, contentHeight() - setH));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------ klawisze / zamykanie
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (VoidClientMod.openKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        closing = true;
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
