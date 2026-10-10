package com.voidclient.hud;

import com.voidclient.VoidConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextModule extends HudModule {
    private final String label;
    private final Supplier<String> value;

    public TextModule(String id, String name, String label, Supplier<String> value,
                      Consumer<VoidConfig.ModuleSettings> defaults) {
        super(id, name, defaults);
        this.label = label;
        this.value = value;
    }

    private String text() {
        String v = value.get();
        return (s.label && !label.isEmpty()) ? label + ": " + v : v;
    }

    @Override
    public int[] size(TextRenderer tr) {
        int bar = s.accent ? 3 : 0;
        return new int[]{tr.getWidth(text()) + 8 + bar, tr.fontHeight + 6};
    }

    @Override
    protected void draw(DrawContext c, TextRenderer tr, int w, int h) {
        drawBackground(c, w, h);
        drawString(c, tr, text(), 4 + (s.accent ? 3 : 0), 3);
    }
}
