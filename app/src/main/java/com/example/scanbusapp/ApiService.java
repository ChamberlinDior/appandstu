package com.example.scanbusapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Vérifier un client via RFID
    @GET("/api/clients/rfid/{rfid}")
    Call<ClientDTO> verifyCard(@Path("rfid") String rfid);

    // Attribuer un forfait à un client
    @POST("/api/forfaits")
    Call<Void> assignForfait(@Body ForfaitDTO forfaitDTO);

    // Vérifier le statut d'un forfait via RFID
    @GET("/api/forfaits/status/{rfid}")
    Call<ForfaitDTO> getForfaitStatus(@Path("rfid") String rfid);

    // Enregistrer la vérification d'un forfait avec les informations supplémentaires
    @POST("/api/forfait-verifications")
    Call<Void> saveForfaitVerification(@Body ForfaitVerificationDTO forfaitVerificationDTO,
                                       @Query("androidId") String androidId,
                                       @Query("batteryLevel") int batteryLevel,
                                       @Query("terminalType") String terminalType,
                                       @Query("chauffeurUniqueNumber") String chauffeurUniqueNumber,
                                       @Query("connectionTime") String connectionTime);

    // Mise à jour de la destination uniquement via l'adresse MAC du terminal
    @POST("/api/buses/mac/{macAddress}/update-destination")
    Call<Void> updateDestination(@Path("macAddress") String macAddress,
                                 @Query("lastDestination") String lastDestination);

    // Vérifier un utilisateur via son numéro unique
    @GET("/api/utilisateurs/unique/{uniqueUserNumber}")
    Call<UtilisateurDTO> getUtilisateurByUniqueUserNumber(@Path("uniqueUserNumber") String uniqueUserNumber);

    // Enregistrer un chauffeur avec l'identifiant unique de l'appareil et les informations du chauffeur
    @POST("/api/buses/mac/{macAddress}/update-chauffeur-destination")
    Call<Void> updateChauffeurAndDestination(@Path("macAddress") String macAddress,
                                             @Query("lastDestination") String lastDestination,
                                             @Query("chauffeurNom") String chauffeurNom,
                                             @Query("chauffeurUniqueNumber") String chauffeurUniqueNumber,
                                             @Query("batteryLevel") int batteryLevel,  // Ajout du niveau de batterie
                                             @Query("androidId") String androidId,     // Ajout de l'ID Android
                                             @Query("terminalType") String terminalType);  // Ajout du type de terminal

    // Démarrer un trajet avec les informations supplémentaires
    @POST("/api/buses/mac/{macAddress}/start-trip")
    Call<Void> startTrip(@Path("macAddress") String macAddress,
                         @Query("lastDestination") String lastDestination,
                         @Query("chauffeurNom") String chauffeurNom,
                         @Query("chauffeurUniqueNumber") String chauffeurUniqueNumber,
                         @Query("batteryLevel") int batteryLevel,
                         @Query("androidId") String androidId,
                         @Query("terminalType") String terminalType);

    // Terminer un trajet
    @POST("/api/buses/mac/{macAddress}/end-trip")
    Call<Void> endTrip(@Path("macAddress") String macAddress);

    // Mise à jour du niveau de batterie et état de charge du bus avec les informations supplémentaires
    @POST("/api/buses/mac/{macAddress}/update-battery")
    Call<Void> updateBusBatteryLevel(@Path("macAddress") String macAddress,
                                     @Query("niveauBatterie") Integer niveauBatterie,
                                     @Query("isCharging") boolean isCharging,
                                     @Query("androidId") String androidId,
                                     @Query("terminalType") String terminalType);

    // Méthode pour obtenir tous les clients
    @GET("/api/clients")
    Call<List<ClientDTO>> getAllClients();

    // Récupérer toutes les lignes de trajet (destinations)
    @GET("/api/lignes")
    Call<List<LigneTrajetDTO>> getAllLignes();  // Ajout pour récupérer toutes les lignes de trajet

    // Enregistrer l'historique d'un trajet de bus
    @POST("/api/bus-history/create")
    Call<Void> saveBusHistory(@Body BusHistoryDTO busHistoryDTO);
}
