package com.example.scanbusapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import utils.BatteryUtils;

public class BusTripActivity extends AppCompatActivity {

    private ApiService apiService;
    private Spinner destinationSpinner;
    private TextView startTimeView, endTimeView, macAddressView, navbarTitle;
    private ListView scannedCardsListView;
    private Button startTripButton, endTripButton, logoutButton, newTripButton;
    private ImageView destinationArrow;
    private String macAddress, userName, userRole, chauffeurUniqueNumber;
    private ArrayList<String> scannedCards;
    private ArrayAdapter<String> cardsAdapter;
    private static final String TAG = "BusTripActivity";
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    private LocalDatabaseHelper dbHelper;
    private ConnectivityManager connectivityManager;

    // Ajout pour gérer les scans hors ligne
    private SharedPreferences sharedPreferences;
    private static final String OFFLINE_SCANS_PREF = "offline_scans_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_trip);

        // Récupérer les informations utilisateur à partir de l'intent
        macAddress = getIntent().getStringExtra("deviceId");
        userName = getIntent().getStringExtra("nom");
        userRole = getIntent().getStringExtra("role");
        chauffeurUniqueNumber = getIntent().getStringExtra("chauffeurUniqueNumber");

        // Initialisation des vues
        destinationSpinner = findViewById(R.id.destination_spinner);
        startTimeView = findViewById(R.id.startTime_view);
        endTimeView = findViewById(R.id.endTime_view);
        macAddressView = findViewById(R.id.macAddress_view);
        navbarTitle = findViewById(R.id.navbar_title);
        scannedCardsListView = findViewById(R.id.scanned_cards_list);
        startTripButton = findViewById(R.id.startTrip_button);
        endTripButton = findViewById(R.id.endTrip_button);
        newTripButton = findViewById(R.id.newTrip_button);
        logoutButton = findViewById(R.id.logout_button);
        destinationArrow = findViewById(R.id.destination_arrow);

        macAddressView.setText("Adresse MAC : " + macAddress);
        navbarTitle.setText(userName + " - " + (userRole != null ? userRole : "Rôle non défini") + " - " + chauffeurUniqueNumber);

        // Masquer les vues inutiles au départ
        startTripButton.setVisibility(View.GONE); // Masquer le bouton jusqu'à sélection de destination
        endTripButton.setVisibility(View.GONE);
        newTripButton.setVisibility(View.GONE);
        scannedCardsListView.setVisibility(View.GONE);
        destinationArrow.setVisibility(View.GONE);  // Masquer la flèche

