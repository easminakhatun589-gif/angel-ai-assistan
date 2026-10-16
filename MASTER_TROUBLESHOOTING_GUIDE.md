# 📘 Master Troubleshooting Guide: Git Remote & Android Studio USB Deployment

This comprehensive master guide provides terminal command sequences to re-verify Git remote tracking and a step-by-step diagnostic sequence for troubleshooting Android Studio USB debugging and driver installation failures.

---

## PART 1: Git Remote Tracking & Push Troubleshooting

If you encounter `git push` failures (such as authentication errors or remote rejection), follow these diagnostic terminal commands:

### 1. Re-verify Git Remote URL
```bash
git remote -v
```
* **Expected Result**: Displays `origin  https://github.com/<your-username>/<your-repo>.git`.
* **Fix (if incorrect)**:
  ```bash
  git remote set-url origin https://github.com/<your-username>/<your-repo>.git
  ```

### 2. Verify Branch Tracking & Upstream Link
```bash
git branch -vv
```
* **Expected Result**: Shows your local branch tracking `origin/main` (or `origin/master`).
* **Fix (if untracked)**:
  ```bash
  git push --set-upstream origin main
  ```

### 3. Check Credential Helper (PAT Authentication)
GitHub requires a **Personal Access Token (PAT)** with `repo` scope instead of your account password. Configure credential caching so you don't have to re-enter it constantly:
```bash
git config --global credential.helper cache
```

---

## PART 2: Android Studio USB Debugging & Driver Diagnostic Sequence

If your physical Android phone is not recognized by Android Studio or USB installation fails, follow this diagnostic sequence:

### Step 1: Phone Developer Options & USB Debugging
1. **Enable Developer Options**: Go to **Settings** > **About Phone** > **Software Information** and tap **Build Number** 7 times.
2. **Enable USB Debugging**: In **Settings** > **Developer Options**, toggle **USB Debugging** to **ON**.
3. **Enable USB Install & Verify**: Enable **"Install via USB"** and **"Verify apps over USB"**.
4. **Revoke & Re-authorize**: If connection is blocked, tap **"Revoke USB debugging authorizations"**, unplug and replug the cable, and tap **Allow** on the phone prompt.

### Step 2: Verify ADB (Android Debug Bridge) Connectivity
Open your computer terminal or command prompt and run:
```bash
adb devices
```
* **`device`**: ✅ Successfully connected!
* **`unauthorized`**: ⚠️ Unlock your phone and accept the RSA fingerprint prompt.
* **Empty list / No device**: ❌ Check cable, USB port, or drivers.
* **Restart ADB if stuck**:
  ```bash
  adb kill-server
  adb start-server
  adb devices
  ```

### Step 3: Install & Verify PC USB Drivers
* **Windows**: Open **Device Manager**, locate your phone (with yellow warning sign), right-click -> **Update driver** -> Browse computer -> Pick from list -> Select **Android Device** -> **Android Composite ADB Interface** (or install the official Google USB Driver).
* **macOS / Linux**: macOS requires no drivers (`brew install android-platform-tools`). Linux users ensure `udev` rules and `plugdev` group membership are configured.

### Step 4: Run from Android Studio
1. Select your physical device from the device drop-down menu next to the **Run** (green play) button in Android Studio.
2. Click **Run** (`Shift + F10`) to build, sign, and install the APK directly onto your phone.
