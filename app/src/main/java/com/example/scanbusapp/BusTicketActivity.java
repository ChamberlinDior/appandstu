package com.example.scanbusapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BusTicketActivity extends AppCompatActivity {

    private ApiService apiService;
    private TextView rfidDisplay;
    private TextView resultView;
    private Button forfaitDayButton, forfaitWeekButton, forfaitMonthButton, checkForfaitStatusButton, logoutButton;
    private String macAddress, userName, userRole;
    private static final String TAG = "BusTicketActivity";
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_ticket);

        // Récupérer les informations utilisateur depuis l'intent
        userName = getIntent().getStringExtra("nom");
        userRole = getIntent().getStringExtra("role");
        macAddress = getIntent().getStringExtra("deviceId");

        // Initialisation des éléments UI
        rfidDisplay = findViewById(R.id.rfid_display);
        resultView = findViewById(R.id.result_view);
        forfaitDayButton = findViewById(R.id.forfait_day_button);
        forfaitWeekButton = findViewById(R.id.forfait_week_button);
        forfaitMonthButton = findViewById(R.id.forfait_month_button);
        checkForfaitStatusButton = findViewById(R.id.check_forfait_status_button);
        logoutButton = findViewById(R.id.logout_button);

        // Configuration de Retrofit pour les appels API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.67:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // Configuration du NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        // Gestion du bouton de déconnexion
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(BusTicketActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BusTicketActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Gestion des boutons de forfaits
        forfaitDayButton.setOnClickListener(v -> showConfirmationDialog("jour"));
        forfaitWeekButton.setOnClickListener(v -> showConfirmationDialog("semaine"));
        forfaitMonthButton.setOnClickListener(v -> showConfirmationDialog("mois"));

        // Vérifier le statut du forfait via le bouton dédié
        checkForfaitStatusButton.setOnClickListener(v -> {
            String rfid = rfidDisplay.getText().toString();
            if (!rfid.isEmpty()) {
                checkForfaitStatus(rfid);
            } else {
                Toast.makeText(BusTicketActivity.this, "Veuillez scanner un numéro RFID", Toast.LENGTH_SHORT).show();
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
            rfidDisplay.setText(rfid);
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

    // Afficher une boîte de dialogue pour confirmer l'attribution du forfait
    private void showConfirmationDialog(String forfaitType) {
        String rfid = rfidDisplay.getText().toString();
        if (rfid.isEmpty()) {
            Toast.makeText(this, "Veuillez scanner un numéro RFID", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Confirmer l'activation du forfait " + forfaitType + " ?")
                .setCancelable(false)
                .setPositiveButton("Confirmer", (dialog, id) -> {
                    Log.d(TAG, "Forfait " + forfaitType + " confirmé pour RFID : " + rfid);
                    assignForfait(rfid, forfaitType);
                })
                .setNegativeButton("Annuler", (dialog, id) -> dialog.dismiss());
        AlertDialog alert = builder.create();
        alert.show();
    }

    // Attribuer un forfait à l'utilisateur
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

    // Vérification du forfait et enregistrement des informations
    private void checkForfaitStatus(String rfid) {
        Call<ClientDTO> call = apiService.verifyCard(rfid);
        call.enqueue(new Callback<ClientDTO>() {
            @Override
            public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClientDTO client = response.body();
                    Call<ForfaitDTO> forfaitCall = apiService.getForfaitStatus(rfid);
                    forfaitCall.enqueue(new Callback<ForfaitDTO>() {
                        @Override
                        public void onResponse(Call<ForfaitDTO> call, Response<ForfaitDTO> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ForfaitDTO forfait = response.body();
                                String statutForfait = forfait.getDateExpiration() != null ?
                                        "Forfait Actif jusqu'à : " + new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.FRENCH).format(forfait.getDateExpiration()) :
                                        "Aucun forfait actif";

                                resultView.setText("Client : " + client.getNom() + "\n" + statutForfait);

                                // Enregistrement de la vérification dans le backend
                                ForfaitVerificationDTO verification = new ForfaitVerificationDTO(
                                        client.getNom(), rfid, statutForfait, macAddress, userRole, userName
                                );
                                saveForfaitVerification(verification);
                            } else {
                                resultView.setText("Aucun forfait actif trouvé.");
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
    }

    // Enregistrer la vérification du forfait avec le nom de l'utilisateur
    private void saveForfaitVerification(ForfaitVerificationDTO verification) {
        Call<Void> verificationCall = apiService.saveForfaitVerification(verification);
        verificationCall.enqueue(new Callback<Void>() {
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
}
