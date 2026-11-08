package com.viltrum.client;

import com.viltrum.data.PowerData;
import com.viltrum.entity.ViltrumiteEntity;
import com.viltrum.registry.ModAttachments;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Limb poses applied after vanilla setupAnim (called from HumanoidModelMixin): superman flight, charge wind-up, barrage. */
public final class PoseHooks {
    private PoseHooks() {}

    public static void apply(HumanoidModel<?> m, LivingEntity e, float age) {
        FlightVisuals.Track t = FlightVisuals.TRACKS.get(e.getId());
        if (e instanceof Player p) {
            PowerData d = p.getData(ModAttachments.POWER.get());
            if (!d.viltrumite) return;
            if (d.flying && t != null) { flight(m, t, age); return; }
            if (d.barraging) { barrage(m, age); return; }
            if (d.charging) { charge(m, age); }
        } else if (e instanceof ViltrumiteEntity v && v.isFlyingPose() && t != null) {
            flight(m, t, age);
        }
    }

    private static void neutral(HumanoidModel<?> m) {
        m.body.xRot = 0; m.body.y = 0; m.head.y = 0;
        m.rightLeg.z = 0.1f; m.leftLeg.z = 0.1f; m.rightLeg.y = 12f; m.leftLeg.y = 12f;
        m.rightArm.y = 2f; m.leftArm.y = 2f; m.rightArm.z = 0f; m.leftArm.z = 0f;
    }

    private static void flight(HumanoidModel<?> m, FlightVisuals.Track t, float age) {
        neutral(m);
        float lean = Mth.clamp(t.alpha / 90f, 0f, 1f);
        float boost = (float) Mth.clamp((t.speed - 1.2) / 1.0, 0.0, 1.0);
        float sway = Mth.sin(age * 0.12f) * 0.04f;
        float kick = Mth.sin(age * 0.25f) * 0.05f;

        m.rightArm.xRot = Mth.lerp(lean, sway, -3.05f + sway * 0.5f);
        m.leftArm.xRot = Mth.lerp(lean, -sway, Mth.lerp(boost, 0.45f, -3.05f));
        m.rightArm.yRot = 0f; m.leftArm.yRot = 0f;
        m.rightArm.zRot = Mth.lerp(lean, 0.12f, -0.05f);
        m.leftArm.zRot = Mth.lerp(lean, -0.12f, 0.05f);

        m.rightLeg.xRot = Mth.lerp(lean, 0.05f, 0.12f + kick);
        m.leftLeg.xRot = Mth.lerp(lean, 0.05f, 0.28f - kick);
        m.rightLeg.yRot = 0f; m.leftLeg.yRot = 0f;
        m.rightLeg.zRot = 0.03f; m.leftLeg.zRot = -0.03f;

        m.head.xRot = Mth.lerp(lean, m.head.xRot * 0.3f, -1.25f);
        m.hat.copyFrom(m.head);
    }

    private static void charge(HumanoidModel<?> m, float age) {
        float tremble = Mth.sin(age * 1.7f) * 0.05f;
        m.body.yRot = 0.45f;
        m.rightArm.xRot = 1.05f + tremble;
        m.rightArm.yRot = 0.55f;
        m.rightArm.zRot = 0.35f;
        m.leftArm.xRot = -1.05f;
        m.leftArm.yRot = -0.4f;
        m.leftArm.zRot = -0.1f;
        m.head.yRot += 0.3f;
        m.hat.copyFrom(m.head);
    }

    private static void barrage(HumanoidModel<?> m, float age) {
        float s = Mth.sin(age * 1.9f);
        m.body.yRot = s * 0.18f;
        m.rightArm.xRot = -1.45f + s * 0.65f;
        m.leftArm.xRot = -1.45f - s * 0.65f;
        m.rightArm.yRot = -0.25f;
        m.leftArm.yRot = 0.25f;
        m.rightArm.zRot = 0f; m.leftArm.zRot = 0f;
    }
}
