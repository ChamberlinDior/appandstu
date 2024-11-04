package com.example.scanbusapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.Date;
import java.util.List;

@Dao
public interface ForfaitDao {

    @Insert
    void insert(Forfait forfait);

    @Update
    void update(Forfait forfait);

    // Correctement annotée avec @Query pour obtenir les forfaits par carte ID
    @Query("SELECT * FROM forfaits WHERE carteId = :carteId")
    List<Forfait> getForfaitsByCarteId(Long carteId);

    // Récupère les forfaits actifs (non expirés)
    @Query("SELECT * FROM forfaits WHERE dateExpiration >= :currentDate")
    List<Forfait> getActiveForfaits(Date currentDate);

    // Récupère tous les forfaits
    @Query("SELECT * FROM forfaits")
    List<Forfait> getAllForfaits();
}
