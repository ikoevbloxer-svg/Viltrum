package com.viltrum.server;

import com.viltrum.Viltrum;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import com.viltrum.network.SyncPowerPayload;
import com.viltrum.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.viltrum.entity.ReanimenEntity;
import com.viltrum.entity.ViltrumiteEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Viltrum.MODID)
public class PowerEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            PowerData d = PowerSync.get(sp);
            d.flying = false; d.boosting = false; d.charging = false; d.barraging = false;
            if (!d.rolled) { // first time in this world: a chance that the blood simply awakens
                d.rolled = true;
                if (!d.viltrumite && sp.getRandom().nextDouble() < com.viltrum.Config.FIRST_JOIN_CHANCE.get()) {
                    d.viltrumite = true;
                    ServerPower.awakenFx(sp);
                }
            }
            PowerSync.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            PowerData d = PowerSync.get(sp);
            d.flying = false; d.boosting = false; d.charging = false; d.barraging = false;
            PowerSync.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) PowerSync.sync(sp);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new SyncPowerPayload(target.getId(), PowerSync.get(target)));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            ServerPower.tick(sp, sp.getData(ModAttachments.POWER.get()));
        }
    }

    /** victims with power take less damage; attackers with power hit harder when bare-handed. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim instanceof ServerPlayer vp) {
            PowerData vd = PowerSync.get(vp);
            // human shield: projectiles and attacks from the front hit the held creature instead
            if (vd.grabbedId >= 0 && !Combat.internal) {
                LivingEntity shield = GrabHandler.target(vp);
                var attacker = e.getSource().getEntity();
                if (shield != null && attacker != shield) {
                    boolean front = e.getSource().getDirectEntity() instanceof Projectile
                            || (attacker != null && vp.getLookAngle().dot(attacker.position().subtract(vp.position()).normalize()) > 0.3);
                    if (front) {
                        boolean prev = Combat.internal;
                        Combat.internal = true;
                        try { shield.invulnerableTime = 0; shield.hurt(e.getSource(), e.getAmount() * 0.8f); } finally { Combat.internal = prev; }
                        e.setCanceled(true);
                        return;
                    }
                }
            }
            if (vd.viltrumite) {
                if (e.getSource().is(DamageTypes.FALL) || e.getSource().is(DamageTypes.FLY_INTO_WALL) && !Combat.internal) {
                    e.setCanceled(true);
                    return;
                }
                float resist = (float) PowerMath.damageResist(vd);
                if (e.getSource().getEntity() instanceof ViltrumiteEntity) resist *= 0.6f; // their blows hurt even a Viltrumite
                if (e.getSource().is(DamageTypes.EXPLOSION) || e.getSource().is(DamageTypes.PLAYER_EXPLOSION)) resist = Math.min(0.97f, resist + 0.25f);
                e.setAmount(e.getAmount() * (1f - resist));
            }
        }

        if (Combat.internal) return;
        if (e.getSource().getEntity() instanceof ServerPlayer ap && e.getSource().is(DamageTypes.PLAYER_ATTACK)
                && e.getSource().getDirectEntity() == ap && ap.getMainHandItem().isEmpty()) {
            PowerData ad = PowerSync.get(ap);
            if (ad.viltrumite) {
                float scale = ap.getAttackStrengthScale(0.5f);
                float cool = 0.2f + 0.8f * scale * scale;
                float dmg = PowerMath.punchDamage(ad) * PowerMath.suitDamageMult(ap) * cool;
                if (victim instanceof Player && !com.viltrum.Config.PVP_SUPER_DAMAGE.get()) dmg = Math.min(dmg, 8f);
                e.setAmount(Math.max(e.getAmount(), dmg));
            }
        }
    }

    /** launches the victim of a bare-handed Viltrumite hit; also awards experience. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post e) {
        if (Combat.internal) return;
        if (e.getSource().getEntity() instanceof ServerPlayer ap && e.getSource().is(DamageTypes.PLAYER_ATTACK)
                && e.getSource().getDirectEntity() == ap && ap.getMainHandItem().isEmpty()) {
            PowerData ad = PowerSync.get(ap);
            if (!ad.viltrumite) return;
            LivingEntity v = e.getEntity();
            if (GrabHandler.isGrabbed(v)) return;
            float scale = ap.getAttackStrengthScale(0.5f);
            double kb = PowerMath.knockback(ad) * (0.35 + 0.65 * scale);
            Vec3 dir = v.position().subtract(ap.position()).multiply(1, 0, 1);
            if (dir.lengthSqr() < 1.0e-4) dir = ap.getLookAngle().multiply(1, 0, 1);
            Vec3 launch = dir.normalize().scale(kb).add(0, 0.12 + 0.1 * kb, 0);
            v.setDeltaMovement(v.getDeltaMovement().multiply(0.2, 0.2, 0.2).add(launch));
            v.hurtMarked = true;
            ServerPower.addXp(ap, ad, e.getNewDamage() * 0.35);
        }
    }

    @SubscribeEvent
    public static void onDeathRevive(LivingDeathEvent e) {
        if (e.getEntity() instanceof ReanimenEntity r && !Combat.noRevive && r.tryRevive()) e.setCanceled(true);
        if (e.getEntity() instanceof ServerPlayer sp) GrabHandler.forget(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) GrabHandler.forget(sp);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        GrabHandler.tickThrown(e.getServer());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer ap) {
            PowerData ad = PowerSync.get(ap);
            if (ad.viltrumite) ServerPower.addXp(ap, ad, e.getEntity().getMaxHealth() * 0.8);
        }
    }
}
