package com.viltrum.client;

import com.viltrum.data.PowerData;
import com.viltrum.entity.ViltrumiteEntity;
import com.viltrum.registry.ModAttachments;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Limb poses applied after vanilla setupAnim (called from HumanoidModelMixin): flight, charge, barrage, neck grab. */
public final class PoseHooks {
    private PoseHooks() {}

    public static void apply(HumanoidModel<?> m, LivingEntity e, float age) {
        // the one being held dangles
        if (ActionVisuals.HELD.containsKey(e.getId())) { dangling(m, age); return; }

        FlightVisuals.Track t = FlightVisuals.TRACKS.get(e.getId());
        if (e instanceof Player p) {
            PowerData d = p.getData(ModAttachments.POWER.get());
            if (!d.viltrumite) return;
            if (d.flying && t != null) { flight(m, t, age); return; }
            if (d.barraging) { barrage(m, age, d.grabbedId >= 0); return; }
            if (d.grabbedId >= 0) { hold(m, age); return; }
            if (d.charging) charge(m, age);
        } else if (e instanceof ViltrumiteEntity v) {
            if (v.isFlyingPose() && t != null) flight(m, t, age);
            else if (v.getGrabTarget() >= 0) hold(m, age);
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

    /** alternating straight punches: arms snap out and back, torso twists into each blow, braced stance. */
    private static void barrage(HumanoidModel<?> m, float age, boolean holding) {
        neutral(m);
        float ph = age * 1.9f;
        float r = Mth.sin(ph) * 0.5f + 0.5f;                 // right arm extension 0..1
        float l = Mth.sin(ph + Mth.PI) * 0.5f + 0.5f;        // left arm extension (opposite phase)
        float twist = (r - l) * 0.55f;

        m.body.xRot = 0.22f;
        m.body.yRot = twist;
        m.head.yRot = -twist * 0.6f;
        m.head.xRot = -0.12f;

        m.rightArm.xRot = Mth.lerp(r, -0.95f, -1.62f);
        m.rightArm.yRot = Mth.lerp(r, 0.55f, -0.12f) + twist * 0.3f;
        m.rightArm.zRot = Mth.lerp(r, 0.18f, 0f);
        m.leftArm.xRot = Mth.lerp(l, -0.95f, -1.62f);
        m.leftArm.yRot = Mth.lerp(l, -0.55f, 0.12f) + twist * 0.3f;
        m.leftArm.zRot = Mth.lerp(l, -0.18f, 0f);

        m.rightLeg.xRot = -0.38f; m.rightLeg.zRot = 0.1f; m.rightLeg.yRot = 0.1f;
        m.leftLeg.xRot = 0.46f; m.leftLeg.zRot = -0.1f; m.leftLeg.yRot = -0.1f;
        if (holding) { m.rightArm.xRot -= 0.1f; m.leftArm.xRot -= 0.1f; }
        m.hat.copyFrom(m.head);
    }

    /** both arms thrust forward at throat height. */
    private static void hold(HumanoidModel<?> m, float age) {
        neutral(m);
        float strain = Mth.sin(age * 0.9f) * 0.03f;
        m.body.xRot = 0.1f;
        m.rightArm.xRot = -1.52f + strain;
        m.rightArm.yRot = -0.14f;
        m.leftArm.xRot = -1.38f - strain;
        m.leftArm.yRot = 0.3f;
        m.head.xRot = Mth.clamp(m.head.xRot, -0.5f, 0.4f);
        m.hat.copyFrom(m.head);
    }

    /** limp: arms clawing at the hand around the throat, legs kicking. */
    private static void dangling(HumanoidModel<?> m, float age) {
        neutral(m);
        float kick = Mth.sin(age * 0.45f) * 0.22f;
        m.body.xRot = -0.08f;
        m.rightArm.xRot = -2.45f + Mth.sin(age * 0.8f) * 0.1f;
        m.rightArm.zRot = -0.32f;
        m.rightArm.yRot = 0.28f;
        m.leftArm.xRot = -2.45f - Mth.sin(age * 0.8f) * 0.1f;
        m.leftArm.zRot = 0.32f;
        m.leftArm.yRot = -0.28f;
        m.rightLeg.xRot = 0.15f + kick;
        m.leftLeg.xRot = 0.15f - kick;
        m.rightLeg.zRot = 0.05f; m.leftLeg.zRot = -0.05f;
        m.head.xRot = -0.55f;
        m.hat.copyFrom(m.head);
    }
}
