package model.common.vo.spreadsheet;

import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SpreadsheetReferenceTest {

    @Test
    public void shouldCreateValidColumnReference() {
        SpreadsheetReference ref = SpreadsheetReference.ofColumn("A");
        assertEquals("A", ref.getReference());
        assertEquals(SpreadsheetReference.ReferenceType.COLUMN, ref.getType());
        assertNull(ref.getSheetTitle());
    }

    @Test
    public void shouldCreateValidRowReference() {
        SpreadsheetReference ref = SpreadsheetReference.ofRow("1");
        assertEquals("1", ref.getReference());
        assertEquals(SpreadsheetReference.ReferenceType.ROW, ref.getType());
        assertNull(ref.getSheetTitle());
    }

    @Test
    public void shouldCreateValidCellReference() {
        SpreadsheetReference ref = SpreadsheetReference.ofCell("B2");
        assertEquals("B2", ref.getReference());
        assertEquals(SpreadsheetReference.ReferenceType.CELL, ref.getType());
        assertNull(ref.getSheetTitle());
    }

    @Test
    public void shouldCreateValidRangeReference() {
        SpreadsheetReference ref = SpreadsheetReference.ofRange("A1:B2");
        assertEquals("A1:B2", ref.getReference());
        assertEquals(SpreadsheetReference.ReferenceType.RANGE, ref.getType());
        assertNull(ref.getSheetTitle());
    }

    @Test
    public void shouldCreateReferenceWithSheetTitle() {
        SpreadsheetReference ref = SpreadsheetReference.ofCell("Sheet1", "C3");
        assertEquals("C3", ref.getReference());
        assertEquals("Sheet1", ref.getSheetTitle());
        assertEquals(SpreadsheetReference.ReferenceType.CELL, ref.getType());
    }

    @Test
    public void shouldParseGoogleReferenceWithSheet() {
        SpreadsheetReference ref = SpreadsheetReference.fromGoogleReference("Sheet1!A1:B2");
        assertEquals("A1:B2", ref.getReference());
        assertEquals("Sheet1", ref.getSheetTitle());
        assertEquals(SpreadsheetReference.ReferenceType.RANGE, ref.getType());
    }

    @Test
    public void shouldParseGoogleReferenceWithoutSheet() {
        SpreadsheetReference ref = SpreadsheetReference.fromGoogleReference("A1");
        assertEquals("A1", ref.getReference());
        assertNull(ref.getSheetTitle());
        assertEquals(SpreadsheetReference.ReferenceType.CELL, ref.getType());
    }

    @Test
    public void shouldConvertToGoogleSheetsReference() {
        SpreadsheetReference ref = SpreadsheetReference.ofCell("Sheet1", "A1");
        assertEquals("Sheet1!A1:A1", ref.getGoogleSheetsReference());

        SpreadsheetReference colRef = SpreadsheetReference.ofColumn("B");
        assertEquals("B:B", colRef.getGoogleSheetsReference());

        SpreadsheetReference rowRef = SpreadsheetReference.ofRow(2);
        assertEquals("2:2", rowRef.getGoogleSheetsReference());

        SpreadsheetReference rangeRef = SpreadsheetReference.ofRange("A1:B2");
        assertEquals("A1:B2", rangeRef.getGoogleSheetsReference());
    }

    @Test
    public void shouldThrowOnInvalidColumnReference() {
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofColumn("a"));
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofColumn("1A"));
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofColumn(""));
    }

    @Test
    public void shouldThrowOnInvalidRowReference() {
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofRow("0"));
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofRow("-1"));
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofRow("A"));
    }

    @Test
    public void shouldThrowOnInvalidCellReference() {
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofCell("A"));
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofCell("1A"));
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofCell("AA0"));
    }

    @Test
    public void shouldThrowOnInvalidRangeReference() {
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofRange("A1B2"));
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofRange("A1:B2:C3"));
    }

    @Test
    public void shouldTestEquality() {
        SpreadsheetReference ref1 = SpreadsheetReference.ofCell("A1");
        SpreadsheetReference ref2 = SpreadsheetReference.ofCell("A1");
        SpreadsheetReference ref3 = SpreadsheetReference.ofCell("Sheet1", "A1");

        assertEquals(ref1, ref2);
        assertNotEquals(ref1, ref3);
        assertNotEquals(ref1, "A1");
    }

    @Test
    public void shouldExtractRowNumber() {
        SpreadsheetReference ref = SpreadsheetReference.ofRow("5");
        assertEquals(5, ref.extractRowNumber());
        assertThrows(IllegalArgumentException.class, () -> SpreadsheetReference.ofColumn("A").extractRowNumber());
    }

    @Test
    public void toStringShouldReturnReferenceOrSheetQualifiedReference() {
        SpreadsheetReference ref1 = SpreadsheetReference.ofCell("A1");
        assertEquals("A1", ref1.toString());

        SpreadsheetReference ref2 = SpreadsheetReference.ofCell("Sheet1", "A1");
        assertEquals("A1", ref2.toString());
    }
}