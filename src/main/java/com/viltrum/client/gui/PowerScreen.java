package com.viltrum.client.gui;

import com.viltrum.client.ModKeys;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.network.SettingsPayload;
import com.viltrum.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The power menu: output limiter ("hold back"), flight speed limiter, toggles and a live preview of what the numbers mean. */
public class PowerScreen extends Screen {
    private static final int W = 400, H = 296;
    private static final int BG = 0xE80B0F17, PANEL = 0xFF131A26, EDGE = 0xFF263246;
    private static final int CYAN = 0xFF35D6FF, GOLD = 0xFFF7C61C, TEXT = 0xFFEAF2FF, DIM = 0xFF8A97AD;

    private PowerData d;
    private float open = 0f, openO = 0f;
    private int dragging = -1;       // 0 = output, 1 = flight
    private int sendTimer = 0;
    private boolean dirty = false;
    private float shownStrength, shownFlight;

    // layout (computed in init)
    private int px, py;
    private int sx, sw;              // slider x / width
    private int s0y, s1y;            // slider y
    private int[][] chips = new int[4][4];
    private final float[] presetVals = {0.10f, 0.35f, 0.70f, 1.00f};
    private final String[] presetKeys = {"gui.viltrum.preset.gentle", "gui.viltrum.preset.careful", "gui.viltrum.preset.heroic", "gui.viltrum.preset.full"};
    private int[][] toggles = new int[3][4];

    public PowerScreen() {
        super(Component.translatable("gui.viltrum.title"));
    }

    @Override
    protected void init() {
        d = Minecraft.getInstance().player.getData(ModAttachments.POWER.get());
        shownStrength = d.strength;
        shownFlight = d.flightLimit;
        px = (width - W) / 2;
        py = (height - H) / 2;
        sx = px + 148;
        sw = W - 148 - 20;
        s0y = py + 74;
        s1y = py + 140;
        int gap = 6, cw = (sw - 3 * gap) / 4;
        for (int i = 0; i < 4; i++) chips[i] = new int[]{sx + i * (cw + gap) + (i > 0 ? 0 : 0), s0y + 20, cw, 16};
        int tw = (sw - 2 * 6) / 3;
        for (int i = 0; i < 3; i++) toggles[i] = new int[]{sx + i * (tw + 6), s1y + 36, tw, 22};
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        openO = open;
        open = Math.min(1f, open + 0.2f);
        if (dirty && ++sendTimer >= 5) { send(); sendTimer = 0; }
        shownStrength = Mth.lerp(0.35f, shownStrength, d.strength);
        shownFlight = Mth.lerp(0.35f, shownFlight, d.flightLimit);
    }

    private void send() {
        PacketDistributor.sendToServer(new SettingsPayload(d.strength, d.flightLimit, d.allowDestruction, d.sonicFx, d.blood));
        dirty = false;
    }

    @Override
    public void onClose() {
        if (dirty) send();
        super.onClose();
    }

