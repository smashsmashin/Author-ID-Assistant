package app.smashsmashin.authoridassistant;

import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.util.Log;
import android.content.Context;

public class AppState {
    private static final String TAG = "AuthorIDAssistant";

    public static boolean isKeyFobActionPending = false;
    public static boolean shouldActivate = false;
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static int timers = 0;
    
    // Trigger source tracking - expanded to include Android Auto
    public static boolean wirelessChargingActive = false;
    public static boolean bluetoothTriggerActive = false;
    public static boolean androidAutoTriggerActive = false;

    public static void cancelAllTimers() {
        handler.removeCallbacksAndMessages(AppState.class);
        if (timers > 0) {
            Log.d(TAG, "Canceled " + timers + " timers.");
            timers = 0;
        }
    }

    private static void startTimer(Runnable r, long millis) {
        handler.postDelayed(r, AppState.class, millis);
        Log.d(TAG, "Started " + (millis / 1000) + " seconds timer.");
        timers++;
    }

    public static void triggerAuthorIDActivity(Context context) {
        triggerAuthorIDActivity(context, "unknown");
    }
    
    public static void triggerAuthorIDActivity(Context context, String triggerSource) {
        isKeyFobActionPending = true;
        Log.d(TAG, "Triggering Author ID activity from: " + triggerSource);
        proceedWithTheActivity(context);

        // ensure to stop it later
        if (shouldActivate) {
            cancelAllTimers();

            // Start a 30-seconds timer
            startTimer(() -> {
                timers--;
                Log.d(TAG, "30-seconds timer finished. Attempting to uncheck button.");
                isKeyFobActionPending = true;
                shouldActivate = false;
                proceedWithTheActivity(context);
            }, 30000);
        }
    }
    
    /**
     * Checks if any trigger source is currently active
     */
    public static boolean isAnyTriggerActive() {
        return wirelessChargingActive || bluetoothTriggerActive || androidAutoTriggerActive;
    }
    
    /**
     * Updates trigger state with three sources and determines if Key FOB should be activated
     */
    public static void updateTriggerState(Context context, boolean wirelessCharging, boolean bluetoothConnected, boolean androidAutoConnected) {
        boolean previouslyActive = isAnyTriggerActive();
        wirelessChargingActive = wirelessCharging;
        bluetoothTriggerActive = bluetoothConnected;
        androidAutoTriggerActive = androidAutoConnected;
        boolean nowActive = isAnyTriggerActive();
        
        Log.d(TAG, "Trigger state update - Wireless: " + wirelessCharging + 
              ", Bluetooth: " + bluetoothConnected + ", Android Auto: " + androidAutoConnected + 
              ", Active: " + nowActive);
        
        if (nowActive && !previouslyActive) {
            // New trigger activated
            shouldActivate = true;
            String source = getActiveTriggerSource();
            triggerAuthorIDActivity(context, source);
        } else if (!nowActive && previouslyActive) {
            // All triggers deactivated
            cancelAllTimers();
            shouldActivate = false;
            triggerAuthorIDActivity(context, "all triggers disconnected");
        }
        // If already active and still active, maintain current state
    }
    
    /**
     * Maintains backwards compatibility with existing two-parameter method
     */
    public static void updateTriggerState(Context context, boolean wirelessCharging, boolean bluetoothConnected) {
        updateTriggerState(context, wirelessCharging, bluetoothConnected, androidAutoTriggerActive);
    }
    
    /**
     * Gets the name of the currently active trigger source
     */
    private static String getActiveTriggerSource() {
        if (androidAutoTriggerActive) return "android auto";
        if (bluetoothTriggerActive) return "bluetooth";
        if (wirelessChargingActive) return "wireless charging";
        return "unknown";
    }

    private static void proceedWithTheActivity(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.dma.author.authorid", "com.dma.author.authorid.view.SplashActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}