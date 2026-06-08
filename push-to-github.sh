#!/bin/bash
# INDRA IPTV App - GitHub Release Script
# This script pushes code and creates a release on GitHub

echo "============================================"
echo "INDRA IPTV v1.5 - GitHub Release Script"
echo "============================================"
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "❌ Git is not installed. Please install Git first."
    exit 1
fi

# Get GitHub repository URL from user
read -p "Enter your GitHub repository URL (e.g., https://github.com/username/iptv.git): " GITHUB_REPO

if [ -z "$GITHUB_REPO" ]; then
    echo "❌ Repository URL cannot be empty"
    exit 1
fi

echo ""
echo "📦 Setting up GitHub remote..."

# Add GitHub remote
git remote add origin "$GITHUB_REPO" 2>/dev/null || git remote set-url origin "$GITHUB_REPO"

echo "✅ Remote set to: $GITHUB_REPO"
echo ""

echo "📤 Pushing commits to GitHub..."
git push -u origin master

if [ $? -ne 0 ]; then
    echo "❌ Failed to push commits"
    echo "Make sure you have proper authentication set up"
    exit 1
fi

echo "✅ Commits pushed successfully"
echo ""

echo "🏷️  Pushing release tag..."
git push origin v1.5

if [ $? -ne 0 ]; then
    echo "❌ Failed to push tag"
    exit 1
fi

echo "✅ Tag pushed successfully"
echo ""

echo "✨ GitHub integration complete!"
echo ""
echo "Next steps:"
echo "1. Go to: $GITHUB_REPO/releases"
echo "2. Find the v1.5 tag"
echo "3. Click 'Edit' or 'Create Release'"
echo "4. Add release notes from RELEASE_NOTES.md"
echo "5. Upload the APK file: INDRAiptv_v1.5.260608.1938.apk"
echo "6. Publish the release!"
echo ""

