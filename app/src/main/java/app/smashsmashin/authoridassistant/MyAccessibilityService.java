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

    if (event == null) return;

    // We care about events from the target package
    CharSequence packageName = event.getPackageName();
    if (packageName == null || !"com.dma.author.authorid".equals(packageName.toString())) {
      return;
    }

    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
    if (rootNode == null) return;

    if (findAndClickToggleButton(rootNode)) {
      Log.d(TAG, "Action performed successfully. Clearing pending flag.");
      AppState.isKeyFobActionPending = false;

      // Close the Author ID app after a short delay to ensure the action is processed
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        Log.d(TAG, "Performing global action back.");
        performGlobalAction(GLOBAL_ACTION_BACK);
      }, 500);
    }
    rootNode.recycle();
  }

  private boolean findAndClickToggleButton(AccessibilityNodeInfo rootNode) {
    if (rootNode == null) return false;

    List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(TOGGLE_BUTTON_ID);
    if (nodes != null && !nodes.isEmpty()) {
      AccessibilityNodeInfo toggleButton = nodes.get(0);
      boolean isChecked = toggleButton.isChecked();
      Log.d(TAG, "ToggleButton found. Is checked: " + isChecked);

      boolean success = false;
      if (AppState.shouldActivate && !isChecked) {
        Log.d(TAG, "ToggleButton is not checked. Performing click to activate.");
        success = toggleButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      } else if (!AppState.shouldActivate && isChecked) {
        Log.d(TAG, "ToggleButton is checked. Performing click to deactivate.");
        success = toggleButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      } else {
        Log.d(TAG, "ToggleButton is already in the desired state. No action needed.");
        success = true;
      }

      toggleButton.recycle();
      return success;
    } else {
      // Log only once in a while or use a different level to avoid spam
      // Log.v(TAG, "ToggleButton with ID " + TOGGLE_BUTTON_ID + " not found in current window.");
      return false;
    }
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
        boolean currentlyWireless = (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);
        boolean previouslyWireless = isWirelessChargingServiceScope;
        isWirelessChargingServiceScope = currentlyWireless;

        if (isWirelessChargingServiceScope != previouslyWireless || Intent.ACTION_POWER_CONNECTED.equals(action)) {
          Log.d(TAG, "BroadcastReceiver: Wireless charging status: " + isWirelessChargingServiceScope);
        }

        if (isWirelessChargingServiceScope) {
          if (isDeviceUnlocked(context)) {
            if (!activityLaunched) {
              Log.d(TAG, "BroadcastReceiver: Device unlocked and wireless charging. Triggering activation.");
              AppState.shouldActivate = true;
              triggerAuthorIDActivity(context);
              activityLaunched = true;
            }
          } else {
            Log.d(TAG, "BroadcastReceiver: Device is locked. Waiting for unlock.");
          }
        }
      } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
        Log.d(TAG, "BroadcastReceiver: Power disconnected. Ensuring deactivation.");
        isWirelessChargingServiceScope = false;
        activityLaunched = false;
        AppState.shouldActivate = false;
        triggerAuthorIDActivity(context);
      } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
        Log.d(TAG, "BroadcastReceiver: Device unlocked by user.");

        boolean wireless = Boolean.TRUE.equals(isWirelessCharging(context));
        isWirelessChargingServiceScope = wireless;

        if (wireless && !activityLaunched) {
          Log.d(TAG, "BroadcastReceiver: Device unlocked while wireless charging. Triggering activation.");
          AppState.shouldActivate = true;
          triggerAuthorIDActivity(context);
          activityLaunched = true;
        } else if (AppState.isKeyFobActionPending || !wireless) {
          // If deactivation (or activation) was pending but didn't finish, or if we're not charging,
          // ensure we're in the right state.
          if (!wireless) {
              AppState.shouldActivate = false;
              activityLaunched = false;
          }
          
          Log.d(TAG, "BroadcastReceiver: Ensuring state on unlock. shouldActivate=" + AppState.shouldActivate);
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            triggerAuthorIDActivity(context);
          }, 500);
        }
      } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
        // We don't necessarily want to cancel timers on screen off,
        // because we want deactivation to happen even if screen is off.
        // AppState.cancelAllTimers();
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
      AccessibilityNodeInfo rootNode = getRootInActiveWindow();
      if (findAndClickToggleButton(rootNode)) {
        Log.d(TAG, "Toggle button found and handled. No need to trigger Author ID activity.");
        if (rootNode != null) rootNode.recycle();

        // If we handled it, we can return to whatever was before after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
          performGlobalAction(GLOBAL_ACTION_BACK);
        }, 500);
        return;
      }
      if (rootNode != null) rootNode.recycle();

      Log.d(TAG, "Toggle button not found. Checking if state already matches via notification.");
      boolean isAlreadyActive = MyNotificationListenerService.getInstance()
          .map(MyNotificationListenerService::isAuthorIDNotificationPresent)
          .orElse(false);

      if (AppState.shouldActivate && isAlreadyActive) {
          Log.d(TAG, "Key FOB activation requested, but it is already active (notification found).");
          AppState.isKeyFobActionPending = false;
          return;
      } else if (!AppState.shouldActivate && !isAlreadyActive) {
          Log.d(TAG, "Key FOB deactivation requested, but it is already inactive (no notification).");
          AppState.isKeyFobActionPending = false;
          return;
      }

      Log.d(TAG, "Triggering Author ID activity.");
      AppState.triggerAuthorIDActivity(context);
    }
  };
}
