package com.mailscheduler.infrastructure.persistence.entity;

/**
 * Database entity representing the contacts table
 */
public class ContactEntity extends TableEntity {
    private String name;
    private String website;
    private String phoneNumber;
    private String sheetTitle;
    private int spreadsheetRow;

    public ContactEntity(Long id, String name, String website, String phoneNumber, String sheetTitle, int spreadsheetRow) {
        super.setId(id);
        this.name = name;
        this.website = website;
        this.phoneNumber = phoneNumber;
        this.sheetTitle = sheetTitle;
        this.spreadsheetRow = spreadsheetRow;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSheetTitle() {
        return sheetTitle;
    }

    public void setSheetTitle(String sheetTitle) {
        this.sheetTitle = sheetTitle;
    }

    public int getSpreadsheetRow() {
        return spreadsheetRow;
    }

    public void setSpreadsheetRow(int spreadsheetRow) {
        this.spreadsheetRow = spreadsheetRow;
    }

}