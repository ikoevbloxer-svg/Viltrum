package com.viltrum.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.viltrum.Viltrum;
import com.viltrum.data.PowerData;
import com.viltrum.data.PowerMath;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Viltrum.MODID)
public class ViltrumCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("viltrum").requires(s -> s.hasPermission(2))
                .then(Commands.literal("become")
                        .executes(c -> become(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(c -> become(c.getSource(), EntityArgument.getPlayer(c, "target")))))
                .then(Commands.literal("revoke")
                        .executes(c -> revoke(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(c -> revoke(c.getSource(), EntityArgument.getPlayer(c, "target")))))
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, PowerMath.MAX_LEVEL))
                                .executes(c -> level(c.getSource(), c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "level")))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(c -> level(c.getSource(), EntityArgument.getPlayer(c, "target"), IntegerArgumentType.getInteger(c, "level")))))));
    }

    private static int become(CommandSourceStack src, ServerPlayer p) {
        PowerData d = PowerSync.get(p);
        d.viltrumite = true;
        PowerSync.sync(p);
        src.sendSuccess(() -> Component.translatable("msg.viltrum.cmd.become", p.getDisplayName()), true);
        return 1;
    }

    private static int revoke(CommandSourceStack src, ServerPlayer p) {
        PowerData d = PowerSync.get(p);
        d.viltrumite = false;
        d.flying = false; d.boosting = false; d.charging = false; d.barraging = false;
        PowerSync.sync(p);
        src.sendSuccess(() -> Component.translatable("msg.viltrum.cmd.revoke", p.getDisplayName()), true);
        return 1;
    }

    private static int level(CommandSourceStack src, ServerPlayer p, int level) {
        PowerData d = PowerSync.get(p);
        d.viltrumite = true;
        d.level = level;
        d.xp = 0;
        PowerSync.sync(p);
        src.sendSuccess(() -> Component.translatable("msg.viltrum.cmd.level", p.getDisplayName(), level), true);
        return 1;
    }
}
