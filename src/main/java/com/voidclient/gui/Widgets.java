package com.voidclient.gui;

import com.voidclient.VoidConfig.ModuleSettings;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Widgets {
    private Widgets() {}

    public abstract static class Widget {
        public int h = 18;
        protected int lx, ly, lw;

        public final void draw(DrawContext c, int x, int y, int w, int mx, int my) {
            lx = x;
            ly = y;
            lw = w;
            render(c, x, y, w, mx, my);
        }

        protected abstract void render(DrawContext c, int x, int y, int w, int mx, int my);

        public boolean click(double mx, double my, int btn) { return false; }

        public void drag(double mx, double my) {}

        public void release() {}
    }

    public static class Section extends Widget {
        private final String name;

        public Section(String name) {
            this.name = name;
            this.h = 16;
        }

        @Override
        protected void render(DrawContext c, int x, int y, int w, int mx, int my) {
            String n = name.toUpperCase(Locale.ROOT);
            Ui.text(c, n, x + 2, y + 6, Ui.mulA(Ui.ACCENT, 0.85f), false);
        }
    }

    public static class Toggle extends Widget {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private final Anim knob;
        private final Anim hov = new Anim(0);

        public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            this.label = label;
            this.get = get;
            this.set = set;
            this.knob = new Anim(get.getAsBoolean() ? 1f : 0f);
            this.h = 18;
        }

        @Override
        protected void render(DrawContext c, int x, int y, int w, int mx, int my) {
            float hv = hov.to(Ui.inside(mx, my, x, y, w, h) ? 1f : 0f, 12f);
            Ui.round(c, x, y, w, h, Ui.lerp(0x00FFFFFF, 0x0AFFFFFF, hv));
            Ui.text(c, label, x + 4, y + 5, Ui.TEXT, false);
            float t = knob.to(get.getAsBoolean() ? 1f : 0f, 14f);
            int sw = 26, sh = 11;
            int sx = x + w - sw - 4, sy = y + 4;
            int trackCol = Ui.lerp(Ui.mulA(Ui.TRACK, 0.8f), Ui.mulA(Ui.ACCENT, 0.55f), t);
            Ui.round(c, sx, sy, sw, sh, trackCol);
            int kx = sx + 2 + (int) (t * (sw - sh));
            Ui.round(c, kx, sy + 2, sh - 4, sh - 4, 0xFFFDFCFF);
        }

        @Override
        public boolean click(double mx, double my, int btn) {
            if (btn == 0 && Ui.inside(mx, my, lx, ly, lw, h)) {
                set.accept(!get.getAsBoolean());
                return true;
            }
            return false;
        }
    }

    public static class Slider extends Widget {
        private final String label;
        private final float min, max;
        private final Supplier<Float> get;
        private final Consumer<Float> set;
        private final Function<Float, String> fmt;
        private final Anim disp;
        private boolean dragging;

        public Slider(String label, float min, float max, Supplier<Float> get, Consumer<Float> set,
                      Function<Float, String> fmt) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
            this.fmt = fmt;
            this.h = 24;
            this.disp = new Anim(norm(get.get()));
        }

        private float norm(float v) {
            return Ui.clamp((v - min) / (max - min), 0f, 1f);
        }

        @Override
        protected void render(DrawContext c, int x, int y, int w, int mx, int my) {
            Ui.text(c, label, x + 4, y + 3, Ui.TEXT, false);
            String val = fmt.apply(get.get());
            Ui.text(c, val, x + w - 4 - Ui.tr().getWidth(val), y + 3, Ui.DIM, false);
            int tx = x + 4, tw = w - 8, ty = y + 16;
            float t = disp.to(norm(get.get()), 20f);
            Ui.round(c, tx, ty, tw, 3, Ui.mulA(Ui.TRACK, 0.75f));
            int fw = (int) (tw * t);
            if (fw > 1) Ui.round(c, tx, ty, fw, 3, Ui.mulA(Ui.ACCENT, 0.85f));
            int hx = tx + fw;
            Ui.round(c, hx - 4, ty - 3, 8, 9, 0xFFFDFCFF);
        }

        private void update(double mx) {
            float t = Ui.clamp((float) ((mx - (lx + 4)) / (double) (lw - 8)), 0f, 1f);
            set.accept(min + t * (max - min));
        }

        @Override
        public boolean click(double mx, double my, int btn) {
            if (btn == 0 && Ui.inside(mx, my, lx, ly, lw, h)) {
                dragging = true;
                update(mx);
                return true;
            }
            return false;
        }

        @Override
        public void drag(double mx, double my) {
            if (dragging) update(mx);
        }

        @Override
        public void release() {
            dragging = false;
        }
    }

    public static class Button extends Widget {
        private final String label;
        private final boolean primary;
        private final Runnable run;
        private final Anim hov = new Anim(0);

        public Button(String label, boolean primary, Runnable run) {
            this.label = label;
            this.primary = primary;
            this.run = run;
            this.h = 20;
        }

        @Override
        protected void render(DrawContext c, int x, int y, int w, int mx, int my) {
            float hv = hov.to(Ui.inside(mx, my, x, y, w, h) ? 1f : 0f, 12f);
            if (primary) {
                Ui.shadow(c, x, y, w, h, 3, 0.16f + hv * 0.06f);
                Ui.round(c, x, y, w, h, Ui.lerp(Ui.mulA(Ui.ACCENT, 0.88f), Ui.ACCENT, hv));
                Ui.textC(c, label, x + w / 2, y + (h - 8) / 2, 0xFF1A1424, false);
            } else {
                Ui.round(c, x, y, w, h, Ui.lerp(0x14FFFFFF, 0x1EFFFFFF, hv));
                Ui.textC(c, label, x + w / 2, y + (h - 8) / 2, Ui.lerp(Ui.DIM, Ui.TEXT, hv), false);
            }
        }

        @Override
        public boolean click(double mx, double my, int btn) {
            if (btn == 0 && Ui.inside(mx, my, lx, ly, lw, h)) {
                run.run();
                return true;
            }
            return false;
        }
    }

    public static class ColorPicker extends Widget {
        private static final int[] PRESETS = {
                0xFFFFFF, 0xF87171, 0xFB923C, 0xFACC15, 0x4ADE80,
                0x22D3EE, 0x60A5FA, 0x8727F5, 0xF472B6, 0x9CA3AF
        };
        private final ModuleSettings s;
        private final Slider r, g, b;
        private Slider activeSlider;

        public ColorPicker(ModuleSettings s) {
            this.s = s;
            this.h = 20 + 3 * 24;
            this.r = new Slider("Red", 0f, 255f, () -> (float) ((s.color >> 16) & 255),
                    v -> s.color = (s.color & 0x00FFFF) | (Math.round(v) << 16),
                    v -> String.valueOf(Math.round(v)));
            this.g = new Slider("Green", 0f, 255f, () -> (float) ((s.color >> 8) & 255),
                    v -> s.color = (s.color & 0xFF00FF) | (Math.round(v) << 8),
                    v -> String.valueOf(Math.round(v)));
            this.b = new Slider("Blue", 0f, 255f, () -> (float) (s.color & 255),
                    v -> s.color = (s.color & 0xFFFF00) | Math.round(v),
                    v -> String.valueOf(Math.round(v)));
        }

        @Override
        protected void render(DrawContext c, int x, int y, int w, int mx, int my) {
            for (int i = 0; i < PRESETS.length; i++) {
                int sx = x + 6 + i * 18, sy = y + 2;
                Ui.round(c, sx, sy, 14, 14, 0xFF000000 | PRESETS[i]);
                boolean sel = (s.color & 0xFFFFFF) == PRESETS[i];
                if (sel) {
                    Ui.roundStroke(c, sx - 2, sy - 2, 18, 18, 2, 0xFFFFFFFF);
                } else if (Ui.inside(mx, my, sx, sy, 14, 14)) {
                    Ui.roundStroke(c, sx - 2, sy - 2, 18, 18, 1, 0x99FFFFFF);
                }
            }
            r.draw(c, x, y + 20, w, mx, my);
            g.draw(c, x, y + 44, w, mx, my);
            b.draw(c, x, y + 68, w, mx, my);
        }

        @Override
        public boolean click(double mx, double my, int btn) {
            if (btn != 0) return false;
            for (int i = 0; i < PRESETS.length; i++) {
                if (Ui.inside(mx, my, lx + 6 + i * 18, ly + 2, 14, 14)) {
                    s.color = PRESETS[i];
                    s.chroma = false;
                    return true;
                }
            }
            Slider[] all = {r, g, b};
            for (Slider sl : all) {
                if (sl.click(mx, my, btn)) {
                    activeSlider = sl;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void drag(double mx, double my) {
            if (activeSlider != null) activeSlider.drag(mx, my);
        }

        @Override
        public void release() {
            if (activeSlider != null) activeSlider.release();
            activeSlider = null;
        }
    }
}
