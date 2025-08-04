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
    
    // Settings and notification managers
    private static SettingsManager settingsManager;
    private static NotificationManager notificationManager;

    /**
     * Initialize the AppState with settings and notification managers
     */
    public static void initialize(Context context) {
        if (settingsManager == null) {
            settingsManager = SettingsManager.getInstance(context);
        }
        if (notificationManager == null) {
            notificationManager = new NotificationManager(context);
        }
        Log.d(TAG, "AppState initialized with settings support");
    }

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
        // Initialize if not already done
        initialize(context);
        
        isKeyFobActionPending = true;
        proceedWithTheActivity(context);

        // ensure to stop it later
        if (shouldActivate) {
            cancelAllTimers();

            // Use configurable timeout from settings instead of hardcoded 30 seconds
            long timeoutMillis = settingsManager != null ? 
                settingsManager.getKeyFobTimeoutMillis() : 30000;
            
            startTimer(() -> {
                timers--;
                Log.d(TAG, "Timeout timer finished (" + (timeoutMillis / 1000) + "s). Attempting to uncheck button.");
                isKeyFobActionPending = true;
                shouldActivate = false;
                proceedWithTheActivity(context);
            }, timeoutMillis);
        }
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
    
    /**
     * Show the settings notification for easy access
     */
    public static void showSettingsNotification(Context context) {
        initialize(context);
        if (notificationManager != null) {
            notificationManager.showSettingsNotification();
        }
    }
    
    /**
     * Hide all notifications
     */
    public static void hideAllNotifications(Context context) {
        initialize(context);
        if (notificationManager != null) {
            notificationManager.hideAllNotifications();
        }
    }
    
    /**
     * Get the settings manager instance
     */
    public static SettingsManager getSettingsManager(Context context) {
        initialize(context);
        return settingsManager;
    }
    
    /**
     * Get the notification manager instance
     */
    public static NotificationManager getNotificationManager(Context context) {
        initialize(context);
        return notificationManager;
    }
    
    /**
     * Reset all static state for testing purposes
     */
    public static void resetForTesting() {
        isKeyFobActionPending = false;
        shouldActivate = false;
        cancelAllTimers();
        settingsManager = null;
        notificationManager = null;
    }
}