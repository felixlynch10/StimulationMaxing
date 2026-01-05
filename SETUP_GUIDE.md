# Complete Setup Guide - Video Player PiP Mod

## What You Have Now

A fully functional Minecraft mod that displays videos in a picture-in-picture window while you play! The mod is built and ready to use at `build/libs/videoplayer-1.0.0.jar`.

## Quick Start (5 Steps)

### 1. Install VLC Media Player (REQUIRED!)
The mod uses VLC under the hood to play videos, so you MUST have it installed:

**On macOS (your system):**
```bash
# If you have Homebrew:
brew install --cask vlc

# Or download from: https://www.videolan.org/
# Install to /Applications/VLC.app
```

To verify VLC is installed:
```bash
ls /Applications/VLC.app
```

### 2. Install Minecraft Fabric Loader
1. Go to https://fabricmc.net/use/
2. Download the Fabric Installer
3. Run it and select:
   - Minecraft Version: **1.21.5**
   - Loader Version: **0.16.10** (should be default)
   - Install Location: (leave default)
4. Click "Install"

### 3. Download Fabric API Mod
You need this dependency for the mod to work:
- Download: https://modrinth.com/mod/fabric-api/version/0.119.5+1.21.5
- Save the JAR file somewhere you can find it

### 4. Install the Mods
Copy both JAR files to your Minecraft mods folder:

**On macOS:**
```bash
# Create mods folder if it doesn't exist
mkdir -p ~/Library/Application\ Support/minecraft/mods

# Copy Fabric API (adjust path to where you downloaded it)
cp ~/Downloads/fabric-api-0.119.5+1.21.5.jar ~/Library/Application\ Support/minecraft/mods/

# Copy Video Player mod
cp build/libs/videoplayer-1.0.0.jar ~/Library/Application\ Support/minecraft/mods/
```

**On Windows:**
- Open `%APPDATA%\.minecraft\mods` in File Explorer
- Copy both JAR files there

**On Linux:**
```bash
mkdir -p ~/.minecraft/mods
cp build/libs/videoplayer-1.0.0.jar ~/.minecraft/mods/
```

### 5. Configure a Test Video (IMPORTANT!)
Before running, you need to set a video file path:

1. Find a video file on your computer (MP4, MKV, AVI, etc.)
2. Get its full path:
   ```bash
   # Example on macOS:
   # /Users/yourname/Movies/test_video.mp4
   ```

3. Edit the mod to load your video:
   - Open `src/main/java/com/videoplayer/VideoPlayerScreen.java`
   - Find line 28 (in the `init()` method)
   - Uncomment and update the path:
   ```java
   videoPlayer.loadVideo("/Users/yourname/Movies/test_video.mp4");
   ```

4. Rebuild the mod:
   ```bash
   gradle build
   ```

5. Copy the updated JAR to your mods folder again (same as step 4)

## Running the Mod

1. Launch Minecraft Launcher
2. Select the "Fabric" profile from the dropdown
3. Click "Play"
4. Once in-game:
   - Press **V** to open the video player
   - Press **Space** to play/pause
   - **Left-click and drag** to move the window
   - Press **ESC** to close the GUI

## How It Works

### Architecture
```
VideoPlayerMod.java
  └─ Initializes mod and registers keybinds
  └─ Opens VideoPlayerScreen when V is pressed

VideoPlayerScreen.java
  └─ GUI overlay that renders on top of Minecraft
  └─ Handles dragging, input, and display
  └─ Creates VideoPlayer instance

VideoPlayer.java
  └─ Integrates with VLC using VLCJ library
  └─ Decodes video frames
  └─ Converts frames to Minecraft textures
  └─ Updates texture every frame
```

