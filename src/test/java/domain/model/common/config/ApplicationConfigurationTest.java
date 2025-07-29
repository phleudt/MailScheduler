package domain.model.common.config;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.config.ApplicationConfiguration;
import com.mailscheduler.domain.model.common.vo.email.EmailAddress;
import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationConfigurationTest {

    private ApplicationConfiguration.Builder defaultBuilder() {
        return new ApplicationConfiguration.Builder()
                .setId(EntityId.of(1L))
                .spreadsheetId("spreadsheet123")
                .senderEmailAddress(EmailAddress.of("test@mail.com"))
                .saveMode(true)
                .sendingCriteriaColumn(SpreadsheetReference.ofColumn("A"))
                .columnMappings(List.of(
                        new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Website", SpreadsheetReference.ofColumn("A")),
                        new ColumnMapping(ColumnMapping.MappingType.RECIPIENT, "EmailAddress", SpreadsheetReference.ofColumn("B"))
                ));
    }

    @Test
    public void shouldBuildValidConfiguration() {
        ApplicationConfiguration config = defaultBuilder().build();

        assertNotNull(config.getId());
        assertEquals("spreadsheet123", config.getSpreadsheetId());
        assertEquals(EmailAddress.of("test@mail.com"), config.getSenderEmailAddress());
        assertTrue(config.getSaveMode());
        assertEquals(SpreadsheetReference.ofColumn("A"), config.getSendingCriteriaColumn());
        assertEquals(2, config.getColumnMappings().size());
    }

    @Test
    public void shouldReturnColumnMappingsByType() {
        ApplicationConfiguration config = defaultBuilder().build();
        List<ColumnMapping> contactMappings = config.getColumnMappings(ColumnMapping.MappingType.CONTACT);
        assertEquals(1, contactMappings.size());
        assertEquals("Website", contactMappings.get(0).columnName());
        assertEquals(ColumnMapping.MappingType.CONTACT, contactMappings.get(0).type());

        List<ColumnMapping> recipientMappings = config.getColumnMappings(ColumnMapping.MappingType.RECIPIENT);
        assertEquals(1, recipientMappings.size());
        assertEquals("EmailAddress", recipientMappings.get(0).columnName());
        assertEquals(ColumnMapping.MappingType.RECIPIENT, recipientMappings.get(0).type());
    }

    @Test
    public void shouldThrowIfMappingTypeIsNull() {
        ApplicationConfiguration config = defaultBuilder().build();
        assertThrows(NullPointerException.class, () -> config.getColumnMappings(null));
    }

    @Test
    public void shouldGroupColumnMappingsByType() {
        ApplicationConfiguration config = defaultBuilder().build();
        Map<ColumnMapping.MappingType, List<ColumnMapping>> grouped = config.groupColumnMappingsByType();
        assertTrue(grouped.containsKey(ColumnMapping.MappingType.CONTACT));
        assertTrue(grouped.containsKey(ColumnMapping.MappingType.RECIPIENT));
        assertEquals(2, grouped.values().stream().mapToInt(List::size).sum());
    }

    @Test
    public void isCompleteShouldReturnTrueForValidConfig() {
        ApplicationConfiguration config = defaultBuilder().build();
        assertTrue(config.isComplete());
    }

    @Test
    public void isCompleteShouldReturnFalseIfMissingFields() {
        ApplicationConfiguration.Builder builder = new ApplicationConfiguration.Builder();
        ApplicationConfiguration config = builder.build();
        assertFalse(config.isComplete());
    }

    @Test
    public void equalsShouldReturnTrueForSameValues() {
        ApplicationConfiguration config1 = defaultBuilder().build();
        ApplicationConfiguration config2 = defaultBuilder().build();
        assertEquals(config1, config2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentValues() {
        ApplicationConfiguration config1 = defaultBuilder().build();
        ApplicationConfiguration config2 = new ApplicationConfiguration.Builder()
                .setId(EntityId.of(2L))
                .spreadsheetId("spreadsheet456")
                .senderEmailAddress(EmailAddress.of("other@mail.com"))
                .saveMode(false)
                .sendingCriteriaColumn(SpreadsheetReference.ofColumn("B"))
                .columnMappings(List.of(
                        new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Name", SpreadsheetReference.ofColumn("C"))
                ))
                .build();
        assertNotEquals(config1, config2);
    }

    @Test
    public void toStringShouldContainKeyFields() {
        ApplicationConfiguration config = defaultBuilder().build();
        String str = config.toString();
        assertTrue(str.contains("spreadsheetId='spreadsheet123'"));
        assertTrue(str.contains("senderEmailAddress=test@mail.com"));
        assertTrue(str.contains("columnMappings=2"));
    }

    @Test
    public void builderFromShouldCopyValues() {
        ApplicationConfiguration config = defaultBuilder().build();
        ApplicationConfiguration copy = new ApplicationConfiguration.Builder().from(config).build();
        assertEquals(config, copy);
    }

    @Test
    public void builderMergeShouldOverwriteNonNullFields() {
        ApplicationConfiguration base = defaultBuilder().build();
        ApplicationConfiguration update = new ApplicationConfiguration.Builder()
                .spreadsheetId("updatedId")
                .senderEmailAddress(EmailAddress.of("updated@mail.com"))
                .saveMode(false)
                .sendingCriteriaColumn(SpreadsheetReference.ofColumn("Z"))
                .columnMappings(List.of(
                        new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Updated", SpreadsheetReference.ofColumn("Z"))
                ))
                .build();

        ApplicationConfiguration merged = new ApplicationConfiguration.Builder()
                .from(base)
                .merge(update)
                .build();

        assertEquals("updatedId", merged.getSpreadsheetId());
        assertEquals(EmailAddress.of("updated@mail.com"), merged.getSenderEmailAddress());
        assertFalse(merged.getSaveMode());
        assertEquals(SpreadsheetReference.ofColumn("Z"), merged.getSendingCriteriaColumn());
        assertEquals(1, merged.getColumnMappings().size());
    }
}