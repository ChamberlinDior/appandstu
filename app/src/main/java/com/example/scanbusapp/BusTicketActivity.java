package com.example.scanbusapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
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
    private LocalDatabaseHelper dbHelper;
    private ConnectivityManager connectivityManager;

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
        userInfoDisplay = findViewById(R.id.user_info_display);
        forfaitDayButton = findViewById(R.id.forfait_day_button);
        forfaitWeekButton = findViewById(R.id.forfait_week_button);
        forfaitMonthButton = findViewById(R.id.forfait_month_button);
        checkForfaitStatusButton = findViewById(R.id.check_forfait_status_button);
        generateTicketButton = findViewById(R.id.generate_ticket_button);
        logoutButton = findViewById(R.id.logout_button);

        dbHelper = new LocalDatabaseHelper(this);

        // Display connected user info
        userInfoDisplay.setText("Connecté en tant que : " + userRole + " - " + userName);

        // Masquer les boutons de forfait si l'utilisateur a le rôle de 'controleur'
        if ("controleur".equalsIgnoreCase(userRole)) {
            forfaitDayButton.setVisibility(View.GONE);
            forfaitWeekButton.setVisibility(View.GONE);
            forfaitMonthButton.setVisibility(View.GONE);
        }

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

        // Setup network connectivity listener
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerNetworkCallback();

        // Generate ticket button logic
        generateTicketButton.setOnClickListener(v -> {
            String selectedDestination = "Votre destination"; // This can be retrieved from UI in a real case
            String ticketContent = generateTicketContent(selectedDestination, getFormattedDate());
            resultView.setText(ticketContent);
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

    // Register for network changes
    private void registerNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Connexion Internet rétablie. Synchronisation des transactions hors ligne...");
                synchronizeForfaits();
                synchronizeOfflineTransactions();
                updateUI();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Connexion Internet perdue.");
            }
        });
    }

    // Synchroniser les transactions hors ligne avec le serveur
    private void synchronizeOfflineTransactions() {
        Cursor cursor = dbHelper.getOfflineTransactions();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String rfid = cursor.getString(cursor.getColumnIndexOrThrow("rfid"));
                String forfaitType = cursor.getString(cursor.getColumnIndexOrThrow("forfait_type"));

                ForfaitDTO forfaitDTO = new ForfaitDTO(forfaitType, rfid);
                Call<Void> call = apiService.assignForfait(forfaitDTO);
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Forfait hors ligne synchronisé avec succès.");
                            // Supprimer la transaction hors ligne une fois synchronisée
                            dbHelper.deleteOfflineTransaction(rfid, forfaitType);
                        } else {
                            Log.e(TAG, "Erreur lors de la synchronisation du forfait hors ligne.");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Échec de la synchronisation du forfait hors ligne : " + t.getMessage());
                    }
                });
            } while (cursor.moveToNext());
        }
    }

    // Synchroniser les forfaits depuis le serveur
    private void synchronizeForfaits() {
        Call<List<ClientDTO>> call = apiService.getAllClients();
        call.enqueue(new Callback<List<ClientDTO>>() {
            @Override
            public void onResponse(Call<List<ClientDTO>> call, Response<List<ClientDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ClientDTO> clients = response.body();
                    for (ClientDTO client : clients) {
                        dbHelper.saveCardInfo(client.getRfid(), client.getNom(), client.isForfaitActif(), client.getForfaitExpiration());
                    }
                    Toast.makeText(BusTicketActivity.this, "Données synchronisées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Échec de la synchronisation : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<ClientDTO>> call, Throwable t) {
                Log.e(TAG, "Erreur lors de la synchronisation des forfaits : " + t.getMessage());
            }
        });
    }

    // Méthode pour vérifier la connexion à Internet
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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
        if (isConnected()) {
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
        } else {
            dbHelper.saveOfflineTransaction(rfid, forfaitType);
            resultView.setText("Forfait " + forfaitType + " enregistré localement. Synchronisation une fois la connexion rétablie.");
        }
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
                                dbHelper.saveCardInfo(client.getRfid(), client.getNom(), client.isForfaitActif(), client.getForfaitExpiration());
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
                checkForfaitStatusOffline(rfid);
            }
        });
    }

    // Check offline status in local SQLite database
    private void checkForfaitStatusOffline(String rfid) {
        Cursor cursor = dbHelper.getCardInfo(rfid);

        if (cursor != null && cursor.moveToFirst()) {
            String clientName = cursor.getString(cursor.getColumnIndexOrThrow("client_name"));
            boolean forfaitActive = cursor.getInt(cursor.getColumnIndexOrThrow("forfait_active")) == 1;
            String forfaitExpiration = cursor.getString(cursor.getColumnIndexOrThrow("forfait_expiration"));

            String statutForfait = forfaitActive ?
                    "Forfait Actif jusqu'à : " + forfaitExpiration :
                    "Aucun forfait actif";

            resultView.setText("Client : " + clientName + "\n" + statutForfait);
        } else {
            resultView.setText("Carte non trouvée. Aucun forfait actif.");
            Toast.makeText(this, "Cette carte n'a jamais été scannée auparavant.", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode pour rafraîchir l'interface utilisateur sans redémarrer l'activité
    private void updateUI() {
        runOnUiThread(() -> {
            // Mettre à jour uniquement les éléments nécessaires, sans redémarrer l'activité
            rfidDisplay.setText(""); // Par exemple, vider l'affichage de l'RFID après vérification
            resultView.setText("");  // Réinitialiser les résultats
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
