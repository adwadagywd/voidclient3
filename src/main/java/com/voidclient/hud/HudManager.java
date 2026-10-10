package com.voidclient.hud;

import com.voidclient.gui.HudEditorScreen;
import com.voidclient.gui.Ui;
import com.voidclient.stats.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudManager {
    public static final List<HudModule> MODULES = new ArrayList<>();

    private HudManager() {}

    public static void init() {
        if (!MODULES.isEmpty()) return;
        MODULES.add(new TextModule("watermark", "Watermark", "", () -> "VoidClient", d -> d.chroma = true));
        MODULES.add(new TextModule("fps", "FPS", "FPS",
                () -> String.valueOf(MinecraftClient.getInstance().getCurrentFps()), d -> {}));
        MODULES.add(new TextModule("ping", "Ping", "Ping", HudManager::ping, d -> {}));
        MODULES.add(new TextModule("tps", "TPS", "TPS", Stats::tpsText, d -> {}));
        MODULES.add(new TextModule("cps", "CPS", "CPS",
                () -> Stats.leftCps() + " | " + Stats.rightCps(), d -> {}));
        MODULES.add(new TextModule("coords", "Coordinates", "XYZ", HudManager::coords, d -> {}));
        MODULES.add(new TextModule("direction", "Direction", "Facing", HudManager::direction, d -> {}));
        MODULES.add(new TextModule("speed", "Speed", "Speed",
                () -> String.format(Locale.ROOT, "%.2f b/s", Stats.speed), d -> d.enabled = false));
        MODULES.add(new TextModule("memory", "Memory", "RAM", HudManager::memory, d -> d.enabled = false));
        MODULES.add(new TextModule("clock", "Clock", "",
                () -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), d -> d.enabled = false));
        MODULES.add(new KeystrokesModule());
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (mc.currentScreen instanceof HudEditorScreen) return;
        Ui.alpha = 1f;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (HudModule m : MODULES) {
            if (m.s.enabled) m.render(ctx, mc.textRenderer, sw, sh);
        }
    }

    private static String ping() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return "--";
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return (e == null ? 0 : e.getLatency()) + " ms";
    }

    private static String coords() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return "-";
        return p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
    }

    private static String direction() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return "-";
        String n = p.getHorizontalFacing().asString();
        n = Character.toUpperCase(n.charAt(0)) + n.substring(1);
        return String.format(Locale.ROOT, "%s (%.0f)", n, MathHelper.wrapDegrees(p.getYaw()));
    }

    private static String memory() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long max = rt.maxMemory() / 1048576L;
        return used + " / " + max + " MB";
    }
}
