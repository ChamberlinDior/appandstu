package com.example.scanbusapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "forfaits",
        foreignKeys = @ForeignKey(entity = Carte.class,
                parentColumns = "id",
                childColumns = "carteId",
                onDelete = ForeignKey.CASCADE))
public class Forfait {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "typeForfait")
    private String typeForfait;

    @ColumnInfo(name = "dateActivation")
    private Date dateActivation; // Utilisation du convertisseur

    @ColumnInfo(name = "dateExpiration")
    private Date dateExpiration; // Utilisation du convertisseur

    @ColumnInfo(name = "carteId")
    private Long carteId;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTypeForfait() { return typeForfait; }
    public void setTypeForfait(String typeForfait) { this.typeForfait = typeForfait; }

    public Date getDateActivation() { return dateActivation; }
    public void setDateActivation(Date dateActivation) { this.dateActivation = dateActivation; }

    public Date getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(Date dateExpiration) { this.dateExpiration = dateExpiration; }

    public Long getCarteId() { return carteId; }
    public void setCarteId(Long carteId) { this.carteId = carteId; }
}
