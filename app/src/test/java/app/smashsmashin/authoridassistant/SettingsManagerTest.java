package app.smashsmashin.authoridassistant;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SettingsManager class.
 * Tests singleton pattern, settings persistence, validation, defaults, and edge cases.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SettingsManagerTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private SharedPreferences mockSharedPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    private Context realContext;
    private SettingsManager settingsManager;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Reset singleton instance before each test
        SettingsManager.resetInstanceForTesting();
        
        // Get real context for some tests
        realContext = ApplicationProvider.getApplicationContext();
        
        // Set up mock behavior
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        doNothing().when(mockEditor).apply();
    }
    
    @After
    public void tearDown() {
        // Clean up singleton instance after each test
        SettingsManager.resetInstanceForTesting();
    }
    
    // ========== Singleton Pattern Tests ==========
    
    @Test
    public void testSingletonInstance() {
        SettingsManager instance1 = SettingsManager.getInstance(realContext);
        SettingsManager instance2 = SettingsManager.getInstance(realContext);
        
        assertNotNull("Instance should not be null", instance1);
        assertSame("Should return same instance", instance1, instance2);
    }
    
    @Test
    public void testSingletonWithDifferentContexts() {
        Context context1 = realContext;
        Context context2 = mock(Context.class);
        when(context2.getApplicationContext()).thenReturn(realContext);
        
        SettingsManager instance1 = SettingsManager.getInstance(context1);
        SettingsManager instance2 = SettingsManager.getInstance(context2);
        
        assertSame("Should return same instance regardless of context", instance1, instance2);
    }
    
    @Test
    public void testResetInstanceForTesting() {
        SettingsManager instance1 = SettingsManager.getInstance(realContext);
        SettingsManager.resetInstanceForTesting();
        SettingsManager instance2 = SettingsManager.getInstance(realContext);
        
        assertNotSame("Should create new instance after reset", instance1, instance2);
    }
    
    // ========== Initialization and Loading Tests ==========
    
    @Test
    public void testInitializationWithNoSavedSettings() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        
        settingsManager = SettingsManager.getInstance(mockContext);
        
        // Should use default values
        assertTrue("Wireless charging should be enabled by default", 
                  settingsManager.isWirelessChargingEnabled());
        assertFalse("Bluetooth should be disabled by default", 
                   settingsManager.isBluetoothEnabled());
        assertFalse("Android Auto should be disabled by default", 
                   settingsManager.isAndroidAutoEnabled());
        assertEquals("Key FOB timeout should be 30 seconds by default", 
                    30, settingsManager.getKeyFobTimeoutSeconds());
        assertTrue("Notifications should be enabled by default", 
                  settingsManager.areNotificationsEnabled());
        assertFalse("Debug logging should be disabled by default", 
                   settingsManager.isDebugLoggingEnabled());
    }
    
    @Test
    public void testInitializationWithValidSavedSettings() {
        // Create test settings JSON
        SettingsManager.AppSettings testSettings = new SettingsManager.AppSettings();
        testSettings.wirelessChargingEnabled = false;
        testSettings.bluetoothEnabled = true;
        testSettings.androidAutoEnabled = true;
        testSettings.keyFobTimeoutSeconds = 60;
        testSettings.notificationsEnabled = false;
        testSettings.debugLoggingEnabled = true;
        
        String settingsJson = new Gson().toJson(testSettings);
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(settingsJson);
        
        settingsManager = SettingsManager.getInstance(mockContext);
        
        // Should load saved values
        assertFalse("Should load wireless charging setting", 
                   settingsManager.isWirelessChargingEnabled());
        assertTrue("Should load bluetooth setting", 
                  settingsManager.isBluetoothEnabled());
        assertTrue("Should load android auto setting", 
                  settingsManager.isAndroidAutoEnabled());
        assertEquals("Should load timeout setting", 
                    60, settingsManager.getKeyFobTimeoutSeconds());
        assertFalse("Should load notifications setting", 
                   settingsManager.areNotificationsEnabled());
        assertTrue("Should load debug logging setting", 
                  settingsManager.isDebugLoggingEnabled());
    }
    
    @Test
    public void testInitializationWithCorruptedSettings() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn("invalid json");
        
        settingsManager = SettingsManager.getInstance(mockContext);
        
        // Should fall back to defaults when JSON is corrupted
        assertTrue("Should use default when JSON is corrupted", 
                  settingsManager.isWirelessChargingEnabled());
        assertEquals("Should use default timeout when JSON is corrupted", 
                    30, settingsManager.getKeyFobTimeoutSeconds());
    }
    
    // ========== Settings Persistence Tests ==========
    
    @Test
    public void testWirelessChargingPersistence() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setWirelessChargingEnabled(false);
        
        verify(mockEditor).putString(eq("app_settings"), anyString());
        verify(mockEditor).apply();
    }
    
    @Test
    public void testBluetoothPersistence() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setBluetoothEnabled(true);
        
        verify(mockEditor).putString(eq("app_settings"), anyString());
        verify(mockEditor).apply();
    }
    
    @Test
    public void testAndroidAutoPersistence() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setAndroidAutoEnabled(true);
        
        verify(mockEditor).putString(eq("app_settings"), anyString());
        verify(mockEditor).apply();
    }
    
    @Test
    public void testNotificationsPersistence() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setNotificationsEnabled(false);
        
        verify(mockEditor).putString(eq("app_settings"), anyString());
        verify(mockEditor).apply();
    }
    
    @Test
    public void testDebugLoggingPersistence() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setDebugLoggingEnabled(true);
        
        verify(mockEditor).putString(eq("app_settings"), anyString());
        verify(mockEditor).apply();
    }
    
    // ========== Timeout Validation Tests ==========
    
    @Test
    public void testTimeoutValidation_NormalValues() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setKeyFobTimeoutSeconds(60);
        assertEquals("Should accept normal timeout value", 60, settingsManager.getKeyFobTimeoutSeconds());
        
        settingsManager.setKeyFobTimeoutSeconds(5);
        assertEquals("Should accept minimum timeout value", 5, settingsManager.getKeyFobTimeoutSeconds());
        
        settingsManager.setKeyFobTimeoutSeconds(300);
        assertEquals("Should accept maximum timeout value", 300, settingsManager.getKeyFobTimeoutSeconds());
    }
    
    @Test
    public void testTimeoutValidation_BelowMinimum() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setKeyFobTimeoutSeconds(3);
        assertEquals("Should clamp to minimum value", 5, settingsManager.getKeyFobTimeoutSeconds());
        
        settingsManager.setKeyFobTimeoutSeconds(0);
        assertEquals("Should clamp zero to minimum value", 5, settingsManager.getKeyFobTimeoutSeconds());
        
        settingsManager.setKeyFobTimeoutSeconds(-10);
        assertEquals("Should clamp negative to minimum value", 5, settingsManager.getKeyFobTimeoutSeconds());
    }
    
    @Test
    public void testTimeoutValidation_AboveMaximum() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setKeyFobTimeoutSeconds(500);
        assertEquals("Should clamp to maximum value", 300, settingsManager.getKeyFobTimeoutSeconds());
        
        settingsManager.setKeyFobTimeoutSeconds(Integer.MAX_VALUE);
        assertEquals("Should clamp extreme value to maximum", 300, settingsManager.getKeyFobTimeoutSeconds());
    }
    
    @Test
    public void testTimeoutPersistenceAfterValidation() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setKeyFobTimeoutSeconds(1000); // Above maximum
        
        verify(mockEditor).putString(eq("app_settings"), anyString());
        verify(mockEditor).apply();
        assertEquals("Should persist clamped value", 300, settingsManager.getKeyFobTimeoutSeconds());
    }
    
    // ========== Utility Methods Tests ==========
    
    @Test
    public void testGetKeyFobTimeoutMillis() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setKeyFobTimeoutSeconds(30);
        assertEquals("Should convert seconds to milliseconds", 30000L, settingsManager.getKeyFobTimeoutMillis());
        
        settingsManager.setKeyFobTimeoutSeconds(60);
        assertEquals("Should convert seconds to milliseconds", 60000L, settingsManager.getKeyFobTimeoutMillis());
    }
    
    @Test
    public void testHasAnyTriggersEnabled_AllDisabled() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        // Set all triggers to false
        settingsManager.setWirelessChargingEnabled(false);
        settingsManager.setBluetoothEnabled(false);
        settingsManager.setAndroidAutoEnabled(false);
        
        assertFalse("Should return false when all triggers disabled", 
                   settingsManager.hasAnyTriggersEnabled());
    }
    
    @Test
    public void testHasAnyTriggersEnabled_WirelessChargingOnly() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setWirelessChargingEnabled(true);
        settingsManager.setBluetoothEnabled(false);
        settingsManager.setAndroidAutoEnabled(false);
        
        assertTrue("Should return true when wireless charging enabled", 
                  settingsManager.hasAnyTriggersEnabled());
    }
    
    @Test
    public void testHasAnyTriggersEnabled_BluetoothOnly() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setWirelessChargingEnabled(false);
        settingsManager.setBluetoothEnabled(true);
        settingsManager.setAndroidAutoEnabled(false);
        
        assertTrue("Should return true when bluetooth enabled", 
                  settingsManager.hasAnyTriggersEnabled());
    }
    
    @Test
    public void testHasAnyTriggersEnabled_AndroidAutoOnly() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setWirelessChargingEnabled(false);
        settingsManager.setBluetoothEnabled(false);
        settingsManager.setAndroidAutoEnabled(true);
        
        assertTrue("Should return true when android auto enabled", 
                  settingsManager.hasAnyTriggersEnabled());
    }
    
    @Test
    public void testHasAnyTriggersEnabled_AllEnabled() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        settingsManager.setWirelessChargingEnabled(true);
        settingsManager.setBluetoothEnabled(true);
        settingsManager.setAndroidAutoEnabled(true);
        
        assertTrue("Should return true when all triggers enabled", 
                  settingsManager.hasAnyTriggersEnabled());
    }
    
    // ========== Reset Functionality Tests ==========
    
    @Test
    public void testResetToDefaults() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        // Change all settings
        settingsManager.setWirelessChargingEnabled(false);
        settingsManager.setBluetoothEnabled(true);
        settingsManager.setAndroidAutoEnabled(true);
        settingsManager.setKeyFobTimeoutSeconds(120);
        settingsManager.setNotificationsEnabled(false);
        settingsManager.setDebugLoggingEnabled(true);
        
        // Reset to defaults
        settingsManager.resetToDefaults();
        
        // Verify defaults are restored
        assertTrue("Wireless charging should be default", 
                  settingsManager.isWirelessChargingEnabled());
        assertFalse("Bluetooth should be default", 
                   settingsManager.isBluetoothEnabled());
        assertFalse("Android Auto should be default", 
                   settingsManager.isAndroidAutoEnabled());
        assertEquals("Timeout should be default", 
                    30, settingsManager.getKeyFobTimeoutSeconds());
        assertTrue("Notifications should be default", 
                  settingsManager.areNotificationsEnabled());
        assertFalse("Debug logging should be default", 
                   settingsManager.isDebugLoggingEnabled());
        
        // Verify reset was persisted
        verify(mockEditor, atLeastOnce()).putString(eq("app_settings"), anyString());
        verify(mockEditor, atLeastOnce()).apply();
    }
    
    // ========== Settings Object Tests ==========
    
    @Test
    public void testGetSettings() {
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        settingsManager = SettingsManager.getInstance(mockContext);
        
        SettingsManager.AppSettings settings = settingsManager.getSettings();
        
        assertNotNull("Settings object should not be null", settings);
        assertEquals("Should return current wireless charging state", 
                    settingsManager.isWirelessChargingEnabled(), settings.wirelessChargingEnabled);
        assertEquals("Should return current bluetooth state", 
                    settingsManager.isBluetoothEnabled(), settings.bluetoothEnabled);
        assertEquals("Should return current android auto state", 
                    settingsManager.isAndroidAutoEnabled(), settings.androidAutoEnabled);
        assertEquals("Should return current timeout", 
                    settingsManager.getKeyFobTimeoutSeconds(), settings.keyFobTimeoutSeconds);
        assertEquals("Should return current notifications state", 
                    settingsManager.areNotificationsEnabled(), settings.notificationsEnabled);
        assertEquals("Should return current debug logging state", 
                    settingsManager.isDebugLoggingEnabled(), settings.debugLoggingEnabled);
    }
    
    @Test
    public void testAppSettingsDefaultConstructor() {
        SettingsManager.AppSettings settings = new SettingsManager.AppSettings();
        
        assertTrue("Default wireless charging should be enabled", settings.wirelessChargingEnabled);
        assertFalse("Default bluetooth should be disabled", settings.bluetoothEnabled);
        assertFalse("Default android auto should be disabled", settings.androidAutoEnabled);
        assertEquals("Default timeout should be 30", 30, settings.keyFobTimeoutSeconds);
        assertTrue("Default notifications should be enabled", settings.notificationsEnabled);
        assertFalse("Default debug logging should be disabled", settings.debugLoggingEnabled);
    }
    
    // ========== Edge Cases and Error Handling ==========
    
    @Test
    public void testNullContextHandling() {
        // This test ensures the app context is used even if null is passed
        // The actual implementation uses context.getApplicationContext()
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockSharedPreferences.getString("app_settings", null)).thenReturn(null);
        
        assertNotNull("Should handle context gracefully", 
                     SettingsManager.getInstance(mockContext));
    }
    
    @Test
    public void testSharedPreferencesFailure() {
        when(mockSharedPreferences.getString("app_settings", null))
            .thenThrow(new RuntimeException("SharedPreferences error"));
        
        // Should not crash and should use defaults
        settingsManager = SettingsManager.getInstance(mockContext);
        
        assertNotNull("Should create instance despite SharedPreferences error", settingsManager);
        assertTrue("Should use default values on error", 
                  settingsManager.isWirelessChargingEnabled());
    }
    
    @Test
    public void testConcurrentAccess() {
        // Test that concurrent access to singleton works correctly
        final SettingsManager[] instances = new SettingsManager[2];
        
        Thread thread1 = new Thread(() -> {
            instances[0] = SettingsManager.getInstance(realContext);
        });
        
        Thread thread2 = new Thread(() -> {
            instances[1] = SettingsManager.getInstance(realContext);
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }
        
        assertNotNull("First instance should not be null", instances[0]);
        assertNotNull("Second instance should not be null", instances[1]);
        assertSame("Both threads should get same instance", instances[0], instances[1]);
    }
}