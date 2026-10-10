package com.voidclient.gui;

import com.voidclient.VoidConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Prosty ekran z polem tekstowym do nazwania nowego configu - oparty w calosci o standardowe widgety gry. */
public class CreateConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget field;
    private final Starfield stars = new Starfield();

    public CreateConfigScreen(Screen parent) {
        super(Text.literal("Nowy config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 220, h = 20;
        int x = (width - w) / 2, y = height / 2 - 10;

        field = new TextFieldWidget(this.textRenderer, x, y, w, h, Text.literal("Nazwa configu"));
        field.setMaxLength(32);
        field.setPlaceholder(Text.literal("np. PvP, Survival..."));
        addDrawableChild(field);
        setInitialFocus(field);

        addDrawableChild(ButtonWidget.builder(Text.literal("Utworz"), b -> create())
                .dimensions(x, y + 28, w / 2 - 4, h).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                .dimensions(x + w / 2 + 4, y + 28, w / 2 - 4, h).build());
    }

    private void create() {
        String name = field.getText().trim();
        if (name.isEmpty()) return;
        VoidConfig.saveProfile(name);
        if (parent instanceof VoidMenuScreen menu) {
            menu.notifyProfileSaved();
        }
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
            create();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        stars.draw(ctx, width, height, 0.7f);
        Ui.textC(ctx, "Nazwij nowy config", width / 2, height / 2 - 34, Ui.TEXT, true);
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
