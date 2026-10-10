package com.voidclient.gui;

import com.voidclient.VoidClientMod;
import com.voidclient.VoidConfig;
import com.voidclient.VoidConfig.ColorSettings;
import com.voidclient.VoidConfig.HandSettings;
import com.voidclient.VoidConfig.ModuleSettings;
import com.voidclient.VoidConfig.ThemeSettings;
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
    private static final int HEADER = 28, SIDE = 96, LISTW = 112, ROWH = 20, GAP = 4;

    private enum Tab {
        HUD("HUD"), WORLD("World"), VIEWMODEL("View Model"), THEME("Theme"), CONFIG("Config");
        final String label;

        Tab(String l) {
            label = l;
        }
    }

    private Tab tab = Tab.HUD;
    private HudModule selected = HudManager.MODULES.get(0);
    private final List<Widgets.Widget> widgets = new ArrayList<>();
    private Widgets.Widget active;
    private float listScroll, setScroll, cfgScroll;

    private final Anim open = new Anim(0);
    private final Anim tabY = new Anim(0);
    private boolean tabInit;
    private final Anim[] tabHover;
    private final Anim[] rowHover, rowOn;
    private boolean closing, closeQueued;
    private final Starfield stars = new Starfield();
    private final Widgets.Button editorBtn;
    private final Widgets.Button newConfigBtn;

    private List<String> profiles = new ArrayList<>();
    private String profileMsg;
    private long profileMsgUntil;

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
        tabHover = new Anim[Tab.values().length];
        for (int i = 0; i < tabHover.length; i++) tabHover[i] = new Anim(0);

        editorBtn = new Widgets.Button("HUD Editor", true,
                () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen()));
        newConfigBtn = new Widgets.Button("+ Nowy config", true,
                () -> MinecraftClient.getInstance().setScreen(new CreateConfigScreen(this)));
        refreshProfiles();
        rebuild();
    }

    private void refreshProfiles() {
        profiles = VoidConfig.listProfiles();
    }

    public void notifyProfileSaved() {
        refreshProfiles();
        profileMsg = "Zapisano config";
        profileMsgUntil = System.currentTimeMillis() + 2000;
    }

    // ------------------------------------------------------------ budowa widgetow
    private void rebuild() {
        widgets.clear();
        active = null;
        switch (tab) {
            case HUD -> buildHud();
            case WORLD -> buildWorld();
            case VIEWMODEL -> buildViewModel();
            case THEME -> buildTheme();
            case CONFIG -> { /* wlasny, niestandardowy rendering ponizej */ }
        }
    }

    private void buildHud() {
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
    }

    private void buildWorld() {
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
    }

    private void handWidgets(String title, HandSettings h) {
        widgets.add(new Widgets.Section(title));
        widgets.add(new Widgets.Toggle("Enabled", () -> h.enabled, v -> h.enabled = v));
        widgets.add(new Widgets.Slider("Scale", 0.2f, 2f, () -> h.scale, v -> h.scale = v,
                v -> String.format(Locale.ROOT, "%.2fx", v)));
        widgets.add(new Widgets.Slider("Left / Right", -2f, 2f, () -> h.offsetX, v -> h.offsetX = v,
                v -> String.format(Locale.ROOT, "%.2f", v)));
        widgets.add(new Widgets.Slider("Up / Down", -2f, 2f, () -> h.offsetY, v -> h.offsetY = v,
                v -> String.format(Locale.ROOT, "%.2f", v)));
        widgets.add(new Widgets.Slider("Close / Far", -2f, 2f, () -> h.offsetZ, v -> h.offsetZ = v,
                v -> String.format(Locale.ROOT, "%.2f", v)));
        widgets.add(new Widgets.Button("Reset " + title.toLowerCase(Locale.ROOT), false, () -> {
            h.reset();
            rebuild();
        }));
    }

    private void buildViewModel() {
        handWidgets("Main hand", VoidConfig.data.viewModel.main);
        handWidgets("Off hand", VoidConfig.data.viewModel.off);
    }

    private void buildTheme() {
        ThemeSettings t = VoidConfig.data.theme;
        widgets.add(new Widgets.Section("Theme"));
        widgets.add(new Widgets.Slider("Accent hue", -0.5f, 0.5f, () -> t.hue, v -> t.hue = v,
                v -> String.format(Locale.ROOT, "%.0f\u00B0", v * 360f)));
        widgets.add(new Widgets.Toggle("Animated background", () -> t.animatedBg, v -> t.animatedBg = v));
        widgets.add(new Widgets.Button("Reset theme", false, () -> {
            t.reset();
            rebuild();
        }));
    }

    private void layout() {
        pw = Math.min(width - 20, 520);
        ph = Math.min(height - 20, 320);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listX = px + SIDE + 8;
        listY = py + HEADER + 8;
        listH = ph - HEADER - 16;
        if (tab == Tab.HUD) {
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
        Ui.applyTheme(VoidConfig.data.theme.hue);
        float o = open.to(closing ? 0f : 1f, 12f);
        if (closing && o < 0.03f && !closeQueued) {
            closeQueued = true;
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(null));
        }
        Ui.alpha = o;
        sc = 0.94f + 0.06f * o;

        if (VoidConfig.data.theme.animatedBg) {
            stars.draw(ctx, width, height, 1f);
        } else {
            ctx.fillGradient(0, 0, width, height, Ui.a(0xD0070312), Ui.a(0xE0140828));
        }

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
        int panelCol = tab == Tab.WORLD ? Ui.PANEL_GLASS : Ui.PANEL;
        Ui.round(ctx, px, py, pw, ph, panelCol);
        ctx.fillGradient(px + 1, py, px + pw - 1, py + HEADER, Ui.a(0x558B5CF6), Ui.a(0x008B5CF6));
        Ui.outline(ctx, px, py, pw, ph, 0x558B5CF6);

        // logo
        Ui.textGrad(ctx, "VOID", px + 12, py + 10, Ui.ACCENT2, Ui.ACCENT, true);
        int vw = Ui.tr().getWidth("VOID");
        Ui.text(ctx, "CLIENT", px + 12 + vw + 3, py + 10, Ui.TEXT, true);
        String ver = "v1.1  |  1.21.4";
        Ui.text(ctx, ver, px + pw - 12 - Ui.tr().getWidth(ver), py + 10, Ui.DIM, false);
        Ui.rect(ctx, px + 1, py + HEADER, pw - 2, 1, 0x22FFFFFF);

        // sidebar
        Ui.rect(ctx, px + 1, py + HEADER + 1, SIDE, ph - HEADER - 2, 0x40000000);
        Ui.rect(ctx, px + SIDE + 1, py + HEADER + 1, 1, ph - HEADER - 2, 0x18FFFFFF);
        Tab[] tabs = Tab.values();
        int activeY = 0;
        for (int i = 0; i < tabs.length; i++) {
            int tx = px + 6, ty = py + HEADER + 8 + i * 22, tw = SIDE - 10, th = 18;
            boolean act = tabs[i] == tab;
            if (act) activeY = ty;
            float hv = tabHover[i].to(Ui.inside(mx, my, tx, ty, tw, th) ? 1f : 0f, 14f);
            if (act) Ui.round(ctx, tx, ty, tw, th, 0x338B5CF6);
            else Ui.round(ctx, tx, ty, tw, th, Ui.lerp(0x00FFFFFF, 0x14FFFFFF, hv));
            Ui.text(ctx, tabs[i].label, tx + 10, ty + 5, act ? Ui.TEXT : Ui.lerp(Ui.DIM, Ui.TEXT, hv), false);
        }
        if (!tabInit) {
            tabY.v = activeY;
            tabInit = true;
        }
        float iy = tabY.to(activeY, 16f);
        Ui.rect(ctx, px + 4, (int) iy + 2, 2, 14, Ui.ACCENT);

        Ui.textC(ctx, "Y = close", px + 1 + SIDE / 2, py + ph - 44, Ui.DIM, false);
        editorBtn.draw(ctx, px + 6, py + ph - 30, SIDE - 10, mx, my);

        if (tab == Tab.HUD) {
            drawModuleList(ctx, mx, my);
            drawSettingsList(ctx, mx, my);
        } else if (tab == Tab.CONFIG) {
            drawConfigTab(ctx, mx, my);
        } else {
            drawSettingsList(ctx, mx, my);
        }
    }

    private void drawSettingsList(DrawContext ctx, int mx, int my) {
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

    private void drawConfigTab(DrawContext ctx, int mx, int my) {
        Ui.text(ctx, "ZAPISANE CONFIGI", setX + 2, setY, Ui.ACCENT, false);
        int btnW = 120;
        newConfigBtn.draw(ctx, setX + setW - btnW, setY - 2, btnW, mx, my);

        int listTop = setY + 18;
        int listBottom = setY + setH;
        int rowH = 22;
        float maxC = Math.max(0, profiles.size() * rowH - (listBottom - listTop));
        cfgScroll = Ui.clamp(cfgScroll, 0, maxC);

        if (profiles.isEmpty()) {
            Ui.text(ctx, "Brak zapisanych configow.", setX + 2, listTop + 6, Ui.DIM, false);
            Ui.text(ctx, "Kliknij \"+ Nowy config\", zeby zapisac biezace ustawienia.",
                    setX + 2, listTop + 20, Ui.DIM, false);
        } else {
            ctx.enableScissor(setX, listTop, setX + setW, listBottom);
            int y = listTop - (int) cfgScroll;
            for (String name : profiles) {
                boolean over = Ui.inside(mx, my, setX, y, setW, rowH - 2);
                Ui.round(ctx, setX, y, setW, rowH - 2, over ? 0x20FFFFFF : 0x12FFFFFF);
                Ui.text(ctx, name, setX + 8, y + 6, Ui.TEXT, false);
                String hint = "Kliknij, by wczytac";
                int hw = Ui.tr().getWidth(hint);
                Ui.text(ctx, hint, setX + setW - hw - 26, y + 6, Ui.DIM, false);
                boolean delOver = Ui.inside(mx, my, setX + setW - 18, y + 3, 14, 14);
                Ui.text(ctx, "x", setX + setW - 15, y + 5, delOver ? 0xFFFF6B6B : Ui.DIM, false);
                y += rowH;
            }
            ctx.disableScissor();
            drawScrollbar(ctx, setX + setW - 2, listTop, listBottom - listTop, cfgScroll, maxC);
        }

        if (profileMsg != null) {
            if (System.currentTimeMillis() < profileMsgUntil) {
                Ui.text(ctx, profileMsg, setX + 2, listBottom - 10, 0xFF7CF59E, false);
            } else {
                profileMsg = null;
            }
        }
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
            if (Ui.inside(mx, my, px + 6, py + HEADER + 8 + i * 22, SIDE - 10, 18)) {
                if (tab != tabs[i]) {
                    tab = tabs[i];
                    setScroll = 0;
                    listScroll = 0;
                    cfgScroll = 0;
                    if (tab == Tab.CONFIG) refreshProfiles();
                    layout();
                    rebuild();
                }
                return true;
            }
        }

        if (tab == Tab.HUD && Ui.inside(mx, my, listX, listY, LISTW, listH)) {
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

        if (tab == Tab.CONFIG) {
            int btnW = 120;
            if (newConfigBtn.click(mx, my, btn)) return true;
            int listTop = setY + 18, rowH = 22;
            if (Ui.inside(mx, my, setX, listTop, setW, setH - 18)) {
                int idx = (int) ((my - listTop + cfgScroll) / rowH);
                if (idx >= 0 && idx < profiles.size()) {
                    String name = profiles.get(idx);
                    int rowY = listTop - (int) cfgScroll + idx * rowH;
                    if (Ui.inside(mx, my, setX + setW - 18, rowY + 3, 14, 14)) {
                        VoidConfig.deleteProfile(name);
                        refreshProfiles();
                    } else {
                        if (VoidConfig.loadProfile(name)) {
                            profileMsg = "Wczytano: " + name;
                            profileMsgUntil = System.currentTimeMillis() + 2000;
                            rebuild();
                        }
                    }
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
        if (tab == Tab.HUD && Ui.inside(mx, my, listX, listY, LISTW, listH)) {
            listScroll = Ui.clamp(listScroll - (float) vAmt * 20f, 0,
                    Math.max(0, HudManager.MODULES.size() * ROWH - listH));
            return true;
        }
        if (tab == Tab.CONFIG && Ui.inside(mx, my, setX, setY, setW, setH)) {
            cfgScroll = Ui.clamp(cfgScroll - (float) vAmt * 20f, 0,
                    Math.max(0, profiles.size() * 22 - (setH - 18)));
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
