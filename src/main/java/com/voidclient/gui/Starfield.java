package com.voidclient.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Animowane kosmiczne tlo: mgławica, migoczace gwiazdy, spadajace gwiazdy. */
public final class Starfield {
    private static final int N = 130;
    private final float[] sx = new float[N], sy = new float[N], sp = new float[N], ph = new float[N];
    private final int[] sz = new int[N], col = new int[N];
    private final Random rnd = new Random(7);
    private long last = -1;
    private float time;
    private float shootT = -1f, shootX, shootY, nextShoot = 2.5f;

    public Starfield() {
        for (int i = 0; i < N; i++) {
            sx[i] = rnd.nextFloat();
            sy[i] = rnd.nextFloat();
            sp[i] = 0.004f + rnd.nextFloat() * 0.012f;
            ph[i] = rnd.nextFloat() * 6.28f;
            sz[i] = rnd.nextInt(5) == 0 ? 2 : 1;
            int k = rnd.nextInt(3);
            col[i] = k == 0 ? 0xFFFFFFFF : (k == 1 ? 0xFFC4B5FD : 0xFFA5F3FC);
        }
    }

    public void draw(DrawContext c, int w, int h, float strength) {
        long n = System.nanoTime();
        float dt = last < 0 ? 0.016f : Math.min(0.05f, (n - last) / 1_000_000_000f);
        last = n;
        time += dt;

        // tlo + mgławice
        c.fillGradient(0, 0, w, h, Ui.a(Ui.mulA(0xD0070312, strength)), Ui.a(Ui.mulA(0xE0140828, strength)));
        c.fillGradient(0, 0, w, (int) (h * 0.6f), Ui.a(Ui.mulA(0x468727F5, strength)), 0x00000000);
        c.fillGradient(0, (int) (h * 0.45f), w, h, 0x00000000, Ui.a(Ui.mulA(0x2E22D3EE, strength)));

        for (int i = 0; i < N; i++) {
            sy[i] += sp[i] * dt;
            sx[i] += sp[i] * 0.25f * dt;
            if (sy[i] > 1f) {
                sy[i] -= 1f;
                sx[i] = rnd.nextFloat();
            }
            if (sx[i] > 1f) sx[i] -= 1f;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * (1.2f + sp[i] * 60f) + ph[i]));
            Ui.rect(c, (int) (sx[i] * w), (int) (sy[i] * h), sz[i], sz[i], Ui.mulA(col[i], tw * strength));
        }

        // spadajaca gwiazda
        nextShoot -= dt;
        if (shootT < 0f && nextShoot <= 0f) {
            shootT = 0f;
            shootX = rnd.nextFloat() * 0.6f * w;
            shootY = rnd.nextFloat() * 0.4f * h;
        }
        if (shootT >= 0f) {
            shootT += dt / 0.9f;
            float hx = shootX + shootT * w * 0.4f;
            float hy = shootY + shootT * h * 0.28f;
            for (int k = 0; k < 14; k++) {
                float f = k / 14f;
                float al = (1f - f) * (1f - Math.min(1f, shootT)) * strength;
                Ui.rect(c, (int) (hx - f * 46f), (int) (hy - f * 32f), 2, 1, Ui.mulA(0xFFE0E7FF, al));
            }
            if (shootT >= 1f) {
                shootT = -1f;
                nextShoot = 4f + rnd.nextFloat() * 6f;
            }
        }
    }
}
