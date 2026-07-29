# Installation on an Android Tablet

Instructions for installing and configuring the Hapanels application on a traditional wall-mounted Android tablet (Android 9 or newer required).

---

### **Step 1. Download and install the application**
Download the `.apk` file directly from the **[GitHub Releases](https://github.com/tw-zs/Hapanels/releases)** section and install it on the tablet.

You can do this directly via a web browser on the tablet or using a computer with ADB:
```bash
adb install -r -d app-github-debug.apk
```

---

### **Step 2. Brightness permission (Optional)**
If your tablet supports automatic brightness control via Android system settings and you want Hapanels to manage it, grant the permission using ADB:

```bash
adb shell appops set com.github.twzs.hapanels android:write_settings allow
```

---

### **Step 3. Connect to Home Assistant**
1. **Sign in:** Open the application on the tablet and connect to your Home Assistant instance via OAuth (the OAuth redirect URI is `r1ha://auth-callback`).
2. **Enable MQTT (Optional):** If you run an MQTT broker in Home Assistant, Hapanels will automatically detect the panel and add its basic states and screen parameters to your system.
