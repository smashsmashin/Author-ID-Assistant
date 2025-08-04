package app.smashsmashin.authoridassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.InputType;

/**
 * Settings Activity for Author ID Assistant.
 * Provides a user-friendly interface to configure app preferences.
 * Designed to work from service context with no main activity.
 */
public class SettingsActivity extends Activity {
    private static final String TAG = "AuthorIDAssistant";
    
    private SettingsManager settingsManager;
    private Switch switchWirelessCharging;
    private Switch switchBluetooth;
    private Switch switchAndroidAuto;
    private Switch switchNotifications;
    private Switch switchDebugLogging;
    private TextView textTimeoutValue;
    private View layoutTimeout;
    private View layoutResetSettings;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        Log.d(TAG, "SettingsActivity created");
        
        // Initialize settings manager
        settingsManager = SettingsManager.getInstance(this);
        
        // Initialize UI components
        initializeViews();
        
        // Set up event listeners
        setupEventListeners();
        
        // Load current settings
        loadCurrentSettings();
    }
    
    private void initializeViews() {
        switchWirelessCharging = findViewById(R.id.switch_wireless_charging);
        switchBluetooth = findViewById(R.id.switch_bluetooth);
        switchAndroidAuto = findViewById(R.id.switch_android_auto);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchDebugLogging = findViewById(R.id.switch_debug_logging);
        textTimeoutValue = findViewById(R.id.text_timeout_value);
        layoutTimeout = findViewById(R.id.layout_timeout);
        layoutResetSettings = findViewById(R.id.layout_reset_settings);
    }
    
    private void setupEventListeners() {
        // Wireless Charging Toggle
        setupToggleListener(switchWirelessCharging, 
            settingsManager::isWirelessChargingEnabled,
            settingsManager::setWirelessChargingEnabled);
        
        // Bluetooth Toggle (disabled for now)
        setupToggleListener(switchBluetooth,
            settingsManager::isBluetoothEnabled,
            settingsManager::setBluetoothEnabled);
        
        // Android Auto Toggle (disabled for now)
        setupToggleListener(switchAndroidAuto,
            settingsManager::isAndroidAutoEnabled,
            settingsManager::setAndroidAutoEnabled);
        
        // Notifications Toggle
        setupToggleListener(switchNotifications,
            settingsManager::areNotificationsEnabled,
            settingsManager::setNotificationsEnabled);
        
        // Debug Logging Toggle
        setupToggleListener(switchDebugLogging,
            settingsManager::isDebugLoggingEnabled,
            settingsManager::setDebugLoggingEnabled);
        
        // Timeout Setting Click Listener
        layoutTimeout.setOnClickListener(v -> showTimeoutDialog());
        
        // Reset Settings Click Listener
        layoutResetSettings.setOnClickListener(v -> showResetConfirmationDialog());
    }
    
    private void setupToggleListener(Switch toggle, BooleanGetter getter, BooleanSetter setter) {
        // Set up the click listener for the entire row
        View parent = (View) toggle.getParent();
        if (parent != null) {
            parent.setOnClickListener(v -> {
                if (toggle.isEnabled()) {
                    boolean newValue = !toggle.isChecked();
                    toggle.setChecked(newValue);
                    setter.set(newValue);
                    showSettingsSaved();
                }
            });
        }
        
        // Also handle direct switch clicks
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Only handle user interactions, not programmatic changes
                setter.set(isChecked);
                showSettingsSaved();
            }
        });
    }
    
    private void loadCurrentSettings() {
        SettingsManager.AppSettings settings = settingsManager.getSettings();
        
        // Load toggle states without triggering listeners
        switchWirelessCharging.setChecked(settings.wirelessChargingEnabled);
        switchBluetooth.setChecked(settings.bluetoothEnabled);
        switchAndroidAuto.setChecked(settings.androidAutoEnabled);
        switchNotifications.setChecked(settings.notificationsEnabled);
        switchDebugLogging.setChecked(settings.debugLoggingEnabled);
        
        // Load timeout value
        updateTimeoutDisplay();
        
        Log.d(TAG, "Settings loaded into UI");
    }
    
    private void updateTimeoutDisplay() {
        int timeoutSeconds = settingsManager.getKeyFobTimeoutSeconds();
        String timeoutText = getString(R.string.timeout_format, timeoutSeconds);
        textTimeoutValue.setText(timeoutText);
    }
    
    private void showTimeoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.timeout_dialog_title);
        builder.setMessage(R.string.timeout_dialog_message);
        
        // Create input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(settingsManager.getKeyFobTimeoutSeconds()));
        input.selectAll();
        builder.setView(input);
        
        // Set up buttons
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            try {
                String inputText = input.getText().toString().trim();
                if (inputText.isEmpty()) {
                    showInvalidTimeoutError();
                    return;
                }
                
                int newTimeout = Integer.parseInt(inputText);
                if (newTimeout < 5 || newTimeout > 300) {
                    showInvalidTimeoutError();
                    return;
                }
                
                settingsManager.setKeyFobTimeoutSeconds(newTimeout);
                updateTimeoutDisplay();
                showSettingsSaved();
                Log.d(TAG, "Timeout updated to: " + newTimeout + " seconds");
                
            } catch (NumberFormatException e) {
                showInvalidTimeoutError();
            }
        });
        
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showResetConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reset_confirm_title);
        builder.setMessage(R.string.reset_confirm_message);
        
        builder.setPositiveButton(R.string.reset, (dialog, which) -> {
            settingsManager.resetToDefaults();
            loadCurrentSettings(); // Refresh the UI
            Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Settings reset to defaults");
        });
        
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showInvalidTimeoutError() {
        Toast.makeText(this, R.string.invalid_timeout, Toast.LENGTH_LONG).show();
    }
    
    private void showSettingsSaved() {
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SettingsActivity destroyed");
    }
    
    // Functional interfaces for cleaner code
    private interface BooleanGetter {
        boolean get();
    }
    
    private interface BooleanSetter {
        void set(boolean value);
    }
}