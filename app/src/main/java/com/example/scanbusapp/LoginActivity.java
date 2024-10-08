package com.example.scanbusapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText uniqueUserNumberInput;
    private EditText nameInput;
    private Button loginButton;
    private String deviceId;
    private int batteryLevel;
    private String terminalType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        uniqueUserNumberInput = findViewById(R.id.uniqueUserNumber_input);
        nameInput = findViewById(R.id.name_input);
        loginButton = findViewById(R.id.login_button);

        // Générer l'ID unique de l'appareil (Android ID)
        deviceId = getDeviceId(this);

        // Récupérer le niveau de batterie
        batteryLevel = getBatteryLevel(this);

        // Récupérer le type de terminal (modèle de l'appareil)
        terminalType = android.os.Build.MODEL;

        // Configuration de Retrofit pour le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://51.178.42.116:8085/")  // URL du backend
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // Action sur le bouton de connexion
        loginButton.setOnClickListener(v -> {
            String uniqueUserNumber = uniqueUserNumberInput.getText().toString();
            String name = nameInput.getText().toString();
            loginUser(uniqueUserNumber, name);
        });
    }

    // Méthode pour gérer la connexion de l'utilisateur
    private void loginUser(String uniqueUserNumber, String name) {
        Call<UtilisateurDTO> call = apiService.getUtilisateurByUniqueUserNumber(uniqueUserNumber);
        call.enqueue(new Callback<UtilisateurDTO>() {
            @Override
            public void onResponse(Call<UtilisateurDTO> call, Response<UtilisateurDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UtilisateurDTO utilisateur = response.body();
                    if (utilisateur.getNom().equals(name)) {
                        // L'utilisateur est authentifié avec succès
                        handleRoleRedirection(utilisateur);
                    } else {
                        Toast.makeText(LoginActivity.this, "Nom incorrect", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Utilisateur non trouvé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UtilisateurDTO> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Erreur de connexion: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Méthode pour rediriger l'utilisateur en fonction de son rôle
    private void handleRoleRedirection(UtilisateurDTO utilisateur) {
        Intent intent;
        if ("chauffeur".equalsIgnoreCase(utilisateur.getRole())) {
            // Si chauffeur, rediriger vers BusTripActivity
            intent = new Intent(LoginActivity.this, BusTripActivity.class);
            // Passer l'ID Android, le nom, le niveau de batterie, le terminal et le numéro unique du chauffeur à l'activité suivante
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("nom", utilisateur.getNom());
            intent.putExtra("role", utilisateur.getRole());
            intent.putExtra("chauffeurUniqueNumber", utilisateur.getUniqueUserNumber());
            intent.putExtra("batteryLevel", batteryLevel);
            intent.putExtra("terminalType", terminalType);

            // Enregistrer les informations du chauffeur dans la base de données (table Bus)
            registerChauffeurToBusTable(utilisateur.getNom(), utilisateur.getUniqueUserNumber(), deviceId);
        } else {
            // Sinon, rediriger vers une autre activité (par exemple, BusTicketActivity pour les caissiers)
            intent = new Intent(LoginActivity.this, BusTicketActivity.class);
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("nom", utilisateur.getNom());
            intent.putExtra("role", utilisateur.getRole());
            intent.putExtra("batteryLevel", batteryLevel);
            intent.putExtra("terminalType", terminalType);
        }
        startActivity(intent);
        finish(); // Fermer l'activité après la connexion

        // Afficher le popup avec l'ID Android
        showAndroidIdPopup(deviceId);
    }

    // Méthode pour enregistrer le chauffeur dans la table Bus
    private void registerChauffeurToBusTable(String chauffeurNom, String chauffeurUniqueNumber, String macAddress) {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String terminalType = android.os.Build.MODEL; // Type de terminal (ex: modèle du terminal)

        Call<Void> call = apiService.updateChauffeurAndDestination(
                macAddress,
                "Destination par défaut",
                chauffeurNom,
                chauffeurUniqueNumber,
                batteryLevel,
                androidId,
                terminalType
        );

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("LoginActivity", "Chauffeur enregistré avec succès dans la table Bus.");
                } else {
                    Log.e("LoginActivity", "Erreur lors de l'enregistrement du chauffeur.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("LoginActivity", "Erreur lors de l'enregistrement du chauffeur : " + t.getMessage());
            }
        });
    }

    // Méthode pour obtenir l'ID unique de l'appareil (Android ID)
    private String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    // Méthode pour obtenir le niveau de batterie
    private int getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1; // Si la version Android est trop ancienne
    }

    // Méthode pour afficher un popup avec l'Android ID
    private void showAndroidIdPopup(String androidId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Connexion réussie");
        builder.setMessage("Votre Android ID est : " + androidId);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
