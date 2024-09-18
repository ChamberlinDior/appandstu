package com.example.scanbusapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
    private TextView rfidDisplay;
    private TextView resultView;
    private TextView profileTextView;
    private TextView driverNameTextView;
    private TextView driverRoleTextView;
    private Button forfaitDayButton, forfaitWeekButton, forfaitMonthButton, checkForfaitStatusButton, logoutButton;
    private Button generateTicketButton;
    private String macAddress;
    private static final String TAG = "BusTicketActivity";
    private LocalDatabaseHelper dbHelper;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_ticket);

        dbHelper = new LocalDatabaseHelper(this);

        String nom = getIntent().getStringExtra("nom");
        String role = getIntent().getStringExtra("role");
        macAddress = getIntent().getStringExtra("deviceId");

        Log.d(TAG, "Nom utilisateur: " + nom + ", Rôle: " + role + ", MAC: " + macAddress);

        rfidDisplay = findViewById(R.id.rfid_display);
        resultView = findViewById(R.id.result_view);
        profileTextView = findViewById(R.id.profile_text_view);
        driverNameTextView = findViewById(R.id.driver_name_text_view);
        driverRoleTextView = findViewById(R.id.driver_role_text_view);
        forfaitDayButton = findViewById(R.id.forfait_day_button);
        forfaitWeekButton = findViewById(R.id.forfait_week_button);
        forfaitMonthButton = findViewById(R.id.forfait_month_button);
        checkForfaitStatusButton = findViewById(R.id.check_forfait_status_button);
        logoutButton = findViewById(R.id.logout_button);
        generateTicketButton = findViewById(R.id.generate_ticket_button);

        forfaitDayButton.setText("Forfait jour");
        forfaitWeekButton.setText("Forfait semaine");
        forfaitMonthButton.setText("Forfait mois");
        checkForfaitStatusButton.setText("Vérifier statut du forfait");

        driverNameTextView.setText(getString(R.string.user_label) + " " + nom);
        driverRoleTextView.setText(getString(R.string.role_label) + " " + role);

        logoutButton.setOnClickListener(v -> {
            Toast.makeText(BusTicketActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BusTicketActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        if ("chauffeur".equalsIgnoreCase(role) || "controleur".equalsIgnoreCase(role)) {
            hideNonChauffeurOrControleurButtons();
        }

        if ("caissier".equalsIgnoreCase(role)) {
            generateTicketButton.setVisibility(View.VISIBLE);
            generateTicketButton.setOnClickListener(v -> openTicketActivity());
        } else {
            generateTicketButton.setVisibility(View.GONE);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.67:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        forfaitDayButton.setOnClickListener(v -> showConfirmationDialog("jour"));
        forfaitWeekButton.setOnClickListener(v -> showConfirmationDialog("semaine"));
        forfaitMonthButton.setOnClickListener(v -> showConfirmationDialog("mois"));

        checkForfaitStatusButton.setOnClickListener(v -> {
            String rfid = rfidDisplay.getText().toString();
            Log.d(TAG, "Vérification du statut du forfait pour le RFID : " + rfid);
            checkForfaitStatus(rfid);
        });

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
            byte[] tagId = tag.getId();
            String rfid = bytesToHex(tagId);
            rfidDisplay.setText(rfid);
            Log.d(TAG, "RFID scanné : " + rfid);
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

    private void hideNonChauffeurOrControleurButtons() {
        forfaitDayButton.setVisibility(View.GONE);
        forfaitWeekButton.setVisibility(View.GONE);
        forfaitMonthButton.setVisibility(View.GONE);
    }

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

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Méthode pour vérifier le statut du forfait d'un client
    private void checkForfaitStatus(String rfid) {
        if (isInternetAvailable()) {
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
                                    String statutForfait;

                                    if (forfait.getDateExpiration() != null) {
                                        statutForfait = "Forfait Actif";
                                        resultView.setText("Client : " + client.getNom() + " " + client.getPrenom() +
                                                "\nForfait actif jusqu'à : " + forfait.getDateExpiration());
                                    } else {
                                        statutForfait = "Aucun forfait actif";
                                        resultView.setText("Client : " + client.getNom() + " " + client.getPrenom() +
                                                "\nAucun forfait actif trouvé.");
                                    }

                                    // Enregistrer la vérification dans le backend
                                    ForfaitVerificationDTO verification = new ForfaitVerificationDTO(client.getNom(), rfid, statutForfait);
                                    Call<Void> verificationCall = apiService.saveForfaitVerification(verification);
                                    verificationCall.enqueue(new Callback<Void>() {
                                        @Override
                                        public void onResponse(Call<Void> call, Response<Void> response) {
                                            if (response.isSuccessful()) {
                                                Log.d(TAG, "Vérification du forfait enregistrée avec succès.");
                                            } else {
                                                Log.e(TAG, "Échec lors de l'enregistrement de la vérification du forfait.");
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<Void> call, Throwable t) {
                                            Log.e(TAG, "Erreur lors de l'enregistrement de la vérification du forfait.", t);
                                        }
                                    });
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

    private void openTicketActivity() {
        Intent intent = new Intent(BusTicketActivity.this, TicketActivity.class);
        intent.putExtra("ticketDetails", "Trans'urb - Ticket de bus\nPrix : 100 XAF");
        startActivity(intent);
    }
}
