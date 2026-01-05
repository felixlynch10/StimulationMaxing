package com.videoplayer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class VideoPlayerScreen extends Screen {
    private VideoPlayer videoPlayer;
    private int videoX = 100;
    private int videoY = 100;
    private int videoWidth = 640;
    private int videoHeight = 360;
    private float opacity = 0.9f;

    // Dragging state
    private boolean isDragging = false;
    private int dragStartX;
    private int dragStartY;

    // Resizing state
    private boolean isResizing = false;
    private ResizeHandle activeHandle = ResizeHandle.NONE;
    private int resizeStartX;
    private int resizeStartY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private int resizeStartVideoX;
    private int resizeStartVideoY;
    private double aspectRatio = 16.0 / 9.0; // Default aspect ratio

    // UI elements
    private ButtonWidget browseButton;
    private ButtonWidget playPauseButton;
    private ButtonWidget loadUrlButton;
    private TextFieldWidget urlTextField;
    private String currentVideoPath = null;

    private static final int RESIZE_HANDLE_SIZE = 8;
    private static final int MIN_WIDTH = 160;
    private static final int MIN_HEIGHT = 90;

    enum ResizeHandle {
        NONE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public VideoPlayerScreen(VideoPlayer videoPlayer) {
        super(Text.literal("Video Player"));
        this.videoPlayer = videoPlayer;

        // Sync state from HUD if available
        VideoPlayerHud hud = VideoPlayerMod.getHudOverlay();
        if (hud != null) {
            this.videoX = hud.getX();
            this.videoY = hud.getY();
            this.videoWidth = hud.getWidth();
            this.videoHeight = hud.getHeight();
        }

        VideoPlayerMod.setCurrentScreen(this);
    }

    public int getVideoX() { return videoX; }
    public int getVideoY() { return videoY; }
    public int getVideoWidth() { return videoWidth; }
    public int getVideoHeight() { return videoHeight; }

    @Override
    protected void init() {
        super.init();

        // Set aspect ratio from video dimensions
        aspectRatio = (double) videoWidth / (double) videoHeight;

        // Add browse button
        int buttonWidth = 80;
        int buttonHeight = 20;
        int controlsY = videoY + videoHeight + 5;

        browseButton = ButtonWidget.builder(Text.literal("Browse..."), button -> {
            openFilePicker();
        }).dimensions(videoX + 5, controlsY, buttonWidth, buttonHeight).build();
        addDrawableChild(browseButton);

        // Add play/pause button
        playPauseButton = ButtonWidget.builder(Text.literal("Play/Pause"), button -> {
            if (videoPlayer != null) {
                videoPlayer.togglePlayPause();
            }
        }).dimensions(videoX + buttonWidth + 10, controlsY, buttonWidth, buttonHeight).build();
        addDrawableChild(playPauseButton);

        // Add URL text field
        int urlFieldWidth = 300;
        urlTextField = new TextFieldWidget(textRenderer, videoX + 5, controlsY + 25, urlFieldWidth, buttonHeight, Text.literal("YouTube URL"));
        urlTextField.setPlaceholder(Text.literal("Enter YouTube URL..."));
        urlTextField.setMaxLength(500);
        addDrawableChild(urlTextField);

        // Add Load URL button
        loadUrlButton = ButtonWidget.builder(Text.literal("Load URL"), button -> {
            loadYouTubeUrl(urlTextField.getText());
        }).dimensions(videoX + urlFieldWidth + 10, controlsY + 25, buttonWidth, buttonHeight).build();
        addDrawableChild(loadUrlButton);
    }

    private void openFilePicker() {
        // Run file picker on separate thread to avoid blocking Minecraft
        new Thread(() -> {
            try {
                VideoPlayerMod.LOGGER.info("Opening file picker...");

                // Use LWJGL's TinyFileDialogs for native file picker
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    // Define file filters for video files
                    PointerBuffer filters = stack.mallocPointer(8);
                    filters.put(stack.UTF8("*.mp4"));
                    filters.put(stack.UTF8("*.mkv"));
                    filters.put(stack.UTF8("*.avi"));
                    filters.put(stack.UTF8("*.webm"));
                    filters.put(stack.UTF8("*.mov"));
                    filters.put(stack.UTF8("*.flv"));
                    filters.put(stack.UTF8("*.m4v"));
                    filters.put(stack.UTF8("*.wmv"));
                    filters.flip();

                    // Get user's Downloads directory as default path
                    String defaultPath = System.getProperty("user.home") + "/Downloads";

                    // Open file dialog
                    String selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select Video File",
                        defaultPath,
                        filters,
                        "Video Files (*.mp4, *.mkv, *.avi, *.webm, *.mov, *.flv, *.m4v, *.wmv)",
                        false
                    );

                    if (selectedPath != null) {
                        currentVideoPath = selectedPath;
                        VideoPlayerMod.LOGGER.info("Selected video: {}", currentVideoPath);

                        // Load video on main thread
                        client.execute(() -> {
                            if (videoPlayer != null) {
                                videoPlayer.loadVideo(currentVideoPath);
                                VideoPlayerMod.LOGGER.info("Loading video: {}", currentVideoPath);
                            }
                        });
                    } else {
                        VideoPlayerMod.LOGGER.info("File picker cancelled");
                    }
                }
            } catch (Exception e) {
                VideoPlayerMod.LOGGER.error("Error opening file picker", e);
            }
        }, "FilePicker-Thread").start();
    }

    private void loadYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            VideoPlayerMod.LOGGER.warn("Empty URL provided");
            return;
        }

        new Thread(() -> {
            try {
                VideoPlayerMod.LOGGER.info("Loading YouTube URL: {}", url);

                // Try different yt-dlp paths
                String streamUrl = null;

                // Try Homebrew paths first (Apple Silicon and Intel)
                streamUrl = extractStreamUrl(url, "/opt/homebrew/bin/yt-dlp");
                if (streamUrl == null) {
                    streamUrl = extractStreamUrl(url, "/usr/local/bin/yt-dlp");
                }
                // Try system PATH
                if (streamUrl == null) {
                    streamUrl = extractStreamUrl(url, "yt-dlp");
                }
                // Fallback to youtube-dl
                if (streamUrl == null) {
                    VideoPlayerMod.LOGGER.info("yt-dlp not found, trying youtube-dl...");
                    streamUrl = extractStreamUrl(url, "/opt/homebrew/bin/youtube-dl");
                }
                if (streamUrl == null) {
                    streamUrl = extractStreamUrl(url, "/usr/local/bin/youtube-dl");
                }
                if (streamUrl == null) {
                    streamUrl = extractStreamUrl(url, "youtube-dl");
                }

                if (streamUrl != null) {
                    String finalUrl = streamUrl;
                    client.execute(() -> {
                        if (videoPlayer != null) {
                            videoPlayer.loadVideo(finalUrl);
                            VideoPlayerMod.LOGGER.info("Loaded stream from YouTube");
                        }
                    });
                } else {
                    VideoPlayerMod.LOGGER.error("Failed to extract stream URL. Make sure yt-dlp is installed.");
                    VideoPlayerMod.LOGGER.error("Install with: brew install yt-dlp");
                    VideoPlayerMod.LOGGER.error("After installing, run: which yt-dlp");
                }
            } catch (Exception e) {
                VideoPlayerMod.LOGGER.error("Error loading YouTube URL", e);
            }
        }, "YouTube-Loader-Thread").start();
    }

    private String extractStreamUrl(String youtubeUrl, String command) {
        // First try with browser cookies (for restricted videos)
        String streamUrl = tryExtractWithCookies(youtubeUrl, command);
        if (streamUrl != null) {
            return streamUrl;
        }

        // Fallback to no cookies
        return tryExtractNoCookies(youtubeUrl, command);
    }

    private String tryExtractWithCookies(String youtubeUrl, String command) {
        try {
            VideoPlayerMod.LOGGER.info("Trying {} with browser cookies", command);

            // Try different browsers in order of likelihood
            String[] browsers = {"chrome", "firefox", "safari", "edge"};

            for (String browser : browsers) {
                ProcessBuilder pb = new ProcessBuilder(
                    command,
                    "--cookies-from-browser", browser,
                    "-f", "best[ext=mp4]/best",
                    "-g",
                    youtubeUrl
                );

                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String streamUrl = reader.readLine();

                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine;
                StringBuilder errors = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errors.append(errorLine).append("\n");
                }

                int exitCode = process.waitFor();

                if (exitCode == 0 && streamUrl != null && !streamUrl.isEmpty()) {
                    VideoPlayerMod.LOGGER.info("Successfully extracted stream URL with {} using {} cookies", command, browser);
                    return streamUrl;
                } else {
                    VideoPlayerMod.LOGGER.debug("Failed with {} cookies: {}", browser, errors.toString());
                }
            }
        } catch (Exception e) {
            VideoPlayerMod.LOGGER.debug("Exception trying with cookies: {}", e.getMessage());
        }
        return null;
    }

    private String tryExtractNoCookies(String youtubeUrl, String command) {
        try {
            VideoPlayerMod.LOGGER.info("Trying {} without cookies", command);

            ProcessBuilder pb = new ProcessBuilder(
                command,
                "-f", "best[ext=mp4]/best",
                "-g",
                youtubeUrl
            );

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String streamUrl = reader.readLine();

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String errorLine;
            StringBuilder errors = new StringBuilder();
            while ((errorLine = errorReader.readLine()) != null) {
                errors.append(errorLine).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0 && streamUrl != null && !streamUrl.isEmpty()) {
                VideoPlayerMod.LOGGER.info("Successfully extracted stream URL with {}", command);
                return streamUrl;
            } else {
                if (errors.length() > 0) {
                    VideoPlayerMod.LOGGER.error("yt-dlp error output: {}", errors.toString());
                }
                VideoPlayerMod.LOGGER.warn("Command {} failed with exit code: {}", command, exitCode);
            }
        } catch (Exception e) {
            VideoPlayerMod.LOGGER.warn("Exception running {}: {}", command, e.getMessage());
        }
        return null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update UI element positions if video window moved
        int controlsY = videoY + videoHeight + 5;

        if (browseButton != null) {
            browseButton.setPosition(videoX + 5, controlsY);
        }
        if (playPauseButton != null) {
            playPauseButton.setPosition(videoX + 90, controlsY);
        }
        if (urlTextField != null) {
            urlTextField.setPosition(videoX + 5, controlsY + 25);
        }
        if (loadUrlButton != null) {
            loadUrlButton.setPosition(videoX + 310, controlsY + 25);
        }

        if (videoPlayer != null && videoPlayer.hasTexture()) {
            // Draw video frame
            context.drawTexture(
                RenderLayer::getGuiTextured,
                videoPlayer.getTextureIdentifier(),
                videoX, videoY,
                0.0f, 0.0f,
                videoWidth, videoHeight,
                videoWidth, videoHeight
            );

            // Draw border
            context.drawBorder(videoX - 1, videoY - 1, videoWidth + 2, videoHeight + 2, 0xFFFFFFFF);

            // Draw resize handles
            drawResizeHandles(context, mouseX, mouseY);
        } else {
            // Draw placeholder if no video loaded
            context.fill(videoX, videoY, videoX + videoWidth, videoY + videoHeight, 0x80000000);
            context.drawBorder(videoX - 1, videoY - 1, videoWidth + 2, videoHeight + 2, 0xFFFFFFFF);

            Text placeholderText = Text.literal("Click 'Browse...' to select a video");
            int textX = videoX + (videoWidth - textRenderer.getWidth(placeholderText)) / 2;
            int textY = videoY + (videoHeight - textRenderer.fontHeight) / 2;
            context.drawText(textRenderer, placeholderText, textX, textY, 0xFFFFFFFF, true);

            // Draw resize handles
            drawResizeHandles(context, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawResizeHandles(DrawContext context, int mouseX, int mouseY) {
        ResizeHandle hovered = getResizeHandleAt(mouseX, mouseY);

        // Only corner handles now
        drawHandle(context, videoX - RESIZE_HANDLE_SIZE / 2, videoY - RESIZE_HANDLE_SIZE / 2, hovered == ResizeHandle.TOP_LEFT);
        drawHandle(context, videoX + videoWidth - RESIZE_HANDLE_SIZE / 2, videoY - RESIZE_HANDLE_SIZE / 2, hovered == ResizeHandle.TOP_RIGHT);
        drawHandle(context, videoX - RESIZE_HANDLE_SIZE / 2, videoY + videoHeight - RESIZE_HANDLE_SIZE / 2, hovered == ResizeHandle.BOTTOM_LEFT);
        drawHandle(context, videoX + videoWidth - RESIZE_HANDLE_SIZE / 2, videoY + videoHeight - RESIZE_HANDLE_SIZE / 2, hovered == ResizeHandle.BOTTOM_RIGHT);
    }

    private void drawHandle(DrawContext context, int x, int y, boolean hovered) {
        int color = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        context.fill(x, y, x + RESIZE_HANDLE_SIZE, y + RESIZE_HANDLE_SIZE, color);
        context.drawBorder(x, y, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, 0xFF000000);
    }

    private ResizeHandle getResizeHandleAt(int mouseX, int mouseY) {
        int handleHalf = RESIZE_HANDLE_SIZE / 2;

        // Only check corners
        if (isInHandle(mouseX, mouseY, videoX, videoY, handleHalf)) return ResizeHandle.TOP_LEFT;
        if (isInHandle(mouseX, mouseY, videoX + videoWidth, videoY, handleHalf)) return ResizeHandle.TOP_RIGHT;
        if (isInHandle(mouseX, mouseY, videoX, videoY + videoHeight, handleHalf)) return ResizeHandle.BOTTOM_LEFT;
        if (isInHandle(mouseX, mouseY, videoX + videoWidth, videoY + videoHeight, handleHalf)) return ResizeHandle.BOTTOM_RIGHT;

        return ResizeHandle.NONE;
    }

    private boolean isInHandle(int mouseX, int mouseY, int handleCenterX, int handleCenterY, int handleHalf) {
        return mouseX >= handleCenterX - handleHalf && mouseX <= handleCenterX + handleHalf &&
               mouseY >= handleCenterY - handleHalf && mouseY <= handleCenterY + handleHalf;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check for resize handle click first
            ResizeHandle handle = getResizeHandleAt((int) mouseX, (int) mouseY);
            if (handle != ResizeHandle.NONE) {
                isResizing = true;
                activeHandle = handle;
                resizeStartX = (int) mouseX;
                resizeStartY = (int) mouseY;
                resizeStartWidth = videoWidth;
                resizeStartHeight = videoHeight;
                resizeStartVideoX = videoX;
                resizeStartVideoY = videoY;
                return true;
            }

            // Check if clicking inside video area for dragging
            if (mouseX >= videoX && mouseX <= videoX + videoWidth &&
                mouseY >= videoY && mouseY <= videoY + videoHeight) {
                isDragging = true;
                dragStartX = (int) mouseX - videoX;
                dragStartY = (int) mouseY - videoY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            isResizing = false;
            activeHandle = ResizeHandle.NONE;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isResizing) {
            int dx = (int) mouseX - resizeStartX;
            int dy = (int) mouseY - resizeStartY;

            // Calculate which dimension should drive the resize to maintain aspect ratio
            // Use the larger change to determine the new size
            int newWidth, newHeight;

            switch (activeHandle) {
                case BOTTOM_RIGHT:
                    // Use width change as primary, calculate height from aspect ratio
                    newWidth = Math.max(MIN_WIDTH, resizeStartWidth + dx);
                    newHeight = (int) (newWidth / aspectRatio);
                    if (newHeight < MIN_HEIGHT) {
                        newHeight = MIN_HEIGHT;
                        newWidth = (int) (newHeight * aspectRatio);
                    }
                    videoWidth = newWidth;
                    videoHeight = newHeight;
                    break;

                case BOTTOM_LEFT:
                    // Resize from left, maintain aspect ratio
                    newWidth = Math.max(MIN_WIDTH, resizeStartWidth - dx);
                    newHeight = (int) (newWidth / aspectRatio);
                    if (newHeight < MIN_HEIGHT) {
                        newHeight = MIN_HEIGHT;
                        newWidth = (int) (newHeight * aspectRatio);
                    }
                    videoX = resizeStartVideoX + (resizeStartWidth - newWidth);
                    videoWidth = newWidth;
                    videoHeight = newHeight;
                    break;

                case TOP_RIGHT:
                    // Resize from top, maintain aspect ratio
                    newWidth = Math.max(MIN_WIDTH, resizeStartWidth + dx);
                    newHeight = (int) (newWidth / aspectRatio);
                    if (newHeight < MIN_HEIGHT) {
                        newHeight = MIN_HEIGHT;
                        newWidth = (int) (newHeight * aspectRatio);
                    }
                    videoY = resizeStartVideoY + (resizeStartHeight - newHeight);
                    videoWidth = newWidth;
                    videoHeight = newHeight;
                    break;

                case TOP_LEFT:
                    // Resize from top-left, maintain aspect ratio
                    newWidth = Math.max(MIN_WIDTH, resizeStartWidth - dx);
                    newHeight = (int) (newWidth / aspectRatio);
                    if (newHeight < MIN_HEIGHT) {
                        newHeight = MIN_HEIGHT;
                        newWidth = (int) (newHeight * aspectRatio);
                    }
                    videoX = resizeStartVideoX + (resizeStartWidth - newWidth);
                    videoY = resizeStartVideoY + (resizeStartHeight - newHeight);
                    videoWidth = newWidth;
                    videoHeight = newHeight;
                    break;
            }

            return true;
        } else if (isDragging) {
            videoX = (int) mouseX - dragStartX;
            videoY = (int) mouseY - dragStartY;

            // Keep within screen bounds
            videoX = Math.max(0, Math.min(videoX, width - videoWidth));
            videoY = Math.max(0, Math.min(videoY, height - videoHeight));

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC to close and switch to HUD mode
        if (keyCode == 256) { // ESC key
            // Sync position to HUD
            VideoPlayerHud hud = VideoPlayerMod.getHudOverlay();
            if (hud != null) {
                hud.setPosition(videoX, videoY);
                hud.setSize(videoWidth, videoHeight);
                hud.setVisible(true);
            }
            // Close screen
            this.close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        // Don't stop video when closing GUI - keep it playing
        VideoPlayerMod.setCurrentScreen(null);
        super.close();
    }

    @Override
    public void removed() {
        // Don't cleanup the shared video player - just clear the screen reference
        VideoPlayerMod.setCurrentScreen(null);
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        // Don't pause the game when this screen is open
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
