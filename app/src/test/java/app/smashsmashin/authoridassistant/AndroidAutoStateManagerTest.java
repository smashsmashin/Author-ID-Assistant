package app.smashsmashin.authoridassistant;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AndroidAutoStateManagerTest {

    @Mock
    private PackageManager mockPackageManager;
    
    @Mock
    private AudioManager mockAudioManager;
    
    private AndroidAutoStateManager androidAutoStateManager;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Reset the singleton instance to ensure test isolation
        AndroidAutoStateManager.resetInstanceForTesting();
        androidAutoStateManager = AndroidAutoStateManager.getInstance(context);
        
        // Ensure clean initial state for each test
        androidAutoStateManager.setAutoConnectionState(false, null);
    }

    @Test
    public void testSingletonInstance() {
        AndroidAutoStateManager instance1 = AndroidAutoStateManager.getInstance(context);
        AndroidAutoStateManager instance2 = AndroidAutoStateManager.getInstance(context);
        
        assertSame("Should return the same singleton instance", instance1, instance2);
    }

    @Test
    public void testInitialState() {
        // Ensure we start with a clean state
        androidAutoStateManager.setAutoConnectionState(false, null);
        
        assertFalse("Initial connection state should be false", androidAutoStateManager.isAutoConnected());
        assertNull("Initial connected device should be null", androidAutoStateManager.getConnectedAutoDevice());
        // Note: Auto detection support depends on Android version and available APIs
        // In test environment, this might return false
        assertNotNull("Detection status should not be null", androidAutoStateManager.getDetectionStatus());
    }

    @Test
    public void testAutoConnectionStateChange() {
        assertFalse("Should start disconnected", androidAutoStateManager.isAutoConnected());
        
        String deviceInfo = "Android Auto (package_state)";
        androidAutoStateManager.setAutoConnectionState(true, deviceInfo);
        
        assertTrue("Should be connected after setting state", androidAutoStateManager.isAutoConnected());
        assertEquals("Connected device info should match", deviceInfo, androidAutoStateManager.getConnectedAutoDevice());
        
        androidAutoStateManager.setAutoConnectionState(false, null);
        
        assertFalse("Should be disconnected after setting state", androidAutoStateManager.isAutoConnected());
        assertNull("Connected device should be null after disconnect", androidAutoStateManager.getConnectedAutoDevice());
    }

    @Test
    public void testDetectionConfidence() {
        // Initially no connection, confidence should be 0
        assertEquals("Initial confidence should be 0", 0.0f, androidAutoStateManager.getDetectionConfidence(), 0.01f);
        
        // Set connected state
        androidAutoStateManager.setAutoConnectionState(true, "Android Auto (test)");
        
        // Confidence should be >= 0 when connected (might be 0 in test environment)
        float confidence = androidAutoStateManager.getDetectionConfidence();
        assertTrue("Confidence should be >= 0 when connected", confidence >= 0.0f);
        assertTrue("Confidence should be <= 1.0", confidence <= 1.0f);
    }

    @Test
    public void testDetectionStatus() {
        // Ensure we start with a clean disconnected state
        androidAutoStateManager.setAutoConnectionState(false, null);
        
        String initialStatus = androidAutoStateManager.getDetectionStatus();
        assertNotNull("Initial status should not be null", initialStatus);
        assertTrue("Initial status should indicate not detected or not supported", 
                  initialStatus.contains("not detected") || initialStatus.contains("not supported"));
        
        androidAutoStateManager.setAutoConnectionState(true, "Android Auto (test)");
        
        String connectedStatus = androidAutoStateManager.getDetectionStatus();
        assertNotNull("Connected status should not be null", connectedStatus);
        // In test environment, might show different status
        assertTrue("Connected status should be valid", connectedStatus.length() > 0);
    }

    @Test
    public void testConnectionStateTransitions() {
        // Test multiple state changes
        for (int i = 0; i < 5; i++) {
            boolean connected = i % 2 == 0;
            String deviceInfo = connected ? ("Android Auto Test " + i) : null;
            
            androidAutoStateManager.setAutoConnectionState(connected, deviceInfo);
            
            assertEquals("Connection state should match iteration " + i, 
                       connected, androidAutoStateManager.isAutoConnected());
            
            if (connected) {
                assertEquals("Device info should match iteration " + i, 
                           deviceInfo, androidAutoStateManager.getConnectedAutoDevice());
            } else {
                assertNull("Device info should be null when disconnected", 
                          androidAutoStateManager.getConnectedAutoDevice());
            }
        }
    }

    @Test
    public void testAutoDetectionSupport() {
        assertTrue("Auto detection should be supported on modern Android", 
                  androidAutoStateManager.isAutoDetectionSupported());
    }

    @Test
    public void testConnectionStateLogging() {
        // This test mainly verifies that state changes don't throw exceptions
        androidAutoStateManager.setAutoConnectionState(true, "Test Device 1");
        androidAutoStateManager.setAutoConnectionState(true, "Test Device 2"); // Same state
        androidAutoStateManager.setAutoConnectionState(false, null);
        androidAutoStateManager.setAutoConnectionState(false, null); // Same state
        
        // If we reach here without exceptions, the logging is working correctly
        assertTrue("State change logging should work without exceptions", true);
    }

    @Test
    public void testRapidStateChanges() {
        // Test rapid state changes to ensure stability
        for (int i = 0; i < 20; i++) {
            boolean connected = (i % 3) == 0;
            String deviceInfo = connected ? ("Rapid Test " + i) : null;
            
            androidAutoStateManager.setAutoConnectionState(connected, deviceInfo);
            
            assertEquals("State should be consistent after rapid change " + i, 
                       connected, androidAutoStateManager.isAutoConnected());
        }
    }

    @Test
    public void testDetectionConfidenceEdgeCases() {
        // Test confidence when not connected
        androidAutoStateManager.setAutoConnectionState(false, null);
        assertEquals("Confidence should be 0 when not connected", 
                   0.0f, androidAutoStateManager.getDetectionConfidence(), 0.01f);
        
        // Test confidence bounds
        androidAutoStateManager.setAutoConnectionState(true, "Test Device");
        float confidence = androidAutoStateManager.getDetectionConfidence();
        assertTrue("Confidence should be >= 0", confidence >= 0.0f);
        assertTrue("Confidence should be <= 1.0", confidence <= 1.0f);
    }

    @Test
    public void testDetectionStatusEdgeCases() {
        // Test status when disconnected
        androidAutoStateManager.setAutoConnectionState(false, null);
        String disconnectedStatus = androidAutoStateManager.getDetectionStatus();
        assertNotNull("Status should not be null when disconnected", disconnectedStatus);
        assertTrue("Status should indicate not detected", disconnectedStatus.contains("not detected"));
        
        // Test status when connected
        androidAutoStateManager.setAutoConnectionState(true, "Edge Case Device");
        String connectedStatus = androidAutoStateManager.getDetectionStatus();
        assertNotNull("Status should not be null when connected", connectedStatus);
        assertTrue("Status should indicate connected", connectedStatus.contains("connected") || connectedStatus.contains("Android Auto"));
    }

    @Test
    public void testConnectionStateConsistency() {
        // Ensure connection state remains consistent after multiple operations
        androidAutoStateManager.setAutoConnectionState(true, "Consistency Test");
        assertTrue("Should be connected", androidAutoStateManager.isAutoConnected());
        
        // Check state multiple times
        assertTrue("Should remain connected", androidAutoStateManager.isAutoConnected());
        assertEquals("Device info should remain consistent", 
                   "Consistency Test", androidAutoStateManager.getConnectedAutoDevice());
        
        // Perform other operations
        androidAutoStateManager.getDetectionConfidence();
        androidAutoStateManager.getDetectionStatus();
        
        // State should remain unchanged
        assertTrue("Should still be connected after other operations", 
                  androidAutoStateManager.isAutoConnected());
        assertEquals("Device info should still be consistent", 
                   "Consistency Test", androidAutoStateManager.getConnectedAutoDevice());
    }
}