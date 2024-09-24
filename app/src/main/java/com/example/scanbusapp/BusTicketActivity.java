package com.example.scanbusapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BusTicketActivity extends AppCompatActivity {

    private ApiService apiService;
    private TextView rfidDisplay;
    private TextView resultView, userInfoDisplay, ticketDisplay;
    private Button forfaitDayButton, forfaitWeekButton, forfaitMonthButton, checkForfaitStatusButton, generateTicketButton, logoutButton;
    private String macAddress, userName, userRole;
    private static final String TAG = "BusTicketActivity";
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private static int ticketCounter = 1; // Compteur de ticket

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_ticket);

        // Retrieve user information from intent
        userName = getIntent().getStringExtra("nom");
        userRole = getIntent().getStringExtra("role");
        macAddress = getIntent().getStringExtra("deviceId");

        // Initialize UI elements
        rfidDisplay = findViewById(R.id.rfid_display);
        resultView = findViewById(R.id.result_view);
        userInfoDisplay = findViewById(R.id.user_info_display);  // TextView to display cashier info
        forfaitDayButton = findViewById(R.id.forfait_day_button);
        forfaitWeekButton = findViewById(R.id.forfait_week_button);
        forfaitMonthButton = findViewById(R.id.forfait_month_button);
        checkForfaitStatusButton = findViewById(R.id.check_forfait_status_button);
        generateTicketButton = findViewById(R.id.generate_ticket_button);
        logoutButton = findViewById(R.id.logout_button);

        // Display connected user info
        userInfoDisplay.setText("Connecté en tant que : " + userRole + " - " + userName);

        // Setup Retrofit for API calls
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://51.178.42.116:8089/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // NFC Setup
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        // Generate ticket button logic
        generateTicketButton.setOnClickListener(v -> {
            String selectedDestination = "Votre destination"; // This can be retrieved from UI in a real case
            // Generate ticket content
            String ticketContent = generateTicketContent(selectedDestination, getFormattedDate());
            // Display ticket in a TextView or similar UI component
            resultView.setText(ticketContent);
            // Call method to print or save the ticket
            generateAndPrintTicket(ticketContent);
        });

        // Logout button logic
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(BusTicketActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(BusTicketActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Forfait buttons management
        forfaitDayButton.setOnClickListener(v -> showConfirmationDialog("jour"));
        forfaitWeekButton.setOnClickListener(v -> showConfirmationDialog("semaine"));
        forfaitMonthButton.setOnClickListener(v -> showConfirmationDialog("mois"));

        // Check forfait status via the dedicated button
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

    // Méthode pour générer le contenu du ticket
    private String generateTicketContent(String destination, String date) {
        return "Nom : Trans'urb\n" +
                "Date : " + date + "\n" +
                "Ticket : 0" + ticketCounter++ + "\n" +
                "Destination : " + destination + "\n";
    }

    // Méthode pour générer et imprimer/sauvegarder le ticket
    private void generateAndPrintTicket(String ticketContent) {
        File ticketFile = new File(getExternalFilesDir(null), "ticket_bus_" + ticketCounter + ".txt");

        try (FileOutputStream fos = new FileOutputStream(ticketFile)) {
            fos.write(ticketContent.getBytes());
            fos.flush();

            // Intent pour imprimer ou afficher le fichier
            Intent printIntent = new Intent(Intent.ACTION_VIEW);
            printIntent.setDataAndType(Uri.fromFile(ticketFile), "text/plain");
            printIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Vérifier s'il existe une application d'impression
            if (printIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(printIntent);
            } else {
                Toast.makeText(this, "Pas d'application pour imprimer. Ticket enregistré localement.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la génération du ticket", e);
            Toast.makeText(this, "Erreur lors de la génération du ticket", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale.FRENCH);
        return sdf.format(new java.util.Date());
    }

    // Display a dialog to confirm the forfait activation
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

    // Assign forfait to the user
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

    // Check the forfait status and save the information
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

                                // Save verification in the backend
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

    // Save forfait verification with the user's name
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
