package com.viltrum.client.hud;

import com.mojang.math.Axis;
import com.viltrum.client.FlightController;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Speed lines, vignette, mach flash, flight gauge, charge ring and the level / output chip. */
public class PowerHud implements LayeredDraw.Layer {
    private static float flash = 0f;
    private static final int LINES = 54;

    public static void flash(float amount) { flash = Math.min(1f, Math.max(flash, amount)); }

    private static int argb(float a, int rgb) { return (Mth.clamp((int) (a * 255f), 0, 255) << 24) | (rgb & 0xFFFFFF); }

    @Override
    public void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        PowerData d = mc.player.getData(ModAttachments.POWER.get());
        if (!d.viltrumite) return;
        float pt = dt.getGameTimeDeltaPartialTick(true);
        int w = g.guiWidth(), h = g.guiHeight();
        float time = (mc.player.tickCount + pt);

        float sf = d.flying ? (float) Mth.clamp((FlightController.speed - 0.8) / 2.6, 0.0, 1.0) : 0f;
        if (d.rageAmt > 0.02f) rageOverlay(g, w, h, d.rageAmt, time);
        if (sf > 0.02f) {
            vignette(g, w, h, sf);
            speedLines(g, w, h, sf, time);
        }
        if (flash > 0.01f) {
            g.fill(0, 0, w, h, argb(flash * 0.65f, 0xFFFFFF));
            flash = Math.max(0f, flash - 0.06f * Math.max(0.3f, dt.getRealtimeDeltaTicks()));
        }
        if (FlightController.charging) chargeRing(g, w / 2, h / 2, FlightController.chargeFrac, time);
        if (d.flying) gauge(g, d, w, h);
        chip(g, mc, d, w, h);
        if (d.grabbedId >= 0) {
            g.drawCenteredString(mc.font, Component.translatable("gui.viltrum.hud.grab", com.viltrum.client.ModKeys.STRIKE.getTranslatedKeyMessage(), com.viltrum.client.ModKeys.KILL.getTranslatedKeyMessage(), com.viltrum.client.ModKeys.BARRAGE.getTranslatedKeyMessage(), com.viltrum.client.ModKeys.GRAB.getTranslatedKeyMessage()), w / 2, h - 80, 0xFFFFC060);
        }
        if (d.senses) g.drawString(mc.font, Component.translatable("gui.viltrum.hud.senses"), 6, h - 34, 0xFF7FE8FF, false);
    }

    private void rageOverlay(GuiGraphics g, int w, int h, float amt, float time) {
        float pulse = 0.75f + 0.25f * Mth.sin(time * 0.35f);
        int c = Math.min(w, h) / 3;
        int edge = argb(0.5f * amt * pulse, 0x8A0A06), clear = argb(0f, 0x8A0A06);
        g.fillGradient(0, 0, w, c, edge, clear);
        g.fillGradient(0, h - c, w, h, clear, edge);
        int strips = 16;
        for (int i = 0; i < strips; i++) {
            float k = 1f - i / (float) strips;
            int a = argb(0.4f * amt * pulse * k * k, 0x8A0A06);
            int sw = c / strips + 1;
            g.fill(i * sw, 0, (i + 1) * sw, h, a);
            g.fill(w - (i + 1) * sw, 0, w - i * sw, h, a);
        }
    }

    private void vignette(GuiGraphics g, int w, int h, float sf) {
        int c = Math.min(w, h) / 3;
        int top = argb(0.55f * sf, 0x0A1430), clear = argb(0f, 0x0A1430);
        g.fillGradient(0, 0, w, c, top, clear);
        g.fillGradient(0, h - c, w, h, clear, top);
        // left / right via thin vertical strips
        int strips = 18;
        for (int i = 0; i < strips; i++) {
            float k = 1f - i / (float) strips;
            int a = argb(0.4f * sf * k * k, 0x0A1430);
            int sw = c / strips + 1;
            g.fill(i * sw, 0, (i + 1) * sw, h, a);
            g.fill(w - (i + 1) * sw, 0, w - i * sw, h, a);
        }
    }

    private void speedLines(GuiGraphics g, int w, int h, float sf, float time) {
        float cx = w / 2f, cy = h / 2f;
        float m = Math.min(w, h);
        int n = (int) (LINES * (0.35f + 0.65f * sf));
        for (int i = 0; i < n; i++) {
            float r1 = hash(i, 1), r2 = hash(i, 2), r3 = hash(i, 3);
            float ang = r1 * (float) (Math.PI * 2);
            float ph = (time * (0.09f + 0.11f * r2 + 0.1f * sf) + r3) % 1f;
            float r0 = m * (0.16f + ph * ph * 0.62f);
            float len = m * (0.03f + 0.22f * sf) * (0.3f + ph * 1.3f);
            float a = sf * (1f - ph) * 0.85f * (0.4f + 0.6f * r2);
            float thick = 0.7f + 1.1f * r3 * sf;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(Axis.ZP.rotation(ang - (float) Math.PI / 2f));
            g.fillGradient(-(int) Math.ceil(thick), (int) r0, (int) Math.ceil(thick), (int) (r0 + len), argb(0f, 0xFFFFFF), argb(a, 0xDCEBFF));
            g.pose().popPose();
        }
    }

    private static float hash(int i, int k) {
        int x = i * 374761393 + k * 668265263;
        x = (x ^ (x >>> 13)) * 1274126177;
        x ^= (x >>> 16);
        return (x & 0xFFFF) / 65535f;
    }

    private void chargeRing(GuiGraphics g, int cx, int cy, float frac, float time) {
        int segs = 64, r = 18;
        for (int i = 0; i < segs; i++) {
            float f = i / (float) segs;
            double a = -Math.PI / 2 + f * Math.PI * 2;
            int x = cx + (int) Math.round(Math.cos(a) * r);
            int y = cy + (int) Math.round(Math.sin(a) * r);
            boolean on = f <= frac;
            int col = on ? (frac > 0.95f ? argb(1f, 0xFFE066) : argb(0.95f, 0x59E1FF)) : argb(0.25f, 0xFFFFFF);
            g.fill(x - 1, y - 1, x + 1, y + 1, col);
        }
        if (frac >= 1f) {
            float pulse = 0.5f + 0.5f * Mth.sin(time * 0.8f);
            g.fill(cx - 2, cy - 2, cx + 2, cy + 2, argb(0.4f + 0.5f * pulse, 0xFFE066));
        }
    }

    private void gauge(GuiGraphics g, PowerData d, int w, int h) {
        var font = Minecraft.getInstance().font;
        double speed = FlightController.speed;
        double max = Math.max(0.1, PowerMath.boostSpeed(d));
        float frac = (float) Mth.clamp(speed / max, 0.0, 1.0);
        int gw = 150, gh = 7;
        int x = (w - gw) / 2, y = h - 64;
        g.fill(x - 1, y - 1, x + gw + 1, y + gh + 1, argb(0.55f, 0x05080F));
        for (int i = 0; i < gw; i += 2) {
            float f = i / (float) gw;
            if (f > frac) break;
            int rgb = f < 0.5f ? lerpRgb(0x35D6FF, 0xFFFFFF, f * 2f) : lerpRgb(0xFFFFFF, 0xFFB43D, (f - 0.5f) * 2f);
            g.fill(x + i, y, x + i + 2, y + gh, argb(1f, rgb));
        }
        // sound-barrier mark
        int mx = x + (int) (Mth.clamp(PowerMath.MACH / max, 0.0, 1.0) * gw);
        g.fill(mx, y - 3, mx + 1, y + gh + 3, argb(0.9f, 0xFFE066));
        String txt = String.format("%d m/s   MACH %.1f", (int) Math.round(speed * 20.0), speed / PowerMath.MACH);
        g.drawCenteredString(font, Component.literal(txt), w / 2, y - 12, speed >= PowerMath.MACH ? 0xFFFFE066 : 0xFFE8F4FF);
    }

    private void chip(GuiGraphics g, Minecraft mc, PowerData d, int w, int h) {
        var font = mc.font;
        String s = "LV " + d.level + "   OUTPUT " + Math.round(PowerMath.eff(d) * 100) + "%";
        int tw = font.width(s);
        int x = 6, y = h - 22;
        g.fill(x, y, x + tw + 12, y + 14, argb(0.55f, 0x05080F));
        g.fill(x, y, x + 2, y + 14, argb(1f, PowerMath.eff(d) > 0.75 ? 0xFF5A3D : PowerMath.eff(d) > 0.4 ? 0xF7C61C : 0x35E08A));
        g.drawString(font, s, x + 7, y + 3, 0xFFE8F4FF, false);
        int bw = tw + 12;
        g.fill(x, y + 14, x + bw, y + 16, argb(0.5f, 0x000000));
        g.fill(x, y + 14, x + (int) (bw * PowerMath.xpRequiredFraction(d)), y + 16, argb(1f, 0x35D6FF));
    }

    private static int lerpRgb(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int gg = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (gg << 8) | bl;
    }
}
