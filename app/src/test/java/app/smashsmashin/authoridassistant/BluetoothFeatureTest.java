package app.smashsmashin.authoridassistant;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

/**
 * Tests for the Bluetooth connectivity feature focusing on AppState integration.
 */
@RunWith(RobolectricTestRunner.class)
public class BluetoothFeatureTest {

    private Context context;
    private BluetoothStateManager bluetoothStateManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        bluetoothStateManager = BluetoothStateManager.getInstance(context);
        
        // Reset app state
        AppState.shouldActivate = false;
        AppState.wirelessChargingActive = false;
        AppState.bluetoothTriggerActive = false;
        AppState.isKeyFobActionPending = false;
        bluetoothStateManager.setCarConnectionState(false, null);
        AppState.cancelAllTimers();
    }

    @Test
    public void testBluetoothStateManagerExists() {
        assertNotNull("BluetoothStateManager should be initialized", bluetoothStateManager);
    }

    @Test
    public void testBluetoothStateManagerSingleton() {
        BluetoothStateManager instance1 = BluetoothStateManager.getInstance(context);
        BluetoothStateManager instance2 = BluetoothStateManager.getInstance(context);
        
        assertSame("BluetoothStateManager should be singleton", instance1, instance2);
    }

    @Test
    public void testCarConnectionState() {
        assertFalse("Should start disconnected", bluetoothStateManager.isCarConnected());
        assertNull("Connected device should be null initially", bluetoothStateManager.getConnectedCarDevice());
        
        String deviceInfo = "Honda Civic (AA:BB:CC:DD:EE:FF)";
        bluetoothStateManager.setCarConnectionState(true, deviceInfo);
        
        assertTrue("Should be connected", bluetoothStateManager.isCarConnected());
        assertEquals("Connected device info should match", deviceInfo, bluetoothStateManager.getConnectedCarDevice());
        
        bluetoothStateManager.setCarConnectionState(false, null);
        
        assertFalse("Should be disconnected", bluetoothStateManager.isCarConnected());
        assertNull("Connected device should be null", bluetoothStateManager.getConnectedCarDevice());
    }

    @Test
    public void testAppStateBluetoothIntegration() {
        // Initial state
        assertFalse("Bluetooth trigger should be inactive initially", AppState.bluetoothTriggerActive);
        assertFalse("Should not activate initially", AppState.shouldActivate);
        assertFalse("Any trigger should be inactive initially", AppState.isAnyTriggerActive());
        
        // Activate bluetooth trigger
        AppState.updateTriggerState(context, false, true);
        
        assertTrue("Bluetooth trigger should be active", AppState.bluetoothTriggerActive);
        assertTrue("Should activate with bluetooth trigger", AppState.shouldActivate);
        assertTrue("Any trigger should be active", AppState.isAnyTriggerActive());
        
        // Deactivate bluetooth trigger
        AppState.updateTriggerState(context, false, false);
        
        assertFalse("Bluetooth trigger should be inactive", AppState.bluetoothTriggerActive);
        assertFalse("Should not activate without triggers", AppState.shouldActivate);
        assertFalse("Any trigger should be inactive", AppState.isAnyTriggerActive());
    }

    @Test
    public void testCombinedTriggers() {
        // Test both wireless and bluetooth triggers
        AppState.updateTriggerState(context, true, true);
        
        assertTrue("Wireless should be active", AppState.wirelessChargingActive);
        assertTrue("Bluetooth should be active", AppState.bluetoothTriggerActive);
        assertTrue("Should activate with both triggers", AppState.shouldActivate);
        assertTrue("Any trigger should be active", AppState.isAnyTriggerActive());
        
        // Remove one trigger
        AppState.updateTriggerState(context, false, true);
        
        assertFalse("Wireless should be inactive", AppState.wirelessChargingActive);
        assertTrue("Bluetooth should still be active", AppState.bluetoothTriggerActive);
        assertTrue("Should still activate with one trigger", AppState.shouldActivate);
        assertTrue("Any trigger should still be active", AppState.isAnyTriggerActive());
        
        // Remove all triggers
        AppState.updateTriggerState(context, false, false);
        
        assertFalse("Wireless should be inactive", AppState.wirelessChargingActive);
        assertFalse("Bluetooth should be inactive", AppState.bluetoothTriggerActive);
        assertFalse("Should not activate without triggers", AppState.shouldActivate);
        assertFalse("No triggers should be active", AppState.isAnyTriggerActive());
    }

    @Test
    public void testTriggerActivation() {
        assertFalse("Should not be pending initially", AppState.isKeyFobActionPending);
        
        AppState.triggerAuthorIDActivity(context, "test");
        
        assertTrue("Should be pending after trigger", AppState.isKeyFobActionPending);
    }

    @Test
    public void testTimerCancellation() {
        // Activate trigger to potentially start timers
        AppState.updateTriggerState(context, false, true);
        
        // Cancel timers (should not throw exception)
        AppState.cancelAllTimers();
        
        // State should remain consistent
        assertTrue("Bluetooth trigger should still be active", AppState.bluetoothTriggerActive);
    }

    @Test
    public void testCompleteWorkflow() {
        // Step 1: Set car connection state
        String deviceInfo = "Honda Civic (AA:BB:CC:DD:EE:FF)";
        bluetoothStateManager.setCarConnectionState(true, deviceInfo);
        
        // Step 2: Update trigger state based on car connection
        AppState.updateTriggerState(context, false, bluetoothStateManager.isCarConnected());
        
        // Step 3: Verify everything works together
        assertTrue("Car should be connected", bluetoothStateManager.isCarConnected());
        assertTrue("Bluetooth trigger should be active", AppState.bluetoothTriggerActive);
        assertTrue("Should activate Key FOB", AppState.shouldActivate);
        
        // Step 4: Disconnect car
        bluetoothStateManager.setCarConnectionState(false, null);
        AppState.updateTriggerState(context, false, bluetoothStateManager.isCarConnected());
        
        // Step 5: Verify disconnection
        assertFalse("Car should be disconnected", bluetoothStateManager.isCarConnected());
        assertFalse("Bluetooth trigger should be inactive", AppState.bluetoothTriggerActive);
        assertFalse("Should not activate Key FOB", AppState.shouldActivate);
    }

    @Test
    public void testTriggerStateTransitions() {
        // Test various state transitions
        AppState.updateTriggerState(context, false, false);
        assertFalse("Should not be active initially", AppState.isAnyTriggerActive());
        
        AppState.updateTriggerState(context, false, true);
        assertTrue("Should be active with bluetooth", AppState.isAnyTriggerActive());
        
        AppState.updateTriggerState(context, true, true);
        assertTrue("Should remain active with both triggers", AppState.isAnyTriggerActive());
        
        AppState.updateTriggerState(context, true, false);
        assertTrue("Should remain active with wireless only", AppState.isAnyTriggerActive());
        
        AppState.updateTriggerState(context, false, false);
        assertFalse("Should be inactive with no triggers", AppState.isAnyTriggerActive());
    }

    @Test
    public void testBluetoothConnectionAndTriggerIntegration() {
        // Test the integration between car connection and trigger state
        bluetoothStateManager.setCarConnectionState(true, "Test Car");
        assertTrue("Car should be connected", bluetoothStateManager.isCarConnected());
        
        AppState.updateTriggerState(context, false, bluetoothStateManager.isCarConnected());
        assertTrue("Bluetooth trigger should be active when car connected", AppState.bluetoothTriggerActive);
        
        bluetoothStateManager.setCarConnectionState(false, null);
        AppState.updateTriggerState(context, false, bluetoothStateManager.isCarConnected());
        assertFalse("Bluetooth trigger should be inactive when car disconnected", AppState.bluetoothTriggerActive);
    }

    @Test
    public void testTriggerWithSourceTracking() {
        // Test different trigger sources
        AppState.triggerAuthorIDActivity(context); // Default source
        assertTrue("Should be pending", AppState.isKeyFobActionPending);
        
        AppState.isKeyFobActionPending = false; // Reset
        
        AppState.triggerAuthorIDActivity(context, "bluetooth"); // Specific source
        assertTrue("Should be pending with specific source", AppState.isKeyFobActionPending);
    }

    @Test
    public void testInitialStates() {
        // Verify all initial states are correct
        assertFalse("Initial wireless charging should be false", AppState.wirelessChargingActive);
        assertFalse("Initial bluetooth trigger should be false", AppState.bluetoothTriggerActive);
        assertFalse("Initial should activate should be false", AppState.shouldActivate);
        assertFalse("Initial Key FOB pending should be false", AppState.isKeyFobActionPending);
        assertFalse("Initial any trigger should be false", AppState.isAnyTriggerActive());
        assertFalse("Initial car connected should be false", bluetoothStateManager.isCarConnected());
        assertNull("Initial connected car should be null", bluetoothStateManager.getConnectedCarDevice());
    }
}