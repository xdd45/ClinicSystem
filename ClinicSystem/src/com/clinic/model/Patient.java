package com.clinic.model;

import java.time.LocalDate;

public class Patient {
    private int id;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String contactNumber;
    private String address;
    private String email;
    private String bloodType;
    private String philhealthNumber;
    private String createdAt;

    public Patient(int id, String fullName, LocalDate dateOfBirth,
                   String gender, String contactNumber,
                   String address, String email) {
        this.id = id;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.contactNumber = contactNumber;
        this.address = address;
        this.email = email;
    }

    public Patient(String fullName, LocalDate dateOfBirth, String gender,
                   String contactNumber, String address, String email) {
        this(-1, fullName, dateOfBirth, gender, contactNumber, address, email);
    }

    // Getters
    public int getId()               { return id; }
    public String getFullName()      { return fullName; }
    public LocalDate getDateOfBirth(){ return dateOfBirth; }
    public String getGender()        { return gender; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress()       { return address; }
    public String getEmail()         { return email; }
    public String getBloodType()     { return bloodType; }
    public String getPhilhealthNumber(){ return philhealthNumber; }
    public String getCreatedAt()     { return createdAt; }

    // Setters
    public void setFullName(String v)         { fullName = v; }
    public void setDateOfBirth(LocalDate v)   { dateOfBirth = v; }
    public void setGender(String v)           { gender = v; }
    public void setContactNumber(String v)    { contactNumber = v; }
    public void setAddress(String v)          { address = v; }
    public void setEmail(String v)            { email = v; }
    public void setBloodType(String v)        { bloodType = v; }
    public void setPhilhealthNumber(String v) { philhealthNumber = v; }
    public void setCreatedAt(String v)        { createdAt = v; }
}