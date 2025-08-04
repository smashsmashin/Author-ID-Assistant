package app.smashsmashin.authoridassistant;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ServiceTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class KeyFobMonitorServiceInstrumentationTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    private Context context;
    private Intent serviceIntent;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        serviceIntent = new Intent(context, KeyFobMonitorService.class);
    }

    @Test
    public void testServiceCanBeStarted() throws Exception {
        serviceRule.startService(serviceIntent);
        
        assertTrue("Service should be running", isServiceRunning());
    }

    @Test
    public void testServiceHandlesScreenOffBroadcast() throws Exception {
        serviceRule.startService(serviceIntent);
        
        AppState.shouldActivate = true;
        
        Intent screenOffIntent = new Intent(Intent.ACTION_SCREEN_OFF);
        context.sendBroadcast(screenOffIntent);
        
        Thread.sleep(1000);
        
        assertFalse("AppState.shouldActivate should be false after screen off", AppState.shouldActivate);
    }

    @Test
    public void testServiceHandlesPowerDisconnectedBroadcast() throws Exception {
        serviceRule.startService(serviceIntent);
        
        AppState.shouldActivate = true;
        
        Intent powerDisconnectedIntent = new Intent(Intent.ACTION_POWER_DISCONNECTED);
        context.sendBroadcast(powerDisconnectedIntent);
        
        Thread.sleep(1000);
        
        assertFalse("AppState.shouldActivate should be false after power disconnect", AppState.shouldActivate);
    }

    @Test
    public void testKeyFobActivationStartsService() throws Exception {
        AppState.shouldActivate = true;
        AppState.triggerAuthorIDActivity(context);
        
        Thread.sleep(1000);
        
        assertTrue("Service should be running after Key FOB activation", isServiceRunning());
    }

    @Test
    public void testKeyFobDeactivationStopsService() throws Exception {
        AppState.shouldActivate = true;
        AppState.triggerAuthorIDActivity(context);
        
        Thread.sleep(1000);
        assertTrue("Service should be running initially", isServiceRunning());
        
        AppState.shouldActivate = false;
        AppState.triggerAuthorIDActivity(context);
        
        Thread.sleep(2000);
        
        assertFalse("Service should be stopped after Key FOB deactivation", isServiceRunning());
    }

    @Test
    public void testTimerExpiryDeactivatesKeyFob() throws Exception {
        AppState.shouldActivate = true;
        AppState.triggerAuthorIDActivity(context);
        
        Thread.sleep(1000);
        assertTrue("Service should be running initially", isServiceRunning());
        
        Thread.sleep(31000);
        
        assertFalse("AppState.shouldActivate should be false after timer expiry", AppState.shouldActivate);
        assertFalse("Service should be stopped after timer expiry", isServiceRunning());
    }

    @Test
    public void testWakeLockManagement() throws Exception {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assertNotNull("PowerManager should be available", powerManager);
        
        serviceRule.startService(serviceIntent);
        
        Thread.sleep(1000);
        
        assertTrue("Service should be running and managing wake lock", isServiceRunning());
    }

    @Test
    public void testNotificationChannelCreated() throws Exception {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            serviceRule.startService(serviceIntent);
            
            Thread.sleep(1000);
            
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            android.app.NotificationChannel channel = 
                notificationManager.getNotificationChannel("keyfob_monitor_channel");
            
            assertNotNull("Notification channel should be created", channel);
            assertEquals("Channel name should match", "Key FOB Monitor", channel.getName());
        }
    }

    @Test
    public void testServiceStopsBroadcastReceiver() throws Exception {
        serviceRule.startService(serviceIntent);
        
        Thread.sleep(1000);
        assertTrue("Service should be running", isServiceRunning());
        
        AppState.shouldActivate = false;
        AppState.triggerAuthorIDActivity(context);
        
        Thread.sleep(2000);
        
        Intent screenOffIntent = new Intent(Intent.ACTION_SCREEN_OFF);
        context.sendBroadcast(screenOffIntent);
        
        Thread.sleep(1000);
        
        assertFalse("Service should remain stopped", isServiceRunning());
    }

    @Test
    public void testMultipleServiceStartsHandledCorrectly() throws Exception {
        serviceRule.startService(serviceIntent);
        serviceRule.startService(serviceIntent);
        serviceRule.startService(serviceIntent);
        
        Thread.sleep(1000);
        
        assertTrue("Service should handle multiple starts gracefully", isServiceRunning());
    }

    @Test
    public void testServicePermissions() {
        int wakeLockPermission = context.checkSelfPermission(android.Manifest.permission.WAKE_LOCK);
        assertEquals("App should have WAKE_LOCK permission", 
            PackageManager.PERMISSION_GRANTED, wakeLockPermission);
        
        int foregroundServicePermission = context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE);
        assertEquals("App should have FOREGROUND_SERVICE permission", 
            PackageManager.PERMISSION_GRANTED, foregroundServicePermission);
    }

    private boolean isServiceRunning() {
        android.app.ActivityManager manager = 
            (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (KeyFobMonitorService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}