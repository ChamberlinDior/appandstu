package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.BatteryManager;
import android.os.Bundle;
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
import java.util.Locale;

public class BusTripActivity extends AppCompatActivity {

    private ApiService apiService;
    private Spinner destinationSpinner;
    private TextView startTimeView, endTimeView, macAddressView, navbarTitle, rfidDisplay, resultView;
    private ListView scannedCardsListView;
    private Button startTripButton, endTripButton, logoutButton, newTripButton;
    private ImageView destinationArrow;  // Flèche pour indiquer la sélection de destination
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
        rfidDisplay = findViewById(R.id.rfid_display);
        resultView = findViewById(R.id.result_view);
        scannedCardsListView = findViewById(R.id.scanned_cards_list);
        startTripButton = findViewById(R.id.startTrip_button);
        endTripButton = findViewById(R.id.endTrip_button);
        newTripButton = findViewById(R.id.newTrip_button);
        logoutButton = findViewById(R.id.logout_button);
        destinationArrow = findViewById(R.id.destination_arrow); // ImageView pour la flèche d'indication

        // Récupérer l'ID Android (adresse MAC) du terminal
        macAddress = getIntent().getStringExtra("deviceId");
        macAddressView.setText("Adresse MAC : " + macAddress);

        // Récupérer les informations du chauffeur
        userName = getIntent().getStringExtra("nom");
        userRole = getIntent().getStringExtra("role");
        chauffeurUniqueNumber = getIntent().getStringExtra("chauffeurUniqueNumber");

        navbarTitle.setText(userName + " - " + (userRole != null ? userRole : "Rôle non défini") + " - " + chauffeurUniqueNumber);

        // Adapter pour la liste des destinations
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.destination_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destinationSpinner.setAdapter(adapter);

        // Masquer les vues inutiles au départ
        destinationSpinner.setVisibility(View.GONE);
        endTripButton.setVisibility(View.GONE);
        newTripButton.setVisibility(View.GONE);
        rfidDisplay.setVisibility(View.GONE);
        resultView.setVisibility(View.GONE);
        scannedCardsListView.setVisibility(View.GONE);
        destinationArrow.setVisibility(View.GONE);  // Masquer la flèche

        // Initialiser la liste des cartes scannées
        scannedCards = new ArrayList<>();
        cardsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scannedCards);
        scannedCardsListView.setAdapter(cardsAdapter);

        // Configuration de Retrofit pour interagir avec le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://51.178.42.116:8089/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Récupérer et envoyer le niveau de batterie au backend
        sendBatteryLevel();

        // Démarrer un trajet
        startTripButton.setOnClickListener(v -> showConfirmationDialog(
                "Démarrer le trajet",
                "Voulez-vous vraiment démarrer ce trajet ?",
                () -> {
                    destinationSpinner.setVisibility(View.VISIBLE);
                    destinationArrow.setVisibility(View.VISIBLE); // Afficher la flèche
                    startTripButton.setVisibility(View.GONE);
                    startTimeView.setText("Début du trajet : " + getFormattedDate());
                }
        ));

        // Sélectionner une destination et activer le scanner
        destinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {  // Une destination est sélectionnée
                    rfidDisplay.setVisibility(View.VISIBLE);
                    scannedCardsListView.setVisibility(View.VISIBLE);
                    endTripButton.setVisibility(View.VISIBLE);
                    destinationArrow.setVisibility(View.GONE);  // Masquer la flèche après sélection
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
            resultView.setText("Le NFC n'est pas supporté sur cet appareil.");
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
            rfidDisplay.setText(rfid);
            Log.d(TAG, "RFID scanné : " + rfid);
            checkForfaitStatusOffline(rfid);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // Méthode qui tente d'envoyer les données en ligne et, en cas d'échec, affiche les données en mode hors ligne
    private void checkForfaitStatusOffline(String rfid) {
        Call<ClientDTO> call = apiService.verifyCard(rfid);
        call.enqueue(new Callback<ClientDTO>() {
            @Override
            public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClientDTO client = response.body();
                    fetchForfaitStatus(rfid, client.getNom());
                } else {
                    displayOfflineResult(rfid, "Client non trouvé");
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
                    String cardInfo = "RFID: " + rfid + " - " + clientName + "\nForfait: " + statutForfait;
                    scannedCards.add(cardInfo);
                    cardsAdapter.notifyDataSetChanged();
                } else {
                    displayOfflineResult(rfid, "Aucun forfait actif trouvé");
                }
            }

            @Override
            public void onFailure(Call<ForfaitDTO> call, Throwable t) {
                displayOfflineResult(rfid, "Forfait non récupéré (mode hors ligne)");
            }
        });
    }

    private void fetchForfaitStatusOffline(String rfid, String clientName) {
        String cardInfo = "RFID: " + rfid + " - " + clientName + "\nForfait: Actif jusqu'à : Hors ligne";
        scannedCards.add(cardInfo);
        cardsAdapter.notifyDataSetChanged();
    }

    private void displayOfflineResult(String rfid, String message) {
        String offlineResult = "RFID: " + rfid + " - " + message;
        scannedCards.add(offlineResult);
        cardsAdapter.notifyDataSetChanged();
    }

    private void resetTrip() {
        startTripButton.setVisibility(View.VISIBLE);
        newTripButton.setVisibility(View.GONE);
        startTimeView.setText("");
        endTimeView.setText("");
        destinationSpinner.setVisibility(View.GONE);
        rfidDisplay.setVisibility(View.GONE);
        scannedCardsListView.setVisibility(View.GONE);
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

        apiService.updateBusBatteryLevel(macAddress, niveauBatterie, isCharging).enqueue(new Callback<Void>() {
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
}
