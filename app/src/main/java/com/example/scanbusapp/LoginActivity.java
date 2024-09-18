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
                .baseUrl("http://192.168.1.67:8080/")  // Remplacez cette URL par l'URL de votre backend
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

    // Méthode pour rediriger l'utilisateur en fonction de son rôle et enregistrer le chauffeur si nécessaire
    private void handleRoleRedirection(UtilisateurDTO utilisateur) {
        if ("chauffeur".equalsIgnoreCase(utilisateur.getRole())) {
            // Enregistrer le chauffeur avec le rôle et le numéro unique
            registerChauffeur(utilisateur.getNom(), deviceId, utilisateur.getUniqueUserNumber(), "Libreville");

            // Rediriger vers BusTripActivity si l'utilisateur est un chauffeur
            Intent intent = new Intent(LoginActivity.this, BusTripActivity.class);
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("nom", utilisateur.getNom());  // Ajout du nom du chauffeur
            intent.putExtra("role", utilisateur.getRole());  // Ajout du rôle du chauffeur
            startActivity(intent);
        } else {
            // Rediriger vers BusTicketActivity pour les autres rôles
            Intent intent = new Intent(LoginActivity.this, BusTicketActivity.class);
            intent.putExtra("nom", utilisateur.getNom());
            intent.putExtra("role", utilisateur.getRole());
            startActivity(intent);
        }

        // Afficher le popup avec l'Android ID
        showAndroidIdPopup(deviceId);
        finish(); // Fermer l'activité après la connexion
    }

    // Méthode pour enregistrer le chauffeur avec l'identifiant de l'appareil (Android ID)
    private void registerChauffeur(String chauffeurNom, String deviceId, String uniqueUserNumber, String lastDestination) {
        Call<Void> call = apiService.updateChauffeurAndDestination(deviceId, lastDestination, chauffeurNom, uniqueUserNumber);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Chauffeur enregistré avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Erreur lors de l'enregistrement du chauffeur", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Erreur: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Méthode pour générer l'ID unique de l'appareil (Android ID)
    private String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    // Méthode pour afficher un popup avec l'Android ID
    private void showAndroidIdPopup(String androidId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Connexion réussie");
        builder.setMessage("Votre Android ID est : " + androidId);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();  // Fermer le popup
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
