package com.example.scanbusapp;

import com.google.gson.annotations.SerializedName;

public class CarteDTO {

    private Long id;
    private String rfid;

    // Si le JSON renvoie les dates sous forme de chaînes, utilisez String
    // Sinon, utilisez Date et configurez le format de date dans Gson
    @SerializedName("dateCreation")
    private String dateCreation;

    @SerializedName("dateExpiration")
    private String dateExpiration;

    private String nomAgent;
    private boolean active;

    @SerializedName("forfaitActif")
    private boolean forfaitActif;

    @SerializedName("forfaitExpiration")
    private String forfaitExpiration;

    // Champs supplémentaires
    private Long clientId;

    @SerializedName("numClient")
    private String numClient;

    @SerializedName("nomClient")
    private String nomClient; // Nouveau champ pour le nom du client

    // Constructeur vide
    public CarteDTO(Carte carte) {}

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public String getRfid() {
        return rfid;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public String getDateExpiration() {
        return dateExpiration;
    }

    public String getNomAgent() {
        return nomAgent;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isForfaitActif() {
        return forfaitActif;
    }

    public String getForfaitExpiration() {
        return forfaitExpiration;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getNumClient() {
        return numClient;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setDateExpiration(String dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public void setNomAgent(String nomAgent) {
        this.nomAgent = nomAgent;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setForfaitActif(boolean forfaitActif) {
        this.forfaitActif = forfaitActif;
    }

    public void setForfaitExpiration(String forfaitExpiration) {
        this.forfaitExpiration = forfaitExpiration;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public void setNumClient(String numClient) {
        this.numClient = numClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    @Override
    public String toString() {
        return "CarteDTO{" +
                "id=" + id +
                ", rfid='" + rfid + '\'' +
                ", dateCreation='" + dateCreation + '\'' +
                ", dateExpiration='" + dateExpiration + '\'' +
                ", nomAgent='" + nomAgent + '\'' +
                ", active=" + active +
                ", forfaitActif=" + forfaitActif +
                ", forfaitExpiration='" + forfaitExpiration + '\'' +
                ", clientId=" + clientId +
                ", numClient='" + numClient + '\'' +
                ", nomClient='" + nomClient + '\'' +
                '}';
    }
}
