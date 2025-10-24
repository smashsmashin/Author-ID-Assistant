package app.smashsmashin.authoridassistant;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppStateAndroidAutoTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        
        // Reset app state
        AppState.shouldActivate = false;
        AppState.wirelessChargingActive = false;
        AppState.bluetoothTriggerActive = false;
        AppState.androidAutoTriggerActive = false;
        AppState.isKeyFobActionPending = false;
        AppState.cancelAllTimers();
    }

    @Test
    public void testAndroidAutoTriggerActivation() {
        assertFalse("Initial Android Auto trigger should be inactive", AppState.androidAutoTriggerActive);
        assertFalse("Initial should activate should be false", AppState.shouldActivate);
        
        // Simulate Android Auto connection
        AppState.updateTriggerState(context, false, false, true);
        
        assertTrue("Android Auto trigger should be active", AppState.androidAutoTriggerActive);
        assertTrue("Should activate Key FOB when Android Auto connects", AppState.shouldActivate);
    }

    @Test
    public void testAndroidAutoTriggerDeactivation() {
        // First activate Android Auto trigger
        AppState.updateTriggerState(context, false, false, true);
        assertTrue("Android Auto trigger should be active", AppState.androidAutoTriggerActive);
        assertTrue("Should activate Key FOB", AppState.shouldActivate);
        
        // Then deactivate
        AppState.updateTriggerState(context, false, false, false);
        
        assertFalse("Android Auto trigger should be inactive", AppState.androidAutoTriggerActive);
        assertFalse("Should not activate Key FOB when Android Auto disconnects", AppState.shouldActivate);
    }

    @Test
    public void testThreeWayTriggerCombination() {
        // Test with all three triggers
        AppState.updateTriggerState(context, true, true, true);
        
        assertTrue("Wireless charging should be active", AppState.wirelessChargingActive);
        assertTrue("Bluetooth trigger should be active", AppState.bluetoothTriggerActive);
        assertTrue("Android Auto trigger should be active", AppState.androidAutoTriggerActive);
        assertTrue("Should activate Key FOB with all triggers", AppState.shouldActivate);
        assertTrue("Any trigger should be active", AppState.isAnyTriggerActive());
    }

    @Test
    public void testAnyTriggerActiveWithAndroidAuto() {
        assertFalse("No triggers should be active initially", AppState.isAnyTriggerActive());
        
        // Activate Android Auto only
        AppState.updateTriggerState(context, false, false, true);
        assertTrue("Android Auto trigger should make any trigger active", AppState.isAnyTriggerActive());
        
        // Add bluetooth
        AppState.updateTriggerState(context, false, true, true);
        assertTrue("Multiple triggers should keep any trigger active", AppState.isAnyTriggerActive());
        
        // Remove Android Auto, keep bluetooth
        AppState.updateTriggerState(context, false, true, false);
        assertTrue("Bluetooth alone should keep any trigger active", AppState.isAnyTriggerActive());
        
        // Remove all triggers
        AppState.updateTriggerState(context, false, false, false);
        assertFalse("No triggers active should make any trigger inactive", AppState.isAnyTriggerActive());
    }

    @Test
    public void testTriggerStateTransitionsWithAndroidAuto() {
        // Start with no triggers
        AppState.updateTriggerState(context, false, false, false);
        assertFalse("Should not be active initially", AppState.isAnyTriggerActive());
        assertFalse("Should not activate Key FOB initially", AppState.shouldActivate);
        
        // Add Android Auto trigger
        AppState.updateTriggerState(context, false, false, true);
        assertTrue("Should be active with Android Auto", AppState.isAnyTriggerActive());
        assertTrue("Should activate Key FOB with Android Auto", AppState.shouldActivate);
        
        // Add wireless charging (Android Auto + wireless)
        AppState.updateTriggerState(context, true, false, true);
        assertTrue("Should remain active with both triggers", AppState.isAnyTriggerActive());
        assertTrue("Should remain activated with both triggers", AppState.shouldActivate);
        
        // Remove Android Auto (wireless only)
        AppState.updateTriggerState(context, true, false, false);
        assertTrue("Should remain active with wireless only", AppState.isAnyTriggerActive());
        assertTrue("Should remain activated with wireless only", AppState.shouldActivate);
        
        // Remove wireless, add bluetooth (bluetooth only)
        AppState.updateTriggerState(context, false, true, false);
        assertTrue("Should remain active with bluetooth only", AppState.isAnyTriggerActive());
        assertTrue("Should remain activated with bluetooth only", AppState.shouldActivate);
        
        // Remove all triggers
        AppState.updateTriggerState(context, false, false, false);
        assertFalse("Should be inactive with no triggers", AppState.isAnyTriggerActive());
        assertFalse("Should not activate Key FOB with no triggers", AppState.shouldActivate);
    }

    @Test
    public void testBackwardsCompatibilityTwoParameterMethod() {
        // Test that the old two-parameter method still works
        AppState.updateTriggerState(context, true, false);
        
        assertTrue("Wireless charging should be active", AppState.wirelessChargingActive);
        assertFalse("Bluetooth should be inactive", AppState.bluetoothTriggerActive);
        assertFalse("Android Auto should remain inactive", AppState.androidAutoTriggerActive);
        assertTrue("Should activate with wireless charging", AppState.shouldActivate);
        
        // Manually set Android Auto active, then use two-parameter method
        AppState.androidAutoTriggerActive = true;
        AppState.updateTriggerState(context, false, true);
        
        assertFalse("Wireless charging should be inactive", AppState.wirelessChargingActive);
        assertTrue("Bluetooth should be active", AppState.bluetoothTriggerActive);
        assertTrue("Android Auto should remain active from previous state", AppState.androidAutoTriggerActive);
        assertTrue("Should still activate with any trigger active", AppState.shouldActivate);
    }

    @Test
    public void testTriggerSourcePriority() {
        // Test which trigger source is reported when multiple are active
        
        // Android Auto only
        AppState.updateTriggerState(context, false, false, true);
        // We can't directly test getActiveTriggerSource() as it's private, 
        // but we can verify the trigger states are correct
        assertTrue("Android Auto should be the active trigger", AppState.androidAutoTriggerActive);
        assertFalse("Other triggers should be inactive", AppState.bluetoothTriggerActive || AppState.wirelessChargingActive);
        
        // Multiple triggers - Android Auto should have priority in the private method
        AppState.updateTriggerState(context, true, true, true);
        assertTrue("All triggers should be active", 
                  AppState.androidAutoTriggerActive && AppState.bluetoothTriggerActive && AppState.wirelessChargingActive);
    }

    @Test
    public void testKeyFobActionPending() {
        assertFalse("Key FOB action should not be pending initially", AppState.isKeyFobActionPending);
        
        // Trigger Key FOB activation with Android Auto
        AppState.triggerAuthorIDActivity(context, "android auto");
        
        assertTrue("Key FOB action should be pending after trigger", AppState.isKeyFobActionPending);
    }

    @Test
    public void testRapidTriggerChangesWithAndroidAuto() {
        // Test rapid changes in trigger states
        for (int i = 0; i < 10; i++) {
            boolean wireless = i % 2 == 0;
            boolean bluetooth = i % 3 == 0;
            boolean androidAuto = i % 4 == 0;
            
            AppState.updateTriggerState(context, wireless, bluetooth, androidAuto);
            
            assertEquals("Wireless state should match iteration " + i, wireless, AppState.wirelessChargingActive);
            assertEquals("Bluetooth state should match iteration " + i, bluetooth, AppState.bluetoothTriggerActive);
            assertEquals("Android Auto state should match iteration " + i, androidAuto, AppState.androidAutoTriggerActive);
            
            boolean expectedActive = wireless || bluetooth || androidAuto;
            assertEquals("Should activate state should match for iteration " + i, 
                        expectedActive, AppState.shouldActivate);
            assertEquals("Any trigger active should match for iteration " + i,
                        expectedActive, AppState.isAnyTriggerActive());
        }
    }

    @Test
    public void testTriggerActivationSources() {
        // Test different trigger source activations
        AppState.triggerAuthorIDActivity(context); // Default source
        AppState.triggerAuthorIDActivity(context, "android auto"); // Android Auto source
        AppState.triggerAuthorIDActivity(context, "bluetooth"); // Bluetooth source
        AppState.triggerAuthorIDActivity(context, "wireless charging"); // Wireless source
        
        // These should all complete without exceptions
        assertTrue("Key FOB action should be pending after triggers", AppState.isKeyFobActionPending);
    }

    @Test
    public void testCompleteAndroidAutoWorkflow() {
        // Simulate complete workflow
        
        // Step 1: Initial state
        assertFalse("Should start with no triggers", AppState.isAnyTriggerActive());
        assertFalse("Should not activate initially", AppState.shouldActivate);
        
        // Step 2: Android Auto connects
        AppState.updateTriggerState(context, false, false, true);
        assertTrue("Android Auto should be active", AppState.androidAutoTriggerActive);
        assertTrue("Should activate Key FOB", AppState.shouldActivate);
        assertTrue("Any trigger should be active", AppState.isAnyTriggerActive());
        
        // Step 3: Add wireless charging while Android Auto is connected
        AppState.updateTriggerState(context, true, false, true);
        assertTrue("Both triggers should be active", AppState.androidAutoTriggerActive && AppState.wirelessChargingActive);
        assertTrue("Should still activate Key FOB", AppState.shouldActivate);
        
        // Step 4: Android Auto disconnects, wireless remains
        AppState.updateTriggerState(context, true, false, false);
        assertFalse("Android Auto should be inactive", AppState.androidAutoTriggerActive);
        assertTrue("Wireless should still be active", AppState.wirelessChargingActive);
        assertTrue("Should still activate Key FOB", AppState.shouldActivate);
        
        // Step 5: All triggers disconnect
        AppState.updateTriggerState(context, false, false, false);
        assertFalse("All triggers should be inactive", AppState.isAnyTriggerActive());
        assertFalse("Should not activate Key FOB", AppState.shouldActivate);
    }

    @Test
    public void testTimerCancellationWithAndroidAuto() {
        // Activate Android Auto trigger to potentially start timers
        AppState.updateTriggerState(context, false, false, true);
        
        // Cancel timers (should not throw exception)
        AppState.cancelAllTimers();
        
        // State should remain consistent
        assertTrue("Android Auto trigger should still be active", AppState.androidAutoTriggerActive);
    }

    @Test
    public void testInitialStatesWithAndroidAuto() {
        // Test the initial state of all variables including Android Auto
        assertFalse("Initial wireless charging should be false", AppState.wirelessChargingActive);
        assertFalse("Initial bluetooth trigger should be false", AppState.bluetoothTriggerActive);
        assertFalse("Initial Android Auto trigger should be false", AppState.androidAutoTriggerActive);
        assertFalse("Initial should activate should be false", AppState.shouldActivate);
        assertFalse("Initial Key FOB pending should be false", AppState.isKeyFobActionPending);
        assertFalse("Initial any trigger should be false", AppState.isAnyTriggerActive());
    }
}