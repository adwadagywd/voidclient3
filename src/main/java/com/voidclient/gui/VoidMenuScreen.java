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
    private static final int HEADER = 46, SIDE = 150, ROWH = 42, GAP = 4;

    private enum Tab {
        HUD("HUD", "H", "Wlacz i dostosuj elementy wyswietlane na ekranie"),
        WORLD("World", "W", "Fullbright, usuwanie mgly i inne ulepszenia widocznosci"),
        COLOR("Color", "C", "Jasnosc, kontrast, nasycenie i barwa swiata"),
        VIEWMODEL("View Model", "V", "Pozycja i skala trzymanych przedmiotow"),
        THEME("Theme", "T", "Wyglad i animacje interfejsu VoidClient"),
        CONFIG("Config", "C", "Zapisuj i wczytuj wlasne zestawy ustawien");

        final String label, glyph, desc;

        Tab(String l, String g, String d) {
            label = l;
            glyph = g;
            desc = d;
        }
    }

    private static final Tab[] FEATURES = {Tab.HUD, Tab.WORLD, Tab.COLOR, Tab.VIEWMODEL, Tab.THEME};
    private static final Tab[] SYSTEM = {Tab.CONFIG};

    private Tab tab = Tab.HUD;
    private boolean hudDetail = false;
    private HudModule selected = HudManager.MODULES.get(0);
    private final List<Widgets.Widget> widgets = new ArrayList<>();
    private Widgets.Widget active;
    private float cardScroll, setScroll, cfgScroll;
    private String search = "";

    private final Anim open = new Anim(0);
    private boolean closing, closeQueued;
    private final long openStartNanos = System.nanoTime();
    private long cardsRevealNanos = System.nanoTime();
    private long detailRevealNanos = System.nanoTime();
    private final Starfield stars = new Starfield();
    private final Widgets.Button editorBtn;
    private final Widgets.Button newConfigBtn;
    private final Widgets.Button backBtn;
    private final java.util.Map<Tab, Anim> tabHover = new java.util.HashMap<>();
    private final java.util.Map<HudModule, Anim> cardHover = new java.util.HashMap<>();
    private final java.util.Map<HudModule, Anim> cardOn = new java.util.HashMap<>();

    private List<String> profiles = new ArrayList<>();
    private String profileMsg;
    private long profileMsgUntil;

    private int px, py, pw, ph;
    private int bodyX, bodyY, bodyW, bodyH;
    private float sc = 1f;

    public VoidMenuScreen() {
        super(Text.literal("VoidClient"));
        for (Tab t : Tab.values()) tabHover.put(t, new Anim(0));
        for (HudModule m : HudManager.MODULES) {
            cardHover.put(m, new Anim(0));
            cardOn.put(m, new Anim(m.s.enabled ? 1f : 0f));
        }
        editorBtn = new Widgets.Button("HUD Editor", true,
                () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen()));
        newConfigBtn = new Widgets.Button("+ Nowy config", true,
                () -> MinecraftClient.getInstance().setScreen(new CreateConfigScreen(this)));
        backBtn = new Widgets.Button("< Modules", false, () -> {
            hudDetail = false;
            setScroll = 0;
            cardsRevealNanos = System.nanoTime();
        });
        refreshProfiles();
    }

    /** Otwiera menu od razu na ustawieniach wskazanego modulu (np. z PPM w HUD Editorze). */
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
        cardScroll = 0;
        cardsRevealNanos = System.nanoTime();
    }

    // ------------------------------------------------------------ budowa widgetow ustawien
    private void rebuild() {
        widgets.clear();
        active = null;
        detailRevealNanos = System.nanoTime();
        switch (tab) {
            case HUD -> buildHud();
            case WORLD -> buildWorldFeatures();
            case COLOR -> buildColor();
            case VIEWMODEL -> buildViewModel();
            case THEME -> buildTheme();
            case CONFIG -> { /* wlasny rendering ponizej */ }
        }
    }

    private void buildHud() {
        ModuleSettings s = selected.s;
        widgets.add(new Widgets.Toggle("Enabled", () -> s.enabled, v -> s.enabled = v));
        if (selected instanceof com.voidclient.hud.TitleModule) {
            widgets.add(new Widgets.Section("Tresc"));
            widgets.add(new Widgets.Button("Edytuj tekst", true, () -> MinecraftClient.getInstance().setScreen(
                    new SearchPromptScreen(this, s.text, v -> {
                        s.text = v;
                        VoidConfig.save();
                    }, "Tresc tytulu", "Wpisz tekst..."))));
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
        widgets.add(new Widgets.Slider("Accent hue", -0.5f, 0.5f, () -> t.hue, v -> t.hue = v,
                v -> String.format(Locale.ROOT, "%.0f\u00B0", v * 360f)));
        widgets.add(new Widgets.Toggle("Animated background", () -> t.animatedBg, v -> t.animatedBg = v));
        widgets.add(new Widgets.Button("Reset theme", false, () -> {
            t.reset();
            rebuild();
        }));
    }

    private void layout() {
        pw = Math.min(width - 24, 620);
        ph = Math.min(height - 24, 380);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bodyX = px + SIDE + 1;
        bodyY = py + HEADER + 1;
        bodyW = px + pw - bodyX - 1;
        bodyH = py + ph - bodyY - 1;
    }

    private int contentHeight() {
        int t = 0;
        for (Widgets.Widget w : widgets) t += w.h + GAP;
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

    // ------------------------------------------------------------ rysowanie
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        layout();
        Ui.applyTheme(VoidConfig.data.theme.hue);
        float o;
        if (closing) {
            o = open.to(0f, 12f);
            sc = 0.95f + 0.05f * o;
        } else {
            float elapsed = (System.nanoTime() - openStartNanos) / 1_000_000_000f;
            float ot = Math.min(1f, elapsed / 0.32f);
            o = Ui.easeOutCubic(ot);
            sc = 0.78f + 0.22f * Ui.easeOutBack(ot);
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
        Ui.roundBordered(ctx, px, py, pw, ph, Ui.PANEL, 0x668727F5, 1);
        drawOpenSweep(ctx);

        drawSidebar(ctx, mx, my);
        drawHeader(ctx, mx, my);

        if (tab == Tab.HUD) {
            if (hudDetail) drawDetailPane(ctx, mx, my);
            else drawModuleCards(ctx, mx, my);
        } else if (tab == Tab.CONFIG) {
            drawConfigTab(ctx, mx, my);
        } else {
            drawDetailPane(ctx, mx, my);
        }
    }

    /** Blysk swiatla przebiegajacy raz przez panel zaraz po otwarciu menu - efekt "wow". */
    private void drawOpenSweep(DrawContext ctx) {
        if (closing) return;
        float elapsed = (System.nanoTime() - openStartNanos) / 1_000_000_000f;
        if (elapsed >= 0.65f) return;
        float st = Ui.clamp(elapsed / 0.65f, 0f, 1f);
        float fade = 1f - st;
        int sweepX = px + (int) (Ui.easeOutCubic(st) * pw);
        for (int dxs = -16; dxs <= 16; dxs++) {
            int xx = sweepX + dxs;
            if (xx < px || xx > px + pw) continue;
            float band = Math.max(0f, 1f - Math.abs(dxs) / 16f);
            Ui.rect(ctx, xx, py + 2, 1, ph - 4, Ui.mulA(Ui.ACCENT, band * band * fade * 0.6f));
        }
    }

    // ------------------------------------------------------------ sidebar
    private void drawSidebar(DrawContext ctx, int mx, int my) {
        Ui.round(ctx, px + 4, py + 4, SIDE - 4, ph - 8, 0x33050309);
        Ui.rect(ctx, px + SIDE, py + 1, 1, ph - 2, 0x18FFFFFF);

        // marka - skromnie, bez duzego awatara
        int ax = px + 16, ay = py + 15;
        Ui.text(ctx, "VoidClient", ax, ay, Ui.TEXT, false);
        Ui.text(ctx, "1.21.4", ax, ay + 11, Ui.DIM, false);
        Ui.rect(ctx, px + 10, py + HEADER - 1, SIDE - 20, 1, 0x12FFFFFF);

        int y = py + HEADER + 10;
        y = drawNavGroup(ctx, mx, my, "FEATURES", FEATURES, y, 0);
        y += 6;
        drawNavGroup(ctx, mx, my, "SYSTEM", SYSTEM, y, FEATURES.length);
    }

    private int drawNavGroup(DrawContext ctx, int mx, int my, String title, Tab[] items, int y, int indexOffset) {
        Ui.text(ctx, title, px + 16, y, Ui.DIM, false);
        y += 14;
        for (int i = 0; i < items.length; i++) {
            Tab t = items[i];
            float reveal = Ui.easeOutCubic(revealT(openStartNanos, indexOffset + i, 0.05f, 0.3f));
            int slide = (int) ((1f - reveal) * 18f);
            float prevAlpha = Ui.alpha;
            Ui.alpha = prevAlpha * reveal;

            int tx = px + 8 - slide, tw = SIDE - 16, th = 24;
            boolean act = t == tab;
            Anim hv = tabHover.get(t);
            float h = hv.to(Ui.inside(mx, my, tx, y, tw, th) ? 1f : 0f, 14f);
            if (act) {
                Ui.round(ctx, tx, y, tw, th, 0x1EB9A4F7);
            } else if (h > 0.01f) {
                Ui.round(ctx, tx, y, tw, th, Ui.lerp(0x00FFFFFF, 0x0EFFFFFF, h));
            }
            if (act) {
                Ui.round(ctx, tx + 2, y + 5, 2, th - 10, Ui.ACCENT);
            }
            Ui.text(ctx, t.label, tx + 12, y + 8, act ? Ui.TEXT : Ui.lerp(Ui.DIM, Ui.TEXT, h), false);
            Ui.alpha = prevAlpha;
            y += th + 4;
        }
        return y;
    }

    /** Staggerowane wejscie elementu listy: 0 przed startem, narasta do 1 po `duration` sekund,
     *  opoznione o index * perItemDelay (efekt "fali" elementow pojawiajacych sie kolejno). */
    private static float revealT(long startNanos, int index, float perItemDelay, float duration) {
        float elapsed = (System.nanoTime() - startNanos) / 1_000_000_000f - index * perItemDelay;
        if (elapsed <= 0f) return 0f;
        return Math.min(1f, elapsed / duration);
    }

    // ------------------------------------------------------------ header / breadcrumb
    private void drawHeader(DrawContext ctx, int mx, int my) {
        int hx = bodyX, hy = py + 10, hw = bodyW;
        String crumbDesc = tab == Tab.HUD && hudDetail ? selected.desc : tab.desc;
        String crumbTitle = tab == Tab.HUD && hudDetail ? selected.name : tab.label;

        Ui.text(ctx, crumbTitle, hx + 2, hy, Ui.TEXT, true);
        int tw = Ui.tr().getWidth(crumbTitle);
        Ui.text(ctx, "  \u2022  " + crumbDesc, hx + 2 + tw, hy + 1, Ui.DIM, false);

        // wyszukiwarka (tylko zakladka HUD ma realne dzialanie)
        int sw = 150, sh = 20, sx = hx + hw - sw, sy = py + 7;
        boolean over = Ui.inside(mx, my, sx, sy, sw, sh);
        Ui.roundBordered(ctx, sx, sy, sw, sh, over ? 0x22FFFFFF : 0x16FFFFFF, 0x40FFFFFF, 1);
        String placeholder = search.isEmpty() ? "Szukaj..." : search;
        Ui.text(ctx, "\u26B2 " + placeholder, sx + 8, sy + 6, search.isEmpty() ? Ui.DIM : Ui.TEXT, false);

        if (tab == Tab.HUD && !hudDetail) {
            editorBtn.draw(ctx, hx, py + ph - 24, 96, mx, my);
        }
        if (tab == Tab.HUD && hudDetail) {
            backBtn.draw(ctx, hx, py + ph - 24, 90, mx, my);
        }
        Ui.rect(ctx, bodyX, bodyY - 1, bodyW, 1, 0x18FFFFFF);
    }

    // ------------------------------------------------------------ karty modulow (HUD)
    private void drawModuleCards(DrawContext ctx, int mx, int my) {
        List<HudModule> list = filteredModules();
        int top = bodyY + 2, bottom = bodyY + bodyH - 28;
        int cols = bodyW > 420 ? 2 : 1;
        int gap = 8;
        int cardW = (bodyW - gap * (cols - 1) - 4) / cols;
        int cardH = 40;

        float maxScroll = Math.max(0, ((list.size() + cols - 1) / cols) * (cardH + gap) - (bottom - top));
        cardScroll = Ui.clamp(cardScroll, 0, maxScroll);

        boolean mouseIn = Ui.inside(mx, my, bodyX, top, bodyW, bottom - top);
        ctx.enableScissor(bodyX, top, bodyX + bodyW, bottom);
        for (int i = 0; i < list.size(); i++) {
            HudModule hm = list.get(i);
            int col = i % cols, row = i / cols;
            int cx = bodyX + 2 + col * (cardW + gap);
            int cy = top - (int) cardScroll + row * (cardH + gap);
            if (cy + cardH < top || cy > bottom) continue;

            float reveal = Ui.easeOutCubic(revealT(cardsRevealNanos, i, 0.035f, 0.3f));
            int riseY = (int) ((1f - reveal) * 14f);
            float prevAlpha = Ui.alpha;
            Ui.alpha = prevAlpha * reveal;
            cy += riseY;

            boolean over = mouseIn && Ui.inside(mx, my, cx, cy, cardW, cardH);
            Anim hv = cardHover.computeIfAbsent(hm, k -> new Anim(0));
            float h = hv.to(over ? 1f : 0f, 16f);
            int bg = Ui.lerp(0x16120A24, 0x241B3350, h);
            Ui.round(ctx, cx, cy, cardW, cardH, bg);
            if (hm.s.enabled) {
                Ui.round(ctx, cx, cy, 3, cardH, Ui.ACCENT);
            }

            Ui.text(ctx, hm.name, cx + 12, cy + 7, Ui.TEXT, false);
            String d = hm.desc;
            int maxW = cardW - 56;
            while (Ui.tr().getWidth(d) > maxW && d.length() > 3) {
                d = d.substring(0, d.length() - 2);
            }
            if (!d.equals(hm.desc)) d = d + "..";
            Ui.text(ctx, d, cx + 12, cy + 20, Ui.DIM, false);

            Anim on = cardOn.computeIfAbsent(hm, k -> new Anim(hm.s.enabled ? 1f : 0f));
            float t = on.to(hm.s.enabled ? 1f : 0f, 18f);
            int sw = 24, shh = 12, swx = cx + cardW - sw - 10, swy = cy + (cardH - shh) / 2;
            Ui.round(ctx, swx, swy, sw, shh, Ui.lerp(Ui.TRACK, Ui.ACCENT, t));
            Ui.round(ctx, swx + 2 + (int) (t * (sw - shh)), swy + 2, shh - 4, shh - 4, 0xFFFFFFFF);
            Ui.alpha = prevAlpha;
        }
        ctx.disableScissor();
        drawScrollbar(ctx, bodyX + bodyW - 2, top, bottom - top, cardScroll, maxScroll);

        if (list.isEmpty()) {
            Ui.textC(ctx, "Brak modulow dla \"" + search + "\"", bodyX + bodyW / 2, top + 20, Ui.DIM, false);
        }
    }

    // ------------------------------------------------------------ panel ustawien (detal / World / VM / Theme)
    private void drawDetailPane(DrawContext ctx, int mx, int my) {
        int top = bodyY + 2;
        int bottom = bodyY + bodyH;
        int left = bodyX + 2, w = bodyW - 4;

        int hmx = Ui.inside(mx, my, left, top, w, bottom - top) ? mx : -10000;
        int hmy = Ui.inside(mx, my, left, top, w, bottom - top) ? my : -10000;
        float maxS = Math.max(0, contentHeight() - (bottom - top));
        setScroll = Ui.clamp(setScroll, 0, maxS);
        ctx.enableScissor(left, top, left + w, bottom);
        int y = top - (int) setScroll;
        for (int i = 0; i < widgets.size(); i++) {
            Widgets.Widget wd = widgets.get(i);
            if (y + wd.h >= top && y <= bottom) {
                float reveal = Ui.easeOutCubic(revealT(detailRevealNanos, i, 0.025f, 0.26f));
                int slide = (int) ((1f - reveal) * 12f);
                float prevAlpha = Ui.alpha;
                Ui.alpha = prevAlpha * reveal;
                wd.draw(ctx, left + slide, y, w - slide, hmx, hmy);
                Ui.alpha = prevAlpha;
            }
            y += wd.h + GAP;
        }
        ctx.disableScissor();
        drawScrollbar(ctx, left + w - 2, top, bottom - top, setScroll, maxS);
    }

    // ------------------------------------------------------------ config
    private void drawConfigTab(DrawContext ctx, int mx, int my) {
        int top = bodyY + 2, bottom = bodyY + bodyH, left = bodyX + 2, w = bodyW - 4;
        int btnW = 120;
        newConfigBtn.draw(ctx, left + w - btnW, top, btnW, mx, my);

        int listTop = top + 26;
        int rowH = 32;
        float maxC = Math.max(0, profiles.size() * rowH - (bottom - listTop));
        cfgScroll = Ui.clamp(cfgScroll, 0, maxC);

        if (profiles.isEmpty()) {
            Ui.text(ctx, "Brak zapisanych configow.", left + 2, listTop + 6, Ui.DIM, false);
            Ui.text(ctx, "Kliknij \"+ Nowy config\", zeby zapisac biezace ustawienia.",
                    left + 2, listTop + 20, Ui.DIM, false);
        } else {
            ctx.enableScissor(left, listTop, left + w, bottom);
            int y = listTop - (int) cfgScroll;
            for (String name : profiles) {
                boolean over = Ui.inside(mx, my, left, y, w, rowH - 4);
                Ui.round(ctx, left, y, w, rowH - 4, over ? 0x22FFFFFF : 0x14FFFFFF);
                Ui.round(ctx, left, y, 3, rowH - 4, Ui.ACCENT);
                Ui.text(ctx, name, left + 12, y + 6, Ui.TEXT, false);
                Ui.text(ctx, "Kliknij, by wczytac", left + 12, y + 17, Ui.DIM, false);
                boolean delOver = Ui.inside(mx, my, left + w - 20, y + 7, 14, 14);
                Ui.text(ctx, "x", left + w - 17, y + 9, delOver ? 0xFFFF6B6B : Ui.DIM, false);
                y += rowH;
            }
            ctx.disableScissor();
            drawScrollbar(ctx, left + w - 2, listTop, bottom - listTop, cfgScroll, maxC);
        }

        if (profileMsg != null) {
            if (System.currentTimeMillis() < profileMsgUntil) {
                Ui.text(ctx, profileMsg, left + 2, bottom - 10, 0xFF7CF59E, false);
            } else {
                profileMsg = null;
            }
        }
    }

    private void drawScrollbar(DrawContext ctx, int x, int y, int h, float scroll, float max) {
        if (max <= 0) return;
        int bar = Math.max(12, (int) (h * h / (h + max)));
        int by = y + (int) ((h - bar) * (scroll / max));
        Ui.rect(ctx, x, by, 2, bar, 0x558727F5);
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

        // sidebar nav
        int y = py + HEADER + 10 + 14;
        for (Tab t : FEATURES) {
            if (clickTab(mx, my, btn, t, y)) return true;
            y += 28;
        }
        y += 6 + 14;
        for (Tab t : SYSTEM) {
            if (clickTab(mx, my, btn, t, y)) return true;
            y += 28;
        }

        // wyszukiwarka
        int sw = 150, sh = 20, sx = bodyX + bodyW - sw, sy = py + 7;
        if (Ui.inside(mx, my, sx, sy, sw, sh)) {
            MinecraftClient.getInstance().setScreen(new SearchPromptScreen(this, search, this::applySearch));
            return true;
        }

        if (tab == Tab.HUD && !hudDetail) {
            if (editorBtn.click(mx, my, btn)) return true;
            if (clickModuleCards(mx, my, btn)) return true;
            return true;
        }
        if (tab == Tab.HUD && hudDetail) {
            if (backBtn.click(mx, my, btn)) return true;
        }

        if (tab == Tab.CONFIG) {
            if (clickConfig(mx, my, btn)) return true;
            return true;
        }

        int top = bodyY + 2, bottom = bodyY + bodyH, left = bodyX + 2, w = bodyW - 4;
        if (Ui.inside(mx, my, left, top, w, bottom - top)) {
            int wy = top - (int) setScroll;
            for (Widgets.Widget wd : widgets) {
                if (my >= wy && my < wy + wd.h && wd.click(mx, my, btn)) {
                    active = wd;
                    return true;
                }
                wy += wd.h + GAP;
            }
            return true;
        }
        return true;
    }

    private boolean clickTab(double mx, double my, int btn, Tab t, int y) {
        int tx = px + 8, tw = SIDE - 16, th = 24;
        if (Ui.inside(mx, my, tx, y, tw, th)) {
            if (tab != t) {
                tab = t;
                setScroll = 0;
                cardScroll = 0;
                cfgScroll = 0;
                hudDetail = false;
                cardsRevealNanos = System.nanoTime();
                if (tab == Tab.CONFIG) refreshProfiles();
                rebuild();
            }
            return true;
        }
        return false;
    }

    private boolean clickModuleCards(double mx, double my, int btn) {
        List<HudModule> list = filteredModules();
        int top = bodyY + 2, bottom = bodyY + bodyH - 28;
        int cols = bodyW > 420 ? 2 : 1;
        int gap = 8;
        int cardW = (bodyW - gap * (cols - 1) - 4) / cols;
        int cardH = 40;
        if (!Ui.inside(mx, my, bodyX, top, bodyW, bottom - top)) return false;
        for (int i = 0; i < list.size(); i++) {
            HudModule hm = list.get(i);
            int col = i % cols, row = i / cols;
            int cx = bodyX + 2 + col * (cardW + gap);
            int cy = top - (int) cardScroll + row * (cardH + gap);
            if (!Ui.inside(mx, my, cx, cy, cardW, cardH)) continue;
            int sw = 24, shh = 12, swx = cx + cardW - sw - 10, swy = cy + (cardH - shh) / 2;
            if (Ui.inside(mx, my, swx, swy, sw, shh)) {
                hm.s.enabled = !hm.s.enabled;
            } else {
                selected = hm;
                hudDetail = true;
                setScroll = 0;
                rebuild();
            }
            return true;
        }
        return true;
    }

    private boolean clickConfig(double mx, double my, int btn) {
        int top = bodyY + 2, bottom = bodyY + bodyH, left = bodyX + 2, w = bodyW - 4;
        int btnW = 120;
        if (newConfigBtn.click(mx, my, btn)) return true;
        int listTop = top + 26, rowH = 32;
        if (!Ui.inside(mx, my, left, listTop, w, bottom - listTop)) return true;
        int idx = (int) ((my - listTop + cfgScroll) / rowH);
        if (idx >= 0 && idx < profiles.size()) {
            String name = profiles.get(idx);
            int rowY = listTop - (int) cfgScroll + idx * rowH;
            if (Ui.inside(mx, my, left + w - 20, rowY + 7, 14, 14)) {
                VoidConfig.deleteProfile(name);
                refreshProfiles();
            } else if (VoidConfig.loadProfile(name)) {
                profileMsg = "Wczytano: " + name;
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
        if (tab == Tab.HUD && !hudDetail && Ui.inside(mx, my, bodyX, bodyY, bodyW, bodyH)) {
            List<HudModule> list = filteredModules();
            int cols = bodyW > 420 ? 2 : 1;
            int bottom = bodyY + bodyH - 28;
            float max = Math.max(0, ((list.size() + cols - 1) / cols) * (40 + 8) - (bottom - (bodyY + 2)));
            cardScroll = Ui.clamp(cardScroll - (float) vAmt * 24f, 0, max);
            return true;
        }
        if (tab == Tab.CONFIG && Ui.inside(mx, my, bodyX, bodyY, bodyW, bodyH)) {
            cfgScroll = Ui.clamp(cfgScroll - (float) vAmt * 24f, 0,
                    Math.max(0, profiles.size() * 32 - (bodyH - 26)));
            return true;
        }
        if (Ui.inside(mx, my, bodyX, bodyY, bodyW, bodyH)) {
            setScroll = Ui.clamp(setScroll - (float) vAmt * 20f, 0, Math.max(0, contentHeight() - bodyH));
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
