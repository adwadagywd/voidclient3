package com.voidclient.stats;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Locale;

public final class Stats {
    private Stats() {}

    // ---------- CPS (polling klawiszy myszy co klatke) ----------
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean prevL, prevR;

    public static void frame(MinecraftClient mc) {
        if (mc == null || mc.getWindow() == null) return;
        long handle = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            if (l && !prevL) LEFT.addLast(now);
            if (r && !prevR) RIGHT.addLast(now);
        }
        prevL = l;
        prevR = r;
    }

    private static int count(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.pollFirst();
        return q.size();
    }

    public static int leftCps() { return count(LEFT); }

    public static int rightCps() { return count(RIGHT); }

    // ---------- TPS (szacowane z odstepow pakietow czasu swiata) ----------
    private static long lastPacket = 0;
    private static double tps = -1;

    public static void onTimePacket() {
        long now = System.nanoTime();
        if (lastPacket != 0) {
            double dt = (now - lastPacket) / 1.0e9;
            if (dt > 0.05 && dt < 30.0) {
                double inst = Math.min(20.0, 20.0 / dt);
                tps = tps < 0 ? inst : tps * 0.6 + inst * 0.4;
            }
        }
        lastPacket = now;
    }

    public static void reset() {
        lastPacket = 0;
        tps = -1;
    }

    public static String tpsText() {
        if (tps < 0 || (System.nanoTime() - lastPacket) / 1.0e9 > 10.0) return "--";
        return String.format(Locale.ROOT, "%.1f", tps);
    }

    // ---------- predkosc gracza ----------
    private static double lastX, lastZ;
    private static boolean hasLast;
    public static double speed;

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            hasLast = false;
            speed = 0;
            return;
        }
        double x = mc.player.getX(), z = mc.player.getZ();
        if (hasLast) {
            double v = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ)) * 20.0;
            speed = speed * 0.6 + v * 0.4;
        }
        lastX = x;
        lastZ = z;
        hasLast = true;
    }
}
