package app.smashsmashin.authoridassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Manages notifications for the Author ID Assistant app.
 * Provides settings access through notification actions since there's no main activity.
 */
public class NotificationManager {
    private static final String TAG = "AuthorIDAssistant";
    private static final String CHANNEL_ID = "author_id_assistant_channel";
    private static final String SETTINGS_CHANNEL_ID = "author_id_assistant_settings_channel";
    private static final int SETTINGS_NOTIFICATION_ID = 1001;
    
    private final Context context;
    private final android.app.NotificationManager notificationManager;
    private final SettingsManager settingsManager;
    
    public NotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (android.app.NotificationManager) 
            context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.settingsManager = SettingsManager.getInstance(context);
        
        createNotificationChannels();
    }
    
    /**
     * Create notification channels for Android O and above
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main channel for app notifications
            NotificationChannel mainChannel = new NotificationChannel(
                CHANNEL_ID,
                "Author ID Assistant",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            mainChannel.setDescription("Notifications for Author ID Assistant operations");
            mainChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mainChannel);
            
            // Settings channel for settings access
            NotificationChannel settingsChannel = new NotificationChannel(
                SETTINGS_CHANNEL_ID,
                "Settings Access",
                android.app.NotificationManager.IMPORTANCE_LOW
            );
            settingsChannel.setDescription("Quick access to app settings");
            settingsChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(settingsChannel);
            
            Log.d(TAG, "Notification channels created");
        }
    }
    
    /**
     * Show a persistent notification with settings access
     */
    public void showSettingsNotification() {
        if (!settingsManager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled, not showing settings notification");
            return;
        }
        
        // Create intent for settings activity
        Intent settingsIntent = new Intent(context, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, SETTINGS_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        
        builder.setSmallIcon(android.R.drawable.ic_menu_manage) // Using system icon
            .setContentTitle(context.getString(R.string.settings_notification_title))
            .setContentText(context.getString(R.string.settings_notification_text))
            .setContentIntent(settingsPendingIntent)
            .setOngoing(false) // Allow user to dismiss
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_preferences,
                context.getString(R.string.settings_notification_action),
                settingsPendingIntent
            );
        
        // Set priority for older Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        
        notificationManager.notify(SETTINGS_NOTIFICATION_ID, builder.build());
        Log.d(TAG, "Settings notification shown");
    }
    
    /**
     * Hide the settings notification
     */
    public void hideSettingsNotification() {
        notificationManager.cancel(SETTINGS_NOTIFICATION_ID);
        Log.d(TAG, "Settings notification hidden");
    }
    
    /**
     * Show a status notification (e.g., when Key FOB is activated/deactivated)
     */
    public void showStatusNotification(String title, String message, boolean isOngoing) {
        if (!settingsManager.areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled, not showing status notification");
            return;
        }
        
        // Create intent for settings activity as fallback action
        Intent settingsIntent = new Intent(context, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(settingsPendingIntent)
            .setOngoing(isOngoing)
            .setAutoCancel(!isOngoing);
        
        // Set priority for older Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(isOngoing ? Notification.PRIORITY_DEFAULT : Notification.PRIORITY_LOW);
        }
        
        notificationManager.notify(SETTINGS_NOTIFICATION_ID + 1, builder.build());
        Log.d(TAG, "Status notification shown: " + title);
    }
    
    /**
     * Hide all notifications from this app
     */
    public void hideAllNotifications() {
        notificationManager.cancelAll();
        Log.d(TAG, "All notifications hidden");
    }
    
    /**
     * Check if notifications are enabled at the system level
     */
    public boolean areNotificationsEnabledAtSystemLevel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return notificationManager.areNotificationsEnabled();
        }
        return true; // Assume enabled for older versions
    }
    
    /**
     * Get current notification settings summary for display
     */
    public String getNotificationStatusSummary() {
        if (!settingsManager.areNotificationsEnabled()) {
            return "Disabled in app settings";
        } else if (!areNotificationsEnabledAtSystemLevel()) {
            return "Disabled in system settings";
        } else {
            return "Enabled";
        }
    }
}