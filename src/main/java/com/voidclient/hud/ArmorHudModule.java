package com.voidclient.hud;

import com.voidclient.VoidConfig;
import com.voidclient.gui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

/** Pokazuje 4 zalozone czesci pancerza jako ikony przedmiotow (z paskiem trwalosci jak w ekwipunku). */
public class ArmorHudModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int CELL = 18;

    public ArmorHudModule(String id, String name, Consumer<VoidConfig.ModuleSettings> defaults) {
        super(id, name, defaults);
    }

    @Override
    public int[] size(TextRenderer tr) {
        return new int[]{CELL * SLOTS.length, CELL};
    }

    @Override
    protected void draw(DrawContext c, TextRenderer tr, int w, int h) {
        var player = MinecraftClient.getInstance().player;
        for (int i = 0; i < SLOTS.length; i++) {
            int x = i * CELL;
            if (s.background) {
                Ui.round(c, x + 1, 1, CELL - 2, CELL - 2, (int) (s.bgOpacity * 255f) << 24 | 0x0A0614);
            }
            ItemStack stack = player == null ? ItemStack.EMPTY : player.getEquippedStack(SLOTS[i]);
            if (!stack.isEmpty()) {
                c.drawItem(stack, x + 1, 1);
            }
        }
    }
}
