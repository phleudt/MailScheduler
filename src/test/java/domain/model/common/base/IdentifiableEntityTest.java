package domain.model.common.base;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.IdentifiableEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IdentifiableEntityTest {

    static class TestEntity extends IdentifiableEntity<TestEntity> {}

    @Test
    public void shouldSetAndGetId() {
        TestEntity entity = new TestEntity();
        EntityId<TestEntity> id = EntityId.of(42L);
        entity.setId(id);
        assertEquals(id, entity.getId());
        assertTrue(entity.hasId());
    }

    @Test
    public void shouldNotHaveIdInitially() {
        TestEntity entity = new TestEntity();
        assertFalse(entity.hasId());
    }

    @Test
    public void equalsShouldReturnTrueForSameId() {
        TestEntity e1 = new TestEntity();
        TestEntity e2 = new TestEntity();
        EntityId<TestEntity> id = EntityId.of(1L);
        e1.setId(id);
        e2.setId(id);
        assertEquals(e1, e2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentId() {
        TestEntity e1 = new TestEntity();
        TestEntity e2 = new TestEntity();
        e1.setId(EntityId.of(1L));
        e2.setId(EntityId.of(2L));
        assertNotEquals(e1, e2);
    }

    @Test
    public void equalsShouldReturnTrueForSameInstanceWithoutId() {
        TestEntity e1 = new TestEntity();
        assertEquals(e1, e1);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentClass() {
        TestEntity e1 = new TestEntity();
        Object o = new Object();
        assertNotEquals(e1, o);
    }

    @Test
    public void hashCodeShouldUseIdIfPresent() {
        TestEntity e1 = new TestEntity();
        e1.setId(EntityId.of(99L));
        assertEquals(e1.getId().hashCode(), e1.hashCode());
    }

    @Test
    public void toStringShouldIncludeIdOrUnsaved() {
        TestEntity e1 = new TestEntity();
        assertTrue(e1.toString().contains("unsaved"));
        e1.setId(EntityId.of(123L));
        assertTrue(e1.toString().contains("id=123"));
    }
}