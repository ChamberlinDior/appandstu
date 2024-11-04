package com.example.scanbusapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "utilisateurs")
public class Utilisateur {
    @PrimaryKey(autoGenerate = true)
    private Long id;

    private String uniqueUserNumber;
    private String nom;
    private String prenom;
    private String role;
    private String rfid;
    private String dateCreation;

    // Constructeurs
    public Utilisateur() {
    }

    public Utilisateur(String uniqueUserNumber, String nom, String prenom, String role, String rfid, String dateCreation) {
        this.uniqueUserNumber = uniqueUserNumber;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
        this.rfid = rfid;
        this.dateCreation = dateCreation;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueUserNumber() {
        return uniqueUserNumber;
    }

    public void setUniqueUserNumber(String uniqueUserNumber) {
        this.uniqueUserNumber = uniqueUserNumber;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }
}
