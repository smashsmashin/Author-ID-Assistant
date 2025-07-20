# Author-ID-Assistant

## Overview

The Author-ID-Assistant is a security-focused Android application designed to enhance the security of your Author-ID Key FOB by intelligently managing its accessibility. For critical security reasons, this assistant ensures your Key FOB is enabled only for a short, thirty-second window after your phone is placed unlocked into the car's wireless charging tray, or for the first successful unlock during a wireless charging session. For added security, the Key FOB is disabled on every unlock if the phone is not currently wireless charging.

This application operates discreetly in the background, requiring specific user-granted permissions for its functionality.

## Features

*   **Conditional Key FOB Activation:** Automatically enables Author-ID Key FOB for a limited time (30 seconds) upon specific unlock conditions related to wireless charging.
*   **Enhanced Security:** Disables Key FOB when not actively charging or unlocking under the specified conditions.
*   **Background Operation:** Works seamlessly in the background without a user interface.
*   **Secure by Design:** Focuses on providing enhanced security for your Author-ID Key FOB.

## Permissions Required

This application requires the following permissions to be manually granted by the user, as it does not have a user interface:

*   **Accessibility Service:** This permission is used solely to launch the "Author ID" application and to activate/deactivate the Key FOB.
*   **Notification Access:** This permission is utilized to detect the active state of the Key FOB by monitoring the notification of the foreground service. The foreground service is initiated when the Key FOB is activated and terminated when the Key FOB is deactivated.

**Note:** Granting these permissions is essential for the app to function as intended. Instructions on how to grant these permissions can typically be found in your device's Accessibility and Notification Access settings.

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
    
## Future Enhancements

We are continuously working to improve the Author-ID-Assistant. Potential future enhancements include:

*   **Foreground Service with WakeLock:** Start the foreground service upon Key FOB activation to enable Key FOB deactivation when the screen is shutting off, utilizing WakeLock.
*   **Bluetooth Connectivity Trigger:** Activate the Key FOB when the car's Bluetooth is connected.
*   **Android Auto Integration:** Activate the Key FOB when Android Auto is connected.
*   **Customizable Settings:** Introduce a settings dialog allowing users to configure when and for how long the Key FOB should be activated.

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