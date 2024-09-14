package com.example.scanbusapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View; // Ajout de l'import pour View
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BusTripActivity extends AppCompatActivity {

    private ApiService apiService;
    private Spinner destinationSpinner;
    private TextView startTimeView, endTimeView, macAddressView, navbarTitle;
    private Button startTripButton, endTripButton;
    private String macAddress; // ID Android du terminal
    private static final String TAG = "BusTripActivity";  // Log tag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_trip);

        // Initialisation des vues
        destinationSpinner = findViewById(R.id.destination_spinner);
        startTimeView = findViewById(R.id.startTime_view);
        endTimeView = findViewById(R.id.endTime_view);
        macAddressView = findViewById(R.id.macAddress_view);
        navbarTitle = findViewById(R.id.navbar_title);  // Ajout de la vue de la navbar pour le nom et rôle
        startTripButton = findViewById(R.id.startTrip_button);
        endTripButton = findViewById(R.id.endTrip_button);

        // Récupérer l'ID Android (adresse MAC) du terminal
        macAddress = getIntent().getStringExtra("deviceId");
        macAddressView.setText("MAC Address: " + macAddress);

        // Rendre l'ID Android visible pendant 3 secondes après connexion
        macAddressView.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> macAddressView.setVisibility(View.GONE), 3000); // Cache après 3 secondes

        // Récupérer les informations du chauffeur depuis l'intent
        String chauffeurNom = getIntent().getStringExtra("nom");
        String chauffeurRole = getIntent().getStringExtra("role");

        // Mettre à jour la navbar avec le nom et le rôle du chauffeur
        navbarTitle.setText(chauffeurNom + " - " + chauffeurRole);

        // Initialisation du Spinner avec les destinations
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.destination_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        destinationSpinner.setAdapter(adapter);

        // Configuration de Retrofit pour interagir avec le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.68:8080/")  // Assurez-vous que cette URL est correcte
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // Démarrer un trajet
        startTripButton.setOnClickListener(v -> {
            String selectedDestination = destinationSpinner.getSelectedItem().toString();
            if (!selectedDestination.isEmpty()) {
                startTrip(macAddress, selectedDestination);
            } else {
                Toast.makeText(BusTripActivity.this, "Veuillez sélectionner une destination", Toast.LENGTH_SHORT).show();
            }
        });

        // Terminer un trajet
        endTripButton.setOnClickListener(v -> endTrip(macAddress));
    }

    // Méthode pour démarrer un trajet
    private void startTrip(String macAddress, String lastDestination) {
        Call<Void> call = apiService.startTrip(macAddress, lastDestination);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Trajet démarré avec succès", Toast.LENGTH_SHORT).show();
                    // Afficher la date et l'heure du début de trajet en lettres et chiffres
                    startTimeView.setText("Début du trajet : " + getFormattedDate());
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

    // Méthode pour terminer un trajet
    private void endTrip(String macAddress) {
        Call<Void> call = apiService.endTrip(macAddress);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Trajet terminé avec succès", Toast.LENGTH_SHORT).show();
                    // Afficher la date et l'heure de fin de trajet en lettres et chiffres
                    endTimeView.setText("Fin du trajet : " + getFormattedDate());
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

    // Méthode pour obtenir la date et l'heure actuelle en lettres et chiffres en français
    private String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale.FRENCH);
        return sdf.format(new Date());
    }
}
