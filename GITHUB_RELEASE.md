# GitHub Release Instructions

## Summary of Release v1.5

This release includes the new Watch History feature and stability improvements.

### Files in This Release

1. **INDRAiptv_v1.5.260608.1938.apk** (126 MB)
   - The compiled Android application
   - Ready to install on Android 8.0+ devices
   - Contains all features including watch history tracking

2. **Documentation Files**:
   - `README.md` - Main project documentation
   - `INSTALL.md` - Complete installation guide
   - `CHANGELOG.md` - Version history and changes
   - `RELEASE_NOTES.md` - Release details and features

3. **Source Code**:
   - Complete Kotlin source code
   - Gradle build configuration
   - All resources and assets

## Release Information

- **Version**: 1.5
- **Release Date**: June 8, 2026
- **Build Date**: June 8, 2026
- **APK Size**: 126 MB
- **Minimum Android**: 8.0 (API 26)

## What's New in v1.5

### Features Added
✅ Watch History Tracking
✅ Played Movies Screen
✅ History Management (delete, clear)
✅ Automatic watch tracking

### Improvements
✅ Removed intro video
✅ Updated streaming server (vidsrc.xyz → vidsrc.to)
✅ Better error handling
✅ Faster app startup

### Files Modified
- `MainActivity.kt` - Removed intro video, added history navigation
- `MovieDetailScreen.kt` - Added watch history tracking
- `IptvViewModel.kt` - Added watch history state management
- New: `WatchHistory.kt` - Watch history model
- New: `PlayedMoviesScreen.kt` - Watch history UI

## How to Create GitHub Release

### Option 1: Via GitHub Web Interface

1. Go to: https://github.com/yourusername/iptv/releases
2. Click "Create a new release"
3. Fill in:
   - Tag version: `v1.5`
   - Release title: `INDRA IPTV v1.5 - Watch History Release`
   - Description: Copy from `RELEASE_NOTES.md`
4. Upload `INDRAiptv_v1.5.260608.1938.apk`
5. Click "Publish release"

### Option 2: Via Git Command Line

```bash
# Push commits
git push origin master

# Push tags
git push origin v1.5

# Or create release directly (if using GitHub CLI)
gh release create v1.5 \
  --title "INDRA IPTV v1.5 - Watch History Release" \
  --notes-file RELEASE_NOTES.md \
  INDRAiptv_v1.5.260608.1938.apk
```

## Installation from Release

Users can download the APK directly from the release page and install it on their Android devices.

## Testing Checklist Before Release

- [x] App builds successfully
- [x] No critical errors
- [x] Watch history feature works
- [x] Server selection functional
- [x] App launches without intro
- [x] IPTV channels load
- [x] Movie search works
- [x] History tracking automatic

## Known Issues

- Some deprecation warnings (non-critical)
- Minor type mismatch warnings from JSON parsing

## Future Releases

Planned features:
- Cloud sync for watch history
- Resume playback from last position
- Favorites/Bookmarks
- Advanced filtering
- Offline download support

## Support

For issues:
- Check `INSTALL.md` for troubleshooting
- Review `README.md` for features
- See `CHANGELOG.md` for version history

---

**Release Date**: June 8, 2026  
**Version**: 1.5.260608.1938

