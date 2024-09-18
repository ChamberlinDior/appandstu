package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BusTripActivity extends AppCompatActivity {

    private ApiService apiService;
    private Spinner destinationSpinner;
    private TextView startTimeView, endTimeView, macAddressView, navbarTitle, rfidDisplay, resultView;
    private Button startTripButton, endTripButton, logoutButton, newTripButton;
    private String macAddress;
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
        rfidDisplay = findViewById(R.id.rfid_display);  // Ajout pour afficher le RFID scanné
        resultView = findViewById(R.id.result_view);  // Ajout pour afficher les résultats du client
        startTripButton = findViewById(R.id.startTrip_button);
        endTripButton = findViewById(R.id.endTrip_button);
        newTripButton = findViewById(R.id.newTrip_button);
        logoutButton = findViewById(R.id.logout_button);

        // Récupérer l'ID Android (adresse MAC) du terminal et l'afficher
        macAddress = getIntent().getStringExtra("deviceId");
        macAddressView.setText("Adresse MAC : " + macAddress);

        // Récupérer le nom et le rôle du chauffeur depuis l'intent
        String chauffeurNom = getIntent().getStringExtra("nom");
        String chauffeurRole = getIntent().getStringExtra("role");
        navbarTitle.setText(chauffeurNom + " - " + chauffeurRole);

        // Récupérer les destinations définies dans strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.destination_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destinationSpinner.setAdapter(adapter);

        // Configuration de Retrofit pour interagir avec le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.67:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // Récupérer et envoyer le niveau de batterie au backend
        sendBatteryLevel();

        // Démarrer un trajet avec confirmation
        startTripButton.setOnClickListener(v -> showConfirmationDialog(
                "Démarrer le trajet",
                "Voulez-vous vraiment démarrer ce trajet ?",
                () -> {
                    String selectedDestination = destinationSpinner.getSelectedItem().toString();
                    if (!selectedDestination.isEmpty()) {
                        startTrip(macAddress, selectedDestination);
                    } else {
                        Toast.makeText(BusTripActivity.this, "Veuillez sélectionner une destination", Toast.LENGTH_SHORT).show();
                    }
                }));

        // Terminer un trajet
        endTripButton.setOnClickListener(v -> endTrip(macAddress));

        // Action de déconnexion
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(BusTripActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BusTripActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Démarrer un nouveau trajet
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
        // Activer la détection NFC en premier plan
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Désactiver la détection NFC en premier plan
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Gérer les intents NFC
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            byte[] tagId = tag.getId();
            String rfid = bytesToHex(tagId);  // Convertir le tag en un format lisible
            rfidDisplay.setText(rfid);  // Afficher le numéro RFID dans le TextView
            Log.d(TAG, "RFID scanné : " + rfid);
            // Appeler la fonction de vérification de statut du forfait
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
                    resultView.setText("Client : " + client.getNom() + " " + client.getPrenom());
                } else {
                    resultView.setText("Client non trouvé.");
                }
            }

            @Override
            public void onFailure(Call<ClientDTO> call, Throwable t) {
                resultView.setText("Erreur de connexion : " + t.getMessage());
            }
        });
    }

    // Méthode pour afficher un popup de confirmation
    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirmer", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Annuler", null)
                .show();
    }

    // Méthode pour envoyer le niveau de batterie au backend
    private void sendBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int niveauBatterie = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        apiService.updateBusBatteryLevel(macAddress, niveauBatterie).enqueue(new Callback<Void>() {
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

    private void startTrip(String macAddress, String lastDestination) {
        Call<Void> call = apiService.startTrip(macAddress, lastDestination);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Trajet démarré avec succès", Toast.LENGTH_SHORT).show();
                    startTimeView.setText("Début du trajet : " + getFormattedDate());
                    startTripButton.setVisibility(View.GONE);
                    endTripButton.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors du démarrage du trajet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Erreur : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors de la fin du trajet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Erreur : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetTrip() {
        startTripButton.setVisibility(View.VISIBLE);
        newTripButton.setVisibility(View.GONE);
        endTimeView.setText("");
        startTimeView.setText("");
    }

    private String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", java.util.Locale.FRENCH);
        return sdf.format(new java.util.Date());
    }
}
