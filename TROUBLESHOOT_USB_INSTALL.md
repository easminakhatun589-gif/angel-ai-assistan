# 📱 USB APK Installation & Troubleshooting Guide

If you are trying to install the ANGEL AI APK on your physical Android phone via USB connection or direct file transfer and encountering issues, follow this step-by-step troubleshooting guide.

---

## Step 1: Enable Developer Options & USB Debugging

By default, Android hides developer settings for security. To enable them:
1. Open your phone's **Settings**.
2. Scroll down and tap **About Phone** (or *Device Information*).
3. Tap **Software Information** (if applicable).
4. Find **Build Number** and tap it **7 times** rapidly. You will see a toast notification saying *"You are now a developer!"*
5. Go back to main **Settings**, scroll down to the newly appeared **Developer Options**.
6. Toggle **USB Debugging** to **ON**.
7. Also enable **Install via USB** and **Verify apps over USB** if available on your device manufacturer UI (Xiaomi, Samsung, Oppo, etc.).

---

## Step 2: Configure USB Connection Mode

When connecting your phone to your PC via a USB cable:
1. Swipe down your notification shade and tap the USB notification (set to **Charging this device via USB**).
2. Change the USB mode from *No data transfer* or *Charging* to **File Transfer** (MTP) or **Android Auto / Transferring Files**.
3. If a prompt appears on your phone asking *"Allow USB debugging?"*, check **"Always allow from this computer"** and tap **Allow**.

---

## Step 3: Verify USB Drivers (PC / Mac)

If your computer cannot detect your phone for adb installation or file transfer:
* **Windows**: 
  * Download and install the official [Google USB Driver](https://developer.android.com/studio/run/win-usb) or manufacturer-specific drivers (Samsung Smart Switch, Xiaomi PC Suite).
  * Open Device Manager and ensure your phone is listed under *Android Device* without any yellow warning triangles.
* **Mac / Linux**:
  * macOS does not require special drivers for MTP/ADB. Ensure Android File Transfer or `adb` command-line tools are installed (`brew install android-platform-tools`).
  * Run `adb devices` in your terminal to verify connection:
    ```bash
    adb devices
    ```
    Your device serial number should appear with the status `device`.

---

## Step 4: Resolve "App Not Installed" Errors (Signature Conflicts)

If the installation fails with *"App not installed"* or *"App not installed as package conflicts with an existing package"* when installing via USB:
* **Cause**: You have an older version of ANGEL AI already installed on your phone signed with a different key (e.g., a test build vs. a release build).
* **Fix**: 
  1. Go to your phone's app list, find **ANGEL AI**.
  2. Completely **Uninstall** the existing app from your phone.
  3. Reinstall the new APK file.

---

## Step 5: Bypassing Google Play Protect Warnings

* **Cause**: Sideloaded APKs built outside Google Play Store may trigger Play Protect warning dialogs ("Blocked by Play Protect" or "Unsafe app blocked").
* **Fix**:
  1. Tap **"More details"** on the warning prompt.
  2. Tap **"Install anyway (unsafe)"** to proceed with installation.
