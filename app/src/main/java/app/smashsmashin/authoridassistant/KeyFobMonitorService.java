package app.smashsmashin.authoridassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class KeyFobMonitorService extends Service {
    
    private static final String TAG = "AuthorIDAssistant";
    private static final String CHANNEL_ID = "keyfob_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver screenStateReceiver;
    private boolean isServiceRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "KeyFobMonitorService created");
        
        // Create notification channel for Android O and above
        createNotificationChannel();
        
        // Acquire partial wake lock to detect screen state changes
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":KeyFobMonitor");
        
        // Set up screen state receiver
        setupScreenStateReceiver();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "KeyFobMonitorService started");
        
        if (!isServiceRunning) {
            startForeground(NOTIFICATION_ID, createNotification());
            
            // Acquire wake lock
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
                Log.d(TAG, "WakeLock acquired");
            }
            
            // Register receivers for screen state and power events
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            registerReceiver(screenStateReceiver, filter);
            
            isServiceRunning = true;
            Log.d(TAG, "KeyFobMonitorService now monitoring screen state");
        }
        
        // Return START_NOT_STICKY so service doesn't restart if killed
        return START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "KeyFobMonitorService destroying");
        
        if (isServiceRunning) {
            // Release wake lock
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "WakeLock released");
            }
            
            // Unregister receiver
            if (screenStateReceiver != null) {
                try {
                    unregisterReceiver(screenStateReceiver);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Receiver was not registered: " + e.getMessage());
                }
            }
            
            isServiceRunning = false;
        }
        
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    private void setupScreenStateReceiver() {
        screenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d(TAG, "Screen turned off - deactivating Key FOB");
                    deactivateKeyFobAndStop(context);
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    Log.d(TAG, "Power disconnected - deactivating Key FOB");
                    deactivateKeyFobAndStop(context);
                }
            }
            
            private void deactivateKeyFobAndStop(Context context) {
                // Cancel any pending timers
                AppState.cancelAllTimers();
                
                // Set state to deactivate Key FOB
                AppState.shouldActivate = false;
                AppState.triggerAuthorIDActivity(context);
                
                // Stop the service since Key FOB should now be deactivated
                stopSelf();
            }
        };
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Key FOB Monitor",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors screen state while Key FOB is active");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Notification.Builder builder;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        return builder
            .setContentTitle("Author ID Assistant")
            .setContentText("Key FOB is active - monitoring screen state")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setAutoCancel(false)
            .build();
    }
    
    /**
     * Static method to start the monitoring service
     */
    public static void startMonitoring(Context context) {
        Intent serviceIntent = new Intent(context, KeyFobMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Log.d(TAG, "KeyFobMonitorService start requested");
    }
    
    /**
     * Static method to stop the monitoring service
     */
    public static void stopMonitoring(Context context) {
        Intent serviceIntent = new Intent(context, KeyFobMonitorService.class);
        context.stopService(serviceIntent);
        Log.d(TAG, "KeyFobMonitorService stop requested");
    }
}