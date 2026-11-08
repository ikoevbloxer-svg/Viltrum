package com.viltrum.client;

import com.viltrum.data.PowerData;
import com.viltrum.network.FxPayload;
import com.viltrum.network.SyncPowerPayload;
import com.viltrum.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientPayloadHandlers {
    private static final Map<Integer, PowerData> PENDING = new HashMap<>();

    private ClientPayloadHandlers() {}

    public static void onSync(SyncPowerPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(p.entityId());
        if (e instanceof Player pl) apply(pl, p.data());
        else PENDING.put(p.entityId(), p.data());
    }

    /** applies packets that arrived before the entity existed on this client. */
    public static void tickPending(Minecraft mc) {
        if (PENDING.isEmpty() || mc.level == null) return;
        Iterator<Map.Entry<Integer, PowerData>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Entity e = mc.level.getEntity(en.getKey());
            if (e instanceof Player pl) { apply(pl, en.getValue()); it.remove(); }
        }
        if (PENDING.size() > 64) PENDING.clear();
    }

    private static void apply(Player pl, PowerData nd) {
        PowerData d = pl.getData(ModAttachments.POWER.get());
        Minecraft mc = Minecraft.getInstance();
        if (pl == mc.player) {
            // the local player owns flight/charge state; the server may only switch things off
            boolean keepFlying = d.flying, keepBoost = d.boosting, keepCharge = d.charging, keepBarrage = d.barraging;
            long now = pl.level().getGameTime();
            d.copyNetFrom(nd);
            if (nd.viltrumite) {
                d.flying = nd.flying ? true : (keepFlying && now - FlightController.lastToggle < 15);
                d.boosting = nd.boosting || (keepBoost && d.flying && now - FlightController.lastToggle < 15);
                d.charging = keepCharge && nd.charging;
                d.barraging = keepBarrage && nd.barraging;
            }
        } else {
            d.copyNetFrom(nd);
        }
    }

    public static void onFx(FxPayload p) {
        FxHandler.play(p);
    }
}
