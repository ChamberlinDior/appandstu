package com.example.scanbusapp;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

@Database(entities = {Utilisateur.class, Carte.class, Forfait.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class}) // Ajout du convertisseur de type
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract UtilisateurDao utilisateurDao();
    public abstract CarteDao carteDao();
    public abstract ForfaitDao forfaitDao(); // Méthode DAO pour Forfait

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "controller_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
