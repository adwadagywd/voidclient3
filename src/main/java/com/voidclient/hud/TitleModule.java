package com.voidclient.hud;

import com.voidclient.VoidConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/** Edytowalny dowolny napis na ekranie (np. tag gracza, motto) - tresc ustawiana w menu. */
public class TitleModule extends HudModule {

    public TitleModule(String id, String name, Consumer<VoidConfig.ModuleSettings> defaults) {
        super(id, name, defaults);
    }

    private String text() {
        String t = s.text == null ? "" : s.text.trim();
        return t.isEmpty() ? "Twoj tytul" : t;
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
