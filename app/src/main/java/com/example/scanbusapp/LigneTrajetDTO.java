package com.example.scanbusapp;

public class LigneTrajetDTO {

    private Long id;
    private String nomLigne;
    private String typeLigne;
    private String ville;

    // Constructeur par défaut
    public LigneTrajetDTO() {}

    // Constructeur avec paramètres
    public LigneTrajetDTO(Long id, String nomLigne, String typeLigne, String ville) {
        this.id = id;
        this.nomLigne = nomLigne;
        this.typeLigne = typeLigne;
        this.ville = ville;
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomLigne() {
        return nomLigne;
    }

    public void setNomLigne(String nomLigne) {
        this.nomLigne = nomLigne;
    }

    public String getTypeLigne() {
        return typeLigne;
    }

    public void setTypeLigne(String typeLigne) {
        this.typeLigne = typeLigne;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }
}
