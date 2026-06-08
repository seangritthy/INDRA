# Installation Guide - INDRA IPTV App

## System Requirements
- **Android Version**: 8.0 or higher
- **RAM**: Minimum 2 GB (4 GB recommended)
- **Storage**: 150 MB free space for app and cache
- **Internet**: Required for streaming

## Step-by-Step Installation

### Method 1: Direct APK Installation (Easiest)

#### On Your Android Device:

1. **Enable Unknown Sources**
   - Open Settings → Security (or Safety & privacy)
   - Enable "Unknown Sources" or "Install Unknown Apps"
   - Grant permission to your file manager/browser

2. **Download the APK**
   - Download `INDRAiptv_v1.5.260608.1938.apk` from the releases page
   - File size: ~126 MB

3. **Install the App**
   - Open your file manager
   - Navigate to Downloads folder
   - Tap the APK file
   - Tap "Install"
   - Wait for installation to complete

4. **Launch the App**
   - Tap "Open" when prompted, or
   - Go to your app drawer and find "INDRA" or "IPTV"
   - Tap to launch

### Method 2: Via Android Studio (For Developers)

1. Install Android Studio
2. Clone this repository
3. Open project in Android Studio
4. Connect Android device with USB debugging enabled
5. Click "Run" or press Shift+F10
6. Select your device and wait for build/installation

## First Launch Setup

### Initial Permissions
The app will request the following permissions on first launch:
- **Notification**: For update notifications (optional)
- **Storage** (on older Android): For cache storage

Grant these permissions as needed.

### First Time Use
1. App will launch directly to the main screen (no intro video)
2. You'll see the main menu with tabs:
   - **Categories** - Browse by content type
   - **Countries** - Browse by region
   - **Regions** - Browse by geographic region
   - **Languages** - Browse by language

3. Tap the **search icon** (magnifying glass) to search for movies
4. Tap the **history icon** (clock) to view your watch history
5. Tap **settings** icon for app preferences

## Troubleshooting

### "Installation Blocked" Error
**Solution**: 
- Go to Settings → Apps & permissions → Permission Manager
- Find your file manager
- Allow "Install unknown apps" permission

### "File is Corrupted" Error
**Solution**:
- Delete the downloaded APK
- Re-download from official release page
- Ensure download completed fully (check file size)

### App Crashes on Launch
**Solution**:
1. Uninstall the app
2. Restart your device
3. Reinstall the app
4. Grant all requested permissions

### No Video Playing
**Solution**:
1. Check your internet connection
2. Try a different streaming server (tap server selector)
3. Check if content is available in your region
4. Ensure you have sufficient bandwidth (min 5 Mbps recommended)

### Memory/Performance Issues
**Solution**:
1. Close other apps
2. Clear app cache: Settings → Apps → INDRA → Storage → Clear Cache
3. Restart your device
4. Consider a device with more RAM

## Usage Guide

### Watching Movies
1. Tap "Movie Hub" or search icon
2. Browse or search for movies
3. Tap a movie to view details
4. Tap the play button or server button
5. Select a streaming server if prompted
6. Enjoy! Your watch will be automatically tracked in history

### Watching TV Shows
1. Search for a TV show
2. Tap to view details
3. Select Season and Episode
4. Tap play to watch
5. History will show the specific episode

### Watching IPTV Channels
1. Select Categories, Countries, Regions, or Languages tabs
2. Browse available channels
3. Tap a channel to start streaming
4. Channels don't count toward watch history

### Viewing Watch History
1. Tap the **history icon** (clock) in the top app bar
2. View all watched movies with timestamps
3. Tap any movie to view details again
4. Swipe or hold to delete individual entries
5. Tap "Clear All" to delete entire history

### Settings
1. Tap the **settings icon** (gear)
2. Available options:
   - **Dark Mode** - Toggle dark/light theme
   - **Language** - Change app language
   - **Theme** - Change color scheme
   - **Background** - Change UI background

## Performance Tips

1. **Better Streaming Performance**:
   - Close background apps
   - Use 5GHz WiFi if available
   - Ensure stable connection
   - Reduce screen resolution if lagging

2. **Reduce App Size**:
   - Clear cache regularly
   - Remove watch history of old entries
   - Uninstall and reinstall if storage issue

3. **Battery Optimization**:
   - Enable battery saver mode for streaming
   - Use Picture-in-Picture mode
   - Close app when not in use

## Regional Content

Some content may be geo-blocked or unavailable in your region. The app includes:
- Movies from worldwide TMDB database
- IPTV channels from 200+ countries
- Multiple streaming servers for redundancy

## Advanced Settings

### For Power Users:
- Enable Picture-in-Picture: Use in system settings
- Change refresh rate: System display settings
- Adjust streaming quality: Depends on server availability

## Uninstallation

1. Long press the app icon on home screen
2. Tap "Uninstall"
3. Confirm uninstallation
4. To clear app data: Settings → Apps → INDRA → Storage → Clear All

## Support & Feedback

For issues:
1. Check this troubleshooting guide
2. Review README.md for additional info
3. Check RELEASE_NOTES.md for known issues

## Backup & Migration

### Backup Watch History
Currently, watch history is stored locally. To backup:
1. Enable Developer Options
2. Use "Backup my data" to backup app data
3. Or export via settings (if available in future)

### Restore History
1. Install fresh app
2. Restore backup from Android device settings
3. Or watch history syncs with cloud backup if enabled

---

## Version Information
- **Current Version**: 1.5
- **Build Date**: June 8, 2026
- **APK Size**: 126 MB
- **Min Android**: 8.0 (API 26)
- **Target Android**: 14+ (API 34+)

## License & Disclaimer
This app is for personal and educational use only. Users are responsible for ensuring compliance with local laws regarding streaming content.

---

**Last Updated**: June 8, 2026

