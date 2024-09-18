package com.example.scanbusapp;

public class ForfaitVerificationDTO {
    private String nomClient;
    private String rfid;
    private String statutForfait;

    public ForfaitVerificationDTO() {
    }

    public ForfaitVerificationDTO(String nomClient, String rfid, String statutForfait) {
        this.nomClient = nomClient;
        this.rfid = rfid;
        this.statutForfait = statutForfait;
    }

    // Getters et Setters
    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getStatutForfait() {
        return statutForfait;
    }

    public void setStatutForfait(String statutForfait) {
        this.statutForfait = statutForfait;
    }
}
