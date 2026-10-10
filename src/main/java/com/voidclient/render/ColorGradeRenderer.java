package com.voidclient.render;

import com.voidclient.VoidConfig;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Fullscreen pass na surowym OpenGL (bez zależności od wewnętrznego systemu post-effectów MC),
 * dzięki czemu jest odporny na Sodium / Iris / Lithium itd. Stan GL jest zapisywany i przywracany.
 */
public final class ColorGradeRenderer {
    private static boolean initialized, failed;
    private static int program, vao, copyFbo, copyTex, texW, texH;
    private static int uTex, uBri, uCon, uSat, uHue, uGam;

    private static final String VERT = """
            #version 150
            out vec2 uv;
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                uv = p;
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String FRAG = """
            #version 150
            uniform sampler2D uTex;
            uniform float uBrightness;
            uniform float uContrast;
            uniform float uSaturation;
            uniform float uHue;
            uniform float uGamma;
            in vec2 uv;
            out vec4 fragColor;

            const mat3 RGB2YIQ = mat3(0.299, 0.596, 0.211,
                                      0.587, -0.274, -0.523,
                                      0.114, -0.322, 0.312);
            const mat3 YIQ2RGB = mat3(1.0, 1.0, 1.0,
                                      0.956, -0.272, -1.106,
                                      0.621, -0.647, 1.703);

            void main() {
                vec3 c = texture(uTex, uv).rgb;

                vec3 yiq = RGB2YIQ * c;
                float s = sin(uHue);
                float co = cos(uHue);
                yiq.yz = vec2(yiq.y * co - yiq.z * s, yiq.y * s + yiq.z * co);
                c = YIQ2RGB * yiq;

                float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
                c = mix(vec3(l), c, uSaturation);
                c = (c - 0.5) * uContrast + 0.5;
                c += uBrightness;
                c = pow(max(c, vec3(0.0)), vec3(1.0 / uGamma));

                fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
            }
            """;

    private ColorGradeRenderer() {}

    public static void apply() {
        if (failed || !VoidConfig.data.color.enabled || VoidConfig.data.color.isNeutral()) return;

        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        int w = main.textureWidth, h = main.textureHeight;
        if (w <= 0 || h <= 0) return;

        // --- zapis stanu GL ---
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] vp = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try {
            if (!initialized) init();
            ensureTexture(w, h);

            // kopia main framebuffera (nie można czytać i pisać do tej samej tekstury)
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copyFbo);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            // rysowanie z powrotem do main framebuffera
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.fbo);
            GL11.glViewport(0, 0, w, h);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            GL20.glUseProgram(program);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
            GL20.glUniform1i(uTex, 0);
            GL20.glUniform1f(uBri, VoidConfig.data.color.brightness);
            GL20.glUniform1f(uCon, VoidConfig.data.color.contrast);
            GL20.glUniform1f(uSat, VoidConfig.data.color.saturation);
            GL20.glUniform1f(uHue, (float) Math.toRadians(VoidConfig.data.color.hue));
            GL20.glUniform1f(uGam, Math.max(0.1f, VoidConfig.data.color.gamma));

            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } catch (Throwable t) {
            failed = true;
            System.err.println("[ColorGrade] Błąd renderowania, efekt wyłączony: " + t);
        } finally {
            // --- przywrócenie stanu GL ---
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL13.glActiveTexture(prevActive);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
            GL11.glViewport(vp[0], vp[1], vp[2], vp[3]);
            setCap(GL11.GL_BLEND, blend);
            setCap(GL11.GL_DEPTH_TEST, depth);
            setCap(GL11.GL_CULL_FACE, cull);
            setCap(GL11.GL_SCISSOR_TEST, scissor);
            GL20.glUseProgram(prevProgram);
            GL30.glBindVertexArray(prevVao);
        }
    }

    private static void setCap(int cap, boolean on) {
        if (on) GL11.glEnable(cap); else GL11.glDisable(cap);
    }

    private static void init() {
        int vs = compile(GL20.GL_VERTEX_SHADER, VERT);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAG);
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Link: " + GL20.glGetProgramInfoLog(program, 4096));
        }
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        uTex = GL20.glGetUniformLocation(program, "uTex");
        uBri = GL20.glGetUniformLocation(program, "uBrightness");
        uCon = GL20.glGetUniformLocation(program, "uContrast");
        uSat = GL20.glGetUniformLocation(program, "uSaturation");
        uHue = GL20.glGetUniformLocation(program, "uHue");
        uGam = GL20.glGetUniformLocation(program, "uGamma");

        vao = GL30.glGenVertexArrays();
        copyFbo = GL30.glGenFramebuffers();
        copyTex = GL11.glGenTextures();
        initialized = true;
    }

    private static void ensureTexture(int w, int h) {
        if (w == texW && h == texH) return;
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, copyTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12ClampToEdge());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12ClampToEdge());
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, copyFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, copyTex, 0);
        texW = w;
        texH = h;
    }

    private static int GL12ClampToEdge() {
        return org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
    }

    private static int compile(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader: " + GL20.glGetShaderInfoLog(s, 4096));
        }
        return s;
    }
}
