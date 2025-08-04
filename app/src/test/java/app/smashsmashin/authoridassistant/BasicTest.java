package app.smashsmashin.authoridassistant;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Basic test to verify test infrastructure works
 */
public class BasicTest {

    @Test
    public void testBasicFunctionality() {
        assertTrue("Basic test should pass", true);
        assertEquals("Basic math should work", 2, 1 + 1);
    }

    @Test
    public void testKeyFobMonitorServiceExists() {
        KeyFobMonitorService service = new KeyFobMonitorService();
        assertNotNull("KeyFobMonitorService should be instantiable", service);
    }
}