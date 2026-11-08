package com.viltrum.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
    public static final String CATEGORY = "key.categories.viltrum";
    public static final KeyMapping MENU = new KeyMapping("key.viltrum.menu", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping FLIGHT = new KeyMapping("key.viltrum.flight", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping STRIKE = new KeyMapping("key.viltrum.strike", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping BARRAGE = new KeyMapping("key.viltrum.barrage", GLFW.GLFW_KEY_Z, CATEGORY);

    public static final KeyMapping GRAB = new KeyMapping("key.viltrum.grab", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping SENSES = new KeyMapping("key.viltrum.senses", GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping CLAP = new KeyMapping("key.viltrum.clap", GLFW.GLFW_KEY_H, CATEGORY);

    private ModKeys() {}
}
