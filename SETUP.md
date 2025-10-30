# Setup (Manjaro/Arch + Android Studio 2025 Oct build)

1. Install Android Studio (uses bundled JDK 17).
2. Manjaro: `sudo pacman -S android-udev` then `sudo udevadm control --reload-rules && sudo udevadm trigger`.
3. Enable USB debugging on your phone, connect it, and run `adb devices` (accept the prompt on phone).
4. Open this repo in Android Studio and let it sync.
5. Run ▶️ to install the app.

Optional: Install GitHub Copilot via Settings > Plugins > Marketplace.
