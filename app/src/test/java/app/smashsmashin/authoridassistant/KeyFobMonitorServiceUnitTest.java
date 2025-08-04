package app.smashsmashin.authoridassistant;

import android.content.Intent;
import android.os.IBinder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class KeyFobMonitorServiceUnitTest {

    private KeyFobMonitorService service;

    @Before
    public void setUp() {
        service = new KeyFobMonitorService();
    }

    @Test
    public void testOnBind_ReturnsNull() {
        IBinder binder = service.onBind(new Intent());
        
        assertNull("Service should not be bindable", binder);
    }

    @Test
    public void testServiceConstants() {
        assertEquals("Notification ID should be 1001", 1001, getNotificationIdConstant());
        assertEquals("Channel ID should be correct", "keyfob_monitor_channel", getChannelIdConstant());
    }

    private int getNotificationIdConstant() {
        try {
            java.lang.reflect.Field field = KeyFobMonitorService.class.getDeclaredField("NOTIFICATION_ID");
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            return -1;
        }
    }

    private String getChannelIdConstant() {
        try {
            java.lang.reflect.Field field = KeyFobMonitorService.class.getDeclaredField("CHANNEL_ID");
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}