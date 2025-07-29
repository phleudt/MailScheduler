package domain.model.common.base;

import com.mailscheduler.domain.model.common.base.EntityId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityIdTest {

    @Test
    public void shouldCreateValidEntityId() {
        EntityId<String> id = EntityId.of(123L);
        assertNotNull(id);
        assertEquals(123L, id.value());
        assertTrue(id.toString().contains("EntityId<123>"));
    }

    @Test
    public void shouldThrowIfNullValue() {
        assertThrows(NullPointerException.class, () -> EntityId.of(null));
    }

    @Test
    public void equalsShouldReturnTrueForSameValue() {
        EntityId<String> id1 = EntityId.of(1L);
        EntityId<String> id2 = EntityId.of(1L);
        assertEquals(id1, id2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentValue() {
        EntityId<String> id1 = EntityId.of(1L);
        EntityId<String> id2 = EntityId.of(2L);
        assertNotEquals(id1, id2);
    }
}