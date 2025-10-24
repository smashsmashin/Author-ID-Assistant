package app.smashsmashin.authoridassistant;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Integration tests for Android Auto connectivity feature.
 * Tests the complete integration between AndroidAutoStateManager and AppState.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AndroidAutoIntegrationTest {

    private Context context;
    private AndroidAutoStateManager androidAutoStateManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        androidAutoStateManager = AndroidAutoStateManager.getInstance(context);
        
        // Reset app state
        AppState.shouldActivate = false;
        AppState.wirelessChargingActive = false;
        AppState.bluetoothTriggerActive = false;
        AppState.androidAutoTriggerActive = false;
        AppState.isKeyFobActionPending = false;
        androidAutoStateManager.setAutoConnectionState(false, null);
        AppState.cancelAllTimers();
    }

    @Test
    public void testAndroidAutoStateManagerExists() {
        assertNotNull("AndroidAutoStateManager should be initialized", androidAutoStateManager);
        assertTrue("Android Auto detection should be supported", 
                  androidAutoStateManager.isAutoDetectionSupported());
    }

    @Test
    public void testAndroidAutoStateManagerSingleton() {
        AndroidAutoStateManager instance1 = AndroidAutoStateManager.getInstance(context);
        AndroidAutoStateManager instance2 = AndroidAutoStateManager.getInstance(context);
        
        assertSame("AndroidAutoStateManager should be singleton", instance1, instance2);
    }

    @Test
    public void testAndroidAutoConnectionState() {
        assertFalse("Should start disconnected", androidAutoStateManager.isAutoConnected());
        assertNull("Connected device should be null initially", androidAutoStateManager.getConnectedAutoDevice());
        
        String deviceInfo = "Android Auto (package_state)";
        androidAutoStateManager.setAutoConnectionState(true, deviceInfo);
        
        assertTrue("Should be connected", androidAutoStateManager.isAutoConnected());
        assertEquals("Connected device info should match", deviceInfo, androidAutoStateManager.getConnectedAutoDevice());
        
        androidAutoStateManager.setAutoConnectionState(false, null);
        
        assertFalse("Should be disconnected", androidAutoStateManager.isAutoConnected());
        assertNull("Connected device should be null", androidAutoStateManager.getConnectedAutoDevice());
    }

    @Test
    public void testAppStateAndroidAutoIntegration() {
        // Initial state
        assertFalse("Android Auto trigger should be inactive initially", AppState.androidAutoTriggerActive);
        assertFalse("Should not activate initially", AppState.shouldActivate);
        assertFalse("Any trigger should be inactive initially", AppState.isAnyTriggerActive());
        
        // Activate Android Auto trigger
        AppState.updateTriggerState(context, false, false, true);
        
        assertTrue("Android Auto trigger should be active", AppState.androidAutoTriggerActive);
        assertTrue("Should activate with Android Auto trigger", AppState.shouldActivate);
        assertTrue("Any trigger should be active", AppState.isAnyTriggerActive());
        
        // Deactivate Android Auto trigger
        AppState.updateTriggerState(context, false, false, false);
        
        assertFalse("Android Auto trigger should be inactive", AppState.androidAutoTriggerActive);
        assertFalse("Should not activate without triggers", AppState.shouldActivate);
        assertFalse("Any trigger should be inactive", AppState.isAnyTriggerActive());
    }

    @Test
    public void testTripleTriggerSystem() {
        // Test all three triggers: wireless, Bluetooth, Android Auto
        AppState.updateTriggerState(context, true, true, true);
        
        assertTrue("Wireless should be active", AppState.wirelessChargingActive);
        assertTrue("Bluetooth should be active", AppState.bluetoothTriggerActive);
        assertTrue("Android Auto should be active", AppState.androidAutoTriggerActive);
        assertTrue("Should activate with all triggers", AppState.shouldActivate);
        assertTrue("Any trigger should be active", AppState.isAnyTriggerActive());
        
        // Remove triggers one by one
        AppState.updateTriggerState(context, false, true, true);
        assertTrue("Should remain active with two triggers", AppState.isAnyTriggerActive());
        
        AppState.updateTriggerState(context, false, false, true);
        assertTrue("Should remain active with Android Auto only", AppState.isAnyTriggerActive());
        
        AppState.updateTriggerState(context, false, false, false);
        assertFalse("Should be inactive with no triggers", AppState.isAnyTriggerActive());
    }

    @Test
    public void testCompleteAndroidAutoWorkflow() {
        // Step 1: Set Android Auto connection state
        String deviceInfo = "Android Auto Integration Test";
        androidAutoStateManager.setAutoConnectionState(true, deviceInfo);
        
        // Step 2: Update trigger state based on Android Auto connection
        AppState.updateTriggerState(context, false, false, androidAutoStateManager.isAutoConnected());
        
        // Step 3: Verify everything works together
        assertTrue("Android Auto should be connected", androidAutoStateManager.isAutoConnected());
        assertTrue("Android Auto trigger should be active", AppState.androidAutoTriggerActive);
        assertTrue("Should activate Key FOB", AppState.shouldActivate);
        
        // Step 4: Disconnect Android Auto
        androidAutoStateManager.setAutoConnectionState(false, null);
        AppState.updateTriggerState(context, false, false, androidAutoStateManager.isAutoConnected());
        
        // Step 5: Verify disconnection
        assertFalse("Android Auto should be disconnected", androidAutoStateManager.isAutoConnected());
        assertFalse("Android Auto trigger should be inactive", AppState.androidAutoTriggerActive);
        assertFalse("Should not activate Key FOB", AppState.shouldActivate);
    }

    @Test
    public void testAndroidAutoConnectionTransitions() {
        // Test multiple connection/disconnection cycles
        for (int i = 0; i < 5; i++) {
            String deviceInfo = "Android Auto Cycle " + i;
            
            // Connect
            androidAutoStateManager.setAutoConnectionState(true, deviceInfo);
            assertTrue("Should be connected in cycle " + i, androidAutoStateManager.isAutoConnected());
            assertEquals("Device info should match in cycle " + i, deviceInfo, 
                        androidAutoStateManager.getConnectedAutoDevice());
            
            // Update app state
            AppState.updateTriggerState(context, false, false, androidAutoStateManager.isAutoConnected());
            assertTrue("Android Auto trigger should be active in cycle " + i, AppState.androidAutoTriggerActive);
            
            // Disconnect
            androidAutoStateManager.setAutoConnectionState(false, null);
            assertFalse("Should be disconnected in cycle " + i, androidAutoStateManager.isAutoConnected());
            
            // Update app state
            AppState.updateTriggerState(context, false, false, androidAutoStateManager.isAutoConnected());
            assertFalse("Android Auto trigger should be inactive in cycle " + i, AppState.androidAutoTriggerActive);
        }
    }

    @Test
    public void testAndroidAutoWithOtherTriggers() {
        // Test Android Auto combined with other triggers
        
        // Start with wireless charging
        AppState.updateTriggerState(context, true, false, false);
        assertTrue("Should activate with wireless charging", AppState.shouldActivate);
        
        // Add Android Auto
        androidAutoStateManager.setAutoConnectionState(true, "Android Auto + Wireless");
        AppState.updateTriggerState(context, true, false, androidAutoStateManager.isAutoConnected());
        assertTrue("Should still activate with both triggers", AppState.shouldActivate);
        assertTrue("Both triggers should be active", 
                  AppState.wirelessChargingActive && AppState.androidAutoTriggerActive);
        
        // Remove wireless, keep Android Auto
        AppState.updateTriggerState(context, false, false, androidAutoStateManager.isAutoConnected());
        assertTrue("Should still activate with Android Auto only", AppState.shouldActivate);
        assertTrue("Android Auto should still be active", AppState.androidAutoTriggerActive);
        assertFalse("Wireless should be inactive", AppState.wirelessChargingActive);
        
        // Add Bluetooth
        AppState.updateTriggerState(context, false, true, androidAutoStateManager.isAutoConnected());
        assertTrue("Should activate with Android Auto + Bluetooth", AppState.shouldActivate);
        assertTrue("Both should be active", 
                  AppState.bluetoothTriggerActive && AppState.androidAutoTriggerActive);
    }

    @Test
    public void testAndroidAutoDetectionConfidence() {
        // Test detection confidence integration
        androidAutoStateManager.setAutoConnectionState(false, null);
        assertEquals("Confidence should be 0 when disconnected", 
                   0.0f, androidAutoStateManager.getDetectionConfidence(), 0.01f);
        
        androidAutoStateManager.setAutoConnectionState(true, "Test Device");
        float confidence = androidAutoStateManager.getDetectionConfidence();
        assertTrue("Confidence should be > 0 when connected", confidence > 0.0f);
        assertTrue("Confidence should be <= 1.0", confidence <= 1.0f);
    }

    @Test
    public void testAndroidAutoDetectionStatus() {
        // Test detection status integration
        androidAutoStateManager.setAutoConnectionState(false, null);
        String disconnectedStatus = androidAutoStateManager.getDetectionStatus();
        assertTrue("Status should indicate not detected", disconnectedStatus.contains("not detected"));
        
        androidAutoStateManager.setAutoConnectionState(true, "Status Test Device");
        String connectedStatus = androidAutoStateManager.getDetectionStatus();
        assertTrue("Status should indicate connected", connectedStatus.contains("connected"));
        assertTrue("Status should include confidence", connectedStatus.contains("confidence"));
    }

    @Test
    public void testAndroidAutoTriggerActivation() {
        assertFalse("Should not be pending initially", AppState.isKeyFobActionPending);
        
        AppState.triggerAuthorIDActivity(context, "android auto");
        
        assertTrue("Should be pending after Android Auto trigger", AppState.isKeyFobActionPending);
    }

    @Test
    public void testAndroidAutoTimerIntegration() {
        // Activate Android Auto trigger to potentially start timers
        AppState.updateTriggerState(context, false, false, true);
        
        // Cancel timers (should not throw exception)
        AppState.cancelAllTimers();
        
        // State should remain consistent
        assertTrue("Android Auto trigger should still be active", AppState.androidAutoTriggerActive);
    }

    @Test
    public void testErrorHandlingInAndroidAutoIntegration() {
        // Test error conditions don't break the integration
        
        // Null device info
        androidAutoStateManager.setAutoConnectionState(true, null);
        assertTrue("Should handle null device info", androidAutoStateManager.isAutoConnected());
        
        // Empty device info
        androidAutoStateManager.setAutoConnectionState(true, "");
        assertTrue("Should handle empty device info", androidAutoStateManager.isAutoConnected());
        
        // Multiple rapid state changes
        for (int i = 0; i < 50; i++) {
            boolean connected = i % 2 == 0;
            androidAutoStateManager.setAutoConnectionState(connected, connected ? ("Device " + i) : null);
            AppState.updateTriggerState(context, false, false, connected);
        }
        
        // Should complete without errors
        assertTrue("Should handle rapid state changes without errors", true);
    }

    @Test
    public void testBackwardsCompatibilityWithAndroidAuto() {
        // Test that existing two-parameter updateTriggerState still works
        AppState.updateTriggerState(context, true, false);
        
        assertTrue("Wireless should be active", AppState.wirelessChargingActive);
        assertFalse("Bluetooth should be inactive", AppState.bluetoothTriggerActive);
        assertFalse("Android Auto should remain inactive", AppState.androidAutoTriggerActive);
        
        // Manually activate Android Auto, then use old method
        AppState.androidAutoTriggerActive = true;
        AppState.updateTriggerState(context, false, true);
        
        assertFalse("Wireless should be inactive", AppState.wirelessChargingActive);
        assertTrue("Bluetooth should be active", AppState.bluetoothTriggerActive);
        assertTrue("Android Auto should remain active", AppState.androidAutoTriggerActive);
        assertTrue("Should still activate with any trigger", AppState.shouldActivate);
    }
}