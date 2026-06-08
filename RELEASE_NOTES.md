# INDRA IPTV App - Release v1.5 (June 8, 2026)

## 🎉 Version 1.5 - Watch History Feature Release

### ✨ New Features
- **📺 Watch History Tracking** - Track all movies and shows you've watched
  - Automatic tracking when viewing movie details
  - Shows watch timestamp for each entry
  - Dedicated "Played Movies" screen accessible from main menu
  
- **🗑️ History Management**
  - Delete individual watch history entries
  - Clear entire watch history with one tap
  - Most recently watched appear first
  
- **⚡ App Launch Improvements**
  - Removed intro video - app launches directly to main screen
  - Faster app startup
  - No missing file errors

### 🔧 Server Updates
- Updated default streaming server from vidsrc.xyz to vidsrc.to
- All streaming servers now point to active domains
- Better fallback options when primary server is unavailable

### 🐛 Bug Fixes
- Fixed movie detail screen layout
- Improved video URL construction for all streaming providers
- Enhanced error handling for missing resources

### 📱 Technical Details
**File Size**: 126 MB  
**Minimum Android**: 8.0+  
**Target Android**: Latest (API 34+)

**Architecture Changes**:
- Added WatchHistory.kt model for tracking watched content
- Added PlayedMoviesScreen.kt for history display
- Enhanced IptvViewModel with watch history state management
- Integrated watch history tracking in MovieDetailScreen

### 📦 Installation Instructions

1. Download `INDRAiptv_v1.5.260608.1938.apk`
2. Enable "Unknown Sources" in Android Settings (Security)
3. Open the APK file and tap Install
4. Launch the app

### 🚀 Features Included

**Core Features**:
- ✅ Movie Hub with search, trending, popular, top-rated, upcoming
- ✅ TV Shows with season/episode selection
- ✅ IPTV Channels organized by category, country, region, language
- ✅ Picture-in-Picture mode
- ✅ Dark/Light mode support
- ✅ Watch history tracking (NEW)

**Streaming**:
- Multiple server options (vidsrc.to, 2embed.cc, embed.su, etc.)
- Automatic fallback to alternative servers
- Direct links to streaming content

**Supported Content**:
- Live IPTV channels
- Movies (thousands from TMDB)
- TV Series and Episodes
- Drama content via web viewer

### 🌐 Data Sources
- **Movies/TV**: The Movie Database (TMDB)
- **Channels**: iptv-org/iptv project
- **Streaming**: Various free streaming providers

### ⚠️ Important Notes
- This app requires active internet connection
- Streaming availability depends on regional restrictions
- For educational and personal use only

### 🔒 Privacy
- No user data collection
- All operations are local to your device
- Watch history stored only on your device

### 📝 Known Limitations
- Some streaming servers may be geo-blocked
- Availability of content may vary by region
- Requires sufficient bandwidth for streaming

### 🤝 Support
For issues or suggestions, check the README.md file included in the repository.

---

**Build Date**: June 8, 2026  
**Build ID**: INDRAiptv_v1.5.260608.1938  
**SHA256**: Available in release assets

