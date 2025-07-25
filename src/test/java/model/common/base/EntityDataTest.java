package model.common.base;

import com.mailscheduler.domain.model.common.base.EntityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityDataTest {

    @Test
    public void shouldCreateValidEntityData() {
        String entity = "TestEntity";
        String metadata = "Meta";
        EntityData<String, String> data = EntityData.of(entity, metadata);
        assertEquals(entity, data.entity());
        assertEquals(metadata, data.metadata());
    }

    @Test
    public void shouldThrowIfEntityIsNull() {
        assertThrows(NullPointerException.class, () -> EntityData.of(null, "meta"));
    }

    @Test
    public void shouldThrowIfMetadataIsNull() {
        assertThrows(NullPointerException.class, () -> EntityData.of("entity", null));
    }
}