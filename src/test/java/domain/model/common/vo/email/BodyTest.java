package domain.model.common.vo.email;


import com.mailscheduler.domain.model.common.vo.email.Body;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BodyTest {

    @Test
    public void shouldCreateValidBody() {
        String rawValue = "Test Body";
        Body body = new Body(rawValue);

        assertNotNull(body);
        assertEquals(rawValue, body.value());
        assertEquals(rawValue, body.toString());
    }

    @Test
    public void shouldCreateInvalidBody() {
        assertThrows(IllegalArgumentException.class, () -> Body.of(null));
    }

    @Test
    public void toStringShouldReturnMaxLength50() {
        // Check for valid 50 character string
        StringBuilder sbLength50 = new StringBuilder();
        sbLength50.append("x".repeat(50));

        Body bodyLength50 = new Body(sbLength50.toString());
        assertEquals(sbLength50.toString(), bodyLength50.toString());

        // Check for valid 51 character string, which will be shortened
        StringBuilder sbLength51 = new StringBuilder();
        sbLength51.append("x".repeat(51));
        Body bodyLength51 = new Body(sbLength51.toString());

        assertEquals(50, bodyLength51.toString().length());
        assertEquals("...", bodyLength51.toString().substring(bodyLength51.toString().length() - 3));


        // Check for valid 51 character string, which will be shortened
        StringBuilder sbLength100 = new StringBuilder();
        sbLength51.append("x".repeat(100));
        Body bodyLength100 = new Body(sbLength51.toString());

        assertEquals(50, bodyLength51.toString().length());
        assertEquals("...", bodyLength51.toString().substring(bodyLength51.toString().length() - 3));
    }

    @Test
    public void equalsShouldReturnTrueWhenSameObject() {
        Body body = new Body("Test Body");
        assertEquals(body, body);
    }

    @Test
    public void equalsShouldReturnTrueWhenValue() {
        Body body1 = new Body("Test Body");
        Body body2 = new Body("Test Body");

        assertEquals(body1, body2);
        assertEquals(body2, body1);
    }

    @Test
    public void equalsShouldReturnFalseWhenDifferentObject() {
        Body body1 = new Body("Test Body");
        String body2 = "Test Body";

        assertNotEquals(body1, body2);
        assertNotEquals(body2, body1);
    }

    @Test
    public void equalsShouldReturnFalseWhenNullObject() {
        Body body1 = new Body("Test Body");
        assertNotEquals(body1, null);
        assertNotEquals(body1, new Object());
    }

    @Test
    public void isEmptyShouldReturnTrueWhenEmpty() {
        Body body1 = new Body("Test Body");
        assertFalse(body1.isEmpty());

        Body body2 = new Body("");
        assertTrue(body2.isEmpty());
    }

    @Test
    public void isEmptyShouldReturnTrueWhenOnlySpaces() {
        Body body1 = new Body("Test Body");
        assertFalse(body1.isEmpty());

        Body body2 = new Body("               ");
        assertTrue(body2.isEmpty());
    }
}
