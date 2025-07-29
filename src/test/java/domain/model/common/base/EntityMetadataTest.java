package domain.model.common.base;

import com.mailscheduler.domain.model.common.base.EntityMetadata;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityMetadataTest {

    @Test
    public void shouldImplementEntityMetadata() {
        EntityMetadata metadata = NoMetadata.getInstance();
        assertTrue(metadata instanceof EntityMetadata);
    }
}