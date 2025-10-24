package app.smashsmashin.authoridassistant;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import java.util.Locale;

/**
 * Manages Android Auto connection state detection for the Author ID Assistant.
 * Provides multiple detection methods to reliably determine when Android Auto is connected.
 */
public class AndroidAutoStateManager {
    private static final String TAG = "AndroidAutoStateManager";
    
    // Android Auto package names
    private static final String AUTO_PACKAGE_PHONE = "com.google.android.projection.gearhead";
    private static final String AUTO_PACKAGE_CAR = "com.android.car.dialer";
    
    // Detection method flags
    private static final int DETECTION_PACKAGE_STATE = 1;
    private static final int DETECTION_AUDIO_ROUTING = 2;
    private static final int DETECTION_BLUETOOTH_STATE = 4;
    
    private static AndroidAutoStateManager instance;
    private final Context context;
    private final PackageManager packageManager;
    private final AudioManager audioManager;
    private boolean isAutoConnected = false;
    private String connectedAutoDevice = null;
    
    // Detection configuration
    private int enabledDetectionMethods = DETECTION_PACKAGE_STATE | DETECTION_AUDIO_ROUTING;
    private long lastDetectionTime = 0;
    private static final long DETECTION_INTERVAL_MS = 5000; // 5 seconds
    
