# 🔌 Android Studio USB Installation Troubleshooting Guide

Follow this focused troubleshooting guide if you are experiencing issues installing or running your ANGEL AI app via USB from Android Studio to your physical mobile device.

---

## 1. Enabling USB Debugging in Developer Options

By default, Android phones protect devices by disabling debugging tools. To enable USB Debugging:
1. Open your phone **Settings** -> **About Phone** (or *System Info*).
2. Tap **Build Number** **7 times** until you see the message *"You are now a developer!"*.
3. Go back to main **Settings** -> **Developer Options**.
4. Scroll down and toggle **USB Debugging** to **ON**.
5. *Optional/Recommended*: Enable **"Install via USB"** and **"Verify apps over USB"** if your manufacturer requires it.
6. When you connect your phone to your PC via USB, a prompt will appear asking *"Allow USB debugging?"*. Check **"Always allow from this computer"** and tap **Allow**.

---

## 2. Verifying ADB Connectivity with `adb devices`

Android Studio relies on ADB (Android Debug Bridge) to communicate with your mobile device. To verify connection:
1. Open your PC terminal or Command Prompt.
2. Run the command:
   ```bash
   adb devices
   ```
3. Check the status in the output:
   * **`device`**: ✅ Connected and authorized! Ready to install apps from Android Studio.
   * **`unauthorized`**: ⚠️ Your phone is locked or waiting for you to accept the RSA prompt. Unlock your phone and tap **Allow**.
   * **Empty list (`List of devices attached` with nothing below it)**: ❌ PC does not recognize the phone (check cable, USB port, or drivers).
4. **Fix stuck connections by restarting ADB**:
   ```bash
   adb kill-server
   adb start-server
   adb devices
   ```

---

## 3. Ensuring Correct USB Drivers are Installed

If `adb devices` returns an empty list or your device shows a yellow warning triangle in Device Manager:
* **Windows**:
  1. Open **Device Manager** on Windows.
  2. Locate your phone (often under *Other devices* or *Portable devices*).
  3. Right-click -> **Update driver** -> **Browse my computer for drivers** -> **Let me pick from a list...** -> Choose **Android Device** -> **Android Composite ADB Interface**.
  4. If needed, download the official **Google USB Driver** package from the Android developer website and install `android_winusb.inf`.
* **macOS**:
  * macOS natively supports ADB. Ensure your platform tools are up to date (`brew install android-platform-tools`).
* **Linux**:
  * Ensure your user account belongs to the `plugdev` group and that udev rules for Android devices are active.
