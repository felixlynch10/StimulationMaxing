package com.videoplayer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;

public class VideoPlayerHud implements HudRenderCallback {
    private VideoPlayer videoPlayer;
    private int videoX = 100;
    private int videoY = 100;
    private int videoWidth = 640;
    private int videoHeight = 360;
    private boolean visible = false;

    private static final int PLAY_PAUSE_BUTTON_SIZE = 48;

    public VideoPlayerHud(VideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public VideoPlayer getVideoPlayer() {
        return videoPlayer;
    }

    public void setPosition(int x, int y) {
        this.videoX = x;
        this.videoY = y;
    }

    public void setSize(int width, int height) {
        this.videoWidth = width;
        this.videoHeight = height;
    }

    public int getX() {
        return videoX;
    }

    public int getY() {
        return videoY;
    }

    public int getWidth() {
        return videoWidth;
    }

    public int getHeight() {
        return videoHeight;
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!visible || videoPlayer == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        // Only render if we have a texture
        if (videoPlayer.hasTexture()) {
            // Draw video frame
            drawContext.drawTexture(
                RenderLayer::getGuiTextured,
                videoPlayer.getTextureIdentifier(),
                videoX, videoY,
                0.0f, 0.0f,
                videoWidth, videoHeight,
                videoWidth, videoHeight
            );

            // Check if mouse is over video to show play/pause overlay
            double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

            if (isMouseOverVideo((int) mouseX, (int) mouseY)) {
                drawPlayPauseOverlay(drawContext, (int) mouseX, (int) mouseY);
            }
        }
    }

    private boolean isMouseOverVideo(int mouseX, int mouseY) {
        return mouseX >= videoX && mouseX <= videoX + videoWidth &&
               mouseY >= videoY && mouseY <= videoY + videoHeight;
    }

    private void drawPlayPauseOverlay(DrawContext context, int mouseX, int mouseY) {
        int buttonX = videoX + (videoWidth - PLAY_PAUSE_BUTTON_SIZE) / 2;
        int buttonY = videoY + (videoHeight - PLAY_PAUSE_BUTTON_SIZE) / 2;

        // Check if mouse is over the button
        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + PLAY_PAUSE_BUTTON_SIZE &&
                           mouseY >= buttonY && mouseY <= buttonY + PLAY_PAUSE_BUTTON_SIZE;

        // Semi-transparent background
        int bgColor = isHovered ? 0xCC000000 : 0x99000000;
        context.fill(buttonX, buttonY, buttonX + PLAY_PAUSE_BUTTON_SIZE, buttonY + PLAY_PAUSE_BUTTON_SIZE, bgColor);
        context.drawBorder(buttonX, buttonY, PLAY_PAUSE_BUTTON_SIZE, PLAY_PAUSE_BUTTON_SIZE, 0xFFFFFFFF);

        // Draw play triangle symbol
        int symbolX = buttonX + PLAY_PAUSE_BUTTON_SIZE / 2;
        int symbolY = buttonY + PLAY_PAUSE_BUTTON_SIZE / 2;
        int symbolSize = PLAY_PAUSE_BUTTON_SIZE / 3;

        int playSize = symbolSize;
        for (int i = 0; i < playSize; i++) {
            int lineHeight = (int) ((double) i / playSize * playSize * 2);
            int lineY = symbolY - lineHeight / 2;
            context.fill(symbolX - playSize / 2 + i, lineY, symbolX - playSize / 2 + i + 1, lineY + lineHeight, 0xFFFFFFFF);
        }
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) {
            return false;
        }

        // Check if clicking on play/pause overlay button
        if (isMouseOverVideo((int) mouseX, (int) mouseY)) {
            int buttonX = videoX + (videoWidth - PLAY_PAUSE_BUTTON_SIZE) / 2;
            int buttonY = videoY + (videoHeight - PLAY_PAUSE_BUTTON_SIZE) / 2;

            if (mouseX >= buttonX && mouseX <= buttonX + PLAY_PAUSE_BUTTON_SIZE &&
                mouseY >= buttonY && mouseY <= buttonY + PLAY_PAUSE_BUTTON_SIZE) {
                if (videoPlayer != null) {
                    videoPlayer.togglePlayPause();
                }
                return true;
            }
        }

        return false;
    }

    public void cleanup() {
        if (videoPlayer != null) {
            videoPlayer.cleanup();
            videoPlayer = null;
        }
    }
}