        // Initialiser la liste des cartes scannées
        scannedCards = new ArrayList<>();
        cardsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scannedCards);
        scannedCardsListView.setAdapter(cardsAdapter);

        dbHelper = new LocalDatabaseHelper(this);

        // Initialisation des SharedPreferences pour stocker les scans hors ligne
        sharedPreferences = getSharedPreferences("BusTripAppPrefs", MODE_PRIVATE);

        // Configuration de Retrofit pour interagir avec le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://51.178.42.116:8085/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Configuration du NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            Toast.makeText(this, "Le NFC n'est pas supporté sur cet appareil.", Toast.LENGTH_SHORT).show();
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        // Configuration de la surveillance réseau
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerNetworkCallback();

        // Récupérer les destinations depuis la base de données
        fetchDestinations();

        // Récupérer et envoyer le niveau de batterie au backend
        sendBatteryLevel();

        // Sélectionner une destination
        destinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {  // Une destination est sélectionnée
                    scannedCardsListView.setVisibility(View.VISIBLE); // Montrer le tableau
                    startTripButton.setVisibility(View.VISIBLE); // Montrer le bouton pour démarrer le trajet
                    destinationArrow.setVisibility(View.GONE);  // Masquer la flèche après sélection
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                startTripButton.setVisibility(View.GONE);  // Cacher le bouton si aucune destination n'est sélectionnée
            }
        });

        // Démarrer un trajet
        startTripButton.setOnClickListener(v -> {
            String selectedDestination = destinationSpinner.getSelectedItem().toString();
            if (!selectedDestination.equals("Sélectionner une destination")) {
                showConfirmationDialog(
                        "Démarrer le trajet",
                        "Voulez-vous vraiment démarrer ce trajet avec la destination : " + selectedDestination + " ?",
                        () -> {
                            startTripButton.setVisibility(View.GONE);
                            startTimeView.setText("Début du trajet : " + getFormattedDate());
                            endTripButton.setVisibility(View.VISIBLE); // Montrer le bouton pour terminer le trajet
                            // Appel API pour démarrer le trajet
                            startTrip(macAddress, selectedDestination, userName, chauffeurUniqueNumber);
                        }
                );
            } else {
                Toast.makeText(BusTripActivity.this, "Veuillez sélectionner une destination", Toast.LENGTH_SHORT).show();
            }
        });

        // Terminer un trajet
        endTripButton.setOnClickListener(v -> endTrip(macAddress));

        // Déconnexion
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(BusTripActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(BusTripActivity.this, LoginActivity.class));
            finish();
        });

        // Nouveau trajet
        newTripButton.setOnClickListener(v -> showConfirmationDialog(
                "Commencer un nouveau trajet",
                "Voulez-vous vraiment démarrer un nouveau trajet ?",
                this::resetTrip));
    }

    // Méthode pour vérifier la connexion Internet
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Enregistrement pour les changements de réseau
    private void registerNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Connexion Internet rétablie. Synchronisation des transactions hors ligne...");
                synchronizeForfaits();
                synchronizeOfflineTransactions();
                processOfflineScans(); // Traiter les scans hors ligne
                updateUI();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Connexion Internet perdue.");
            }
        });
    }

    // Synchronisation des scans hors ligne
    private void processOfflineScans() {
        Set<String> offlineScans = new HashSet<>(sharedPreferences.getStringSet(OFFLINE_SCANS_PREF, new HashSet<>()));

        for (String rfid : offlineScans) {
            // Re-traiter le scan comme s'il venait d'être scanné
            checkForfaitStatus(rfid);
        }

        // Effacer les scans hors ligne après traitement
        sharedPreferences.edit().remove(OFFLINE_SCANS_PREF).apply();
    }

    // Méthode pour enregistrer les scans hors ligne
    private void saveOfflineScan(String rfid) {
        Set<String> offlineScans = new HashSet<>(sharedPreferences.getStringSet(OFFLINE_SCANS_PREF, new HashSet<>()));
        offlineScans.add(rfid);
        sharedPreferences.edit().putStringSet(OFFLINE_SCANS_PREF, offlineScans).apply();
    }

    // Enregistrer une vérification hors ligne
    private void saveOfflineVerification(ForfaitVerificationDTO verificationDTO) {
        // Enregistrer dans la base de données locale ou SharedPreferences
        // Ici, nous utilisons SharedPreferences pour simplifier
        Set<String> offlineVerifications = new HashSet<>(sharedPreferences.getStringSet("offline_verifications", new HashSet<>()));
        offlineVerifications.add(verificationDTO.getRfid()); // Vous pouvez stocker plus d'informations si nécessaire
        sharedPreferences.edit().putStringSet("offline_verifications", offlineVerifications).apply();
    }

    // Synchronisation des transactions hors ligne avec le serveur
    private void synchronizeOfflineTransactions() {
        Cursor cursor = dbHelper.getOfflineTransactions();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String rfid = cursor.getString(cursor.getColumnIndexOrThrow("rfid"));
                String forfaitType = cursor.getString(cursor.getColumnIndexOrThrow("forfait_type"));

                ForfaitDTO forfaitDTO = new ForfaitDTO(forfaitType, rfid);
                Call<Void> call = apiService.assignForfait(forfaitDTO);
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Forfait hors ligne synchronisé avec succès.");
                            // Supprimer la transaction hors ligne une fois synchronisée
                            dbHelper.deleteOfflineTransaction(rfid, forfaitType);
                        } else {
                            Log.e(TAG, "Erreur lors de la synchronisation du forfait hors ligne.");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Échec de la synchronisation du forfait hors ligne : " + t.getMessage());
                    }
                });
            } while (cursor.moveToNext());
        }
    }

    // Synchronisation des forfaits depuis le serveur
    private void synchronizeForfaits() {
        Call<List<ClientDTO>> call = apiService.getAllClients();
        call.enqueue(new Callback<List<ClientDTO>>() {
            @Override
            public void onResponse(Call<List<ClientDTO>> call, Response<List<ClientDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ClientDTO> clients = response.body();
                    for (ClientDTO client : clients) {
                        dbHelper.saveCardInfo(client.getRfid(), client.getNom(), client.isForfaitActif(), client.getForfaitExpiration());
                    }
                    Toast.makeText(BusTripActivity.this, "Données synchronisées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Échec de la synchronisation : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<ClientDTO>> call, Throwable t) {
                Log.e(TAG, "Erreur lors de la synchronisation des forfaits : " + t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            byte[] tagId = tag.getId();
            String rfid = bytesToHex(tagId);
            Log.d(TAG, "RFID scanné : " + rfid);

            // Vérifier le statut de la carte
            checkForfaitStatus(rfid);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private void checkForfaitStatus(String rfid) {
        if (isConnected()) {
            Call<ClientDTO> call = apiService.verifyCard(rfid);
            call.enqueue(new Callback<ClientDTO>() {
                @Override
                public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ClientDTO client = response.body();
                        fetchForfaitStatus(rfid, client.getNom());
                    } else {
                        displayResult(rfid, "Client non trouvé");
                    }
                }

                @Override
                public void onFailure(Call<ClientDTO> call, Throwable t) {
                    fetchForfaitStatusOffline(rfid);
                }
            });
        } else {
            // Enregistrer le scan hors ligne
            saveOfflineScan(rfid);
            fetchForfaitStatusOffline(rfid);
        }
    }

    private void fetchForfaitStatus(String rfid, String clientName) {
        Call<ForfaitDTO> forfaitCall = apiService.getForfaitStatus(rfid);
        forfaitCall.enqueue(new Callback<ForfaitDTO>() {
            @Override
            public void onResponse(Call<ForfaitDTO> call, Response<ForfaitDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ForfaitDTO forfait = response.body();
                    String statutForfait = forfait.getDateExpiration() != null ?
                            "Forfait Actif jusqu'à : " + new SimpleDateFormat("d MMMM yyyy", Locale.FRENCH).format(forfait.getDateExpiration()) :
                            "Aucun forfait actif";
                    displayResult(rfid, clientName + "\n" + statutForfait);

                    // Enregistrer la vérification du forfait
                    saveForfaitVerification(clientName, rfid, statutForfait);

                } else {
                    displayResult(rfid, "Aucun forfait actif trouvé");
                }
            }

            @Override
            public void onFailure(Call<ForfaitDTO> call, Throwable t) {
                displayResult(rfid, "Erreur lors de la récupération du forfait.");
            }
        });
    }

    private void fetchForfaitStatusOffline(String rfid) {
        Cursor cursor = dbHelper.getCardInfo(rfid);
        if (cursor != null && cursor.moveToFirst()) {
            String clientName = cursor.getString(cursor.getColumnIndexOrThrow("client_name"));
            boolean forfaitActive = cursor.getInt(cursor.getColumnIndexOrThrow("forfait_active")) == 1;
            String forfaitExpiration = cursor.getString(cursor.getColumnIndexOrThrow("forfait_expiration"));

            String statutForfait;
            if (forfaitActive) {
                // Conversion de la date en format lisible
                String formattedDate = formatDate(forfaitExpiration);
                statutForfait = "Forfait Actif jusqu'à : " + formattedDate;
            } else {
                statutForfait = "Aucun forfait actif";
            }

            displayResult(rfid, "Client : " + clientName + "\n" + statutForfait);

            // Enregistrer la vérification du forfait hors ligne
            saveForfaitVerification(clientName, rfid, statutForfait);

        } else {
            displayResult(rfid, "Carte non trouvée. Aucun forfait actif.");
            Toast.makeText(this, "Cette carte n'a jamais été scannée auparavant.", Toast.LENGTH_SHORT).show();
        }
    }

    // Nouvelle méthode pour formater la date
    private String formatDate(String dateString) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM yyyy", Locale.FRENCH);
        try {
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // Retourne la date originale en cas d'erreur
        }
    }

    private void displayResult(String rfid, String message) {
        String cardInfo = "RFID: " + rfid + " - " + message;
        scannedCards.add(cardInfo);
        cardsAdapter.notifyDataSetChanged();
    }

    // Méthode pour enregistrer la vérification du forfait avec des informations supplémentaires
    private void saveForfaitVerification(String clientName, String rfid, String statutForfait) {
        ForfaitVerificationDTO verificationDTO = new ForfaitVerificationDTO(
                clientName,
                rfid,
                statutForfait,
                macAddress,
                userRole,
                userName,
                true
        );

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String terminalType = android.os.Build.MODEL;
        String connectionTime = getFormattedDate();

        if (isConnected()) {
            Call<Void> call = apiService.saveForfaitVerification(verificationDTO, androidId, batteryLevel, terminalType, userName, connectionTime);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Vérification du forfait enregistrée avec succès.");
                    } else {
                        Log.e(TAG, "Erreur lors de l'enregistrement de la vérification.");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "Erreur lors de l'enregistrement de la vérification.", t);
                    // Enregistrer localement pour synchronisation ultérieure
                    saveOfflineVerification(verificationDTO);
                }
            });
        } else {
            // Enregistrer localement pour synchronisation ultérieure
            saveOfflineVerification(verificationDTO);
        }
    }

    // Méthode pour rafraîchir l'interface utilisateur sans redémarrer l'activité
    private void updateUI() {
        runOnUiThread(() -> {
            // Mettre à jour uniquement les éléments nécessaires, sans redémarrer l'activité
            // Par exemple, vider l'affichage des cartes scannées
            scannedCards.clear();
            cardsAdapter.notifyDataSetChanged();
        });
    }

    private void resetTrip() {
        startTripButton.setVisibility(View.GONE);
        newTripButton.setVisibility(View.GONE);
        startTimeView.setText("");
        endTimeView.setText("");
        scannedCards.clear();
        cardsAdapter.notifyDataSetChanged();
    }

    private void endTrip(String macAddress) {
        Call<Void> call = apiService.endTrip(macAddress);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Trajet terminé avec succès", Toast.LENGTH_SHORT).show();
                    endTimeView.setText("Fin du trajet : " + getFormattedDate());
                    endTripButton.setVisibility(View.GONE);
                    newTripButton.setVisibility(View.VISIBLE);

                    // Enregistrer l'historique de fin de trajet
                    saveBusHistory();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Erreur : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale.FRENCH);
        return sdf.format(new Date());
    }

    private void sendBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int niveauBatterie = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        boolean isCharging = BatteryUtils.isCharging(this);

        String terminalType = android.os.Build.MODEL;  // Récupérer le type de terminal
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID); // Récupérer l'ID Android

        apiService.updateBusBatteryLevel(macAddress, niveauBatterie, isCharging, androidId, terminalType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Niveau de batterie mis à jour", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors de la mise à jour du niveau de batterie", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Erreur: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirmer", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void startTrip(String macAddress, String destination, String chauffeurNom, String chauffeurUniqueNumber) {
        String terminalType = android.os.Build.MODEL;
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        int batteryLevel = ((BatteryManager) getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        Call<Void> call = apiService.startTrip(macAddress, destination, chauffeurNom, chauffeurUniqueNumber, batteryLevel, androidId, terminalType);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Trajet démarré avec succès pour la destination : " + destination, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors du démarrage du trajet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Échec de la connexion au serveur", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Récupérer les lignes de trajet depuis la base de données
    private void fetchDestinations() {
        Call<List<LigneTrajetDTO>> call = apiService.getAllLignes();
        call.enqueue(new Callback<List<LigneTrajetDTO>>() {
            @Override
            public void onResponse(Call<List<LigneTrajetDTO>> call, Response<List<LigneTrajetDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LigneTrajetDTO> lignes = response.body();
                    List<String> destinationNames = new ArrayList<>();
                    destinationNames.add("Sélectionner une destination");
                    for (LigneTrajetDTO ligne : lignes) {
                        destinationNames.add(ligne.getNomLigne());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(BusTripActivity.this,
                            android.R.layout.simple_spinner_item, destinationNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    destinationSpinner.setAdapter(adapter);
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors de la récupération des destinations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<LigneTrajetDTO>> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Échec de la récupération des destinations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Méthode pour enregistrer l'historique du trajet
    private void saveBusHistory() {
        String destination = destinationSpinner.getSelectedItem().toString();
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String connectionTime = getFormattedDate(); // Utiliser la méthode existante pour obtenir la date/heure actuelle

        int batteryLevel = ((BatteryManager) getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String terminalType = android.os.Build.MODEL;

        // Création de l'objet BusHistoryDTO avec tous les arguments requis
        BusHistoryDTO busHistoryDTO = new BusHistoryDTO(
                macAddress,
                destination,
                userName,
                chauffeurUniqueNumber,
                batteryLevel,
                terminalType,
                androidId,
                connectionTime
        );

        // Envoi des données via l'API pour enregistrer l'historique
        Call<Void> call = apiService.saveBusHistory(busHistoryDTO);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Historique du trajet enregistré avec succès.");
                } else {
                    Log.e(TAG, "Erreur lors de l'enregistrement de l'historique.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Erreur lors de l'enregistrement de l'historique.", t);
            }
        });
    }
}
