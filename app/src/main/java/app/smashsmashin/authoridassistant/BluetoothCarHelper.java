package app.smashsmashin.authoridassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import java.util.Locale;
import java.util.Set;

/**
 * Helper class for managing Bluetooth car device registration and notifications.
 * Provides user-friendly ways to register car devices when they connect.
 */
public class BluetoothCarHelper {
    private static final String TAG = "BluetoothCarHelper";
    private static final String CHANNEL_ID = "bluetooth_car_registration";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    
    public BluetoothCarHelper(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothStateManager = BluetoothStateManager.getInstance(context);
        createNotificationChannel();
    }
    
    /**
     * Creates notification channel for car device registration prompts
     */
    private void createNotificationChannel() {
        CharSequence name = "Car Device Registration";
        String description = "Notifications for registering new car Bluetooth devices";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
    
    /**
     * Shows a notification asking user if they want to register a Bluetooth device as a car
     */
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", "android.permission.POST_NOTIFICATIONS"})
    public void promptToRegisterDevice(BluetoothDevice device) {
        if (device == null || device.getName() == null) return;
        
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();
        
        Log.d(TAG, "Prompting to register device: " + deviceName + " (" + deviceAddress + ")");
        
        // Create intent for registering the device
        Intent registerIntent = new Intent();
        registerIntent.setAction("REGISTER_CAR_DEVICE");
        registerIntent.putExtra("device_name", deviceName);
        registerIntent.putExtra("device_address", deviceAddress);
        
        PendingIntent registerPendingIntent = PendingIntent.getBroadcast(
            context, 
            deviceAddress.hashCode(), 
            registerIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Create intent for dismissing the notification
        Intent dismissIntent = new Intent();
        dismissIntent.setAction("DISMISS_CAR_REGISTRATION");
        dismissIntent.putExtra("device_address", deviceAddress);
        
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
            context, 
            (deviceAddress + "_dismiss").hashCode(), 
            dismissIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Car Device Connected")
            .setContentText("Register '" + deviceName + "' as a car device for Key FOB activation?")
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("The Bluetooth device '" + deviceName + "' has connected. Would you like to register it as a car device to automatically activate your Key FOB when connected?"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_input_add, "Register", registerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_BASE + deviceAddress.hashCode(), builder.build());
    }
    
    /**
     * Registers a device as a car and dismisses the notification
     */
    public void registerDeviceAsCar(String deviceName, String deviceAddress) {
        bluetoothStateManager.registerCarDevice(deviceAddress, deviceName, false);
        dismissRegistrationNotification(deviceAddress);
        
        // Show confirmation notification
        showRegistrationConfirmation(deviceName);
        
        Log.d(TAG, "Registered device as car: " + deviceName + " (" + deviceAddress + ")");
    }
    
    /**
     * Dismisses the registration notification for a device
     */
    public void dismissRegistrationNotification(String deviceAddress) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID_BASE + deviceAddress.hashCode());
    }
    
    /**
     * Shows a confirmation notification that the device was registered
     */
    private void showRegistrationConfirmation(String deviceName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Car Device Registered")
            .setContentText("'" + deviceName + "' is now registered as a car device")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(5000); // Auto-dismiss after 5 seconds
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_BASE + deviceName.hashCode() + 1, builder.build());
    }
    
    /**
     * Checks if a connected device should prompt for registration
     */
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    public boolean shouldPromptForRegistration(BluetoothDevice device) {
        if (device == null || device.getName() == null) return false;
        
        // Don't prompt if already registered
        if (bluetoothStateManager.isConnectedDeviceACar(device)) {
            return false;
        }
        
        // Don't prompt for devices that clearly aren't cars
        String deviceName = device.getName().toLowerCase(Locale.ROOT);
        String[] nonCarIndicators = {
            "headphone", "headset", "speaker", "earbuds", "airpods", "beats",
            "keyboard", "mouse", "trackpad", "phone", "tablet", "watch",
            "fitness", "scale", "thermometer", "printer"
        };
        
        for (String indicator : nonCarIndicators) {
            if (deviceName.contains(indicator)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets a list of currently connected devices that might be cars
     */
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    public void checkForPotentialCarDevices() {
        BluetoothAdapter adapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
            adapter = bluetoothManager.getAdapter();
        } else {
            adapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (adapter == null || !adapter.isEnabled()) {
            return;
        }
        
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                if (shouldPromptForRegistration(device)) {
                    // In a real implementation, you'd check if the device is actually connected
                    // For now, we'll just log potential car devices
                    Log.d(TAG, "Potential car device found: " + device.getName() + 
                          " (" + device.getAddress() + ")");
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth permission not granted for checking devices", e);
        }
    }
}