    // ------------------------------------------------------------------ drawing helpers
    private static int a(float alpha, int rgb) { return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF); }

    private static void rr(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        g.fill(x1 + r, y1, x2 - r, y2, color);
        for (int i = 0; i < r; i++) {
            int inset = r - (int) Math.round(Math.sqrt(r * r - (double) (r - i) * (r - i)));
            g.fill(x1 + inset, y1 + i, x1 + r, y1 + i + 1, color);
            g.fill(x2 - r, y1 + i, x2 - inset, y1 + i + 1, color);
            g.fill(x1 + inset, y2 - i - 1, x1 + r, y2 - i, color);
            g.fill(x2 - r, y2 - i - 1, x2 - inset, y2 - i, color);
        }
        g.fill(x1, y1 + r, x1 + r, y2 - r, color);
        g.fill(x2 - r, y1 + r, x2, y2 - r, color);
    }

    private void text(GuiGraphics g, String s, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    private void textC(GuiGraphics g, String s, int cx, int y, int color, float scale) {
        text(g, s, cx - (int) (font.width(s) * scale / 2f), y, color, scale);
    }

    private static int lerpRgb(int c1, int c2, float t) {
        int r = (int) Mth.lerp(t, (c1 >> 16) & 255, (c2 >> 16) & 255);
        int gg = (int) Mth.lerp(t, (c1 >> 8) & 255, (c2 >> 8) & 255);
        int b = (int) Mth.lerp(t, c1 & 255, c2 & 255);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int heat(float t) {
        return t < 0.5f ? lerpRgb(0x35E08A, 0xF7C61C, t * 2f) : lerpRgb(0xF7C61C, 0xFF4D3D, (t - 0.5f) * 2f);
    }

    private boolean in(double mx, double my, int[] r) { return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]; }

    // ------------------------------------------------------------------ render
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        float o = Mth.lerp(pt, openO, open);
        float e = 1f - (1f - o) * (1f - o);
        float al = e;

        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f + (1f - e) * 14f, 0);
        float sc = 0.94f + 0.06f * e;
        g.pose().scale(sc, sc, 1f);
        g.pose().translate(-width / 2f, -height / 2f, 0);

        // glow + panel
        for (int i = 6; i >= 1; i--) rr(g, px - i * 2, py - i * 2, px + W + i * 2, py + H + i * 2, 8 + i, a(0.035f * al, 0x35D6FF));
        rr(g, px - 1, py - 1, px + W + 1, py + H + 1, 8, a(al, EDGE));
        rr(g, px, py, px + W, py + H, 8, a(al * 0.96f, 0x0B0F17));
        g.fillGradient(px + 2, py + 2, px + W - 2, py + 52, a(0.5f * al, 0x1B3A5C), a(0f, 0x1B3A5C));
        g.fill(px + 14, py + 40, px + 14 + 44, py + 42, a(al, 0xF7C61C));
        g.fill(px + 14 + 44, py + 40, px + W - 14, py + 41, a(0.35f * al, 0x35D6FF));

        // header
        text(g, Component.translatable("gui.viltrum.title").getString(), px + 14, py + 14, a(al, TEXT), 1.5f);
        String lv = "LV " + d.level;
        int lw = font.width(lv) + 14;
        rr(g, px + W - 14 - lw, py + 12, px + W - 14, py + 28, 5, a(al, 0xF7C61C));
        g.drawString(font, lv, px + W - 14 - lw + 7, py + 16, 0xFF1A1200, false);

        xpRing(g, px + 14 + 62, py + 124, 44, al);
        slider(g, Component.translatable("gui.viltrum.output").getString(), Component.translatable("gui.viltrum.output.hint").getString(), s0y, shownStrength, mx, my, 0, al);
        for (int i = 0; i < 4; i++) {
            int[] r = chips[i];
            boolean sel = Math.abs(d.strength - presetVals[i]) < 0.015f;
            boolean hov = in(mx, my, r);
            rr(g, r[0], r[1], r[0] + r[2], r[1] + r[3], 4, a(al, sel ? 0x35D6FF : hov ? 0x2A3A52 : 0x1A2434));
            textC(g, Component.translatable(presetKeys[i]).getString(), r[0] + r[2] / 2, r[1] + 4, a(al, sel ? 0x06121C : TEXT), 0.85f);
        }
        slider(g, Component.translatable("gui.viltrum.flight").getString(), Component.translatable("gui.viltrum.flight.hint").getString(), s1y, shownFlight, mx, my, 1, al);
        toggle(g, 0, "gui.viltrum.t.destroy", d.allowDestruction, mx, my, al);
        toggle(g, 1, "gui.viltrum.t.sonic", d.sonicFx, mx, my, al);
        toggle(g, 2, "gui.viltrum.t.blood", d.blood, mx, my, al);
        unlocks(g, al);
        stats(g, al);

        text(g, Component.translatable("gui.viltrum.keys", ModKeys.FLIGHT.getTranslatedKeyMessage().getString(),
                ModKeys.STRIKE.getTranslatedKeyMessage().getString(), ModKeys.BARRAGE.getTranslatedKeyMessage().getString()).getString(),
                px + 14, py + H - 14, a(al * 0.8f, DIM), 0.8f);
        g.pose().popPose();
    }

    private void xpRing(GuiGraphics g, int cx, int cy, int r, float al) {
        float frac = (float) PowerMath.xpRequiredFraction(d);
        int segs = 120;
        for (int i = 0; i < segs; i++) {
            float f = i / (float) segs;
            double ang = -Math.PI / 2 + f * Math.PI * 2;
            boolean on = f <= frac;
            g.pose().pushPose();
            g.pose().translate(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r, 0);
            g.pose().mulPose(Axis.ZP.rotation((float) (ang + Math.PI / 2)));
            int col = on ? a(al, lerpRgb(0x35D6FF, 0xF7C61C, f)) : a(0.9f * al, 0x1D2838);
            g.fill(-1, -3, 2, 3, col);
            g.pose().popPose();
        }
        textC(g, String.valueOf(d.level), cx, cy - 12, a(al, TEXT), 2.6f);
        String sub = d.level >= PowerMath.MAX_LEVEL ? "MAX" : (int) d.xp + " / " + (int) PowerMath.xpForNext(d.level);
        textC(g, sub, cx, cy + 14, a(al, DIM), 0.8f);
        textC(g, Component.translatable("gui.viltrum.level").getString(), cx, cy + 26, a(al, DIM), 0.8f);
    }

    private void slider(GuiGraphics g, String label, String hint, int y, float v, int mx, int my, int idx, float al) {
        text(g, label, sx, y - 20, a(al, TEXT), 1.0f);
        String pct = Math.round(v * 100) + "%";
        text(g, pct, sx + sw - (int) (font.width(pct) * 1.5f), y - 22, a(al, heat(idx == 0 ? v : v * 0.8f)), 1.5f);
        text(g, hint, sx, y - 9, a(al * 0.85f, DIM), 0.78f);
        int ty = y + 2;
        rr(g, sx, ty, sx + sw, ty + 8, 4, a(al, 0x1B2434));
        int fillW = (int) (sw * v);
        for (int i = 0; i < fillW; i += 2) {
            float f = i / (float) sw;
            g.fill(sx + i, ty + 1, Math.min(sx + i + 2, sx + fillW), ty + 7, a(al, heat(idx == 0 ? f : f * 0.8f)));
        }
        for (int k = 1; k < 4; k++) g.fill(sx + sw * k / 4, ty + 9, sx + sw * k / 4 + 1, ty + 12, a(0.35f * al, 0xFFFFFF));
        int hx = sx + fillW;
        boolean hot = dragging == idx || (mx >= sx - 6 && mx <= sx + sw + 6 && my >= ty - 6 && my <= ty + 14);
        int hr = hot ? 8 : 6;
        rr(g, hx - hr - 3, ty + 4 - hr - 3, hx + hr + 3, ty + 4 + hr + 3, hr + 3, a(0.18f * al, idx == 0 ? 0x35D6FF : 0xF7C61C));
        rr(g, hx - hr, ty + 4 - hr, hx + hr, ty + 4 + hr, hr, a(al, 0xF4F8FF));
        rr(g, hx - hr + 3, ty + 4 - hr + 3, hx + hr - 3, ty + 4 + hr - 3, Math.max(1, hr - 3), a(al, heat(idx == 0 ? v : v * 0.8f)));
    }

    private void toggle(GuiGraphics g, int i, String key, boolean on, int mx, int my, float al) {
        int[] r = toggles[i];
        boolean hov = in(mx, my, r);
        rr(g, r[0], r[1], r[0] + r[2], r[1] + r[3], 5, a(al, on ? 0x143A4A : hov ? 0x22304A : 0x161F2D));
        if (on) g.fill(r[0], r[1] + 4, r[0] + 2, r[1] + r[3] - 4, a(al, 0x35D6FF));
        textC(g, Component.translatable(key).getString(), r[0] + r[2] / 2 - 6, r[1] + 7, a(al, on ? TEXT : DIM), 0.82f);
        int kx = r[0] + r[2] - 18, ky = r[1] + 7;
        rr(g, kx, ky, kx + 12, ky + 8, 4, a(al, on ? 0x35D6FF : 0x2B374B));
        rr(g, kx + (on ? 6 : 1), ky + 1, kx + (on ? 11 : 6), ky + 7, 3, a(al, 0xF4F8FF));
    }

    private void unlocks(GuiGraphics g, float al) {
        String[] names = {"gui.viltrum.u.senses", "gui.viltrum.u.grab", "gui.viltrum.u.clap", "gui.viltrum.u.rage", "gui.viltrum.u.stomp"};
        int[] lvls = {PowerMath.UNLOCK_SENSES, PowerMath.UNLOCK_GRAB, PowerMath.UNLOCK_CLAP, PowerMath.UNLOCK_RAGE, PowerMath.UNLOCK_STOMP};
        net.minecraft.client.KeyMapping[] keys = {ModKeys.SENSES, ModKeys.GRAB, ModKeys.CLAP, ModKeys.RAGE, ModKeys.STOMP};
        int y = py + 202, x0 = px + 14, gap = 6, cw = (W - 28 - 4 * gap) / 5;
        for (int i = 0; i < 5; i++) {
            int x = x0 + i * (cw + gap);
            boolean ok = d.level >= lvls[i];
            int accent = i == 3 ? 0xFF4D3D : 0x35E08A;
            rr(g, x, y, x + cw, y + 26, 5, a(al, ok ? (i == 3 ? 0x3A1512 : 0x123826) : 0x1A1F2B));
            g.fill(x, y + 4, x + 2, y + 22, a(al, ok ? accent : 0x4A5568));
            text(g, Component.translatable(names[i]).getString(), x + 7, y + 4, a(al, ok ? TEXT : DIM), 0.75f);
            String tag = ok ? "[" + keys[i].getTranslatedKeyMessage().getString() + "]" : "LV " + lvls[i];
            text(g, tag, x + 7, y + 15, a(al, ok ? 0xFF7FE8FF : 0xFFF7C61C), 0.75f);
        }
    }

    private void stats(GuiGraphics g, float al) {
        int y = py + H - 62, x0 = px + 14, cw = (W - 28 - 3 * 8) / 4;
        float dmg = PowerMath.punchDamage(d) * PowerMath.suitDamageMult(Minecraft.getInstance().player);
        double kb = PowerMath.knockback(d) * 20.0;
        double cruise = PowerMath.cruiseSpeed(d) * 20.0, boost = PowerMath.boostSpeed(d) * 20.0;
        double hard = PowerMath.breakHardness(d);
        int crater = PowerMath.craterRadius(d, 3.0);
        String[] titles = {"gui.viltrum.s.damage", "gui.viltrum.s.launch", "gui.viltrum.s.flight", "gui.viltrum.s.destroy"};
        String[] values = {
                String.format("%.0f", dmg),
                String.format("%.0f m/s", kb),
                String.format("%.0f-%.0f", cruise, boost),
                hard <= 0 ? Component.translatable("gui.viltrum.off").getString() : "r" + crater};
        String[] subs = {
                String.format("%.1f \u2764", dmg / 2f),
                "",
                String.format("m/s  \u00b7  MACH %.1f", boost / 20.0 / PowerMath.MACH),
                hard <= 0 ? "" : (hard >= 50 ? Component.translatable("gui.viltrum.s.obsidian").getString() : Component.translatable("gui.viltrum.s.stone").getString())};
        for (int i = 0; i < 4; i++) {
            int x = x0 + i * (cw + 8);
            rr(g, x, y, x + cw, y + 42, 5, a(al, 0x151D2B));
            g.fill(x, y + 6, x + 2, y + 36, a(al, i == 0 ? 0xFF5A3D : i == 1 ? 0xF7C61C : i == 2 ? 0x35D6FF : 0x35E08A));
            text(g, Component.translatable(titles[i]).getString(), x + 8, y + 5, a(al, DIM), 0.72f);
            text(g, values[i], x + 8, y + 16, a(al, TEXT), 1.25f);
            text(g, subs[i], x + 8, y + 32, a(al * 0.9f, DIM), 0.7f);
        }
    }

    // ------------------------------------------------------------------ input
    private void setFromMouse(double mx, int idx) {
        float v = Mth.clamp((float) ((mx - sx) / sw), 0f, 1f);
        v = Math.round(v * 100f) / 100f;
        if (idx == 0) d.strength = v; else d.flightLimit = v;
        dirty = true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (my >= s0y - 6 && my <= s0y + 16 && mx >= sx - 8 && mx <= sx + sw + 8) { dragging = 0; setFromMouse(mx, 0); click(1.4f); return true; }
            if (my >= s1y - 6 && my <= s1y + 16 && mx >= sx - 8 && mx <= sx + sw + 8) { dragging = 1; setFromMouse(mx, 1); click(1.4f); return true; }
            for (int i = 0; i < 4; i++) if (in(mx, my, chips[i])) { d.strength = presetVals[i]; dirty = true; click(1.1f + i * 0.1f); return true; }
            if (in(mx, my, toggles[0])) { d.allowDestruction = !d.allowDestruction; dirty = true; click(1.0f); return true; }
            if (in(mx, my, toggles[1])) { d.sonicFx = !d.sonicFx; dirty = true; click(1.0f); return true; }
            if (in(mx, my, toggles[2])) { d.blood = !d.blood; dirty = true; click(1.0f); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) { setFromMouse(mx, dragging); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging >= 0) { dragging = -1; send(); return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sxs, double sys) {
        int idx = (my >= s0y - 24 && my <= s0y + 40) ? 0 : (my >= s1y - 24 && my <= s1y + 24) ? 1 : -1;
        if (idx >= 0) {
            float step = (float) Math.signum(sys) * 0.05f;
            if (idx == 0) d.strength = Mth.clamp(Math.round((d.strength + step) * 100f) / 100f, 0f, 1f);
            else d.flightLimit = Mth.clamp(Math.round((d.flightLimit + step) * 100f) / 100f, 0f, 1f);
            dirty = true;
            return true;
        }
        return super.mouseScrolled(mx, my, sxs, sys);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ModKeys.MENU.matches(key, scan)) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_LEFT) { d.strength = Mth.clamp(d.strength - 0.01f, 0f, 1f); dirty = true; return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { d.strength = Mth.clamp(d.strength + 0.01f, 0f, 1f); dirty = true; return true; }
        return super.keyPressed(key, scan, mods);
    }

    private void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }
}
