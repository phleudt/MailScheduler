package model.common.vo.spreadsheet;

import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SheetConfigurationTest {

    @Test
    public void shouldCreateValidSheetConfiguration() {
        SheetConfiguration config = new SheetConfiguration("sheet1", "Contacts", 0);
        assertNotNull(config);
        assertEquals("sheet1", config.sheetId());
        assertEquals("Contacts", config.title());
        assertEquals(0, config.index());
    }

    @Test
    public void shouldThrowIfTitleIsNull() {
        assertThrows(NullPointerException.class, () -> new SheetConfiguration("sheet1", null, 0));
    }

    @Test
    public void shouldThrowIfTitleIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new SheetConfiguration("sheet1", "   ", 0));
    }

    @Test
    public void shouldThrowIfIndexIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new SheetConfiguration("sheet1", "Contacts", -1));
    }

    @Test
    public void toStringShouldReturnExpectedFormat() {
        SheetConfiguration config = new SheetConfiguration("sheet1", "Contacts", 2);
        assertEquals("Sheet[Contacts, index=2]", config.toString());
    }

    @Test
    public void equalsShouldReturnTrueForSameValues() {
        SheetConfiguration config1 = new SheetConfiguration("sheet1", "Contacts", 0);
        SheetConfiguration config2 = new SheetConfiguration("sheet1", "Contacts", 0);
        assertEquals(config1, config2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentValues() {
        SheetConfiguration config1 = new SheetConfiguration("sheet1", "Contacts", 0);
        SheetConfiguration config2 = new SheetConfiguration("sheet2", "Leads", 1);
        assertNotEquals(config1, config2);
    }
}