# 🔬 Android Studio USB Installation & ADB Diagnostic Guide

If you are trying to install or run your app directly from Android Studio to a physical phone via USB and the device is not recognized or installation fails, use this step-by-step diagnostic guide.

---

## Step 1: Check Phone Developer Options & USB Debugging

1. **Enable Developer Options**: Go to **Settings** > **About Phone** > **Software Information** and tap **Build Number** 7 times.
2. **Enable USB Debugging**: Go back to **Settings** > **Developer Options** and toggle **USB Debugging** to **ON**.
3. **Enable USB Security / Install via USB**: Ensure options like **"Install via USB"** and **"Verify apps over USB"** are enabled (especially required on Xiaomi/MIUI, Realme, Vivo, and Huawei devices).
4. **Revoke USB Debugging Authorizations** (if prompted before and denied): In Developer Options, tap **"Revoke USB debugging authorizations"**, unplug the cable, plug it back in, and accept the new prompt on your phone screen.

---

## Step 2: Verify ADB (Android Debug Bridge) Connectivity

Android Studio communicates with your physical phone using `adb`. Check if your system detects the device:

1. Open your computer's terminal (macOS/Linux) or Command Prompt / PowerShell (Windows).
2. Run the following command to check connected devices:
   ```bash
   adb devices
   ```
3. **Interpreting output**:
   * **`List of devices attached`** with a serial number and **`device`** status: ✅ Connected successfully!
   * **`unauthorized`**: ⚠️ Your phone is locked or you haven't accepted the RSA prompt on your phone screen. Unlock your phone and tap **Allow**.
   * **`no permissions`** (Linux): 🛑 udev rules need configuration.
   * **Empty list**: ❌ The computer does not detect the phone connection (check cable, USB port, or drivers).

4. **Restart ADB Server** if connection is stuck:
   ```bash
   adb kill-server
   adb start-server
   adb devices
   ```

---

## Step 3: Install & Verify PC USB Drivers

If `adb devices` shows an empty list or your device shows up with a warning symbol in Device Manager:

### For Windows:
1. Right-click the Windows Start button and select **Device Manager**.
2. Look for an unknown device or your phone under **Portable Devices** or **Other Devices** (often showing a yellow triangle).
3. Right-click the device -> **Update driver** -> **Browse my computer for drivers** -> **Let me pick from a list...** -> Select **Android Device** -> **Android Composite ADB Interface**.
4. Alternatively, download the official **Google USB Driver** ZIP from developer.android.com, extract it, and point Device Manager to the extracted folder (`android_winusb.inf`).

### For macOS / Linux:
* **macOS**: No drivers needed. Ensure Android Studio has full disk access or platform-tools are up to date (`brew install android-platform-tools`).
* **Linux**: Ensure your user is in the `plugdev` group and `adb` has proper `udev` permissions.

---

## Step 4: Select Device Target in Android Studio

1. In Android Studio, look at the device drop-down menu next to the **Run** (green play) button.
2. Ensure your physical phone model is selected instead of an emulator.
3. Click **Run** (`Shift + F10`). Android Studio will build the APK, sign it with the debug key, and install it directly onto your physical phone via ADB.
