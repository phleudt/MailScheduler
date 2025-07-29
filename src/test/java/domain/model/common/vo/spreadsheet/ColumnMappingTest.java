package domain.model.common.vo.spreadsheet;

import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnMappingTest {

    @Test
    public void shouldCreateValidColumnMapping() {
        ColumnMapping.MappingType type = ColumnMapping.MappingType.CONTACT;
        String columnName = "Email";
        SpreadsheetReference reference = SpreadsheetReference.ofColumn("A");

        ColumnMapping mapping = new ColumnMapping(type, columnName, reference);

        assertNotNull(mapping);
        assertEquals(type, mapping.type());
        assertEquals(columnName, mapping.columnName());
        assertEquals(reference, mapping.columnReference());
    }

    @Test
    public void shouldThrowIfTypeIsNull() {
        assertThrows(NullPointerException.class, () ->
                new ColumnMapping(null, "Email", SpreadsheetReference.ofColumn("A")));
    }

    @Test
    public void shouldThrowIfColumnNameIsNull() {
        assertThrows(NullPointerException.class, () ->
                new ColumnMapping(ColumnMapping.MappingType.CONTACT, null, SpreadsheetReference.ofColumn("A")));
    }

    @Test
    public void shouldThrowIfColumnReferenceIsNull() {
        assertThrows(NullPointerException.class, () ->
                new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Email", null));
    }

    @Test
    public void shouldThrowIfColumnNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new ColumnMapping(ColumnMapping.MappingType.CONTACT, "   ", SpreadsheetReference.ofColumn("A")));
    }

    @Test
    public void equalsShouldReturnTrueForSameValues() {
        ColumnMapping.MappingType type = ColumnMapping.MappingType.CONTACT;
        String columnName = "Email";
        SpreadsheetReference reference = SpreadsheetReference.ofColumn("A");

        ColumnMapping mapping1 = new ColumnMapping(type, columnName, reference);
        ColumnMapping mapping2 = new ColumnMapping(type, columnName, reference);

        assertEquals(mapping1, mapping2);
    }

    @Test
    public void equalsShouldReturnTrueForSameObject() {
        ColumnMapping mapping = new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Email", SpreadsheetReference.ofColumn("A"));
        assertEquals(mapping, mapping);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentValues() {
        ColumnMapping mapping1 = new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Email", SpreadsheetReference.ofColumn("A"));
        ColumnMapping mapping2 = new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Name", SpreadsheetReference.ofColumn("B"));

        assertNotEquals(mapping1, mapping2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentObjectType() {
        ColumnMapping mapping = new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Email", SpreadsheetReference.ofColumn("A"));
        assertNotEquals(mapping, "Email");
    }

    @Test
    public void toStringShouldReturnExpectedFormat() {
        ColumnMapping mapping = new ColumnMapping(ColumnMapping.MappingType.CONTACT, "Email", SpreadsheetReference.ofColumn("A"));
        assertEquals("CONTACT:Email=A", mapping.toString());
    }
}