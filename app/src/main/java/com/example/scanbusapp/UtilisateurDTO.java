package com.example.scanbusapp;

public class UtilisateurDTO {
    private Long id;
    private String uniqueUserNumber;
    private String nom;
    private String prenom;
    private String role;
    private String rfid;
    private String dateCreation;

    // Constructeur par défaut
    public UtilisateurDTO() {
    }

    // Constructeur avec paramètres
    public UtilisateurDTO(Long id, String uniqueUserNumber, String nom, String prenom, String role, String rfid, String dateCreation) {
        this.id = id;
        this.uniqueUserNumber = uniqueUserNumber;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
        this.rfid = rfid;
        this.dateCreation = dateCreation;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUniqueUserNumber() {
        return uniqueUserNumber;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getRole() {
        return role;
    }

    public String getRfid() {
        return rfid;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setUniqueUserNumber(String uniqueUserNumber) {
        this.uniqueUserNumber = uniqueUserNumber;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    // Méthode toString() pour faciliter le débogage et l'affichage
    @Override
    public String toString() {
        return "UtilisateurDTO{" +
                "id=" + id +
                ", uniqueUserNumber='" + uniqueUserNumber + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", role='" + role + '\'' +
                ", rfid='" + rfid + '\'' +
                ", dateCreation='" + dateCreation + '\'' +
                '}';
    }
}
