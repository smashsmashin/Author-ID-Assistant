package app.smashsmashin.authoridassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver to handle car device registration actions from notifications.
 * Processes user responses to register or dismiss car device registration prompts.
 */
public class CarRegistrationReceiver extends BroadcastReceiver {
    private static final String TAG = "CarRegistrationReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);
        
        if ("REGISTER_CAR_DEVICE".equals(action)) {
            String deviceName = intent.getStringExtra("device_name");
            String deviceAddress = intent.getStringExtra("device_address");
            
            if (deviceName != null && deviceAddress != null) {
                BluetoothCarHelper helper = new BluetoothCarHelper(context);
                helper.registerDeviceAsCar(deviceName, deviceAddress);
                Log.d(TAG, "User registered car device: " + deviceName);
            }
            
        } else if ("DISMISS_CAR_REGISTRATION".equals(action)) {
            String deviceAddress = intent.getStringExtra("device_address");
            
            if (deviceAddress != null) {
                BluetoothCarHelper helper = new BluetoothCarHelper(context);
                helper.dismissRegistrationNotification(deviceAddress);
                Log.d(TAG, "User dismissed registration for device: " + deviceAddress);
            }
        }
    }
}