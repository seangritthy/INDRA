# Changelog

All notable changes to this project will be documented in this file.

## [1.5] - 2026-06-08

### Added
- **Watch History Feature**: Track all movies and TV shows you've watched
  - New "Watched Movies" screen accessible from main app bar
  - Automatic tracking when viewing movie details
  - Display watch timestamp for each entry
  - Delete individual history items
  - Clear entire watch history
  - Most recently watched items appear first

- **New Files**:
  - `WatchHistory.kt` - Data model for watch history items
  - `PlayedMoviesScreen.kt` - UI screen for displaying watch history

### Changed
- Removed intro video - app now launches directly to main screen
- Updated default streaming server from vidsrc.xyz to vidsrc.to
- Improved app startup performance
- Enhanced movie detail screen watch history integration

### Fixed
- Fixed video URL construction for streaming providers
- Removed reliance on missing indra_video.mp4 intro file
- Improved error handling in movie detail screen

### Updated Dependencies
- Latest Jetpack Compose libraries
- Updated Material 3 components

## [1.4] - 2026-04-02

### Added
- Initial release with core features
- Movie Hub with TMDB integration
- IPTV channels support
- Multiple streaming servers
- Picture-in-Picture mode
- Dark mode support

### Features
- Search movies and TV shows
- Browse trending, popular, top-rated content
- Stream live IPTV channels
- Multiple language support (English, Khmer)

---

## Installation & Usage

### Requirements
- Android 8.0+
- Minimum 100 MB free storage
- Active internet connection

### How to Watch
1. Launch the app
2. Browse Movies or IPTV Channels
3. Select content to view
4. Choose streaming server if needed
5. Watch history is automatically tracked

### Watch History
- Tap the history icon (clock) in the top app bar
- View all watched movies with timestamps
- Tap a movie to view details again
- Long press to delete individual entries
- Use "Clear All" to delete entire history

---

## Known Issues
- Some streaming servers may be region-blocked
- Content availability varies by region
- Require sufficient bandwidth for smooth playback

## Future Improvements
- Watch history persistence (save to device)
- Watched progress tracking (resume from where you left off)
- Favorites/Bookmarks
- Multi-language subtitles
- Advanced search filters
- Download for offline viewing

---

## Technology Stack
- **Language**: Kotlin
- **Framework**: Jetpack Compose
- **Architecture**: MVVM with Coroutines
- **Data**: TMDB API, iptv-org/iptv project
- **Min SDK**: API 26 (Android 8.0)

---

## License
This project is provided for educational and personal use only.

