package com.example.scanbusapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        uniqueUserNumberInput = findViewById(R.id.uniqueUserNumber_input);
        nameInput = findViewById(R.id.name_input);
        loginButton = findViewById(R.id.login_button);

        // Générer l'ID unique de l'appareil (Android ID)
        deviceId = getDeviceId(this);

        // Configuration de Retrofit pour le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.67:8080/")  // Remplacez cette URL par celle de votre backend
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
        } else {
            // Sinon, rediriger vers BusTicketActivity pour caissier/contrôleur
            intent = new Intent(LoginActivity.this, BusTicketActivity.class);
        }

        // Passer l'ID Android, le nom et le rôle de l'utilisateur à l'activité suivante
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("nom", utilisateur.getNom());
        intent.putExtra("role", utilisateur.getRole());
        startActivity(intent);
        finish(); // Fermer l'activité après la connexion

        // Afficher le popup avec l'ID Android
        showAndroidIdPopup(deviceId);
    }

    // Méthode pour obtenir l'ID unique de l'appareil (Android ID)
    private String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
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
