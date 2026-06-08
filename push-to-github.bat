@echo off
REM INDRA IPTV App - GitHub Release Script (Windows Batch)
REM This script pushes code and creates a release on GitHub

echo.
echo ============================================
echo INDRA IPTV v1.5 - GitHub Release Script
echo ============================================
echo.

REM Check if git is installed
git --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Git is not installed or not in PATH
    echo Please install Git from: https://git-scm.com/download/win
    pause
    exit /b 1
)

REM Get GitHub repository URL from user
set /p GITHUB_REPO="Enter your GitHub repository URL (e.g., https://github.com/username/iptv.git): "

if "%GITHUB_REPO%"=="" (
    echo ERROR: Repository URL cannot be empty
    pause
    exit /b 1
)

echo.
echo 1/3: Setting up GitHub remote...
git remote add origin "%GITHUB_REPO%" 2>nul || git remote set-url origin "%GITHUB_REPO%"

if errorlevel 1 (
    echo ERROR: Failed to set remote
    pause
    exit /b 1
)

echo [OK] Remote set to: %GITHUB_REPO%
echo.

echo 2/3: Pushing commits to GitHub...
git push -u origin master

if errorlevel 1 (
    echo ERROR: Failed to push commits
    echo Make sure you have proper GitHub authentication set up
    echo.
    echo Authentication help:
    echo - Use Personal Access Token: https://github.com/settings/tokens
    echo - Or set up SSH key: https://docs.github.com/en/authentication/connecting-to-github-with-ssh
    pause
    exit /b 1
)

echo [OK] Commits pushed successfully
echo.

echo 3/3: Pushing release tag...
git push origin v1.5

if errorlevel 1 (
    echo ERROR: Failed to push tag
    pause
    exit /b 1
)

echo [OK] Tag pushed successfully
echo.

echo ============================================
echo SUCCESS! GitHub integration complete!
echo ============================================
echo.
echo Next steps:
echo 1. Go to: %GITHUB_REPO%/releases
echo 2. Find the v1.5 tag
echo 3. Click "Edit" or "Create Release"
echo 4. Add release notes from RELEASE_NOTES.md
echo 5. Upload APK: INDRAiptv_v1.5.260608.1938.apk
echo 6. Publish the release!
echo.
echo Your app is now on GitHub! Share the link with others.
echo.
pause

