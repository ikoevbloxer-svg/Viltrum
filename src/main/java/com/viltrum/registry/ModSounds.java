package com.viltrum.registry;

import com.viltrum.Viltrum;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Viltrum.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Viltrum.id(name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> PUNCH_HEAVY = reg("punch_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUNCH_SUPER = reg("punch_super");
    public static final DeferredHolder<SoundEvent, SoundEvent> PUNCH_BARRAGE = reg("punch_barrage");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_CRACK = reg("bone_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVEL_UP = reg("level_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASER = reg("laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLIGHT_START = reg("flight_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLIGHT_LOOP = reg("flight_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLIGHT_FAST = reg("flight_fast");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLIGHT_BOOST = reg("flight_boost");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONIC_BOOM = reg("sonic_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> LANDING = reg("landing");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLAM = reg("slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE = reg("charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUIT_EQUIP = reg("suit_equip");
}
