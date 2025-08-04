package app.smashsmashin.authoridassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Manages user preferences for the Author ID Assistant app.
 * Uses SharedPreferences with Gson serialization following the existing app patterns.
 */
public class SettingsManager {
    private static final String TAG = "AuthorIDAssistant";
    private static final String PREFS_NAME = "AuthorIDAssistantSettings";
    private static final String SETTINGS_KEY = "app_settings";
    
    private static SettingsManager instance;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private AppSettings currentSettings;
    
    private SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new GsonBuilder().setPrettyPrinting().create();
        loadSettings();
    }
    
    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Data class representing all app settings
     */
    public static class AppSettings {
        public boolean wirelessChargingEnabled = true;
        public boolean bluetoothEnabled = false; // Future feature
        public boolean androidAutoEnabled = false; // Future feature
        public int keyFobTimeoutSeconds = 30;
        public boolean notificationsEnabled = true;
        public boolean debugLoggingEnabled = false;
        
        // Default constructor needed for Gson
        public AppSettings() {}
    }
    
    /**
     * Load settings from SharedPreferences
     */
    private void loadSettings() {
        try {
            String settingsJson = sharedPreferences.getString(SETTINGS_KEY, null);
            if (settingsJson != null) {
                currentSettings = gson.fromJson(settingsJson, AppSettings.class);
                Log.d(TAG, "Settings loaded from preferences");
            } else {
                currentSettings = new AppSettings(); // Use defaults
                Log.d(TAG, "No saved settings found, using defaults");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading settings, using defaults", e);
            currentSettings = new AppSettings();
        }
    }
    
    /**
     * Save current settings to SharedPreferences
     */
    private void saveSettings() {
        try {
            String settingsJson = gson.toJson(currentSettings);
            sharedPreferences.edit()
                .putString(SETTINGS_KEY, settingsJson)
                .apply();
            Log.d(TAG, "Settings saved to preferences");
        } catch (Exception e) {
            Log.e(TAG, "Error saving settings", e);
        }
    }
    
    // Getter methods
    public AppSettings getSettings() {
        return currentSettings;
    }
    
    public boolean isWirelessChargingEnabled() {
        return currentSettings.wirelessChargingEnabled;
    }
    
    public boolean isBluetoothEnabled() {
        return currentSettings.bluetoothEnabled;
    }
    
    public boolean isAndroidAutoEnabled() {
        return currentSettings.androidAutoEnabled;
    }
    
    public int getKeyFobTimeoutSeconds() {
        return currentSettings.keyFobTimeoutSeconds;
    }
    
    public boolean areNotificationsEnabled() {
        return currentSettings.notificationsEnabled;
    }
    
    public boolean isDebugLoggingEnabled() {
        return currentSettings.debugLoggingEnabled;
    }
    
    // Setter methods with immediate persistence
    public void setWirelessChargingEnabled(boolean enabled) {
        currentSettings.wirelessChargingEnabled = enabled;
        saveSettings();
        Log.d(TAG, "Wireless charging enabled: " + enabled);
    }
    
    public void setBluetoothEnabled(boolean enabled) {
        currentSettings.bluetoothEnabled = enabled;
        saveSettings();
        Log.d(TAG, "Bluetooth enabled: " + enabled);
    }
    
    public void setAndroidAutoEnabled(boolean enabled) {
        currentSettings.androidAutoEnabled = enabled;
        saveSettings();
        Log.d(TAG, "Android Auto enabled: " + enabled);
    }
    
    public void setKeyFobTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds < 5) {
            timeoutSeconds = 5; // Minimum 5 seconds
        } else if (timeoutSeconds > 300) {
            timeoutSeconds = 300; // Maximum 5 minutes
        }
        currentSettings.keyFobTimeoutSeconds = timeoutSeconds;
        saveSettings();
        Log.d(TAG, "Key FOB timeout set to: " + timeoutSeconds + " seconds");
    }
    
    public void setNotificationsEnabled(boolean enabled) {
        currentSettings.notificationsEnabled = enabled;
        saveSettings();
        Log.d(TAG, "Notifications enabled: " + enabled);
    }
    
    public void setDebugLoggingEnabled(boolean enabled) {
        currentSettings.debugLoggingEnabled = enabled;
        saveSettings();
        Log.d(TAG, "Debug logging enabled: " + enabled);
    }
    
    /**
     * Reset all settings to defaults
     */
    public void resetToDefaults() {
        currentSettings = new AppSettings();
        saveSettings();
        Log.d(TAG, "Settings reset to defaults");
    }
    
    /**
     * Get timeout in milliseconds for use with existing timer code
     */
    public long getKeyFobTimeoutMillis() {
        return currentSettings.keyFobTimeoutSeconds * 1000L;
    }
    
    /**
     * Check if any triggers are enabled
     */
    public boolean hasAnyTriggersEnabled() {
        return currentSettings.wirelessChargingEnabled || 
               currentSettings.bluetoothEnabled || 
               currentSettings.androidAutoEnabled;
    }
    
    /**
     * Reset the singleton instance for testing purposes
     */
    public static void resetInstanceForTesting() {
        instance = null;
    }
}