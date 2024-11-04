package com.example.scanbusapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UtilisateurDao {

    @Insert
    void insert(Utilisateur utilisateur);

    @Query("SELECT * FROM utilisateurs WHERE rfid = :rfid")
    Utilisateur getUtilisateurByRfid(String rfid);

    @Query("SELECT * FROM utilisateurs")
    List<Utilisateur> getAllUtilisateurs();
}
