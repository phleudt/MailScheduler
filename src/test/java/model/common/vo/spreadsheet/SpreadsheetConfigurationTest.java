package model.common.vo.spreadsheet;

import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SpreadsheetConfigurationTest {

    @Test
    public void shouldCreateValidSpreadsheetConfiguration() {
        SheetConfiguration sheet = new SheetConfiguration("sheet1", "Contacts", 0);
        SpreadsheetConfiguration config = new SpreadsheetConfiguration("spreadsheet1", "CRM", List.of(sheet));
        assertNotNull(config);
        assertEquals("spreadsheet1", config.spreadsheetId());
        assertEquals("CRM", config.title());
        assertEquals(1, config.sheetConfigurations().size());
        assertEquals(sheet, config.sheetConfigurations().get(0));
    }

    @Test
    public void shouldThrowIfSpreadsheetIdIsNull() {
        SheetConfiguration sheet = new SheetConfiguration("sheet1", "Contacts", 0);
        assertThrows(NullPointerException.class, () -> new SpreadsheetConfiguration(null, "CRM", List.of(sheet)));
    }

    @Test
    public void shouldThrowIfTitleIsNull() {
        SheetConfiguration sheet = new SheetConfiguration("sheet1", "Contacts", 0);
        assertThrows(NullPointerException.class, () -> new SpreadsheetConfiguration("spreadsheet1", null, List.of(sheet)));
    }

    @Test
    public void shouldThrowIfSpreadsheetIdIsBlank() {
        SheetConfiguration sheet = new SheetConfiguration("sheet1", "Contacts", 0);
        assertThrows(IllegalArgumentException.class, () -> new SpreadsheetConfiguration("   ", "CRM", List.of(sheet)));
    }

    @Test
    public void shouldThrowIfTitleIsBlank() {
        SheetConfiguration sheet = new SheetConfiguration("sheet1", "Contacts", 0);
        assertThrows(IllegalArgumentException.class, () -> new SpreadsheetConfiguration("spreadsheet1", "   ", List.of(sheet)));
    }

    @Test
    public void shouldCreateWithEmptySheetConfigurationsIfNull() {
        SpreadsheetConfiguration config = new SpreadsheetConfiguration("spreadsheet1", "CRM", null);
        assertNotNull(config.sheetConfigurations());
        assertTrue(config.sheetConfigurations().isEmpty());
    }

    @Test
    public void addSheetShouldReturnNewConfigurationWithAddedSheet() {
        SheetConfiguration sheet1 = new SheetConfiguration("sheet1", "Contacts", 0);
        SpreadsheetConfiguration config = new SpreadsheetConfiguration("spreadsheet1", "CRM", List.of(sheet1));
        SheetConfiguration sheet2 = new SheetConfiguration("sheet2", "Leads", 1);

        SpreadsheetConfiguration updated = config.addSheet(sheet2);

        assertEquals(2, updated.sheetConfigurations().size());
        assertTrue(updated.sheetConfigurations().contains(sheet1));
        assertTrue(updated.sheetConfigurations().contains(sheet2));
    }

    @Test
    public void addSheetShouldThrowIfSheetIsNull() {
        SpreadsheetConfiguration config = new SpreadsheetConfiguration("spreadsheet1", "CRM", null);
        assertThrows(NullPointerException.class, () -> config.addSheet(null));
    }

    @Test
    public void toStringShouldReturnExpectedFormat() {
        SheetConfiguration sheet = new SheetConfiguration("sheet1", "Contacts", 0);
        SpreadsheetConfiguration config = new SpreadsheetConfiguration("spreadsheet1", "CRM", List.of(sheet));
        assertEquals("Spreadsheet[CRM, id=spreadsheet1, sheets=1]", config.toString());
    }

    @Test
    public void equalsShouldReturnTrueForSameValues() {
        SheetConfiguration sheet = new SheetConfiguration("sheet1", "Contacts", 0);
        SpreadsheetConfiguration config1 = new SpreadsheetConfiguration("spreadsheet1", "CRM", List.of(sheet));
        SpreadsheetConfiguration config2 = new SpreadsheetConfiguration("spreadsheet1", "CRM", List.of(sheet));
        assertEquals(config1, config2);
    }

    @Test
    public void equalsShouldReturnFalseForDifferentValues() {
        SheetConfiguration sheet1 = new SheetConfiguration("sheet1", "Contacts", 0);
        SheetConfiguration sheet2 = new SheetConfiguration("sheet2", "Leads", 1);
        SpreadsheetConfiguration config1 = new SpreadsheetConfiguration("spreadsheet1", "CRM", List.of(sheet1));
        SpreadsheetConfiguration config2 = new SpreadsheetConfiguration("spreadsheet2", "Sales", List.of(sheet2));
        assertNotEquals(config1, config2);
    }
}