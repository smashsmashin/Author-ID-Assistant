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
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

  private static final String TAG = "AuthorIDAssistant";
  private static final String TOGGLE_BUTTON_ID = "com.dma.author.authorid:id/button";
  private static final String TARGET_CLASS = "com.dma.author.authorid.view.TagActivity";

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

        // Important: recycle when done
        toggleButton.recycle();
      } else {
        Log.w(TAG, "ToggleButton with ID " + TOGGLE_BUTTON_ID + " not found.");
      }

      // Important: recycle when done
      rootNode.recycle();
    }
    return found;
  }

  @Override
  public void onInterrupt() {
    Log.d(TAG, "Accessibility service interrupted.");
  }

  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    Log.d(TAG, "Accessibility service connected (or restarted after boot).");
    // Register broadcast receiver
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_SCREEN_ON);
    filter.addAction(Intent.ACTION_SCREEN_OFF);
    filter.addAction(Intent.ACTION_USER_PRESENT);
    filter.addAction(Intent.ACTION_POWER_CONNECTED);
    filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);

    registerReceiver(requestReceiver, filter);
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
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d(TAG, "BroadcastReceiver: Received action: " + action);

      if (Intent.ACTION_BATTERY_CHANGED.equals(action) || Intent.ACTION_POWER_CONNECTED.equals(action)) {
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean previouslyWirelessCharging = isWirelessChargingServiceScope;
        isWirelessChargingServiceScope = (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);

        if (isWirelessChargingServiceScope != previouslyWirelessCharging
            || Intent.ACTION_POWER_CONNECTED.equals(action)) {
          Log.d(TAG, "BroadcastReceiver: Wireless charging: " + isWirelessChargingServiceScope);
        }

        if (isWirelessChargingServiceScope) {
          if (isDeviceUnlocked(context)) {
            if (!activityLaunched) {
              Log.d(TAG, "BroadcastReceiver: Device unlocked and wireless charging. Triggering action.");
              AppState.shouldActivate = true;
              triggerAuthorIDActivity(context);
              activityLaunched = true; // Mark as launched
            } else {
              Log.d(TAG, "BroadcastReceiver: Action already triggered for this charging session.");
            }
          } else {
            Log.d(TAG, "BroadcastReceiver: Device is locked. Waiting for unlock.");
          }
        }
      } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
        isWirelessChargingServiceScope = false;
        activityLaunched = false; // Reset the flag
        Log.d(TAG, "BroadcastReceiver: Power disconnected. Resetting state.");
      } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
        Log.d(TAG, "BroadcastReceiver: Device unlocked by user.");

        if (Boolean.TRUE.equals(isWirelessCharging(context))) {
          isWirelessChargingServiceScope = true;
        }

        if (isWirelessChargingServiceScope && !activityLaunched) {
          Log.d(TAG, "BroadcastReceiver: Device unlocked while wireless charging. Triggering action.");
          AppState.shouldActivate = true;
          triggerAuthorIDActivity(context);
          activityLaunched = true; // Mark as launched
        } else {
          if (isWirelessChargingServiceScope) {
            Log.d(TAG,
                "BroadcastReceiver: Device unlocked, but action already triggered for this session.");
          } else {
            Log.d(TAG, "BroadcastReceiver: Device unlocked but not on wireless charge.");
          }

          AppState.shouldActivate = false;
          
          AppState.isKeyFobActionPending = true;
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (AppState.isKeyFobActionPending) {
              AppState.isKeyFobActionPending = false;
              triggerAuthorIDActivity(context);
            }
          }, 500);
        }
      } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
        AppState.cancelAllTimers();
      }
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
