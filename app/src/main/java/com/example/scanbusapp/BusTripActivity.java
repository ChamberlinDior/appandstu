package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Intent;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import utils.BatteryUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_trip);

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

        // Récupérer l'ID Android (adresse MAC) du terminal
        macAddress = getIntent().getStringExtra("deviceId");
        macAddressView.setText("Adresse MAC : " + macAddress);

        // Récupérer les informations du chauffeur
        userName = getIntent().getStringExtra("nom");
        userRole = getIntent().getStringExtra("role");
        chauffeurUniqueNumber = getIntent().getStringExtra("chauffeurUniqueNumber");

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

        // Configuration de Retrofit pour interagir avec le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://51.178.42.116:8085/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

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

        // Initialisation du NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            Toast.makeText(this, "Le NFC n'est pas supporté sur cet appareil.", Toast.LENGTH_SHORT).show();
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);
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
            String rfid = bytesToHex(tag.getId());
            Log.d(TAG, "RFID scanné : " + rfid);

            // Effacer les anciens résultats avant d'ajouter le nouveau scan
            scannedCards.clear();
            cardsAdapter.notifyDataSetChanged();

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
                fetchForfaitStatusOffline(rfid, "Nom Hors ligne");
            }
        });
    }

    private void fetchForfaitStatus(String rfid, String clientName) {
        Call<ForfaitDTO> forfaitCall = apiService.getForfaitStatus(rfid);
        forfaitCall.enqueue(new Callback<ForfaitDTO>() {
            @Override
            public void onResponse(Call<ForfaitDTO> call, Response<ForfaitDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ForfaitDTO forfait = response.body();
                    String statutForfait = forfait.getDateExpiration() != null ?
                            "Forfait Actif jusqu'à : " + new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.FRENCH).format(forfait.getDateExpiration()) :
                            "Aucun forfait actif";
                    displayResult(rfid, clientName + "\nForfait: " + statutForfait);

                    // Enregistrer la vérification du forfait
                    saveForfaitVerification(clientName, rfid, statutForfait);

                } else {
                    displayResult(rfid, "Aucun forfait actif trouvé");
                }
            }

            @Override
            public void onFailure(Call<ForfaitDTO> call, Throwable t) {
                displayResult(rfid, "Forfait non récupéré (mode hors ligne)");
            }
        });
    }

    private void fetchForfaitStatusOffline(String rfid, String clientName) {
        String statutForfait = "Forfait Actif jusqu'à : Hors ligne";
        displayResult(rfid, clientName + "\nForfait: " + statutForfait);
    }

    private void displayResult(String rfid, String message) {
        String cardInfo = "RFID: " + rfid + " - " + message;
        scannedCards.add(cardInfo);
        cardsAdapter.notifyDataSetChanged();
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
        return sdf.format(new java.util.Date());
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
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirmer", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Annuler", null)
                .show();
    }

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

        String terminalType = android.os.Build.MODEL;
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        int batteryLevel = ((BatteryManager) getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String connectionTime = getFormattedDate();

        Call<Void> call = apiService.saveForfaitVerification(verificationDTO, androidId, batteryLevel, terminalType, chauffeurUniqueNumber, connectionTime);
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
            }
        });
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
