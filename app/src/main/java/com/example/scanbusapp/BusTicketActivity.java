package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.view.View;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class BusTicketActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText rfidInput;
    private TextView resultView;
    private TextView profileTextView;
    private TextView driverNameTextView;
    private TextView driverRoleTextView;
    private Button verifyButton, forfaitDayButton, forfaitWeekButton, forfaitMonthButton, checkForfaitStatusButton, logoutButton;
    private String macAddress;
    private static final String TAG = "BusTicketActivity";
    private LocalDatabaseHelper dbHelper;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_ticket);

        dbHelper = new LocalDatabaseHelper(this); // Initialisation de la base de données locale

        String nom = getIntent().getStringExtra("nom");
        String role = getIntent().getStringExtra("role");
        macAddress = getIntent().getStringExtra("deviceId");

        Log.d(TAG, "Nom utilisateur: " + nom + ", Rôle: " + role + ", MAC: " + macAddress);

        rfidInput = findViewById(R.id.rfid_input);
        resultView = findViewById(R.id.result_view);
        profileTextView = findViewById(R.id.profile_text_view);
        driverNameTextView = findViewById(R.id.driver_name_text_view);
        driverRoleTextView = findViewById(R.id.driver_role_text_view);
        verifyButton = findViewById(R.id.verify_button);
        forfaitDayButton = findViewById(R.id.forfait_day_button);
        forfaitWeekButton = findViewById(R.id.forfait_week_button);
        forfaitMonthButton = findViewById(R.id.forfait_month_button);
        checkForfaitStatusButton = findViewById(R.id.check_forfait_status_button);
        logoutButton = findViewById(R.id.logout_button);

        // Modification des labels pour les boutons en français
        verifyButton.setText("Vérifier la carte");
        forfaitDayButton.setText("Forfait jour");
        forfaitWeekButton.setText("Forfait semaine");
        forfaitMonthButton.setText("Forfait mois");
        checkForfaitStatusButton.setText("Vérifier statut du forfait");

        driverNameTextView.setText(getString(R.string.user_label) + " " + nom);
        driverRoleTextView.setText(getString(R.string.role_label) + " " + role);

        // Action du bouton de déconnexion
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(BusTicketActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BusTicketActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Fermer l'activité actuelle
        });

        // Masquer les boutons pour les chauffeurs et contrôleurs
        if ("chauffeur".equalsIgnoreCase(role) || "controleur".equalsIgnoreCase(role)) {
            hideNonChauffeurOrControleurButtons();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.68:8080/")  // URL de l'API à ajuster
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        verifyButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "RFID saisi : " + rfid);
            verifyCard(rfid);
        });

        forfaitDayButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Attribuer le forfait jour pour le RFID : " + rfid);
            assignForfait(rfid, "jour");
        });

        forfaitWeekButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Attribuer le forfait semaine pour le RFID : " + rfid);
            assignForfait(rfid, "semaine");
        });

        forfaitMonthButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Attribuer le forfait mois pour le RFID : " + rfid);
            assignForfait(rfid, "mois");
        });

        checkForfaitStatusButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Vérification du statut du forfait pour le RFID : " + rfid);
            checkForfaitStatus(rfid);
        });

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
            String rfid = bytesToHex(tagId); // Convertir le tag en un format lisible
            rfidInput.setText(rfid); // Afficher le numéro RFID dans le champ rfidInput
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

    // Méthode pour masquer les boutons "Forfait" pour les chauffeurs et contrôleurs
    private void hideNonChauffeurOrControleurButtons() {
        forfaitDayButton.setVisibility(View.GONE);
        forfaitWeekButton.setVisibility(View.GONE);
        forfaitMonthButton.setVisibility(View.GONE);
    }

    // Vérifier si l'appareil est connecté à Internet
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Vérifier l'état du forfait et afficher les informations du client
    private void checkForfaitStatus(String rfid) {
        if (isInternetAvailable()) {
            // Cas avec connexion Internet
            Call<ClientDTO> call = apiService.verifyCard(rfid);
            call.enqueue(new Callback<ClientDTO>() {
                @Override
                public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ClientDTO client = response.body();
                        // Appel pour vérifier le forfait du client
                        Call<ForfaitDTO> forfaitCall = apiService.getForfaitStatus(rfid);
                        forfaitCall.enqueue(new Callback<ForfaitDTO>() {
                            @Override
                            public void onResponse(Call<ForfaitDTO> call, Response<ForfaitDTO> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    ForfaitDTO forfait = response.body();
                                    if (forfait.getDateExpiration() != null) {
                                        Log.d(TAG, "Forfait actif jusqu'à : " + forfait.getDateExpiration());
                                        resultView.setText("Client : " + client.getNom() + " " + client.getPrenom() +
                                                "\nForfait actif jusqu'à : " + forfait.getDateExpiration());
                                        // Enregistrer les informations localement
                                        dbHelper.saveCardInfo(rfid, client.getNom(), true, forfait.getDateExpiration().toString());
                                    } else {
                                        Log.d(TAG, "Aucun forfait actif trouvé pour ce client.");
                                        resultView.setText("Client : " + client.getNom() + " " + client.getPrenom() +
                                                "\nAucun forfait actif trouvé.");
                                    }
                                } else {
                                    Log.e(TAG, "Erreur lors de la vérification du forfait.");
                                    resultView.setText("Erreur lors de la vérification du forfait.");
                                }
                            }

                            @Override
                            public void onFailure(Call<ForfaitDTO> call, Throwable t) {
                                Log.e(TAG, "Erreur lors de la vérification du forfait : " + t.getMessage());
                                resultView.setText("Erreur : " + t.getMessage());
                            }
                        });
                    } else {
                        Log.e(TAG, "Erreur : Client non trouvé.");
                        resultView.setText("Client non trouvé. Veuillez vérifier le numéro RFID.");
                    }
                }

                @Override
                public void onFailure(Call<ClientDTO> call, Throwable t) {
                    Log.e(TAG, "Erreur lors de la vérification du client : " + t.getMessage());
                    resultView.setText("Erreur de connexion : " + t.getMessage());
                }
            });
        } else {
            // Cas sans connexion Internet : récupération des données locales
            Cursor cursor = dbHelper.getCardInfo(rfid);
            if (cursor.moveToFirst()) {
                int clientNameIndex = cursor.getColumnIndex("client_name");
                int forfaitActiveIndex = cursor.getColumnIndex("forfait_active");
                int forfaitExpirationIndex = cursor.getColumnIndex("forfait_expiration");

                if (clientNameIndex != -1 && forfaitActiveIndex != -1 && forfaitExpirationIndex != -1) {
                    String clientName = cursor.getString(clientNameIndex);
                    boolean forfaitActive = cursor.getInt(forfaitActiveIndex) == 1;
                    String forfaitExpiration = cursor.getString(forfaitExpirationIndex);

                    resultView.setText("Client : " + clientName + "\nForfait actif : " + (forfaitActive ? "Oui" : "Non") +
                            "\nExpiration : " + forfaitExpiration);
                } else {
                    resultView.setText("Aucun forfait activé pour cette carte.");
                }
            } else {
                resultView.setText("Cette carte n'a jamais été scannée avant ou ne possède aucun forfait activé.");
            }
            cursor.close();
        }
    }

    private void verifyCard(String rfid) {
        Call<ClientDTO> call = apiService.verifyCard(rfid);
        call.enqueue(new Callback<ClientDTO>() {
            @Override
            public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClientDTO client = response.body();
                    Log.d(TAG, "Client trouvé : " + client.getNom() + " " + client.getPrenom());
                    resultView.setText("Client trouvé : " + client.getNom() + " " + client.getPrenom());
                } else {
                    Log.e(TAG, "Erreur : Client non trouvé ou problème de serveur.");
                    resultView.setText("Client non trouvé. Veuillez vérifier le numéro RFID.");
                }
            }

            @Override
            public void onFailure(Call<ClientDTO> call, Throwable t) {
                Log.e(TAG, "Erreur lors de la vérification du client : " + t.getMessage());
                resultView.setText("Erreur de connexion : " + t.getMessage());
            }
        });
    }

    private void assignForfait(String rfid, String forfaitType) {
        ForfaitDTO forfaitDTO = new ForfaitDTO(forfaitType, rfid);
        Call<Void> call = apiService.assignForfait(forfaitDTO);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Forfait " + forfaitType + " attribué avec succès.");
                    resultView.setText("Forfait " + forfaitType + " attribué avec succès.");
                } else {
                    Log.e(TAG, "Erreur lors de l'attribution du forfait.");
                    resultView.setText("Erreur lors de l'attribution du forfait. Veuillez réessayer.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Erreur lors de l'attribution du forfait : " + t.getMessage());
                resultView.setText("Erreur : " + t.getMessage());
            }
        });
    }
}
