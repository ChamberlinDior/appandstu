package com.example.scanbusapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Vérifier un client via RFID
    @GET("/api/clients/rfid/{rfid}")
    Call<ClientDTO> verifyCard(@Path("rfid") String rfid);

    // Récupérer un client par ID
    @GET("/api/clients/{id}")
    Call<ClientDTO> getClientById(@Path("id") Long id);

    // Créer un nouveau client
    @POST("/api/clients")
    Call<ClientDTO> createClient(@Body ClientDTO clientDTO);

    // Ajouter une carte à un client
    @POST("/api/clients/{clientId}/cartes")
    Call<CarteDTO> addCarteToClient(@Path("clientId") Long clientId, @Body CarteDTO carteDTO);

    // Récupérer toutes les cartes d'un client
    @GET("/api/clients/{clientId}/cartes")
    Call<List<CarteDTO>> getClientCartes(@Path("clientId") Long clientId);

    // Mettre à jour une carte
    @PUT("/api/clients/cartes/{carteId}")
    Call<CarteDTO> updateCarte(@Path("carteId") Long carteId, @Body CarteDTO carteDTO);

    // Attribuer un forfait à une carte
    @POST("/api/forfaits")
    Call<Void> assignForfait(@Body ForfaitDTO forfaitDTO);

    // Récupérer le statut d'un forfait via RFID
    @GET("/api/forfaits/status/{rfid}")
    Call<ForfaitDTO> getForfaitStatus(@Path("rfid") String rfid);

    // Récupérer l'historique des forfaits d'une carte par ID
    @GET("/api/forfaits/historique/{carteId}")
    Call<List<ForfaitDTO>> getForfaitHistory(@Path("carteId") Long carteId);

    // Enregistrer la vérification d'un forfait avec des informations supplémentaires
    @POST("/api/forfait-verifications")
    Call<Void> saveForfaitVerification(@Body ForfaitVerificationDTO forfaitVerificationDTO,
                                       @Query("androidId") String androidId,
                                       @Query("batteryLevel") int batteryLevel,
                                       @Query("terminalType") String terminalType,
                                       @Query("chauffeurUniqueNumber") String chauffeurUniqueNumber,
                                       @Query("connectionTime") String connectionTime);

    // Mettre à jour la destination uniquement via l'adresse MAC du terminal
    @POST("/api/buses/mac/{macAddress}/update-destination")
    Call<Void> updateDestination(@Path("macAddress") String macAddress,
                                 @Query("lastDestination") String lastDestination);

    // Récupérer un utilisateur via RFID
    @GET("/api/utilisateurs/rfid/{rfid}")
    Call<UtilisateurDTO> getUtilisateurByRfid(@Path("rfid") String rfid);

    // Vérifier un utilisateur via son numéro unique
    @GET("/api/utilisateurs/unique/{uniqueUserNumber}")
    Call<UtilisateurDTO> getUtilisateurByUniqueUserNumber(@Path("uniqueUserNumber") String uniqueUserNumber);

    // Login de l'utilisateur via son numéro unique et nom (ancienne méthode)
    @GET("/api/utilisateurs/login")
    Call<UtilisateurDTO> loginUtilisateur(@Query("uniqueUserNumber") String uniqueUserNumber,
                                          @Query("nom") String nom);

    // Enregistrer un chauffeur avec l'identifiant unique de l'appareil et les informations du chauffeur
    @POST("/api/buses/mac/{macAddress}/update-chauffeur-destination")
    Call<Void> updateChauffeurAndDestination(@Path("macAddress") String macAddress,
                                             @Query("lastDestination") String lastDestination,
                                             @Query("chauffeurNom") String chauffeurNom,
                                             @Query("chauffeurUniqueNumber") String chauffeurUniqueNumber,
                                             @Query("batteryLevel") int batteryLevel,
                                             @Query("androidId") String androidId,
                                             @Query("terminalType") String terminalType);

    // Démarrer un trajet avec des informations supplémentaires
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

    // Mise à jour du niveau de batterie et état de charge du bus
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
    Call<List<LigneTrajetDTO>> getAllLignes();

    // Enregistrer l'historique d'un trajet de bus avec les informations du chauffeur
    @POST("/api/bus-history/create")
    Call<Void> saveBusHistory(@Body BusHistoryDTO busHistoryDTO);
}
