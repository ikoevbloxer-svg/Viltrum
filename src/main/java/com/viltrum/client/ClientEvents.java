package com.viltrum.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.viltrum.Viltrum;
import com.viltrum.data.PowerData;
import com.viltrum.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;

import java.util.ArrayDeque;

@EventBusSubscriber(modid = Viltrum.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static final ArrayDeque<Boolean> PUSHED = new ArrayDeque<>();
    private static float fovMul = 1.0f;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientPayloadHandlers.tickPending(mc);
        FlightController.clientTick(mc);
        FlightVisuals.tick(mc);
        ActionVisuals.tick(mc);
        FlightSounds.tick(mc);
        CameraShake.tick();

        PowerData d = mc.player.getData(ModAttachments.POWER.get());
        float sf = (float) Mth.clamp((FlightController.speed - 0.6) / 3.6, 0.0, 1.0);
        float target = d.viltrumite && d.flying ? 1.0f + sf * 0.42f + FlightController.boostAmt * 0.12f : 1.0f;
        fovMul = Mth.lerp(0.12f, fovMul, target);
        if (!d.flying) { FlightController.speed = Mth.lerp(0.3f, (float) FlightController.speed, 0f); }
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre e) {
        if (e.getEntity() instanceof LocalPlayer lp) FlightController.pre(lp);
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof LocalPlayer lp) FlightController.post(lp);
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent e) {
        if (e.getPlayer() == Minecraft.getInstance().player && Math.abs(fovMul - 1.0f) > 0.001f) {
            e.setNewFovModifier(e.getNewFovModifier() * fovMul);
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        CameraShake.apply(e);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getData(ModAttachments.POWER.get()).flying) {
            e.setRoll(e.getRoll() + FlightController.strafeSmooth * -4.0f * (float) Mth.clamp(FlightController.speed / 2.0, 0.2, 1.0));
        }
    }

    /** tilts the whole body into the flight direction (superman pose) and banks into turns. */
    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> e) {
        LivingEntity ent = e.getEntity();
        FlightVisuals.Track t = FlightVisuals.TRACKS.get(ent.getId());
        boolean apply = t != null && t.alpha > 0.5f;
        PUSHED.push(apply);
        if (!apply) return;
        float pt = e.getPartialTick();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        float alpha = Mth.lerp(pt, t.alphaO, t.alpha);
        float roll = Mth.lerp(pt, t.rollO, t.roll);
        float yaw = Mth.rotLerp(pt, ent.yBodyRotO, ent.yBodyRot);
        double rad = Math.toRadians(yaw);
        float ax = (float) Math.cos(rad), az = (float) Math.sin(rad);
        ps.translate(0.0, 0.9, 0.0);
        ps.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(alpha), ax, 0f, az));
        ps.mulPose(Axis.YP.rotationDegrees(roll));
        ps.translate(0.0, -0.9, 0.0);
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> e) {
        if (!PUSHED.isEmpty() && PUSHED.pop()) e.getPoseStack().popPose();
    }
}
