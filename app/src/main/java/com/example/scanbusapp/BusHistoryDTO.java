package com.example.scanbusapp;

public class BusHistoryDTO {

    private String macAddress;
    private String destination;
    private String chauffeurNom;
    private String chauffeurUniqueNumber;
    private int batteryLevel;
    private String terminalType;
    private String androidId;
    private String connectionTime;

    // Constructeur complet
    public BusHistoryDTO(String macAddress, String destination, String chauffeurNom, String chauffeurUniqueNumber,
                         int batteryLevel, String terminalType, String androidId, String connectionTime) {
        this.macAddress = macAddress;
        this.destination = destination;
        this.chauffeurNom = chauffeurNom;
        this.chauffeurUniqueNumber = chauffeurUniqueNumber;
        this.batteryLevel = batteryLevel;
        this.terminalType = terminalType;
        this.androidId = androidId;
        this.connectionTime = connectionTime;
    }

    // Getters et Setters

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getChauffeurNom() {
        return chauffeurNom;
    }

    public void setChauffeurNom(String chauffeurNom) {
        this.chauffeurNom = chauffeurNom;
    }

    public String getChauffeurUniqueNumber() {
        return chauffeurUniqueNumber;
    }

    public void setChauffeurUniqueNumber(String chauffeurUniqueNumber) {
        this.chauffeurUniqueNumber = chauffeurUniqueNumber;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    public String getConnectionTime() {
        return connectionTime;
    }

    public void setConnectionTime(String connectionTime) {
        this.connectionTime = connectionTime;
    }
}
