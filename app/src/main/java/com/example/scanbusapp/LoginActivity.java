package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText uniqueUserNumberInput;
    private Button loginButton;
    private TextView scanPromptTextView;
    private String deviceId;
    private int batteryLevel;
    private String terminalType;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private String scannedRfid = null;
    private UtilisateurDTO currentUser = null;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        uniqueUserNumberInput = findViewById(R.id.uniqueUserNumber_input);
        loginButton = findViewById(R.id.login_button);
        scanPromptTextView = findViewById(R.id.scan_prompt_textview);

        // Initialiser Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://51.178.42.116:8089/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Générer l'ID unique de l'appareil
        deviceId = getDeviceId(this);

        // Récupérer le niveau de batterie et le type de terminal
        batteryLevel = getBatteryLevel(this);
        terminalType = android.os.Build.MODEL;

        // Configuration du NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            Toast.makeText(this, "Le NFC n'est pas supporté sur cet appareil.", Toast.LENGTH_SHORT).show();
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        // État initial de l'interface utilisateur
        scanPromptTextView.setText("Veuillez scanner votre carte RFID pour vous connecter.");
        uniqueUserNumberInput.setEnabled(false);
        loginButton.setEnabled(false);

        // Action sur le bouton de connexion
        loginButton.setOnClickListener(v -> {
            String uniqueUserNumber = uniqueUserNumberInput.getText().toString().trim();
            if (currentUser != null && !uniqueUserNumber.isEmpty()) {
                loginUser(uniqueUserNumber);
            } else {
                Toast.makeText(LoginActivity.this, "Veuillez scanner votre carte RFID et entrer votre mot de passe.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Méthode pour gérer la connexion de l'utilisateur
    private void loginUser(String uniqueUserNumber) {
        if (uniqueUserNumber.equalsIgnoreCase(currentUser.getUniqueUserNumber())) {
            handleRoleRedirection(currentUser);
        } else {
            Toast.makeText(LoginActivity.this, "Numéro unique incorrect.", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode pour rediriger l'utilisateur en fonction de son rôle
    private void handleRoleRedirection(UtilisateurDTO utilisateur) {
        Intent intent;
        if ("chauffeur".equalsIgnoreCase(utilisateur.getRole())) {
            intent = new Intent(LoginActivity.this, BusTripActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, BusTicketActivity.class);
        }

        intent.putExtra("deviceId", deviceId);
        intent.putExtra("nom", utilisateur.getNom());
        intent.putExtra("role", utilisateur.getRole());
        intent.putExtra("chauffeurUniqueNumber", utilisateur.getUniqueUserNumber());
        intent.putExtra("batteryLevel", batteryLevel);
        intent.putExtra("terminalType", terminalType);

        startActivity(intent);
        finish();
    }

    // Méthode pour obtenir l'ID unique de l'appareil
    private String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    // Méthode pour obtenir le niveau de batterie
    private int getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
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
            scannedRfid = bytesToHex(tagId);
            getUserInfoByRfid(scannedRfid);
        }
    }

    // Méthode pour récupérer les informations de l'utilisateur via RFID
    private void getUserInfoByRfid(String rfid) {
        Call<UtilisateurDTO> call = apiService.getUtilisateurByRfid(rfid);
        call.enqueue(new Callback<UtilisateurDTO>() {
            @Override
            public void onResponse(Call<UtilisateurDTO> call, Response<UtilisateurDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    saveUserLocally(currentUser); // Enregistrer l'utilisateur localement
                    promptForPassword(); // Demander le mot de passe après le scan
                } else {
                    Toast.makeText(LoginActivity.this, "Utilisateur inconnu. Veuillez contacter l'administrateur.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UtilisateurDTO> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Erreur de connexion: " + t.getMessage() + ". Tentative hors ligne...", Toast.LENGTH_SHORT).show();
                loginOffline(scannedRfid); // Tentative de connexion hors ligne
            }
        });
    }

    // Méthode pour demander le mot de passe après le scan RFID
    private void promptForPassword() {
        runOnUiThread(() -> {
            scanPromptTextView.setText("Bonjour " + currentUser.getNom() + ". Veuillez entrer votre mot de passe.");
            uniqueUserNumberInput.setEnabled(true);
            loginButton.setEnabled(true);
        });
    }

    // Méthode pour enregistrer un utilisateur localement
    private void saveUserLocally(UtilisateurDTO utilisateurDTO) {
        Utilisateur utilisateur = new Utilisateur(
                utilisateurDTO.getUniqueUserNumber(),
                utilisateurDTO.getNom(),
                utilisateurDTO.getPrenom(),
                utilisateurDTO.getRole(),
                utilisateurDTO.getRfid(),
                utilisateurDTO.getDateCreation()
        );

        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext()).utilisateurDao().insert(utilisateur);
            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Utilisateur enregistré localement", Toast.LENGTH_SHORT).show());
        }).start();
    }

    // Méthode pour récupérer un utilisateur hors ligne
    private void loginOffline(String rfid) {
        new Thread(() -> {
            Utilisateur utilisateur = AppDatabase.getInstance(getApplicationContext()).utilisateurDao().getUtilisateurByRfid(rfid);
            runOnUiThread(() -> {
                if (utilisateur != null) {
                    currentUser = new UtilisateurDTO(
                            utilisateur.getId(),
                            utilisateur.getUniqueUserNumber(),
                            utilisateur.getNom(),
                            utilisateur.getPrenom(),
                            utilisateur.getRole(),
                            utilisateur.getRfid(),
                            utilisateur.getDateCreation()
                    );
                    promptForPassword(); // Demander le mot de passe après la détection RFID en mode hors ligne
                } else {
                    Toast.makeText(LoginActivity.this, "Utilisateur non trouvé localement.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // Convertir le tableau de bytes en String hexadécimale
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
