package app.smashsmashin.authoridassistant;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.BatteryManager;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;

public class PowerReceiver extends BroadcastReceiver {
  private static final String TAG = "AuthorIDAssistant";

  private boolean isWirelessChargingServiceScope = false; // Service specific tracking
  private boolean activityLaunched = false; // Add this flag

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

    if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
      Log.d(TAG, "Power connected");
    } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
      Log.d(TAG, "Power disconnected");
    } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
      Log.d(TAG, "Bettry changed");
    } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
      Log.d(TAG, "Screen ON");
    } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
      Log.d(TAG, "Screen OFF");
    } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
      Log.d(TAG, "User unlocked");
    }

    Log.d(TAG, "Programmatic Receiver: Received action: " + action);

    KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

    if (Intent.ACTION_BATTERY_CHANGED.equals(action)
        || Intent.ACTION_POWER_CONNECTED.equals(action)) {
      int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
      boolean previouslyWirelessCharging = isWirelessChargingServiceScope;
      isWirelessChargingServiceScope = (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);

      if (isWirelessChargingServiceScope != previouslyWirelessCharging
          || Intent.ACTION_POWER_CONNECTED.equals(action)) {
        Log.d(TAG, "Programmatic Receiver: Wireless charging: " + isWirelessChargingServiceScope);
      }

      if (isWirelessChargingServiceScope) {
        if (keyguardManager != null && !keyguardManager.isDeviceLocked()) {
          // Device is unlocked and wireless charging is active.
          if (!activityLaunched) {
            Log.d(TAG, "Programmatic Receiver: Device unlocked and wireless charging. Triggering action.");
            triggerAuthorIDActivity(context);
            activityLaunched = true; // Mark as launched
          } else {
            Log.d(TAG, "Programmatic Receiver: Action already triggered for this charging session.");
          }
        } else {
          Log.d(TAG, "Programmatic Receiver: Device is locked. Waiting for unlock.");
        }
      }
    } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
      isWirelessChargingServiceScope = false;
      activityLaunched = false; // Reset the flag
      Log.d(TAG, "Programmatic Receiver: Power disconnected. Resetting state.");
    } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
      Log.d(TAG, "Programmatic Receiver: Device unlocked by user.");
      // Check charging status again on unlock to be sure
      IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      Intent batteryStatus = context.registerReceiver(null, filter);
      if (batteryStatus != null) {
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isWirelessChargingServiceScope = (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);
      }

      if (isWirelessChargingServiceScope && !activityLaunched) {
        Log.d(TAG, "Programmatic Receiver: Device unlocked while wireless charging. Triggering action.");
        triggerAuthorIDActivity(context);
        activityLaunched = true; // Mark as launched
      } else if (isWirelessChargingServiceScope) {
        Log.d(TAG, "Programmatic Receiver: Device unlocked, but action already triggered for this session.");
      } else {
        Log.d(TAG, "Programmatic Receiver: Device unlocked but not on wireless charge.");
      }
    }
  }

  private void triggerAuthorIDActivity(Context context) {
    try {
      AppState.isKeyFobActionPending = true;
      AppState.shouldActivate = true;
      Intent intent = new Intent();
      intent.setComponent(new ComponentName("com.dma.author.authorid", "com.dma.author.authorid.view.SplashActivity"));
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
          | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      context.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Log.e(TAG, e.getMessage());
    }
  }

}
