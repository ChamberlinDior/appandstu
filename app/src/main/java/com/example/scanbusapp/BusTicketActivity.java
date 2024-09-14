package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.view.View;

public class BusTicketActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText rfidInput;
    private TextView resultView;
    private TextView profileTextView;
    private TextView driverNameTextView;
    private TextView driverRoleTextView;
    private Button verifyButton, forfaitDayButton, forfaitWeekButton, forfaitMonthButton, checkForfaitStatusButton;
    private String macAddress;
    private static final String TAG = "BusTicketActivity";

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_ticket);

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

        driverNameTextView.setText(getString(R.string.user_label) + " " + nom);
        driverRoleTextView.setText(getString(R.string.role_label) + " " + role);

        if ("chauffeur".equalsIgnoreCase(role)) {
            hideNonChauffeurButtons();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.68:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        verifyButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "RFID saisi: " + rfid);
            verifyCard(rfid);
        });

        forfaitDayButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Attribuer forfait jour pour RFID: " + rfid);
            assignForfait(rfid, "jour");
        });

        forfaitWeekButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Attribuer forfait semaine pour RFID: " + rfid);
            assignForfait(rfid, "semaine");
        });

        forfaitMonthButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Attribuer forfait mois pour RFID: " + rfid);
            assignForfait(rfid, "mois");
        });

        checkForfaitStatusButton.setOnClickListener(v -> {
            String rfid = rfidInput.getText().toString();
            Log.d(TAG, "Vérification du statut du forfait pour RFID: " + rfid);
            checkForfaitStatus(rfid);
        });

        // Initialisation du NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            resultView.setText("NFC n'est pas supporté sur cet appareil.");
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
            Log.d(TAG, "RFID scanné: " + rfid);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private void hideNonChauffeurButtons() {
        verifyButton.setVisibility(View.GONE);
        forfaitDayButton.setVisibility(View.GONE);
        forfaitWeekButton.setVisibility(View.GONE);
        forfaitMonthButton.setVisibility(View.GONE);
        checkForfaitStatusButton.setVisibility(View.GONE);
    }

    private void verifyCard(String rfid) {
        Call<ClientDTO> call = apiService.verifyCard(rfid);
        call.enqueue(new Callback<ClientDTO>() {
            @Override
            public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClientDTO client = response.body();
                    Log.d(TAG, "Client trouvé: " + client.getNom() + " " + client.getPrenom());
                    resultView.setText(getString(R.string.client_found) + " " + client.getNom() + " " + client.getPrenom());
                } else {
                    Log.e(TAG, "Erreur: Client non trouvé ou problème serveur");
                    resultView.setText(getString(R.string.client_not_found));
                }
            }

            @Override
            public void onFailure(Call<ClientDTO> call, Throwable t) {
                Log.e(TAG, "Erreur de vérification du client: " + t.getMessage());
                resultView.setText("Erreur: " + t.getMessage());
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
                    resultView.setText("Erreur lors de l'attribution du forfait.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Erreur d'attribution du forfait: " + t.getMessage());
                resultView.setText("Erreur: " + t.getMessage());
            }
        });
    }

    private void checkForfaitStatus(String rfid) {
        Call<ForfaitDTO> call = apiService.getForfaitStatus(rfid);
        call.enqueue(new Callback<ForfaitDTO>() {
            @Override
            public void onResponse(Call<ForfaitDTO> call, Response<ForfaitDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ForfaitDTO forfait = response.body();
                    Log.d(TAG, "Forfait actif jusqu'à: " + forfait.getDateExpiration());
                    resultView.setText("Forfait actif jusqu'à: " + forfait.getDateExpiration());
                } else {
                    Log.e(TAG, "Impossible de vérifier le statut du forfait.");
                    resultView.setText("Impossible de vérifier le statut du forfait.");
                }
            }

            @Override
            public void onFailure(Call<ForfaitDTO> call, Throwable t) {
                Log.e(TAG, "Erreur de vérification du forfait: " + t.getMessage());
                resultView.setText("Erreur: " + t.getMessage());
            }
        });
    }
}