### Video Playback Flow
1. VLC decodes video file
2. Video frames are copied to a ByteBuffer
3. ByteBuffer is converted to NativeImage (Minecraft's image format)
4. NativeImage is uploaded as a texture
5. Texture is rendered to screen using DrawContext
6. Process repeats every frame (30-60 FPS)

### Key Files
- `VideoPlayerMod.java` - Main mod entry point, keybind registration
- `VideoPlayerScreen.java` - GUI screen with drag functionality
- `VideoPlayer.java` - VLC integration and texture management
- `build.gradle` - Build configuration with dependencies
- `gradle.properties` - Minecraft and mod version settings
- `fabric.mod.json` - Mod metadata

## Troubleshooting

### "Video player failed to initialize"
**Problem**: VLC is not installed or can't be found

**Solution**:
```bash
# Check if VLC is installed (macOS):
ls /Applications/VLC.app

# If not found, install it:
brew install --cask vlc
```

### Minecraft won't launch with the mod
**Problem**: Missing Fabric API or wrong Minecraft version

**Solution**:
1. Check Minecraft version is 1.21.5
2. Verify Fabric API is in mods folder
3. Check Minecraft logs: `~/Library/Application Support/minecraft/logs/latest.log`

### Video doesn't appear / black screen
**Problem**: Video file path is wrong or file doesn't exist

**Solution**:
1. Double-check the path in `VideoPlayerScreen.java` line 28
2. Make sure path is absolute (starts with `/` on macOS/Linux, `C:\` on Windows)
3. Try a different video file (MP4 with H.264 is most compatible)
4. Check Minecraft logs for errors

### Performance issues / lag
**Problem**: Video decoding is CPU-intensive

**Solution**:
- Use a lower resolution video (720p instead of 1080p/4K)
- Close other applications
- Reduce Minecraft graphics settings
- This will be optimized in future versions with FPS limiting

### Mod doesn't load in Minecraft
**Problem**: Wrong mod version or corrupted build

**Solution**:
```bash
# Clean and rebuild:
gradle clean build

# Check the JAR was created:
ls -lh build/libs/videoplayer-1.0.0.jar
```

## Next Steps / Future Features

The current version is an MVP (Minimum Viable Product). Here's what to add next:

### Phase 2 Features (Easy)
1. **File Picker GUI** - Select videos in-game instead of editing code
2. **Resize Handles** - Drag corners to resize the video window
3. **Video Controls UI** - Play/pause button, seek bar, volume slider
4. **Settings Screen** - Configure keybinds, default size, opacity

### Phase 3 Features (Medium)
1. **Save State** - Remember window position, size, last video
2. **Performance Options** - FPS limiter, resolution scaling
3. **Multiple Videos** - Playlist support, switch between videos
4. **Subtitles** - SRT file support

### Phase 4 Features (Advanced)
1. **Streaming URLs** - YouTube, Twitch, direct HTTP streams
2. **Audio Visualization** - Waveforms, spectrum analyzer
3. **Picture-in-Picture Toggle** - Keep video playing while GUI closed
4. **Multi-window** - Multiple videos at once

## Development Commands

```bash
# Build the mod
gradle build

# Clean build artifacts
gradle clean

# Build without tests (faster)
gradle build -x test

# See all available tasks
gradle tasks

# Run with debug info
gradle build --debug

# Generate IDE project files
gradle idea          # IntelliJ IDEA
gradle eclipse       # Eclipse
```

## Project Structure
```
Videoplayer/
├── build/
│   └── libs/
│       └── videoplayer-1.0.0.jar    ← Your compiled mod
├── gradle/
│   └── wrapper/                      ← Gradle wrapper files
├── src/
│   └── main/
│       ├── java/com/videoplayer/
│       │   ├── VideoPlayerMod.java
│       │   ├── VideoPlayerScreen.java
│       │   └── VideoPlayer.java
│       └── resources/
│           ├── fabric.mod.json       ← Mod metadata
│           ├── videoplayer.mixins.json
│           └── assets/videoplayer/
│               └── lang/
│                   └── en_us.json    ← Keybind translations
├── build.gradle                      ← Build configuration
├── gradle.properties                 ← Version settings
├── settings.gradle
├── gradlew                          ← Build script (Unix)
├── gradlew.bat                      ← Build script (Windows)
└── README.md
```

## Important Notes

1. **VLC is REQUIRED** - The mod cannot work without VLC installed
2. **Video path must be hardcoded** - File picker not implemented yet
3. **Rebuild after changing code** - Run `gradle build` after editing Java files
4. **Copy JAR to mods folder** - After building, copy to Minecraft mods folder
5. **Compatible formats** - MP4 (H.264), MKV, AVI, WebM should all work
6. **Thread safety** - Video decoding happens on background thread, texture updates on main thread

## Getting Help

If you run into issues:

1. **Check the logs**: `~/Library/Application Support/minecraft/logs/latest.log`
2. **Look for errors** starting with `[videoplayer]`
3. **Common errors**:
   - `Failed to initialize video player` = VLC not installed
   - `NullPointerException` in VideoPlayer = Video file not found
   - `ClassNotFoundException` = Missing Fabric API dependency

## Summary

You now have a working Minecraft mod that can play videos in picture-in-picture! The hardest parts (VLC integration, texture rendering, API compatibility) are done. The next steps would be adding a file picker GUI and polish features.

Enjoy watching videos while playing Minecraft!
