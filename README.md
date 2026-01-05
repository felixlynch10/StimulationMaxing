# Video Player PiP - Minecraft Mod

A Fabric mod for Minecraft 1.21.5 that adds a picture-in-picture video player to your game.

## Features (Current MVP)

- Press `V` to open the video player GUI
- Drag the video window anywhere on screen
- Press `Space` to play/pause
- Press `ESC` to close the GUI
- Adjustable opacity (currently hardcoded to 0.9)
- Game continues running while video plays (doesn't pause)

## Prerequisites

Before building this mod, you need:

1. **Java 21** - Download from [Adoptium](https://adoptium.net/)
2. **VLC Media Player** - Download from [videolan.org](https://www.videolan.org/)
   - **IMPORTANT**: VLCJ requires VLC to be installed on your system
   - **Windows**: Install VLC to default location (`C:\Program Files\VideoLAN\VLC`)
   - **macOS**: Install VLC to `/Applications/VLC.app`
   - **Linux**: Install via package manager (`sudo apt install vlc` or similar)

## Building the Mod

1. Clone or download this repository
2. Open a terminal in the project directory
3. Run the build command:

```bash
./gradlew build
```

4. The compiled mod will be in `build/libs/videoplayer-1.0.0.jar`

## Installation

### Step 1: Install VLC Media Player
**This is critical - the mod will NOT work without VLC installed!**

- **macOS**: Download from [videolan.org](https://www.videolan.org/) and install to `/Applications/VLC.app`
- **Windows**: Install to `C:\Program Files\VideoLAN\VLC`
- **Linux**: Run `sudo apt install vlc` (Ubuntu/Debian) or equivalent for your distro

### Step 2: Install Minecraft with Fabric
1. Download and run the [Fabric Installer](https://fabricmc.net/use/) for Minecraft 1.21.5
2. Select "Client" and click "Install"
3. Launch Minecraft Launcher and select the "Fabric" profile

### Step 3: Install Fabric API
1. Download [Fabric API 0.119.5+1.21.5](https://modrinth.com/mod/fabric-api/version/0.119.5+1.21.5) from Modrinth
2. Place the downloaded JAR in your `.minecraft/mods` folder
   - **macOS**: `~/Library/Application Support/minecraft/mods`
   - **Windows**: `%APPDATA%\.minecraft\mods`
   - **Linux**: `~/.minecraft/mods`

### Step 4: Install Video Player Mod
1. Copy `build/libs/videoplayer-1.0.0.jar` to your `.minecraft/mods` folder
2. Launch Minecraft with the Fabric profile

## Usage

### Loading a Video (Currently Hardcoded)

**IMPORTANT**: Right now, you need to manually edit the code to specify your video file path.

1. Open `src/main/java/com/videoplayer/VideoPlayerScreen.java`
2. Find line ~28 (in the `init()` method):
   ```java
   // TODO: Replace with file picker - this is just for testing
   // videoPlayer.loadVideo("/path/to/your/test/video.mp4");
   ```
3. Uncomment the `loadVideo` line and replace with your actual video path:
   ```java
   videoPlayer.loadVideo("/Users/yourname/Videos/test.mp4");
   ```
4. Rebuild the mod with `./gradlew build`

### Controls

- **V** - Open/close video player GUI
- **Left Click + Drag** - Move the video window
- **Space** - Play/pause video
- **ESC** - Close GUI (video keeps playing in background)

## Troubleshooting

### "Video player failed to initialize" error

This usually means VLC is not installed or VLCJ can't find it:

- **Windows**: Make sure VLC is installed to `C:\Program Files\VideoLAN\VLC`
- **macOS**: Check that VLC.app is in `/Applications`
- **Linux**: Verify VLC is installed (`which vlc`)

### No video appears / black screen

- Make sure your video file path is correct and the file exists
- Try using an MP4 file with H.264 codec (most compatible)
- Check Minecraft logs for errors (`.minecraft/logs/latest.log`)

### Performance issues / lag

- Try using a lower resolution video (720p instead of 1080p)
- The current implementation doesn't limit video FPS - this will be improved

## Planned Features (Not Yet Implemented)

- File picker GUI to select videos without editing code
- Resize functionality (drag corners to resize)
- Volume control slider
- Seek bar with timestamp
- Save window position/size between sessions
- Support for streaming URLs (YouTube, Twitch)
- FPS limiter for better performance
- Multiple video format support
- Subtitle rendering

## Development

This is an MVP (Minimum Viable Product). The core functionality works, but many polish features are still to come.

### Project Structure

```
src/main/java/com/videoplayer/
├── VideoPlayerMod.java        # Main mod initialization, keybinds
├── VideoPlayerScreen.java     # GUI screen with drag functionality
└── VideoPlayer.java           # VLC integration and texture management
```

### Next Steps for Development

1. Add file picker GUI (javax.swing.JFileChooser or native Minecraft file dialog)
2. Implement resize handles on video window
3. Add video control UI (play/pause button, seek bar, volume)
4. Save/load configuration (window position, last video, etc.)
5. Performance optimizations (FPS limiting, resolution scaling)

## Known Issues

- Video path must be hardcoded (file picker not yet implemented)
- No resize functionality yet (fixed at 640x360)
- No volume control (uses system volume)
- No seek bar
- Settings don't persist between sessions

## License

MIT License - Feel free to modify and distribute

## Credits

Built with:
- [Fabric](https://fabricmc.net/) - Minecraft modding framework
- [VLCJ](https://github.com/caprica/vlcj) - Java bindings for VLC media player