    private AndroidAutoStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.packageManager = context.getPackageManager();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        configureDetectionMethods();
    }
    
    public static synchronized AndroidAutoStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new AndroidAutoStateManager(context);
        }
        return instance;
    }
    
    /**
     * Reset the singleton instance for testing purposes only.
     * This method should ONLY be used in test environments.
     */
    public static synchronized void resetInstanceForTesting() {
        instance = null;
    }
    
    /**
     * Configure detection methods based on Android version and available APIs
     */
    private void configureDetectionMethods() {
        enabledDetectionMethods = DETECTION_PACKAGE_STATE;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enabledDetectionMethods |= DETECTION_AUDIO_ROUTING;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enabledDetectionMethods |= DETECTION_BLUETOOTH_STATE;
        }
        
        Log.d(TAG, "Detection methods configured: " + enabledDetectionMethods);
    }
    
    /**
     * Checks if Android Auto is currently connected
     */
    public boolean isAutoConnected() {
        return isAutoConnected;
    }
    
    /**
     * Gets information about the connected Auto device
     */
    public String getConnectedAutoDevice() {
        return connectedAutoDevice;
    }
    
    /**
     * Updates the Auto connection state
     */
    public void setAutoConnectionState(boolean connected, String deviceInfo) {
        boolean previousState = isAutoConnected;
        isAutoConnected = connected;
        connectedAutoDevice = connected ? deviceInfo : null;
        
        if (connected != previousState) {
            Log.d(TAG, "Android Auto connection state changed: " + connected + 
                  (connected ? " (" + deviceInfo + ")" : ""));
        }
    }
    
    /**
     * Performs comprehensive Android Auto connection detection
     * Uses multiple detection methods for reliability
     */
    public boolean checkAutoConnectionState() {
        // Rate limiting to preserve battery
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDetectionTime < DETECTION_INTERVAL_MS) {
            return isAutoConnected;
        }
        lastDetectionTime = currentTime;
        
        boolean detected = false;
        String detectionMethod = "none";
        
        try {
            // Method 1: Package state detection
            if ((enabledDetectionMethods & DETECTION_PACKAGE_STATE) != 0) {
                if (isAutoPackageActive()) {
                    detected = true;
                    detectionMethod = "package_state";
                    Log.d(TAG, "Android Auto detected via package state");
                }
            }
            
            // Method 2: Audio routing detection
            if (!detected && (enabledDetectionMethods & DETECTION_AUDIO_ROUTING) != 0) {
                if (isAutoAudioRouting()) {
                    detected = true;
                    detectionMethod = "audio_routing";
                    Log.d(TAG, "Android Auto detected via audio routing");
                }
            }
            
            // Method 3: Bluetooth profile detection
            if (!detected && (enabledDetectionMethods & DETECTION_BLUETOOTH_STATE) != 0) {
                if (isAutoBluetoothProfile()) {
                    detected = true;
                    detectionMethod = "bluetooth_profile";
                    Log.d(TAG, "Android Auto detected via Bluetooth profile");
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error during Android Auto detection", e);
            detected = false;
        }
        
        // Update state if detection result changed
        if (detected != isAutoConnected) {
            String deviceInfo = detected ? ("Android Auto (" + detectionMethod + ")") : null;
            setAutoConnectionState(detected, deviceInfo);
        }
        
        return detected;
    }
    
    /**
     * Detection Method 1: Check if Android Auto package is in foreground/active
     */
    private boolean isAutoPackageActive() {
        try {
            // Check if Android Auto package is installed
            packageManager.getPackageInfo(AUTO_PACKAGE_PHONE, PackageManager.GET_ACTIVITIES);
            
            // For Android 10+, we can check if the package is likely active
            // This is a simplified check - in practice, you might need more sophisticated detection
            return isPackageLikelyActive(AUTO_PACKAGE_PHONE);
            
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Android Auto package not found - feature disabled");
            return false;
        } catch (SecurityException e) {
            Log.w(TAG, "Permission denied for package detection", e);
            return false;
        }
    }
    
    /**
     * Simplified package activity detection
     * Note: This is a basic implementation - real detection would need more sophisticated methods
     */
    private boolean isPackageLikelyActive(String packageName) {
        // This is a placeholder for more sophisticated detection
        // In practice, you might use ActivityManager, UsageStatsManager, or other APIs
        // depending on your target Android version and available permissions
        
        // For now, we return false as a safe default
        // Real implementation would check running processes, recent tasks, etc.
        return false;
    }
    
    /**
     * Detection Method 2: Check audio routing patterns typical of Android Auto
     */
    private boolean isAutoAudioRouting() {
        if (audioManager == null) return false;
        
        try {
            // Check if audio is being routed to Bluetooth A2DP (common with Auto)
            boolean bluetoothA2dpOn = audioManager.isBluetoothA2dpOn();
            
            // Check if we're in a call mode that suggests car connectivity
            int audioMode = audioManager.getMode();
            boolean carAudioMode = (audioMode == AudioManager.MODE_IN_CALL || 
                                  audioMode == AudioManager.MODE_IN_COMMUNICATION);
            
            // Android Auto typically uses Bluetooth A2DP for audio
            return bluetoothA2dpOn && carAudioMode;
            
        } catch (SecurityException e) {
            Log.w(TAG, "Audio permission denied - using fallback detection", e);
            return false;
        }
    }
    
    /**
     * Detection Method 3: Check Bluetooth profiles for Auto-specific connections
     */
    private boolean isAutoBluetoothProfile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        
        try {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) return false;
            
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return false;
            }
            
            // Check for HFP (Hands-Free Profile) connections which are common with Android Auto
            // This is a simplified check - real implementation would use BluetoothProfile APIs
            return bluetoothAdapter.isEnabled() && audioManager.isBluetoothScoOn();
            
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth permission denied for Auto detection", e);
            return false;
        }
    }
    
    /**
     * Provides a confidence score for Android Auto detection
     * Higher scores indicate more reliable detection
     */
    public float getDetectionConfidence() {
        if (!isAutoConnected) return 0.0f;
        
        float confidence = 0.0f;
        int methodCount = 0;
        
        // Each detection method adds to confidence
        if ((enabledDetectionMethods & DETECTION_PACKAGE_STATE) != 0 && isAutoPackageActive()) {
            confidence += 0.4f; // Package state is moderately reliable
            methodCount++;
        }
        
        if ((enabledDetectionMethods & DETECTION_AUDIO_ROUTING) != 0 && isAutoAudioRouting()) {
            confidence += 0.3f; // Audio routing is less reliable (can have false positives)
            methodCount++;
        }
        
        if ((enabledDetectionMethods & DETECTION_BLUETOOTH_STATE) != 0 && isAutoBluetoothProfile()) {
            confidence += 0.3f; // Bluetooth profile is moderately reliable
            methodCount++;
        }
        
        // Multiple detection methods increase confidence
        if (methodCount > 1) {
            confidence += 0.2f * (methodCount - 1);
        }
        
        return Math.min(confidence, 1.0f);
    }
    
    /**
     * Checks if Android Auto detection is supported on this device
     */
    public boolean isAutoDetectionSupported() {
        return enabledDetectionMethods != 0;
    }
    
    /**
     * Gets a human-readable status of Android Auto detection
     */
    public String getDetectionStatus() {
        if (!isAutoDetectionSupported()) {
            return "Android Auto detection not supported on this device";
        }
        
        if (isAutoConnected) {
            float confidence = getDetectionConfidence();
            return String.format(Locale.US, "Android Auto connected (confidence: %.1f%%)", confidence * 100);
        } else {
            return "Android Auto not detected";
        }
    }
}