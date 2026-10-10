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

        public void copyFrom(ModuleSettings o) {
            enabled = o.enabled;
            x = o.x;
            y = o.y;
            color = o.color;
            scale = o.scale;
            shadow = o.shadow;
            background = o.background;
            bgOpacity = o.bgOpacity;
            chroma = o.chroma;
            label = o.label;
            accent = o.accent;
        }
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

        public void copyFrom(ColorSettings o) {
            enabled = o.enabled;
            brightness = o.brightness;
            contrast = o.contrast;
            saturation = o.saturation;
            hue = o.hue;
            gamma = o.gamma;
        }
    }

    /** Ustawienia widoku dla jednej reki (bez rotacji - tylko skala i pozycja). */
    public static class HandSettings {
        public boolean enabled = false;
        public float scale = 1f;    // 0.2 .. 2
        public float offsetX = 0f;  // lewo/prawo, -2 .. 2
        public float offsetY = 0f;  // gora/dol, -2 .. 2
        public float offsetZ = 0f;  // blisko/daleko, -2 .. 2

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

        public void copyFrom(HandSettings o) {
            enabled = o.enabled;
            scale = o.scale;
            offsetX = o.offsetX;
            offsetY = o.offsetY;
            offsetZ = o.offsetZ;
        }
    }

    public static class ViewModelSettings {
        public HandSettings main = new HandSettings();
        public HandSettings off = new HandSettings();

        public void copyFrom(ViewModelSettings o) {
            main.copyFrom(o.main);
            off.copyFrom(o.off);
        }
    }

    public static class WorldSettings {
        public boolean fullbright = false;
        public boolean noFog = false;

        public void copyFrom(WorldSettings o) {
            fullbright = o.fullbright;
            noFog = o.noFog;
        }
    }

    public static class ThemeSettings {
        public float hue = 0f;            // -1 .. 1, przesuniecie barwy akcentu
        public boolean animatedBg = true; // animowane gwiazdy w tle menu

        public void reset() {
            hue = 0f;
            animatedBg = true;
        }

        public void copyFrom(ThemeSettings o) {
            hue = o.hue;
            animatedBg = o.animatedBg;
        }
    }

    public static class Data {
        public Map<String, ModuleSettings> modules = new LinkedHashMap<>();
        public ColorSettings color = new ColorSettings();
        public ViewModelSettings viewModel = new ViewModelSettings();
        public WorldSettings world = new WorldSettings();
        public ThemeSettings theme = new ThemeSettings();
    }

    public static Data data = new Data();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("voidclient");
    private static final Path PATH = DIR.resolve("voidclient.json");

    private VoidConfig() {}

    public static Path profilesDir() {
        return DIR.resolve("profiles");
    }

    public static void load() {
        try {
            Files.createDirectories(DIR);
        } catch (IOException ignored) {
        }
        if (!Files.exists(PATH)) return;
        try {
            Data d = GSON.fromJson(Files.readString(PATH), Data.class);
            if (d == null) return;
            fillDefaults(d);
            data = d;
        } catch (Exception e) {
            System.err.println("[VoidClient] Nie udalo sie wczytac configu: " + e);
        }
    }

    private static void fillDefaults(Data d) {
        if (d.modules == null) d.modules = new LinkedHashMap<>();
        if (d.color == null) d.color = new ColorSettings();
        if (d.viewModel == null) d.viewModel = new ViewModelSettings();
        if (d.viewModel.main == null) d.viewModel.main = new HandSettings();
        if (d.viewModel.off == null) d.viewModel.off = new HandSettings();
        if (d.world == null) d.world = new WorldSettings();
        if (d.theme == null) d.theme = new ThemeSettings();
    }

    public static void save() {
        try {
            Files.createDirectories(DIR);
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

    /** Zapisuje biezacy stan jako nazwany profil na dysku. */
    public static boolean saveProfile(String name) {
        try {
            Files.createDirectories(profilesDir());
            Files.writeString(profilesDir().resolve(safeName(name) + ".json"), GSON.toJson(data));
            return true;
        } catch (IOException e) {
            System.err.println("[VoidClient] Nie udalo sie zapisac profilu: " + e);
            return false;
        }
    }

    /** Wczytuje profil i scala go W MIEJSCE istniejacych obiektow (zeby moduly HUD nadal na nie wskazywaly). */
    public static boolean loadProfile(String name) {
        try {
            Path p = profilesDir().resolve(safeName(name) + ".json");
            if (!Files.exists(p)) return false;
            Data incoming = GSON.fromJson(Files.readString(p), Data.class);
            if (incoming == null) return false;
            fillDefaults(incoming);
            for (Map.Entry<String, ModuleSettings> e : incoming.modules.entrySet()) {
                ModuleSettings cur = data.modules.get(e.getKey());
                if (cur == null) {
                    data.modules.put(e.getKey(), e.getValue());
                } else {
                    cur.copyFrom(e.getValue());
                }
            }
            data.color.copyFrom(incoming.color);
            data.viewModel.copyFrom(incoming.viewModel);
            data.world.copyFrom(incoming.world);
            data.theme.copyFrom(incoming.theme);
            save();
            return true;
        } catch (Exception e) {
            System.err.println("[VoidClient] Nie udalo sie wczytac profilu: " + e);
            return false;
        }
    }

    public static boolean deleteProfile(String name) {
        try {
            return Files.deleteIfExists(profilesDir().resolve(safeName(name) + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    public static java.util.List<String> listProfiles() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            if (!Files.exists(profilesDir())) return out;
            try (var stream = Files.list(profilesDir())) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            String n = p.getFileName().toString();
                            out.add(n.substring(0, n.length() - 5));
                        });
            }
        } catch (IOException ignored) {
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static String safeName(String name) {
        String n = name.trim().replaceAll("[^a-zA-Z0-9 _-]", "").trim();
        if (n.isEmpty()) n = "config";
        if (n.length() > 48) n = n.substring(0, 48);
        return n;
    }
}
