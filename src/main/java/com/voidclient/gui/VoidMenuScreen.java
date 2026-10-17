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

/** Nowa powloka GUI: gorny segmented-control zamiast bocznego panelu, listy zamiast kart,
 *  miekkie cienie zamiast obwodek. Ta sama paleta kolorow, zupelnie inny jezyk wizualny. */
public class VoidMenuScreen extends Screen {

    private enum Tab {
        HUD("HUD", "HUD", "Wlacz i dostosuj elementy wyswietlane na ekranie"),
        WORLD("World", "World", "Fullbright, usuwanie mgly i inne ulepszenia widocznosci"),
        COLOR("Color", "Color", "Jasnosc, kontrast, nasycenie i barwa swiata"),
        VIEWMODEL("VM", "View Model", "Pozycja i skala trzymanych przedmiotow"),
        THEME("Theme", "Theme", "Wyglad i animacje interfejsu VoidClient"),
        CONFIG("Config", "Config", "Zapisuj i wczytuj wlasne zestawy ustawien");

        final String tabLabel, fullLabel, desc;

        Tab(String t, String f, String d) {
            tabLabel = t;
            fullLabel = f;
            desc = d;
        }
    }

    private Tab tab = Tab.HUD;
    private boolean hudDetail = false;
    private HudModule selected = HudManager.MODULES.get(0);
    private final List<Widgets.Widget> widgets = new ArrayList<>();
    private Widgets.Widget active;
    private float listScroll;
    private String search = "";

    private final Anim open = new Anim(0);
    private boolean closing, closeQueued;
    private final long openStartNanos = System.nanoTime();
    private long contentRevealNanos = System.nanoTime();
    private final Anim indicatorX = new Anim(0), indicatorW = new Anim(0);
    private boolean indicatorInit;
    private final java.util.Map<Tab, int[]> tabBounds = new java.util.HashMap<>();
    private final java.util.Map<HudModule, Anim> rowHover = new java.util.HashMap<>();
    private final java.util.Map<HudModule, Anim> rowOn = new java.util.HashMap<>();

