package com.example.scanbusapp;

import java.util.Date;

public class ForfaitDTO {
    private String typeForfait;
    private String rfid;
    private Date dateExpiration;

    // Constructeur avec tous les champs
    public ForfaitDTO(String typeForfait, String rfid, Date dateExpiration) {
        this.typeForfait = typeForfait;
        this.rfid = rfid;
        this.dateExpiration = dateExpiration;
    }

    // Constructeur sans dateExpiration (pour l'attribution du forfait)
    public ForfaitDTO(String typeForfait, String rfid) {
        this.typeForfait = typeForfait;
        this.rfid = rfid;
    }

    // Getters et Setters
    public String getTypeForfait() {
        return typeForfait;
    }

    public void setTypeForfait(String typeForfait) {
        this.typeForfait = typeForfait;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public Date getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(Date dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
}
