package com.mailscheduler.database.entities;

import java.sql.Timestamp;

public class RecipientEntity {
    private int id;
    private String name;
    private String email_address;
    private String domain;
    private String phone_number;
    private Timestamp initial_email_date;
    private boolean has_replied;
    private int spreadsheet_row;

    public RecipientEntity(int id, String name, String email_address, String domain, String phone_number, Timestamp initial_email_date, boolean has_replied, int spreadsheet_row) {
        this.id = id;
        this.name = name;
        this.email_address = email_address;
        this.domain = domain;
        this.phone_number = phone_number;
        this.initial_email_date = initial_email_date;
        this.has_replied = has_replied;
        this.spreadsheet_row = spreadsheet_row;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public Timestamp getInitial_email_date() {
        return initial_email_date;
    }

    public void setInitial_email_date(Timestamp initial_email_date) {
        this.initial_email_date = initial_email_date;
    }

    public boolean has_replied() {
        return has_replied;
    }

    public void setHas_replied(boolean has_replied) {
        this.has_replied = has_replied;
    }

    public int getSpreadsheet_row() {
        return spreadsheet_row;
    }

    public void setSpreadsheet_row(int spreadsheet_row) {
        this.spreadsheet_row = spreadsheet_row;
    }
}
