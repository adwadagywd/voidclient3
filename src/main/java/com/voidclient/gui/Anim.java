package com.voidclient.gui;

/** Plynne wygladzanie wartosci (animacje niezalezne od FPS). */
public final class Anim {
    public float v;
    private long last = -1;

    public Anim(float init) {
        this.v = init;
    }

    public float to(float target, float rate) {
        long n = System.nanoTime();
        float dt = last < 0 ? 0.016f : Math.min(0.05f, (n - last) / 1_000_000_000f);
        last = n;
        v += (target - v) * (1f - (float) Math.exp(-rate * dt));
        return v;
    }
}
