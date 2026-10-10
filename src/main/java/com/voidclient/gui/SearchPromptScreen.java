package com.voidclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/** Prosty ekran z polem tekstowym - uzywany do wpisania frazy wyszukiwania modulow. */
public class SearchPromptScreen extends Screen {
    private final Screen parent;
    private final String initial;
    private final Consumer<String> onConfirm;
    private final String heading;
    private final String fieldHint;
    private TextFieldWidget field;
    private final Starfield stars = new Starfield();

    public SearchPromptScreen(Screen parent, String initial, Consumer<String> onConfirm) {
        this(parent, initial, onConfirm, "Szukaj modulu HUD", "Szukaj");
    }

    public SearchPromptScreen(Screen parent, String initial, Consumer<String> onConfirm, String heading, String fieldHint) {
        super(Text.literal("Szukaj"));
        this.parent = parent;
        this.initial = initial == null ? "" : initial;
        this.onConfirm = onConfirm;
        this.heading = heading;
        this.fieldHint = fieldHint;
    }

    @Override
    protected void init() {
        int w = 220, h = 20;
        int x = (width - w) / 2, y = height / 2 - 10;

        field = new TextFieldWidget(this.textRenderer, x, y, w, h, Text.literal(fieldHint));
        field.setMaxLength(32);
        field.setText(initial);
        addDrawableChild(field);
        setInitialFocus(field);

        addDrawableChild(ButtonWidget.builder(Text.literal("Szukaj"), b -> confirm())
                .dimensions(x, y + 28, w / 2 - 4, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wyczysc"), b -> {
            onConfirm.accept("");
            close();
        }).dimensions(x + w / 2 + 4, y + 28, w / 2 - 4, h).build());
    }

    private void confirm() {
        onConfirm.accept(field.getText());
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        stars.draw(ctx, width, height, 0.7f);
        Ui.textC(ctx, heading, width / 2, height / 2 - 34, Ui.TEXT, true);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
