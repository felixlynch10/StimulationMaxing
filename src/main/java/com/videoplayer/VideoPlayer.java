package com.videoplayer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;

public class VideoPlayer {
    private static final String TEXTURE_ID = "videoplayer:video_frame";

    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer mediaPlayer;
    private NativeImageBackedTexture texture;
    private Identifier textureIdentifier;

    private int videoWidth = 1920;
    private int videoHeight = 1080;
    private ByteBuffer videoBuffer;
    private boolean textureNeedsUpdate = false;

    public VideoPlayer() {
        try {
            // Initialize VLC
            mediaPlayerFactory = new MediaPlayerFactory();
            mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

            // Set up video surface callback
            BufferFormatCallback bufferFormatCallback = new BufferFormatCallback() {
                @Override
                public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
                    videoWidth = sourceWidth;
                    videoHeight = sourceHeight;
                    videoBuffer = BufferUtils.createByteBuffer(videoWidth * videoHeight * 4);

                    // Initialize texture on main thread
                    MinecraftClient.getInstance().execute(() -> {
                        initTexture();
                    });

                    return new RV32BufferFormat(videoWidth, videoHeight);
                }

                @Override
                public void allocatedBuffers(ByteBuffer[] buffers) {
                    // No-op: we don't need to handle buffer allocation
                }
            };

            RenderCallback renderCallback = new RenderCallback() {
                @Override
                public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
                    if (nativeBuffers != null && nativeBuffers.length > 0) {
                        // Copy video data
                        videoBuffer.clear();
                        nativeBuffers[0].rewind();
                        videoBuffer.put(nativeBuffers[0]);
                        videoBuffer.rewind();
                        textureNeedsUpdate = true;
                    }
                }
            };

            CallbackVideoSurface videoSurface = new CallbackVideoSurface(
                bufferFormatCallback,
                renderCallback,
                true,
                VideoSurfaceAdapters.getVideoSurfaceAdapter()
            );

            mediaPlayer.videoSurface().set(videoSurface);

            VideoPlayerMod.LOGGER.info("Video player initialized successfully");
        } catch (Exception e) {
            VideoPlayerMod.LOGGER.error("Failed to initialize video player", e);
        }
    }

    private void initTexture() {
        if (texture != null) {
            texture.close();
        }

        // Create NativeImage and texture
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, videoWidth, videoHeight, false);
        texture = new NativeImageBackedTexture(() -> "video_frame", image);
        textureIdentifier = Identifier.of(TEXTURE_ID);
        MinecraftClient.getInstance().getTextureManager().registerTexture(
            textureIdentifier,
            texture
        );

        VideoPlayerMod.LOGGER.info("Texture initialized: {}x{}", videoWidth, videoHeight);
    }

    public void loadVideo(String filePath) {
        if (mediaPlayer != null) {
            VideoPlayerMod.LOGGER.info("Loading video: {}", filePath);
            mediaPlayer.media().play(filePath);
        }
    }

    public void updateTexture() {
        if (textureNeedsUpdate && texture != null && videoBuffer != null) {
            MinecraftClient.getInstance().execute(() -> {
                try {
                    NativeImage image = texture.getImage();
                    if (image != null) {
                        // Copy pixel data from video buffer to texture
                        videoBuffer.rewind();
                        for (int y = 0; y < videoHeight; y++) {
                            for (int x = 0; x < videoWidth; x++) {
                                int b = videoBuffer.get() & 0xFF;
                                int g = videoBuffer.get() & 0xFF;
                                int r = videoBuffer.get() & 0xFF;
                                int a = videoBuffer.get() & 0xFF;

                                // ABGR format for NativeImage
                                int color = (a << 24) | (b << 16) | (g << 8) | r;
                                image.setColor(x, y, color);
                            }
                        }

                        texture.upload();
                        textureNeedsUpdate = false;
                    }
                } catch (Exception e) {
                    VideoPlayerMod.LOGGER.error("Error updating texture", e);
                }
            });
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
                VideoPlayerMod.LOGGER.info("Video paused");
            } else {
                mediaPlayer.controls().play();
                VideoPlayerMod.LOGGER.info("Video playing");
            }
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            VideoPlayerMod.LOGGER.info("Video stopped");
        }
    }

    public void cleanup() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            mediaPlayer.release();
        }

        if (mediaPlayerFactory != null) {
            mediaPlayerFactory.release();
        }

        if (texture != null && textureIdentifier != null) {
            // Texture will be cleaned up automatically or manually close it
            texture.close();
            textureIdentifier = null;
        }

        VideoPlayerMod.LOGGER.info("Video player cleaned up");
    }

    public boolean hasTexture() {
        return texture != null && textureIdentifier != null;
    }

    public Identifier getTextureIdentifier() {
        // Update texture before returning
        updateTexture();
        return textureIdentifier;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }
}
