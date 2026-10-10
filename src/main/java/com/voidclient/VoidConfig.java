package com.voidclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class VoidConfig {

    public static class ModuleSettings {
        public boolean enabled = true;
        public float x = -1f, y = -1f;     // pozycja w pikselach GUI; -1 = domyslna
        public int color = 0xFFFFFF;       // RGB
        public float scale = 1f;
        public boolean shadow = true;
        public boolean background = true;
        public float bgOpacity = 0.55f;
        public boolean chroma = false;
        public boolean label = true;
        public boolean accent = true;
    }

    public static class ColorSettings {
        public boolean enabled = true;
        public float brightness = 0f;   // -1 .. 1
        public float contrast = 1f;     // 0 .. 2
        public float saturation = 1f;   // 0 .. 2
        public float hue = 0f;          // -180 .. 180
        public float gamma = 1f;        // 0.5 .. 2

        public void reset() {
            brightness = 0f;
            contrast = 1f;
            saturation = 1f;
            hue = 0f;
            gamma = 1f;
        }

        public boolean isNeutral() {
            return Math.abs(brightness) < 0.001f
                    && Math.abs(contrast - 1f) < 0.001f
                    && Math.abs(saturation - 1f) < 0.001f
                    && Math.abs(hue) < 0.01f
                    && Math.abs(gamma - 1f) < 0.001f;
        }
    }

    public static class ViewModelSettings {
        public boolean enabled = false;
        public float scale = 1f;        // 0.2 .. 2
        public float offsetX = 0f;      // lewo/prawo, -1 .. 1
        public float offsetY = 0f;      // gora/dol, -1 .. 1
        public float offsetZ = 0f;      // blisko/daleko, -1 .. 1

        public void reset() {
            scale = 1f;
            offsetX = 0f;
            offsetY = 0f;
            offsetZ = 0f;
        }

        public boolean isNeutral() {
            return Math.abs(scale - 1f) < 0.001f
                    && Math.abs(offsetX) < 0.001f
                    && Math.abs(offsetY) < 0.001f
                    && Math.abs(offsetZ) < 0.001f;
        }
    }

    public static class Data {
        public Map<String, ModuleSettings> modules = new LinkedHashMap<>();
        public ColorSettings color = new ColorSettings();
        public ViewModelSettings viewModel = new ViewModelSettings();
    }

    public static Data data = new Data();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("voidclient.json");

    private VoidConfig() {}

    public static void load() {
        if (!Files.exists(PATH)) return;
        try {
            Data d = GSON.fromJson(Files.readString(PATH), Data.class);
            if (d == null) return;
            if (d.modules == null) d.modules = new LinkedHashMap<>();
            if (d.color == null) d.color = new ColorSettings();
            if (d.viewModel == null) d.viewModel = new ViewModelSettings();
            data = d;
        } catch (Exception e) {
            System.err.println("[VoidClient] Nie udalo sie wczytac configu: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("[VoidClient] Nie udalo sie zapisac configu: " + e);
        }
    }

    public static ModuleSettings settingsFor(String id, Consumer<ModuleSettings> init) {
        ModuleSettings s = data.modules.get(id);
        if (s == null) {
            s = new ModuleSettings();
            if (init != null) init.accept(s);
            data.modules.put(id, s);
        }
        return s;
    }
}
