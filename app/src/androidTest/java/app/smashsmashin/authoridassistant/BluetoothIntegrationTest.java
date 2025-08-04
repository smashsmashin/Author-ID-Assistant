package app.smashsmashin.authoridassistant;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Integration tests for Bluetooth car device functionality.
 * Tests the complete flow from device detection to registration.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BluetoothIntegrationTest {

    private Context context;
    private BluetoothStateManager bluetoothStateManager;
    private BluetoothCarHelper bluetoothCarHelper;
    private CarRegistrationReceiver receiver;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        bluetoothStateManager = BluetoothStateManager.getInstance(context);
        bluetoothCarHelper = new BluetoothCarHelper(context);
        receiver = new CarRegistrationReceiver();
        
        // Clear any existing data
        context.getSharedPreferences("bluetooth_car_devices", Context.MODE_PRIVATE)
               .edit().clear().apply();
        
        // Clear notifications
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Test
    public void testCompleteCarRegistrationFlow() {
        String deviceName = "Honda Civic";
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        
        // Step 1: Initial state - no registered devices
        assertEquals("Should start with no registered devices", 
                    0, bluetoothStateManager.getRegisteredCarDevices().size());
        assertFalse("Should not be connected initially", bluetoothStateManager.isCarConnected());
        
        // Step 2: Register device through receiver (simulating user action)
        Intent registerIntent = new Intent();
        registerIntent.setAction("REGISTER_CAR_DEVICE");
        registerIntent.putExtra("device_name", deviceName);
        registerIntent.putExtra("device_address", deviceAddress);
        
        receiver.onReceive(context, registerIntent);
        
        // Step 3: Verify device was registered
        List<BluetoothStateManager.CarDevice> devices = bluetoothStateManager.getRegisteredCarDevices();
        assertEquals("Should have one registered device", 1, devices.size());
        assertEquals("Device name should match", deviceName, devices.get(0).name);
        assertEquals("Device address should match", deviceAddress, devices.get(0).macAddress);
        assertFalse("Should not be marked as auto-detected", devices.get(0).autoDetected);
        
        // Step 4: Simulate car connection state change
        bluetoothStateManager.setCarConnectionState(true, deviceName + " (" + deviceAddress + ")");
        
        // Step 5: Verify connection state
        assertTrue("Should be connected after state change", bluetoothStateManager.isCarConnected());
        assertEquals("Connected device info should match", 
                    deviceName + " (" + deviceAddress + ")", 
                    bluetoothStateManager.getConnectedCarDevice());
    }

    @Test
    public void testCarRegistrationDismissalFlow() {
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        
        // Step 1: Simulate dismissal through receiver
        Intent dismissIntent = new Intent();
        dismissIntent.setAction("DISMISS_CAR_REGISTRATION");
        dismissIntent.putExtra("device_address", deviceAddress);
        
        receiver.onReceive(context, dismissIntent);
        
        // Step 2: Verify no device was registered (dismissal successful)
        assertEquals("Should have no registered devices after dismissal", 
                    0, bluetoothStateManager.getRegisteredCarDevices().size());
    }

    @Test
    public void testMultipleCarDeviceManagement() {
        // Register multiple car devices
        String[] deviceNames = {"Honda Civic", "Toyota Camry", "BMW 3 Series"};
        String[] deviceAddresses = {"AA:BB:CC:DD:EE:FF", "BB:CC:DD:EE:FF:AA", "CC:DD:EE:FF:AA:BB"};
        
        for (int i = 0; i < deviceNames.length; i++) {
            Intent registerIntent = new Intent();
            registerIntent.setAction("REGISTER_CAR_DEVICE");
            registerIntent.putExtra("device_name", deviceNames[i]);
            registerIntent.putExtra("device_address", deviceAddresses[i]);
            
            receiver.onReceive(context, registerIntent);
        }
        
        // Verify all devices were registered
        List<BluetoothStateManager.CarDevice> devices = bluetoothStateManager.getRegisteredCarDevices();
        assertEquals("Should have three registered devices", 3, devices.size());
        
        // Verify each device is present
        for (int i = 0; i < deviceNames.length; i++) {
            boolean found = false;
            for (BluetoothStateManager.CarDevice device : devices) {
                if (device.name.equals(deviceNames[i]) && device.macAddress.equals(deviceAddresses[i])) {
                    found = true;
                    break;
                }
            }
            assertTrue("Device " + deviceNames[i] + " should be registered", found);
        }
        
        // Test unregistering one device
        bluetoothStateManager.unregisterCarDevice(deviceAddresses[1]);
        
        devices = bluetoothStateManager.getRegisteredCarDevices();
        assertEquals("Should have two devices after unregistering one", 2, devices.size());
        
        // Verify the correct device was removed
        for (BluetoothStateManager.CarDevice device : devices) {
            assertNotEquals("Unregistered device should not be present", deviceAddresses[1], device.macAddress);
        }
    }

    @Test
    public void testDataPersistenceAcrossInstances() {
        String deviceName = "Honda Civic";
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        
        // Register device with first instance
        bluetoothStateManager.registerCarDevice(deviceAddress, deviceName, false);
        
        // Create new instance (simulating app restart)
        BluetoothStateManager newInstance = BluetoothStateManager.getInstance(context);
        
        // Verify data persisted
        List<BluetoothStateManager.CarDevice> devices = newInstance.getRegisteredCarDevices();
        assertEquals("Should have one persisted device", 1, devices.size());
        assertEquals("Device name should persist", deviceName, devices.get(0).name);
        assertEquals("Device address should persist", deviceAddress, devices.get(0).macAddress);
    }

    @Test
    public void testCarConnectionStateTransitions() {
        bluetoothStateManager.setCarConnectionState(false, null);
        assertFalse("Should start disconnected", bluetoothStateManager.isCarConnected());
        assertNull("Connected device should be null", bluetoothStateManager.getConnectedCarDevice());
        
        // Connect to car
        String deviceInfo = "Honda Civic (AA:BB:CC:DD:EE:FF)";
        bluetoothStateManager.setCarConnectionState(true, deviceInfo);
        assertTrue("Should be connected", bluetoothStateManager.isCarConnected());
        assertEquals("Connected device info should match", deviceInfo, bluetoothStateManager.getConnectedCarDevice());
        
        // Disconnect from car
        bluetoothStateManager.setCarConnectionState(false, null);
        assertFalse("Should be disconnected", bluetoothStateManager.isCarConnected());
        assertNull("Connected device should be null after disconnect", bluetoothStateManager.getConnectedCarDevice());
        
        // Reconnect to different car
        String newDeviceInfo = "Toyota Camry (BB:CC:DD:EE:FF:AA)";
        bluetoothStateManager.setCarConnectionState(true, newDeviceInfo);
        assertTrue("Should be connected to new car", bluetoothStateManager.isCarConnected());
        assertEquals("New connected device info should match", newDeviceInfo, bluetoothStateManager.getConnectedCarDevice());
    }

    @Test
    public void testErrorHandlingInRegistrationFlow() {
        // Test registration with missing data
        Intent incompleteIntent = new Intent();
        incompleteIntent.setAction("REGISTER_CAR_DEVICE");
        incompleteIntent.putExtra("device_name", "Honda Civic");
        // Missing device_address
        
        receiver.onReceive(context, incompleteIntent);
        
        // Should not crash and should not register incomplete device
        assertEquals("Should have no registered devices", 0, bluetoothStateManager.getRegisteredCarDevices().size());
        
        // Test dismissal with missing data
        Intent incompleteDismissIntent = new Intent();
        incompleteDismissIntent.setAction("DISMISS_CAR_REGISTRATION");
        // Missing device_address
        
        receiver.onReceive(context, incompleteDismissIntent);
        
        // Should not crash (successful if we reach this point)
        assertEquals("Should still have no registered devices", 0, bluetoothStateManager.getRegisteredCarDevices().size());
    }

    @Test
    public void testDuplicateRegistrationPrevention() {
        String deviceName = "Honda Civic";
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        
        // Register device first time
        Intent registerIntent = new Intent();
        registerIntent.setAction("REGISTER_CAR_DEVICE");
        registerIntent.putExtra("device_name", deviceName);
        registerIntent.putExtra("device_address", deviceAddress);
        
        receiver.onReceive(context, registerIntent);
        assertEquals("Should have one device after first registration", 1, bluetoothStateManager.getRegisteredCarDevices().size());
        
        // Try to register same device again
        receiver.onReceive(context, registerIntent);
        assertEquals("Should still have only one device after duplicate registration", 1, bluetoothStateManager.getRegisteredCarDevices().size());
        
        // Verify the device details are correct
        BluetoothStateManager.CarDevice device = bluetoothStateManager.getRegisteredCarDevices().get(0);
        assertEquals("Device name should match", deviceName, device.name);
        assertEquals("Device address should match", deviceAddress, device.macAddress);
    }

    @Test
    public void testNotificationChannelCreation() {
        // Creating BluetoothCarHelper should create notification channel
        new BluetoothCarHelper(context);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            assertNotNull("Bluetooth car registration channel should exist", 
                         notificationManager.getNotificationChannel("bluetooth_car_registration"));
        }
    }
}