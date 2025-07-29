package domain.model.common.base;

import com.mailscheduler.domain.model.common.base.NoMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NoMetadataTest {

    @Test
    public void shouldReturnSingletonInstance() {
        NoMetadata instance1 = NoMetadata.getInstance();
        NoMetadata instance2 = NoMetadata.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    public void toStringShouldReturnNoMetadata() {
        assertEquals("NoMetadata", NoMetadata.getInstance().toString());
    }
}