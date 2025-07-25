package model.common.vo;

import com.mailscheduler.domain.model.common.vo.ThreadId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadIdTest {

    @Test
    void shouldCreateThreadId() {
        String rawValue = "abc123";
        ThreadId threadId = new ThreadId(rawValue);

        assertNotNull(threadId);
        assertEquals(rawValue, threadId.value());
        assertEquals(rawValue, threadId.toString());
    }

    @Test
    void shouldThrowIfBlankOrNull() {
        assertThrows(IllegalArgumentException.class, () -> ThreadId.of(null));
        assertThrows(IllegalArgumentException.class, () -> ThreadId.of("     "));
    }
}
