# Author-ID-Assistant

## Overview

The Author-ID-Assistant is a security-focused Android application designed to enhance the security of your Author-ID Key FOB by intelligently managing its accessibility. For critical security reasons, this assistant ensures your Key FOB is enabled only for a short, thirty-second window after your phone is placed unlocked into the car's wireless charging tray, or for the first successful unlock during a wireless charging session. For added security, the Key FOB is disabled on every unlock if the phone is not currently wireless charging.

This application operates discreetly in the background, requiring specific user-granted permissions for its functionality.

## Features

*   **Conditional Key FOB Activation:** Automatically enables Author-ID Key FOB for a limited time (30 seconds) upon specific unlock conditions related to wireless charging.
*   **Bluetooth Car Connectivity:** NEW! Automatically activates Key FOB when connected to registered car Bluetooth devices.
*   **Enhanced Security:** Disables Key FOB when not actively charging or unlocking under the specified conditions.
*   **Intelligent Car Detection:** Automatically detects and registers car Bluetooth devices with user confirmation.
*   **Background Operation:** Works seamlessly in the background without a user interface.
*   **Dual Activation Triggers:** Supports both wireless charging and Bluetooth connectivity as activation triggers.
*   **Secure by Design:** Focuses on providing enhanced security for your Author-ID Key FOB.

## Permissions Required

This application requires the following permissions to be manually granted by the user, as it does not have a user interface:

*   **Accessibility Service:** This permission is used solely to launch the "Author ID" application and to activate/deactivate the Key FOB.
*   **Notification Access:** This permission is utilized to detect the active state of the Key FOB by monitoring the notification of the foreground service. The foreground service is initiated when the Key FOB is activated and terminated when the Key FOB is deactivated.
*   **Bluetooth Access:** Required for detecting and connecting to car Bluetooth devices. This includes:
    - `BLUETOOTH` and `BLUETOOTH_ADMIN` permissions for basic Bluetooth functionality
    - `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` permissions for Android 12+ devices
*   **Notifications:** Permission to show car device registration prompts and confirmations.

**Note:** Granting these permissions is essential for the app to function as intended. Instructions on how to grant these permissions can typically be found in your device's Accessibility, Notification Access, and App Permissions settings.

## Installation

**Using the released APK:**

1.  Download the latest release APK from the [Releases](https://github.com/smashsmashin/Author-ID-Assistant/releases) page.
2.  Allow installation from unknown sources in your Android device's security settings.
3.  Install the APK.
4.  Follow the post-installation guide below to grant necessary permissions.

**Building from source:**

1.  Clone the repository:
    ```bash
    git clone https://github.com/smashsmashin/Author-ID-Assistant.git
    ```
2.  Open the project in Android Studio.
3.  Build the project.
4.  Install the generated APK on your device.
5.  Follow the post-installation guide below to grant necessary permissions.

## Post-Installation: Granting Permissions

After installing the application, you will need to manually grant the following permissions:

1.  **Accessibility Service:**
    *   Go to your device's `Settings`.
    *   Navigate to `Accessibility`.
    *   Select `Installed apps` (or `Downloaded services` on some devices).
    *   Find `Author ID Assistant` in the list of apps and enable it.
2.  **Notification Access:**
    *   Go to your device's `Settings`.
    *   Navigate to `Apps`.
    *   Tap the three-dot menu (⋮) in the top-right corner.
    *   Select `Special access`.
    *   Choose `Notification access`.
    *   Find `Author ID Assistant` in the list of apps and enable it.

## How the Bluetooth Feature Works

### Car Device Registration

1. **Automatic Detection:** When a new Bluetooth device connects to your phone, the app analyzes the device name to determine if it might be a car.

2. **User Confirmation:** If the app detects a potential car device, it shows a notification asking if you want to register it as a car device.

3. **Registration Options:**
   - Tap "Register" to add the device to your car registry
   - Tap "Dismiss" to ignore the device
   - The app won't prompt again for devices you've dismissed

4. **Smart Filtering:** The app automatically filters out common non-car devices like headphones, speakers, and keyboards to reduce unnecessary prompts.

### Automatic Car Detection

The app can automatically detect car devices based on common naming patterns including:
- Devices with "Car", "Auto", "Vehicle" in the name
- Major car manufacturer names (Honda, Toyota, BMW, etc.)
- Car-specific Bluetooth system names

### Key FOB Activation

The app activates your Key FOB when:
- **Screen is ON** (traditional behavior), OR
- **Car Bluetooth is connected** (new feature)

This dual-trigger system ensures your Key FOB is available when you're actively using your phone or when you're in your car.

### Security Features

- **Persistent Storage:** Registered car devices are saved and remembered across app restarts
- **Selective Activation:** Only registered car devices trigger Key FOB activation
- **User Control:** Full control over which devices are considered "car devices"
- **Privacy Focused:** No data is transmitted outside your device

## Future Enhancements

We are continuously working to improve the Author-ID-Assistant. Potential future enhancements include:

*   **Foreground Service with WakeLock:** ✅ COMPLETED - Start the foreground service upon Key FOB activation to enable Key FOB deactivation when the screen is shutting off, utilizing WakeLock.
*   **Bluetooth Connectivity Trigger:** ✅ COMPLETED - Activate the Key FOB when the car's Bluetooth is connected.
*   **Android Auto Integration:** Activate the Key FOB when Android Auto is connected.
*   **Customizable Settings:** Introduce a settings dialog allowing users to configure when and for how long the Key FOB should be activated.
*   **Multiple Car Support:** Enhanced support for users with multiple vehicles.

## Contributing

We welcome contributions to the Author-ID-Assistant project! If you have suggestions for improvements, new features, or bug fixes, please feel free to:

*   Fork the repository.
*   Create a new branch for your feature or fix (`git checkout -b feature/your-feature-name`).
*   Make your changes.
*   Commit your changes (`git commit -m 'Add some feature'`).
*   Push to the branch (`git push origin feature/your-feature-name`).
*   Open a Pull Request.

Please ensure your code adheres to the project's coding standards.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.