    private final Starfield stars = new Starfield();
    private final Widgets.Button editorBtn = new Widgets.Button("Editor", false,
            () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen()));
    private final Widgets.Button searchBtn = new Widgets.Button("\u26B2", false,
            () -> MinecraftClient.getInstance().setScreen(new SearchPromptScreen(this, search, this::applySearch)));
    private final Widgets.Button backBtn = new Widgets.Button("\u2190 Back", false, () -> {
        hudDetail = false;
        listScroll = 0;
        contentRevealNanos = System.nanoTime();
    });
    private final Widgets.Button newConfigBtn = new Widgets.Button("+ New", true,
            () -> MinecraftClient.getInstance().setScreen(new CreateConfigScreen(this)));

    private List<String> profiles = new ArrayList<>();
    private String profileMsg;
    private long profileMsgUntil;

    private int px, py, pw, ph;
    private int contentX, contentTop, contentW, contentBottom;
    private float sc = 1f;

    public VoidMenuScreen() {
        super(Text.literal("VoidClient"));
        for (HudModule m : HudManager.MODULES) {
            rowHover.put(m, new Anim(0));
            rowOn.put(m, new Anim(m.s.enabled ? 1f : 0f));
        }
        refreshProfiles();
    }

    public VoidMenuScreen(HudModule preselect) {
        this();
        tab = Tab.HUD;
        selected = preselect;
        hudDetail = true;
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

    public void applySearch(String s) {
        search = s == null ? "" : s.trim();
        listScroll = 0;
        contentRevealNanos = System.nanoTime();
    }

    // ------------------------------------------------------------ widgety ustawien
    private void rebuild() {
        widgets.clear();
        active = null;
        contentRevealNanos = System.nanoTime();
        switch (tab) {
            case HUD -> { if (hudDetail) buildHudDetail(); }
            case WORLD -> buildWorldFeatures();
            case COLOR -> buildColor();
            case VIEWMODEL -> buildViewModel();
            case THEME -> buildTheme();
            case CONFIG -> { /* wlasny rendering */ }
        }
    }

    private void buildHudDetail() {
        ModuleSettings s = selected.s;
        widgets.add(new Widgets.Toggle("Enabled", () -> s.enabled, v -> s.enabled = v));
        if (selected instanceof com.voidclient.hud.TitleModule) {
            widgets.add(new Widgets.Section("Content"));
            widgets.add(new Widgets.Button("Edit text", true, () -> MinecraftClient.getInstance().setScreen(
                    new SearchPromptScreen(this, s.text, v -> {
                        s.text = v;
                        VoidConfig.save();
                    }, "Title content", "Type text..."))));
        }
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

    private void buildWorldFeatures() {
        VoidConfig.WorldSettings w = VoidConfig.data.world;
        widgets.add(new Widgets.Toggle("Fullbright", () -> w.fullbright, v -> w.fullbright = v));
        widgets.add(new Widgets.Toggle("No Fog", () -> w.noFog, v -> w.noFog = v));
    }

    private void buildColor() {
        ColorSettings c = VoidConfig.data.color;
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
        widgets.add(new Widgets.Button("Reset", false, () -> {
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
        widgets.add(new Widgets.Slider("Accent hue", -0.5f, 0.5f, () -> t.hue, v -> t.hue = v,
                v -> String.format(Locale.ROOT, "%.0f\u00B0", v * 360f)));
        widgets.add(new Widgets.Toggle("Animated background", () -> t.animatedBg, v -> t.animatedBg = v));
        widgets.add(new Widgets.Button("Reset", false, () -> {
            t.reset();
            rebuild();
        }));
    }

    private void layout() {
        pw = Math.min(width - 24, 460);
        ph = Math.min(height - 24, 420);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        contentX = px + 18;
        contentW = pw - 36;
        contentTop = py + 92;
        contentBottom = py + ph - 16;
    }

    private int contentHeight() {
        int t = 0;
        for (Widgets.Widget w : widgets) t += w.h + 6;
        return t;
    }

    private List<HudModule> filteredModules() {
        if (search.isEmpty()) return HudManager.MODULES;
        String q = search.toLowerCase(Locale.ROOT);
        List<HudModule> out = new ArrayList<>();
        for (HudModule m : HudManager.MODULES) {
            if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.desc.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        return out;
    }

    private static float revealT(long startNanos, int index, float perItemDelay, float duration) {
        float elapsed = (System.nanoTime() - startNanos) / 1_000_000_000f - index * perItemDelay;
        if (elapsed <= 0f) return 0f;
        return Math.min(1f, elapsed / duration);
    }

    // ------------------------------------------------------------ rysowanie
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        layout();
        Ui.applyTheme(VoidConfig.data.theme.hue);
        float o;
        if (closing) {
            o = open.to(0f, 11f);
            sc = 0.97f + 0.03f * o;
        } else {
            float elapsed = (System.nanoTime() - openStartNanos) / 1_000_000_000f;
            float ot = Math.min(1f, elapsed / 0.3f);
            o = Ui.easeOutCubic(ot);
            sc = 0.94f + 0.06f * o;
            open.v = o;
        }
        if (closing && o < 0.03f && !closeQueued) {
            closeQueued = true;
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(null));
        }
        Ui.alpha = o;

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
        Ui.glow(ctx, px, py, pw, ph, Ui.ACCENT, 0.22f);
        Ui.shadow(ctx, px, py, pw, ph, 6, 0.3f);
        Ui.round(ctx, px, py, pw, ph, Ui.PANEL);

        drawWordmark(ctx);
        drawTabBar(ctx, mx, my);
        drawBreadcrumb(ctx, mx, my);

        if (tab == Tab.HUD && !hudDetail) {
            drawModuleList(ctx, mx, my);
        } else if (tab == Tab.CONFIG) {
            drawConfigList(ctx, mx, my);
        } else {
            drawSettingsList(ctx, mx, my);
        }
    }

    private void drawWordmark(DrawContext ctx) {
        String s = "voidclient";
        Ui.textC(ctx, s, px + pw / 2, py + 14, Ui.mulA(Ui.TEXT, 0.9f), false);
    }

    private void drawTabBar(DrawContext ctx, int mx, int my) {
        int y = py + 30, h = 26;
        Tab[] tabs = Tab.values();
        int gap = 6;
        int[] widths = new int[tabs.length];
        int total = 0;
        for (int i = 0; i < tabs.length; i++) {
            widths[i] = Ui.tr().getWidth(tabs[i].tabLabel) + 20;
            total += widths[i] + (i > 0 ? gap : 0);
        }
        int startX = px + (pw - total) / 2;
        int x = startX;
        int activeX = x, activeW = widths[0];
        for (int i = 0; i < tabs.length; i++) {
            tabBounds.put(tabs[i], new int[]{x, y, widths[i], h});
            if (tabs[i] == tab) {
                activeX = x;
                activeW = widths[i];
            }
            x += widths[i] + gap;
        }
        if (!indicatorInit) {
            indicatorX.v = activeX;
            indicatorW.v = activeW;
            indicatorInit = true;
        }
        float ix = indicatorX.to(activeX, 18f);
        float iw = indicatorW.to(activeW, 18f);
        Ui.shadowTint(ctx, (int) ix, y, (int) iw, h, 3, 0.4f, Ui.ACCENT);
        Ui.round(ctx, (int) ix, y, (int) iw, h, Ui.mulA(Ui.ACCENT, 0.95f));

        for (Tab t : tabs) {
            int[] b = tabBounds.get(t);
            boolean act = t == tab;
            boolean hov = !act && Ui.inside(mx, my, b[0], b[1], b[2], b[3]);
            int col = act ? 0xFF1A1424 : (hov ? Ui.TEXT : Ui.DIM);
            Ui.textC(ctx, t.tabLabel, b[0] + b[2] / 2, b[1] + 9, col, false);
        }
    }

    private void drawBreadcrumb(DrawContext ctx, int mx, int my) {
        int y = py + 66;
        String title = tab == Tab.HUD && hudDetail ? selected.name : tab.fullLabel;
        String desc = tab == Tab.HUD && hudDetail ? selected.desc : tab.desc;
        Ui.text(ctx, title, contentX, y, Ui.TEXT, false);
        Ui.text(ctx, desc, contentX, y + 11, Ui.DIM, false);

        int bw = 56, bh = 18, by = y - 1;
        if (tab == Tab.HUD && hudDetail) {
            backBtn.draw(ctx, px + pw - 18 - bw, by, bw, mx, my);
        } else if (tab == Tab.HUD) {
            editorBtn.draw(ctx, px + pw - 18 - bw, by, bw, mx, my);
            searchBtn.draw(ctx, px + pw - 18 - bw - 24, by, 20, mx, my);
        } else if (tab == Tab.CONFIG) {
            newConfigBtn.draw(ctx, px + pw - 18 - bw, by, bw, mx, my);
        }
        Ui.rect(ctx, contentX, y + 24, contentW, 1, 0x14FFFFFF);
    }

    // ------------------------------------------------------------ lista modulow HUD
    private void drawModuleList(DrawContext ctx, int mx, int my) {
        List<HudModule> list = filteredModules();
        int top = contentTop, bottom = contentBottom;
        int rowH = 34;
        float maxScroll = Math.max(0, list.size() * rowH - (bottom - top));
        listScroll = Ui.clamp(listScroll, 0, maxScroll);
        boolean mouseIn = Ui.inside(mx, my, contentX, top, contentW, bottom - top);

        ctx.enableScissor(contentX, top, contentX + contentW, bottom);
        int y = top - (int) listScroll;
        for (int i = 0; i < list.size(); i++) {
            HudModule hm = list.get(i);
            if (y + rowH < top || y > bottom) {
                y += rowH;
                continue;
            }
            float reveal = Ui.easeOutCubic(revealT(contentRevealNanos, i, 0.022f, 0.24f));
            float prevAlpha = Ui.alpha;
            Ui.alpha = prevAlpha * reveal;
            int rowY = y + (int) ((1f - reveal) * 6f);

            boolean over = mouseIn && Ui.inside(mx, my, contentX, rowY, contentW, rowH - 2);
            Anim hv = rowHover.computeIfAbsent(hm, k -> new Anim(0));
            float h = hv.to(over ? 1f : 0f, 14f);
            if (h > 0.01f) {
                Ui.round(ctx, contentX, rowY, contentW, rowH - 2, Ui.mulA(0xFFFFFFFF, h * 0.05f));
            }
            if (hm.s.enabled) {
                Ui.round(ctx, contentX, rowY + 6, 2, rowH - 14, Ui.ACCENT);
            }

            Ui.text(ctx, hm.name, contentX + 10, rowY + 5, Ui.TEXT, false);
            String d = hm.desc;
            int maxW = contentW - 70;
            while (Ui.tr().getWidth(d) > maxW && d.length() > 3) d = d.substring(0, d.length() - 2);
            if (!d.equals(hm.desc)) d = d + "..";
            Ui.text(ctx, d, contentX + 10, rowY + 17, Ui.DIM, false);

            Anim on = rowOn.computeIfAbsent(hm, k -> new Anim(hm.s.enabled ? 1f : 0f));
            float t = on.to(hm.s.enabled ? 1f : 0f, 16f);
            int sw = 26, shh = 11, swx = contentX + contentW - sw - 4, swy = rowY + (rowH - 2 - shh) / 2;
            Ui.round(ctx, swx, swy, sw, shh, Ui.lerp(Ui.mulA(Ui.TRACK, 0.8f), Ui.mulA(Ui.ACCENT, 0.55f), t));
            int knobX = swx + 2 + (int) (t * (sw - shh));
            Ui.shadow(ctx, knobX, swy + 2, shh - 4, shh - 4, 2, 0.22f);
            Ui.round(ctx, knobX, swy + 2, shh - 4, shh - 4, 0xFFFDFCFF);

            Ui.alpha = prevAlpha;
            y += rowH;
        }
        ctx.disableScissor();
        drawScrollbar(ctx, contentX + contentW - 1, top, bottom - top, listScroll, maxScroll);

        if (list.isEmpty()) {
            Ui.textC(ctx, "No modules match \"" + search + "\"", contentX + contentW / 2, top + 16, Ui.DIM, false);
        }
    }

    // ------------------------------------------------------------ panel ustawien (uniwersalny)
    private void drawSettingsList(DrawContext ctx, int mx, int my) {
        int top = contentTop, bottom = contentBottom;
        float maxS = Math.max(0, contentHeight() - (bottom - top));
        listScroll = Ui.clamp(listScroll, 0, maxS);
        int hmx = Ui.inside(mx, my, contentX, top, contentW, bottom - top) ? mx : -10000;
        int hmy = Ui.inside(mx, my, contentX, top, contentW, bottom - top) ? my : -10000;
        ctx.enableScissor(contentX, top, contentX + contentW, bottom);
        int y = top - (int) listScroll;
        for (int i = 0; i < widgets.size(); i++) {
            Widgets.Widget wd = widgets.get(i);
            if (y + wd.h >= top && y <= bottom) {
                float reveal = Ui.easeOutCubic(revealT(contentRevealNanos, i, 0.02f, 0.22f));
                float prevAlpha = Ui.alpha;
                Ui.alpha = prevAlpha * reveal;
                wd.draw(ctx, contentX, y, contentW, hmx, hmy);
                Ui.alpha = prevAlpha;
            }
            y += wd.h + 6;
        }
        ctx.disableScissor();
        drawScrollbar(ctx, contentX + contentW - 1, top, bottom - top, listScroll, maxS);
    }

    // ------------------------------------------------------------ config
    private void drawConfigList(DrawContext ctx, int mx, int my) {
        int top = contentTop, bottom = contentBottom, rowH = 30;
        float maxC = Math.max(0, profiles.size() * rowH - (bottom - top));
        listScroll = Ui.clamp(listScroll, 0, maxC);

        if (profiles.isEmpty()) {
            Ui.text(ctx, "No saved configs yet.", contentX, top + 4, Ui.DIM, false);
            Ui.text(ctx, "Tap \"+ New\" to save your current setup.", contentX, top + 16, Ui.DIM, false);
        } else {
            ctx.enableScissor(contentX, top, contentX + contentW, bottom);
            int y = top - (int) listScroll;
            for (String name : profiles) {
                boolean over = Ui.inside(mx, my, contentX, y, contentW, rowH - 4);
                if (over) Ui.round(ctx, contentX, y, contentW, rowH - 4, 0x0CFFFFFF);
                Ui.text(ctx, name, contentX + 8, y + 5, Ui.TEXT, false);
                Ui.text(ctx, "Tap to load", contentX + 8, y + 16, Ui.DIM, false);
                boolean delOver = Ui.inside(mx, my, contentX + contentW - 18, y + 7, 14, 14);
                Ui.text(ctx, "x", contentX + contentW - 15, y + 9, delOver ? 0xFFE38A96 : Ui.DIM, false);
                y += rowH;
            }
            ctx.disableScissor();
            drawScrollbar(ctx, contentX + contentW - 1, top, bottom - top, listScroll, maxC);
        }

        if (profileMsg != null) {
            if (System.currentTimeMillis() < profileMsgUntil) {
                Ui.text(ctx, profileMsg, contentX, bottom - 10, 0xFFA8E8BE, false);
            } else {
                profileMsg = null;
            }
        }
    }

    private void drawScrollbar(DrawContext ctx, int x, int y, int h, float scroll, float max) {
        if (max <= 0) return;
        int bar = Math.max(12, (int) (h * h / (h + max)));
        int by = y + (int) ((h - bar) * (scroll / max));
        Ui.round(ctx, x, by, 2, bar, Ui.mulA(Ui.ACCENT, 0.5f));
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

        for (Tab t : Tab.values()) {
            int[] b = tabBounds.get(t);
            if (b != null && Ui.inside(mx, my, b[0], b[1], b[2], b[3])) {
                if (tab != t) {
                    tab = t;
                    listScroll = 0;
                    hudDetail = false;
                    if (tab == Tab.CONFIG) refreshProfiles();
                    rebuild();
                }
                return true;
            }
        }

        if (tab == Tab.HUD && hudDetail) {
            if (backBtn.click(mx, my, btn)) return true;
        } else if (tab == Tab.HUD) {
            if (editorBtn.click(mx, my, btn) || searchBtn.click(mx, my, btn)) return true;
            if (clickModuleList(mx, my, btn)) return true;
            return true;
        } else if (tab == Tab.CONFIG) {
            if (newConfigBtn.click(mx, my, btn)) return true;
            if (clickConfig(mx, my, btn)) return true;
            return true;
        }

        int top = contentTop, bottom = contentBottom;
        if (Ui.inside(mx, my, contentX, top, contentW, bottom - top)) {
            int wy = top - (int) listScroll;
            for (Widgets.Widget wd : widgets) {
                if (my >= wy && my < wy + wd.h && wd.click(mx, my, btn)) {
                    active = wd;
                    return true;
                }
                wy += wd.h + 6;
            }
            return true;
        }
        return true;
    }

    private boolean clickModuleList(double mx, double my, int btn) {
        List<HudModule> list = filteredModules();
        int top = contentTop, bottom = contentBottom, rowH = 34;
        if (!Ui.inside(mx, my, contentX, top, contentW, bottom - top)) return false;
        int idx = (int) ((my - top + listScroll) / rowH);
        if (idx < 0 || idx >= list.size()) return true;
        HudModule hm = list.get(idx);
        int rowY = top - (int) listScroll + idx * rowH;
        int sw = 26, shh = 11, swx = contentX + contentW - sw - 4, swy = rowY + (rowH - 2 - shh) / 2;
        if (Ui.inside(mx, my, swx, swy, sw, shh)) {
            hm.s.enabled = !hm.s.enabled;
        } else {
            selected = hm;
            hudDetail = true;
            listScroll = 0;
            rebuild();
        }
        return true;
    }

    private boolean clickConfig(double mx, double my, int btn) {
        int top = contentTop, bottom = contentBottom, rowH = 30;
        if (!Ui.inside(mx, my, contentX, top, contentW, bottom - top)) return true;
        int idx = (int) ((my - top + listScroll) / rowH);
        if (idx >= 0 && idx < profiles.size()) {
            String name = profiles.get(idx);
            int rowY = top - (int) listScroll + idx * rowH;
            if (Ui.inside(mx, my, contentX + contentW - 18, rowY + 7, 14, 14)) {
                VoidConfig.deleteProfile(name);
                refreshProfiles();
            } else if (VoidConfig.loadProfile(name)) {
                profileMsg = "Loaded: " + name;
                profileMsgUntil = System.currentTimeMillis() + 2000;
                rebuild();
            }
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
        if (!Ui.inside(mx, my, contentX, contentTop, contentW, contentBottom - contentTop)) return false;
        float max;
        if (tab == Tab.HUD && !hudDetail) {
            max = Math.max(0, filteredModules().size() * 34 - (contentBottom - contentTop));
        } else if (tab == Tab.CONFIG) {
            max = Math.max(0, profiles.size() * 30 - (contentBottom - contentTop));
        } else {
            max = Math.max(0, contentHeight() - (contentBottom - contentTop));
        }
        listScroll = Ui.clamp(listScroll - (float) vAmt * 22f, 0, max);
        return true;
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
