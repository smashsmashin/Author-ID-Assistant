package app.smashsmashin.authoridassistant;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import com.google.gson.Gson;
import java.util.Locale;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages Bluetooth state and car device registration for the Author ID Assistant.
 * Handles detection of specific car Bluetooth connections and maintains device registry.
 */
public class BluetoothStateManager {
    private static final String TAG = "BluetoothStateManager";
    private static final String PREFS_NAME = "bluetooth_car_devices";
    private static final String KEY_CAR_DEVICES = "car_devices";
    
    private static BluetoothStateManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private boolean isCarConnected = false;
    private String connectedCarDevice = null;
    
    /**
     * Represents a registered car Bluetooth device
     */
    public static class CarDevice {
        public String macAddress;
        public String name;
        public boolean autoDetected;
        
        public CarDevice(String macAddress, String name, boolean autoDetected) {
            this.macAddress = macAddress;
            this.name = name;
            this.autoDetected = autoDetected;
        }
    }
    
    private BluetoothStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized BluetoothStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothStateManager(context);
        }
        return instance;
    }
    
    /**
     * Checks if the connected Bluetooth device is a registered car
     */
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    public boolean isConnectedDeviceACar(BluetoothDevice device) {
        if (device == null) return false;
        
        List<CarDevice> carDevices = getRegisteredCarDevices();
        
        // Check by MAC address (most reliable)
        for (CarDevice carDevice : carDevices) {
            if (device.getAddress().equals(carDevice.macAddress)) {
                Log.d(TAG, "Connected device matches registered car: " + carDevice.name);
                return true;
            }
        }
        
        // Auto-detect potential car devices by name patterns
        String deviceName = device.getName();
        if (deviceName != null && isLikelyCarDevice(deviceName)) {
            Log.d(TAG, "Device appears to be a car (auto-detection): " + deviceName);
            // Auto-register the device
            registerCarDevice(device.getAddress(), deviceName, true);
            return true;
        }
        
        return false;
    }
    
    /**
     * Determines if a device name suggests it's a car
     */
    private boolean isLikelyCarDevice(String deviceName) {
        if (deviceName == null) return false;
        
        String lowerName = deviceName.toLowerCase(Locale.ROOT);
        String[] carIndicators = {
            "car", "auto", "vehicle", "honda", "toyota", "ford", "bmw", "audi", 
            "mercedes", "volkswagen", "nissan", "hyundai", "kia", "mazda", 
            "subaru", "lexus", "acura", "infiniti", "cadillac", "buick",
            "chevrolet", "dodge", "jeep", "chrysler", "lincoln", "tesla"
        };
        
        for (String indicator : carIndicators) {
            if (lowerName.contains(indicator)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Registers a new car device
     */
    public void registerCarDevice(String macAddress, String name, boolean autoDetected) {
        List<CarDevice> carDevices = getRegisteredCarDevices();
        
        // Check if already registered
        for (CarDevice device : carDevices) {
            if (device.macAddress.equals(macAddress)) {
                Log.d(TAG, "Device already registered: " + name);
                return;
            }
        }
        
        carDevices.add(new CarDevice(macAddress, name, autoDetected));
        saveCarDevices(carDevices);
        Log.d(TAG, "Registered new car device: " + name + " (" + macAddress + ")");
    }
    
    /**
     * Gets all registered car devices
     */
    public List<CarDevice> getRegisteredCarDevices() {
        String json = prefs.getString(KEY_CAR_DEVICES, "[]");
        Type listType = new TypeToken<List<CarDevice>>(){}.getType();
        List<CarDevice> devices = gson.fromJson(json, listType);
        return devices != null ? devices : new ArrayList<>();
    }
    
    /**
     * Saves car devices to SharedPreferences
     */
    private void saveCarDevices(List<CarDevice> devices) {
        String json = gson.toJson(devices);
        prefs.edit().putString(KEY_CAR_DEVICES, json).apply();
    }
    
    /**
     * Removes a car device from registry
     */
    public void unregisterCarDevice(String macAddress) {
        List<CarDevice> carDevices = getRegisteredCarDevices();
        carDevices.removeIf(device -> device.macAddress.equals(macAddress));
        saveCarDevices(carDevices);
        Log.d(TAG, "Unregistered car device: " + macAddress);
    }
    
    /**
     * Updates the car connection state
     */
    public void setCarConnectionState(boolean connected, String deviceInfo) {
        boolean previousState = isCarConnected;
        isCarConnected = connected;
        connectedCarDevice = connected ? deviceInfo : null;
        
        if (connected != previousState) {
            Log.d(TAG, "Car connection state changed: " + connected + 
                  (connected ? " (" + deviceInfo + ")" : ""));
        }
    }
    
    /**
     * Checks if a car is currently connected
     */
    public boolean isCarConnected() {
        return isCarConnected;
    }
    
    /**
     * Gets the currently connected car device info
     */
    public String getConnectedCarDevice() {
        return connectedCarDevice;
    }
    
    /**
     * Checks for currently connected car devices
     */
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    public boolean checkCurrentConnections() {
        BluetoothAdapter adapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
            adapter = bluetoothManager.getAdapter();
        } else {
            adapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (adapter == null || !adapter.isEnabled()) {
            setCarConnectionState(false, null);
            return false;
        }
        
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                if (isConnectedDeviceACar(device)) {
                    // Note: This is a simplified check. In practice, you'd need to check
                    // if the device is actually connected (requires additional APIs)
                    setCarConnectionState(true, device.getName() + " (" + device.getAddress() + ")");
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth permission not granted", e);
        }
        
        setCarConnectionState(false, null);
        return false;
    }
}