package domain.model.recipient;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetReference;
import com.mailscheduler.domain.model.recipient.Contact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContactTest {

    private Contact.Builder defaultBuilder() {
        return new Contact.Builder()
                .setId(EntityId.of(1L))
                .setName("Test")
                .setWebsite("Test-Website.com")
                .setPhoneNumber("Test-Phone-Number")
                .setSheetTitle("Test-Sheet-Title");
    }

    @Test
    public void shouldCreateValidContactWithBuilder() {
        Contact contact = defaultBuilder()
                .setSpreadsheetRow(1)
                .build();

        assertNotNull(contact);
        assertEquals(EntityId.of(1L), contact.getId());
        assertEquals("Test", contact.getName());
        assertEquals("Test-Website.com", contact.getWebsite());
        assertEquals("Test-Phone-Number", contact.getPhoneNumber());
        assertEquals("Test-Sheet-Title", contact.getSheetTitle());
        assertEquals(1, contact.getSpreadsheetRow().extractRowNumber());
    }

    @Test
    public void shouldCreateValidContactWithSpreadsheetReference() {
        Contact contact = defaultBuilder()
                .setSpreadsheetRow(SpreadsheetReference.ofRow(1))
                .build();

        assertNotNull(contact);
        assertEquals(EntityId.of(1L), contact.getId());
        assertEquals("Test", contact.getName());
        assertEquals("Test-Website.com", contact.getWebsite());
        assertEquals("Test-Phone-Number", contact.getPhoneNumber());
        assertEquals("Test-Sheet-Title", contact.getSheetTitle());
        assertEquals(1, contact.getSpreadsheetRow().extractRowNumber());
    }

    @Test
    public void builderFromShouldCopyValues() {
        Contact initialContact = defaultBuilder().build();

        Contact copyContact = new Contact.Builder().from(initialContact).build();

        assertEquals(EntityId.of(1L), copyContact.getId());
        assertEquals("Test", copyContact.getName());
        assertEquals("Test-Website.com", copyContact.getWebsite());
        assertEquals("Test-Phone-Number", copyContact.getPhoneNumber());
        assertEquals("Test-Sheet-Title", copyContact.getSheetTitle());
    }

    @Test
    public void equalsShouldReturnTrueWhenSameInstance() {
        Contact contact = defaultBuilder().build();

        assertEquals(contact, contact);
    }

    @Test
    public void equalsShouldReturnFalseWhenDifferentObject() {
        Contact contact = defaultBuilder().build();
        assertNotEquals(new Object(), contact);
    }

    @Test
    public void equalShouldReturnFalseWhenIdIsDifferent() {
        Contact contact1 = defaultBuilder().build();
        Contact contact2 = defaultBuilder().setId(EntityId.of(2L)).build();

        assertNotEquals(contact1, contact2);
    }

    @Test
    public void equalShouldReturnTrueWhenSameValues() {
        Contact contact1 = defaultBuilder().build();
        Contact contact2 = defaultBuilder().build();
        assertEquals(contact1, contact2);
    }
}
