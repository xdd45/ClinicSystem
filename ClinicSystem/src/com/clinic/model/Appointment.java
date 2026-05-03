package com.clinic.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    private int id;
    private int patientId;
    private String patientName;
    private int userId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String reason;
    private String status;
    private String notes;

    public Appointment(int id, int patientId, String patientName, int userId,
                       LocalDate appointmentDate, LocalTime appointmentTime,
                       String reason, String status, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.userId = userId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
        this.status = status;
        this.notes = notes;
    }

    public int getId()                    { return id; }
    public int getPatientId()             { return patientId; }
    public String getPatientName()        { return patientName; }
    public int getUserId()                { return userId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public String getReason()             { return reason; }
    public String getStatus()             { return status; }
    public String getNotes()              { return notes; }

    public void setAppointmentDate(LocalDate v) { appointmentDate = v; }
    public void setAppointmentTime(LocalTime v) { appointmentTime = v; }
    public void setReason(String v)             { reason = v; }
    public void setStatus(String v)             { status = v; }
    public void setNotes(String v)              { notes = v; }
}