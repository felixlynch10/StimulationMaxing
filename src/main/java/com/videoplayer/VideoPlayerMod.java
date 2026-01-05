package com.videoplayer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoPlayerMod implements ClientModInitializer {
    public static final String MOD_ID = "videoplayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openVideoPlayerKey;
    private static KeyBinding toggleControlsKey;
    private static VideoPlayerScreen currentScreen = null;
    private static VideoPlayerHud hudOverlay = null;
    private static VideoPlayer sharedVideoPlayer = null;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Video Player PiP Mod");

        // Initialize shared video player
        sharedVideoPlayer = new VideoPlayer();

        // Initialize HUD overlay with shared player
        hudOverlay = new VideoPlayerHud(sharedVideoPlayer);
        HudRenderCallback.EVENT.register(hudOverlay);

        // Register keybindings
        openVideoPlayerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.videoplayer.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.videoplayer"
        ));

        toggleControlsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.videoplayer.toggleControls",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.videoplayer"
        ));

        // Register tick event to check for keybind press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle mouse clicks on HUD overlay
            if (hudOverlay != null && hudOverlay.isVisible() && client.currentScreen != null) {
                // When inventory or any screen is open, check for mouse clicks on the overlay
                if (client.mouse.wasLeftButtonClicked()) {
                    double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
                    double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

                    if (hudOverlay.handleMouseClick(mouseX, mouseY, 0)) {
                        LOGGER.info("Clicked play/pause button on HUD overlay");
                    }
                }
            }

            // Handle open/close video player
            while (openVideoPlayerKey.wasPressed()) {
                if (client.currentScreen instanceof VideoPlayerScreen) {
                    // Sync position/size from screen to HUD before switching
                    VideoPlayerScreen screen = (VideoPlayerScreen) client.currentScreen;
                    hudOverlay.setPosition(screen.getVideoX(), screen.getVideoY());
                    hudOverlay.setSize(screen.getVideoWidth(), screen.getVideoHeight());

                    // Close screen, switch to HUD mode
                    client.setScreen(null);
                    currentScreen = null;
                    hudOverlay.setVisible(true);
                    LOGGER.info("Switched to HUD mode");
                } else if (hudOverlay.isVisible()) {
                    // HUD is visible, hide it
                    hudOverlay.setVisible(false);
                    LOGGER.info("Hidden HUD overlay");
                } else if (client.currentScreen == null) {
                    // Open controls screen
                    LOGGER.info("Opening Video Player GUI");
                    currentScreen = new VideoPlayerScreen(sharedVideoPlayer);
                    client.setScreen(currentScreen);
                }
            }

            // Handle toggle controls (C key - switch from HUD to controls)
            while (toggleControlsKey.wasPressed()) {
                if (hudOverlay.isVisible()) {
                    // Switch from HUD mode to controls screen
                    hudOverlay.setVisible(false);
                    currentScreen = new VideoPlayerScreen(sharedVideoPlayer);
                    client.setScreen(currentScreen);
                    LOGGER.info("Toggling to controls view");
                }
            }
        });

        LOGGER.info("Video Player PiP Mod initialized successfully");
    }

    public static VideoPlayerScreen getCurrentScreen() {
        return currentScreen;
    }

    public static void setCurrentScreen(VideoPlayerScreen screen) {
        currentScreen = screen;
    }

    public static VideoPlayerHud getHudOverlay() {
        return hudOverlay;
    }
}
