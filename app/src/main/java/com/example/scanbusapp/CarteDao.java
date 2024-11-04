package com.example.scanbusapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface CarteDao {

    @Insert
    void insert(Carte carte);

    @Update
    void update(Carte carte);

    @Query("SELECT * FROM cartes WHERE rfid = :rfid")
    Carte findByRfid(String rfid);

    @Query("SELECT * FROM cartes")
    List<Carte> getAllCartes();
}
