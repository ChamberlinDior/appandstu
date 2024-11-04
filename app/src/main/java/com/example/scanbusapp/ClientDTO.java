package com.example.scanbusapp;

import com.google.gson.annotations.SerializedName;

public class ClientDTO {
    private Long id;

    @SerializedName("numClient")
    private String numClient;

    private String nom;
    private String prenom;
    private String quartier;
    private String ville;

    @SerializedName("dateCreation")
    private String dateCreation;

    @SerializedName("nomAgent")
    private String nomAgent;

    private String rfid;

    @SerializedName("forfaitActif")
    private boolean forfaitActif;

    @SerializedName("forfaitExpiration")
    private String forfaitExpiration;

    // Nouveau champ pour le statut du forfait
    @SerializedName("forfaitStatus")
    private String forfaitStatus;

    // Constructeur vide
    public ClientDTO() {}

    // Constructeur complet
    public ClientDTO(Long id, String numClient, String nom, String prenom, String quartier,
                     String ville, String dateCreation, String nomAgent, String rfid,
                     boolean forfaitActif, String forfaitExpiration, String forfaitStatus) {
        this.id = id;
        this.numClient = numClient;
        this.nom = nom;
        this.prenom = prenom;
        this.quartier = quartier;
        this.ville = ville;
        this.dateCreation = dateCreation;
        this.nomAgent = nomAgent;
        this.rfid = rfid;
        this.forfaitActif = forfaitActif;
        this.forfaitExpiration = forfaitExpiration;
        this.forfaitStatus = forfaitStatus;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public String getNumClient() {
        return numClient;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getQuartier() {
        return quartier;
    }

    public String getVille() {
        return ville;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public String getNomAgent() {
        return nomAgent;
    }

    public String getRfid() {
        return rfid;
    }

    public boolean isForfaitActif() {
        return forfaitActif;
    }

    public String getForfaitExpiration() {
        return forfaitExpiration;
    }

    // Nouveau getter pour le statut du forfait
    public String getForfaitStatus() {
        return forfaitStatus;
    }

    // Setters existants
    public void setId(Long id) {
        this.id = id;
    }

    public void setNumClient(String numClient) {
        this.numClient = numClient;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setQuartier(String quartier) {
        this.quartier = quartier;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setNomAgent(String nomAgent) {
        this.nomAgent = nomAgent;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public void setForfaitActif(boolean forfaitActif) {
        this.forfaitActif = forfaitActif;
    }

    public void setForfaitExpiration(String forfaitExpiration) {
        this.forfaitExpiration = forfaitExpiration;
    }

    // Nouveau setter pour le statut du forfait
    public void setForfaitStatus(String forfaitStatus) {
        this.forfaitStatus = forfaitStatus;
    }

    @Override
    public String toString() {
        return "ClientDTO{" +
                "id=" + id +
                ", numClient='" + numClient + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", quartier='" + quartier + '\'' +
                ", ville='" + ville + '\'' +
                ", dateCreation='" + dateCreation + '\'' +
                ", nomAgent='" + nomAgent + '\'' +
                ", rfid='" + rfid + '\'' +
                ", forfaitActif=" + forfaitActif +
                ", forfaitExpiration='" + forfaitExpiration + '\'' +
                ", forfaitStatus='" + forfaitStatus + '\'' +
                '}';
    }
}
