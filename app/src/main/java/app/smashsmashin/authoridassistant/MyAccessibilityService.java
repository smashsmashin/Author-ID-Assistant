package app.smashsmashin.authoridassistant;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.os.BatteryManager;
import android.app.KeyguardManager;
import android.os.Handler;
import android.os.Looper;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import androidx.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

  private static final String TAG = "AuthorIDAssistant";
  private static final String TOGGLE_BUTTON_ID = "com.dma.author.authorid:id/button";
  private static final String TARGET_CLASS = "com.dma.author.authorid.view.TagActivity";
  
  private BluetoothStateManager bluetoothStateManager;
  private BluetoothCarHelper bluetoothCarHelper;

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    if (!AppState.isKeyFobActionPending) {
      return;
    }
    // Log.d(TAG, "Accessibility event received: " + event.toString());
    if (event == null || event.getClassName() == null)
      return;

    if (TARGET_CLASS.equals(event.getClassName().toString())) {
      AppState.isKeyFobActionPending = false;

      AccessibilityNodeInfo rootNode = getRootInActiveWindow();
      findAndClickToggleButton(rootNode);

      // Close the Author ID app
      Log.d(TAG, "Performing global action back.");
      performGlobalAction(GLOBAL_ACTION_BACK);
    }
  }

  private boolean findAndClickToggleButton(AccessibilityNodeInfo rootNode) {
    boolean found = false;
    if (rootNode != null) {
      List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(TOGGLE_BUTTON_ID);
      if (nodes != null && !nodes.isEmpty()) {
        found = true;
        AccessibilityNodeInfo toggleButton = nodes.get(0);
        Log.d(TAG, "ToggleButton found. Is checked: " + toggleButton.isChecked());

        if (AppState.shouldActivate && !toggleButton.isChecked()) {
          Log.d(TAG, "ToggleButton is not checked. Performing click to activate.");
          toggleButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else if (!AppState.shouldActivate && toggleButton.isChecked()) {
          Log.d(TAG, "ToggleButton is checked. Performing click to deactivate.");
          toggleButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
          Log.d(TAG, "ToggleButton is already in the desired state. No action needed.");
        }

        // Note: recycle() is deprecated in API 30+, but still needed for backward compatibility
      } else {
        Log.w(TAG, "ToggleButton with ID " + TOGGLE_BUTTON_ID + " not found.");
      }

      // Note: recycle() is deprecated in API 30+, but still needed for backward compatibility
    }
    return found;
  }

  @Override
  public void onInterrupt() {
    Log.d(TAG, "Accessibility service interrupted.");
  }

  @Override
  @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
  protected void onServiceConnected() {
    super.onServiceConnected();
    Log.d(TAG, "Accessibility service connected (or restarted after boot).");
    
    // Initialize Bluetooth state manager and helper
    bluetoothStateManager = BluetoothStateManager.getInstance(this);
    bluetoothCarHelper = new BluetoothCarHelper(this);
    
    // Register broadcast receiver
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_SCREEN_ON);
    filter.addAction(Intent.ACTION_SCREEN_OFF);
    filter.addAction(Intent.ACTION_USER_PRESENT);
    filter.addAction(Intent.ACTION_POWER_CONNECTED);
    filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);
    
    // Bluetooth-related actions
    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
    filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

    registerReceiver(requestReceiver, filter);
    
    // Check for already connected car devices
    bluetoothStateManager.checkCurrentConnections();
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(requestReceiver);
    super.onDestroy();
  }

  private final BroadcastReceiver requestReceiver = new BroadcastReceiver() {

    private boolean isWirelessChargingServiceScope = false; // Service specific tracking
    private boolean activityLaunched = false; // Add this flag

    @Override
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", "android.permission.POST_NOTIFICATIONS"})
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d(TAG, "BroadcastReceiver: Received action: " + action);

      // Handle Bluetooth events
      if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
        handleBluetoothConnection(intent, true);
      } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
        handleBluetoothConnection(intent, false);
      } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
        handleBluetoothAdapterStateChange(intent);
      } else if (Intent.ACTION_BATTERY_CHANGED.equals(action) || Intent.ACTION_POWER_CONNECTED.equals(action)) {
        handlePowerEvents(context, intent);
      } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
        handlePowerDisconnected();
      } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
        handleUserPresent(context);
      } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
        AppState.cancelAllTimers();
      }
    }
    
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    private void handleBluetoothConnection(Intent intent, boolean connected) {
      BluetoothDevice device;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
      } else {
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
      }
      if (device != null) {
        if (bluetoothStateManager.isConnectedDeviceACar(device)) {
          bluetoothStateManager.setCarConnectionState(connected, 
              device.getName() + " (" + device.getAddress() + ")");
          
          Log.d(TAG, "Car Bluetooth " + (connected ? "connected" : "disconnected") + 
                ": " + device.getName());
          
          updateTriggerStates();
        } else if (connected && bluetoothCarHelper.shouldPromptForRegistration(device)) {
          // Show notification to register new potential car device
          // Permissions are checked by the calling method which has @RequiresPermission annotation
          @SuppressLint("MissingPermission")
          boolean triggerNotification = true;
          if (triggerNotification) {
            bluetoothCarHelper.promptToRegisterDevice(device);
          }
        }
      }
    }
    
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    private void handleBluetoothAdapterStateChange(Intent intent) {
      int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
      if (state == BluetoothAdapter.STATE_OFF) {
        bluetoothStateManager.setCarConnectionState(false, null);
        updateTriggerStates();
        Log.d(TAG, "Bluetooth adapter turned off - clearing car connection state");
      } else if (state == BluetoothAdapter.STATE_ON) {
        // Check for already connected devices when Bluetooth turns on
        bluetoothStateManager.checkCurrentConnections();
        updateTriggerStates();
        Log.d(TAG, "Bluetooth adapter turned on - checking for connected cars");
      }
    }
    
    private void handlePowerEvents(Context context, Intent intent) {
      int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
      boolean previouslyWirelessCharging = isWirelessChargingServiceScope;
      isWirelessChargingServiceScope = (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);

      if (isWirelessChargingServiceScope != previouslyWirelessCharging
          || Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
        Log.d(TAG, "BroadcastReceiver: Wireless charging: " + isWirelessChargingServiceScope);
        updateTriggerStates();
      }

      if (isWirelessChargingServiceScope) {
        if (isDeviceUnlocked(context)) {
          if (!activityLaunched) {
            Log.d(TAG, "BroadcastReceiver: Device unlocked and wireless charging. Triggering action.");
            activityLaunched = true; // Mark as launched
          } else {
            Log.d(TAG, "BroadcastReceiver: Action already triggered for this charging session.");
          }
        } else {
          Log.d(TAG, "BroadcastReceiver: Device is locked. Waiting for unlock.");
        }
      }
    }
    
    private void handlePowerDisconnected() {
      isWirelessChargingServiceScope = false;
      activityLaunched = false; // Reset the flag
      Log.d(TAG, "BroadcastReceiver: Power disconnected. Resetting state.");
      updateTriggerStates();
    }
    
    private void handleUserPresent(Context context) {
      Log.d(TAG, "BroadcastReceiver: Device unlocked by user.");

      if (Boolean.TRUE.equals(isWirelessCharging(context))) {
        isWirelessChargingServiceScope = true;
        updateTriggerStates();
      }

      if ((isWirelessChargingServiceScope || bluetoothStateManager.isCarConnected()) && !activityLaunched) {
        Log.d(TAG, "BroadcastReceiver: Device unlocked while trigger active. Triggering action.");
        activityLaunched = true; // Mark as launched
      } else {
        if (isWirelessChargingServiceScope || bluetoothStateManager.isCarConnected()) {
          Log.d(TAG, "BroadcastReceiver: Device unlocked, but action already triggered for this session.");
        } else {
          Log.d(TAG, "BroadcastReceiver: Device unlocked but no active triggers.");
        }

        // Handle case where device is unlocked but no triggers are active
        if (!AppState.isAnyTriggerActive()) {
          AppState.shouldActivate = false;
          AppState.isKeyFobActionPending = true;
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (AppState.isKeyFobActionPending) {
              AppState.isKeyFobActionPending = false;
              triggerAuthorIDActivity(context);
            }
          }, 500);
        }
      }
    }
    
    private void updateTriggerStates() {
      AppState.updateTriggerState(getApplicationContext(), 
          isWirelessChargingServiceScope, 
          bluetoothStateManager.isCarConnected());
    }

    private boolean isDeviceUnlocked(Context context) {
      KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
      return keyguardManager != null && !keyguardManager.isDeviceLocked();
    }

    private Boolean isWirelessCharging(Context context) {
      IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      Intent batteryStatus = context.registerReceiver(null, filter);
      if (batteryStatus != null) {
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;
      }
      return null;
    }

    private void triggerAuthorIDActivity(Context context) {
      if (findAndClickToggleButton(getRootInActiveWindow())) {
        Log.d(TAG, "Toggle button found. No need to trigger Author ID activity.");
        performGlobalAction(GLOBAL_ACTION_BACK);
        return;
      }
      Log.d(TAG, "Toggle button not found. Triggering Author ID activity.");
      if (MyNotificationListenerService.getInstance().isPresent()
          && MyNotificationListenerService.getInstance().get().isAuthorIDNotificationPresent()) {
        if (AppState.shouldActivate) {
          Log.d(TAG, "Key FOB activation requested, but it is already active.");
          return;
        }
      } else if (!AppState.shouldActivate) {
        Log.d(TAG, "Key FOB deactivation requested, but it is already inactive.");
        return;
      }
      AppState.triggerAuthorIDActivity(context);
    }
  };
}
