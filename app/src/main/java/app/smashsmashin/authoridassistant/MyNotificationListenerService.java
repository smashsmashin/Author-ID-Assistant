package app.smashsmashin.authoridassistant;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.KeyguardManager;
import android.os.BatteryManager;
import android.app.Notification;

public class MyNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "AuthorIDAssistant";
    private static final String TARGET_APP_PACKAGE = "com.dma.author.authorid";

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
                    if (!isDeviceLocked(context)) {
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
                    triggerAuthorIDActivity(context);
                }

            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                AppState.cancelAllTimers();
            }
        }

        private boolean isDeviceLocked(Context context) {
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

        private boolean isAuthorIDNotificationPresent() {
            for (StatusBarNotification sbn : getActiveNotifications()) {
                if (TARGET_APP_PACKAGE.equals(sbn.getPackageName())) {
                    Notification notification = sbn.getNotification();
                    if (notification != null && notification.contentIntent != null) {
                        String title = notification.extras.getString(Notification.EXTRA_TITLE);
                        String text = notification.extras.getString(Notification.EXTRA_TEXT);
                        if ("Author ID".equals(title) && "Key FOB activated".equals(text)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void triggerAuthorIDActivity(Context context) {
            if (isAuthorIDNotificationPresent()) {
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

    @Override
    public void onCreate() {
        super.onCreate();

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
}
