# Installation on Shelly Wall Display

Complete guide for preparing and installing Hapanels on a Shelly Wall Display device.

---

### **Step 0. Preparing Shelly Wall Display**
You must first exit the manufacturer's default application (kiosk mode).

Detailed instructions for unlocking the device and accessing the Android operating system are available in the **[ShellyElevate Wiki](https://github.com/RapierXbox/ShellyElevate/wiki/Installation)** guide. The entire unlocking and configuration process can be performed via wired connection (USB ADB) or wireless connection (Wireless ADB).

---

### **Step 0a. Changing the default launcher**
The factory Shelly software (`Stargate`) acts as the default launcher and will automatically reclaim control of the screen. To prevent this, install a lightweight replacement launcher and disable the stock overlay:

1. Download **[Ultra Small Launcher](https://blakadder.com/assets/files/ultra-small-launcher.apk)**.
2. Install it and disable the stock launcher using ADB commands:
```bash
adb install ultra-small-launcher.apk
adb shell pm disable cloud.shelly.stargate
```
3. Press the physical or virtual Home button on the device. Android will prompt you to select a default home screen — choose the installed **Ultra Small Launcher** and confirm as "Always".

---

### **Step 1. Download and install the application**
Download the `.apk` file directly from the **[GitHub Releases](https://github.com/tw-zs/Hapanels/releases)** section and install it on the device.

```bash
adb install -r -d app-github-debug.apk
```

---

### **Step 2. Brightness permission**
To allow the panel to automatically control screen brightness on the Shelly device, grant the required system permission via ADB:

```bash
adb shell appops set com.github.twzs.hapanels android:write_settings allow
```

---

### **Step 3. Connect to Home Assistant**
1. **Sign in:** Open the application on the device and connect to your Home Assistant instance via OAuth (the OAuth redirect URI is `r1ha://auth-callback`).
2. **Enable MQTT (Optional):** If you run an MQTT broker in Home Assistant, Hapanels will automatically detect the panel and add its physical relay and sensors to your system.
