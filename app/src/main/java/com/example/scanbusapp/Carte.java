package com.example.scanbusapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Relation;
import java.util.List;

@Entity(tableName = "cartes")
public class Carte {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    private String rfid;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRfid() { return rfid; }
    public void setRfid(String rfid) { this.rfid = rfid; }
}
