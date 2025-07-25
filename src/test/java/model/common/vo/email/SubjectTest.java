package model.common.vo.email;

import com.mailscheduler.domain.model.common.vo.email.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubjectTest {

    @Test
    public void shouldCreateValidSubject() {
        String rawValue = "Test Subject";
        Subject subject = new Subject(rawValue);

        assertNotNull(subject);
        assertEquals(rawValue, subject.value());
        assertEquals(rawValue, subject.toString());
    }

    @Test
    public void shouldCreateInvalidSubjectWithNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new Subject(null));
    }

    @Test
    public void shouldCreateValidSubjectWithEmptyValue() {
        String rawValue = "    ";
        Subject subject = new Subject(rawValue);

        assertNotNull(subject);
        assertEquals("", subject.value());
        assertEquals("", subject.toString());
    }

    @Test
    public void shouldCreateValidSubjectWithLengthUnder200() {
        String rawValue = "x".repeat(200);
        Subject subject = new Subject(rawValue);

        assertNotNull(subject);
        assertEquals(rawValue.length(), subject.value().length());
    }

    @Test
    public void shouldCreateInvalidSubjectWithLengthOver200() {
        String rawValue = "x".repeat(201);
        assertThrows(IllegalArgumentException.class, () -> new Subject(rawValue));
    }

    @Test
    public void truncateShouldCreateValidSubjectOfLength() {
        String rawValue = "x".repeat(200);
        Subject subject = new Subject(rawValue);

        assertEquals("x".repeat(17), subject.truncated(20).substring(0, 17));
        assertEquals("...", subject.truncated(20).substring(17));
        assertEquals(20, subject.truncated(20).length());
    }

    @Test
    public void truncateShouldCreateSameValueIfLengthLargerThanSubjectLength() {
        String rawValue = "x".repeat(10);
        Subject subject = new Subject(rawValue);

        assertEquals("x".repeat(10), subject.truncated(20));
    }

    @Test
    public void equalsShouldReturnTrueWhenSameSubject() {
        String rawValue = "Test Subject";
        Subject subject1 = new Subject(rawValue);
        Subject subject2 = new Subject(rawValue);

        assertEquals(subject1, subject2);
    }

    @Test
    public void equalsShouldReturnTrueWhenSameObject() {
        Subject subject = new Subject("Test Subject");

        assertEquals(subject, subject);
    }

    @Test
    public void equalsShouldReturnFalseWhenDifferentSubject() {
        Subject subject1 = new Subject("Test Subject1");
        Subject subject2 = new Subject("Test Subject2");

        assertNotEquals(subject1, subject2);
    }

    @Test
    public void equalsShouldReturnFalseWhenDifferentObject() {
        String rawValue = "Test Subject";
        Subject subject = new Subject(rawValue);

        assertNotEquals(rawValue, subject);
    }
}
