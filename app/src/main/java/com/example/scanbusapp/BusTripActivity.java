package com.example.scanbusapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import utils.BatteryUtils;

public class BusTripActivity extends AppCompatActivity {

    private ApiService apiService;
    private Spinner destinationSpinner;
    private TextView startTimeView, endTimeView, macAddressView, navbarTitle;
    private TextView scannedCardInfo;
    private TextView selectedDestinationTitle; // TextView pour la destination sélectionnée
    private LinearLayout startTripLayout, endTripLayout;
    private LinearLayout destinationSelectionLayout;
    private Button logoutButton, newTripButton;
    private ImageView destinationIcon;
    private String macAddress, userName, userRole, chauffeurUniqueNumber;
    private static final String TAG = "BusTripActivity";
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    private LocalDatabaseHelper dbHelper;
    private ConnectivityManager connectivityManager;

    // Gestion des scans hors ligne
    private SharedPreferences sharedPreferences;
    private static final String OFFLINE_SCANS_PREF = "offline_scans_pref";

    // Variables pour sauvegarder l'état
    private boolean isDestinationSelected = false;
    private boolean isTripStarted = false;
    private boolean isTripEnded = false;
    private String selectedDestination = "";
    private String startTime = "";
    private String endTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_trip);

        // Récupérer les informations utilisateur à partir de l'intent
        macAddress = getIntent().getStringExtra("deviceId");
        userName = getIntent().getStringExtra("nom");
        userRole = getIntent().getStringExtra("role");
        chauffeurUniqueNumber = getIntent().getStringExtra("chauffeurUniqueNumber");

        // Initialisation des vues
        destinationSpinner = findViewById(R.id.destination_spinner);
        startTimeView = findViewById(R.id.startTime_view);
        endTimeView = findViewById(R.id.endTime_view);
        macAddressView = findViewById(R.id.macAddress_view);
        navbarTitle = findViewById(R.id.navbar_title);
        scannedCardInfo = findViewById(R.id.scanned_card_info);
        startTripLayout = findViewById(R.id.startTrip_layout);
        endTripLayout = findViewById(R.id.endTrip_layout);
        newTripButton = findViewById(R.id.newTrip_button);
        logoutButton = findViewById(R.id.logout_button);
        destinationIcon = findViewById(R.id.destination_icon);
        destinationSelectionLayout = findViewById(R.id.destination_selection_layout);
        selectedDestinationTitle = findViewById(R.id.selected_destination_title);

        macAddressView.setText("Adresse MAC : " + macAddress);
        navbarTitle.setText(userName + " - " + (userRole != null ? userRole : "Rôle non défini") + " - " + chauffeurUniqueNumber);

        // Masquer les vues inutiles au départ
        startTripLayout.setVisibility(View.GONE);
        endTripLayout.setVisibility(View.GONE);
        newTripButton.setVisibility(View.GONE);
        scannedCardInfo.setVisibility(View.GONE);
        selectedDestinationTitle.setVisibility(View.GONE);

        dbHelper = new LocalDatabaseHelper(this);

        // Initialisation des SharedPreferences pour stocker les scans hors ligne
        sharedPreferences = getSharedPreferences("BusTripAppPrefs", MODE_PRIVATE);

        // Configuration de Retrofit pour interagir avec le backend
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://51.178.42.116:8089/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Configuration du NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            Toast.makeText(this, "Le NFC n'est pas supporté sur cet appareil.", Toast.LENGTH_SHORT).show();
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE);

        // Surveillance réseau
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        registerNetworkCallback();

        // Récupérer les destinations depuis la base de données
        fetchDestinations();

        // Récupérer et envoyer le niveau de batterie au backend
        sendBatteryLevel();

        // Restauration de l'état si disponible
        if (savedInstanceState != null) {
            isDestinationSelected = savedInstanceState.getBoolean("isDestinationSelected", false);
            isTripStarted = savedInstanceState.getBoolean("isTripStarted", false);
            isTripEnded = savedInstanceState.getBoolean("isTripEnded", false);
            selectedDestination = savedInstanceState.getString("selectedDestination", "");
            startTime = savedInstanceState.getString("startTime", "");
            endTime = savedInstanceState.getString("endTime", "");

            restoreUIState();
        }

        // Gestion du Spinner de destinations
        destinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean isFirstSelection = true; // Pour éviter de déclencher lors de la configuration initiale

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstSelection) {
                    isFirstSelection = false;
                    return;
                }

                if (position != 0) {  // Une destination est sélectionnée
                    String selectedDestination = parent.getItemAtPosition(position).toString();

                    // Afficher la boîte de dialogue de confirmation
                    showConfirmationDialog(
                            "Confirmer la destination",
                            "Vous avez sélectionné : " + selectedDestination + ". Voulez-vous confirmer cette destination ?",
                            () -> {
                                onDestinationConfirmed(selectedDestination);
                            },
                            () -> {
                                destinationSpinner.setSelection(0);
                            }
                    );
                } else {
                    selectedDestinationTitle.setVisibility(View.GONE);
                    startTripLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });

        // Déconnexion
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(BusTripActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(BusTripActivity.this, LoginActivity.class));
            finish();
        });

        // Nouveau trajet
        newTripButton.setOnClickListener(v -> showConfirmationDialog(
                "Commencer un nouveau trajet",
                "Voulez-vous vraiment démarrer un nouveau trajet ?",
                this::resetTrip,
                null));
    }

    // Méthode pour obtenir l'adresse IP de l'appareil Android
    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Erreur lors de la récupération de l'adresse IP", ex);
        }
        return "IP non trouvée";
    }

    // Méthode pour gérer la confirmation de la destination
    private void onDestinationConfirmed(String selectedDestination) {
        this.selectedDestination = selectedDestination;
        isDestinationSelected = true;

        selectedDestinationTitle.setText("Destination : " + selectedDestination);
        selectedDestinationTitle.setVisibility(View.VISIBLE);

        startTripLayout.setVisibility(View.VISIBLE);

        destinationSelectionLayout.setVisibility(View.GONE);

        destinationSpinner.setEnabled(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isDestinationSelected", isDestinationSelected);
        outState.putBoolean("isTripStarted", isTripStarted);
        outState.putBoolean("isTripEnded", isTripEnded);
        outState.putString("selectedDestination", selectedDestination);
        outState.putString("startTime", startTime);
        outState.putString("endTime", endTime);
    }

    private void restoreUIState() {
        if (isDestinationSelected) {
            selectedDestinationTitle.setText("Destination : " + selectedDestination);
            selectedDestinationTitle.setVisibility(View.VISIBLE);

            destinationSelectionLayout.setVisibility(View.GONE);
            destinationSpinner.setEnabled(false);

            if (isTripStarted) {
                startTimeView.setText("Début du trajet : " + startTime);
                startTripLayout.setVisibility(View.GONE);
                endTripLayout.setVisibility(View.VISIBLE);
            } else {
                startTripLayout.setVisibility(View.VISIBLE);
                endTripLayout.setVisibility(View.GONE);
            }

            if (isTripEnded) {
                endTimeView.setText("Fin du trajet : " + endTime);
                endTripLayout.setVisibility(View.GONE);
                newTripButton.setVisibility(View.VISIBLE);
            } else {
                newTripButton.setVisibility(View.GONE);
            }
        } else {
            destinationSelectionLayout.setVisibility(View.VISIBLE);
            selectedDestinationTitle.setVisibility(View.GONE);
            startTripLayout.setVisibility(View.GONE);
            endTripLayout.setVisibility(View.GONE);
            newTripButton.setVisibility(View.GONE);
        }
    }

    // Méthode appelée lors du clic sur le bouton "Démarrer le trajet"
    public void onStartTripClick(View view) {
        if (!selectedDestination.isEmpty()) {
            showConfirmationDialog(
                    "Démarrer le trajet",
                    "Voulez-vous vraiment démarrer le trajet pour la destination : " + selectedDestination + " ?",
                    () -> {
                        isTripStarted = true;
                        startTime = getFormattedDate();

                        startTripLayout.setVisibility(View.GONE);
                        startTimeView.setText("Début du trajet : " + startTime);
                        endTripLayout.setVisibility(View.VISIBLE);

                        startTrip(macAddress, selectedDestination, userName, chauffeurUniqueNumber);
                    },
                    null
            );
        } else {
            Toast.makeText(BusTripActivity.this, "Aucune destination sélectionnée", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode appelée lors du clic sur le bouton "Terminer le trajet"
    public void onEndTripClick(View view) {
        showConfirmationDialog(
                "Terminer le trajet",
                "Êtes-vous sûr de vouloir terminer ce trajet ?",
                () -> {
                    isTripEnded = true;
                    endTime = getFormattedDate();

                    endTripLayout.setVisibility(View.GONE);
                    endTimeView.setText("Fin du trajet : " + endTime);
                    newTripButton.setVisibility(View.VISIBLE);

                    endTrip(macAddress);
                },
                null
        );
    }

    // Méthode pour vérifier la connexion Internet
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Enregistrement pour les changements de réseau
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
                processOfflineScans();
                updateUI();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Connexion Internet perdue.");
            }
        });
    }

    // Synchronisation des scans hors ligne
    private void processOfflineScans() {
        Set<String> offlineScans = new HashSet<>(sharedPreferences.getStringSet(OFFLINE_SCANS_PREF, new HashSet<>()));

        for (String rfid : offlineScans) {
            checkForfaitStatus(rfid);
        }

        sharedPreferences.edit().remove(OFFLINE_SCANS_PREF).apply();
    }

    // Méthode pour enregistrer les scans hors ligne
    private void saveOfflineScan(String rfid) {
        Set<String> offlineScans = new HashSet<>(sharedPreferences.getStringSet(OFFLINE_SCANS_PREF, new HashSet<>()));
        offlineScans.add(rfid);
        sharedPreferences.edit().putStringSet(OFFLINE_SCANS_PREF, offlineScans).apply();
    }

    // Enregistrer une vérification hors ligne
    private void saveOfflineVerification(ForfaitVerificationDTO verificationDTO) {
        dbHelper.saveOfflineVerification(
                verificationDTO.getRfid(),
                verificationDTO.getNomClient(),
                verificationDTO.getStatutForfait()
        );
    }

    // Synchronisation des transactions hors ligne avec le serveur
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

    // Synchronisation des forfaits depuis le serveur
    private void synchronizeForfaits() {
        Call<List<ClientDTO>> call = apiService.getAllClients();
        call.enqueue(new Callback<List<ClientDTO>>() {
            @Override
            public void onResponse(Call<List<ClientDTO>> call, Response<List<ClientDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ClientDTO> clients = response.body();
                    for (ClientDTO client : clients) {
                        dbHelper.saveCardInfo(client.getRfid(), client.getNom(), client.isForfaitActif(), client.getForfaitExpiration(), client.getForfaitStatus());

                    }
                    Toast.makeText(BusTripActivity.this, "Données synchronisées avec succès", Toast.LENGTH_SHORT).show();
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

    private void checkForfaitStatus(String rfid) {
        if (isConnected()) {
            Call<ClientDTO> call = apiService.verifyCard(rfid);
            call.enqueue(new Callback<ClientDTO>() {
                @Override
                public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ClientDTO client = response.body();
                        fetchForfaitStatus(rfid, client.getNom());
                    } else {
                        setScreenRed();
                        displayResult(rfid, "Client non trouvé");
                    }
                }

                @Override
                public void onFailure(Call<ClientDTO> call, Throwable t) {
                    fetchForfaitStatusOffline(rfid);
                }
            });
        } else {
            saveOfflineScan(rfid);
            fetchForfaitStatusOffline(rfid);
        }
    }

    private void fetchForfaitStatus(String rfid, String clientName) {
        Call<ForfaitDTO> forfaitCall = apiService.getForfaitStatus(rfid);
        forfaitCall.enqueue(new Callback<ForfaitDTO>() {
            @Override
            public void onResponse(Call<ForfaitDTO> call, Response<ForfaitDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ForfaitDTO forfait = response.body();
                    String statutForfait = forfait.getDateExpiration() != null ?
                            "Forfait Actif jusqu'à : " + new SimpleDateFormat("d MMMM yyyy", Locale.FRENCH).format(forfait.getDateExpiration()) :
                            "Aucun forfait actif";

                    if ("Aucun forfait actif".equals(statutForfait)) {
                        setScreenRed();
                    }

                    displayResult(rfid, clientName + "\n" + statutForfait);
                    saveForfaitVerification(clientName, rfid, statutForfait);
                } else {
                    setScreenRed();
                    displayResult(rfid, "Aucun forfait actif trouvé");
                }
            }

            @Override
            public void onFailure(Call<ForfaitDTO> call, Throwable t) {
                setScreenRed();
                displayResult(rfid, "Erreur lors de la récupération du forfait.");
            }
        });
    }

    private void fetchForfaitStatusOffline(String rfid) {
        Cursor cursor = dbHelper.getCardInfo(rfid);
        if (cursor != null && cursor.moveToFirst()) {
            String clientName = cursor.getString(cursor.getColumnIndexOrThrow("client_name"));
            boolean forfaitActive = cursor.getInt(cursor.getColumnIndexOrThrow("forfait_active")) == 1;
            String forfaitExpiration = cursor.getString(cursor.getColumnIndexOrThrow("forfait_expiration"));

            String statutForfait;
            if (forfaitActive) {
                String formattedDate = formatDate(forfaitExpiration);
                statutForfait = "Forfait Actif jusqu'à : " + formattedDate;
                setScreenGreen(); // Couleur verte pour un forfait actif
            } else {
                statutForfait = "Aucun forfait actif";
                setScreenRed(); // Couleur rouge pour un forfait inactif
            }

            displayResult(rfid, "Client : " + clientName + "\n" + statutForfait);
            saveForfaitVerification(clientName, rfid, statutForfait);
        } else {
            setScreenRed(); // Couleur rouge si aucun forfait trouvé
            displayResult(rfid, "Carte non trouvée. Aucun forfait actif.");
            Toast.makeText(this, "Cette carte n'a jamais été scannée auparavant.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setScreenRed() {
        runOnUiThread(() -> {
            // Changer l'arrière-plan de l'écran en rouge
            getWindow().getDecorView().setBackgroundColor(Color.RED);

            // Changer l'arrière-plan du bloc de forfait en rouge
            scannedCardInfo.setBackgroundColor(Color.RED);

            // Ajouter un délai pour revenir à la couleur d'origine
            new android.os.Handler().postDelayed(() -> {
                // Remettre la couleur d'arrière-plan à sa couleur d'origine (blanc)
                getWindow().getDecorView().setBackgroundColor(Color.WHITE);
                scannedCardInfo.setBackgroundColor(Color.WHITE); // Remettre le bloc à blanc
            }, 2000); // Délai de 2 secondes
        });
    }
    private void setScreenGreen() {
        runOnUiThread(() -> {
            // Changer l'arrière-plan de l'écran en vert
            getWindow().getDecorView().setBackgroundColor(Color.GREEN);

            // Changer l'arrière-plan du bloc de forfait en vert
            scannedCardInfo.setBackgroundColor(Color.GREEN);

            // Ajouter un délai pour revenir à la couleur d'origine
            new android.os.Handler().postDelayed(() -> {
                // Remettre la couleur d'arrière-plan à sa couleur d'origine (blanc)
                getWindow().getDecorView().setBackgroundColor(Color.WHITE);
                scannedCardInfo.setBackgroundColor(Color.WHITE); // Remettre le bloc à blanc
            }, 2000); // Délai de 2 secondes
        });
    }



    // Méthode pour formater la date
    private String formatDate(String dateString) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM yyyy", Locale.FRENCH);
        try {
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }

    private void displayResult(String rfid, String message) {
        String cardInfo = "RFID: " + rfid + " - " + message;
        scannedCardInfo.setText(cardInfo);
        scannedCardInfo.setVisibility(View.VISIBLE);
    }

    // Méthode pour enregistrer la vérification du forfait avec des informations supplémentaires
    private void saveForfaitVerification(String clientName, String rfid, String statutForfait) {
        ForfaitVerificationDTO verificationDTO = new ForfaitVerificationDTO(
                clientName,
                rfid,
                statutForfait,
                macAddress,
                userRole,
                userName,
                true
        );

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String terminalType = android.os.Build.MODEL;
        String connectionTime = getFormattedDate();

        if (isConnected()) {
            Call<Void> call = apiService.saveForfaitVerification(verificationDTO, androidId, batteryLevel, terminalType, userName, connectionTime);
            call.enqueue(new Callback<Void>() {
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
                    saveOfflineVerification(verificationDTO);
                }
            });
        } else {
            saveOfflineVerification(verificationDTO);
        }
    }

    // Méthode pour rafraîchir l'interface utilisateur sans redémarrer l'activité
    private void updateUI() {
        runOnUiThread(() -> {
            scannedCardInfo.setText("");
            scannedCardInfo.setVisibility(View.GONE);
        });
    }

    private void resetTrip() {
        isDestinationSelected = false;
        isTripStarted = false;
        isTripEnded = false;
        selectedDestination = "";
        startTime = "";
        endTime = "";

        newTripButton.setVisibility(View.GONE);
        endTripLayout.setVisibility(View.GONE);
        startTimeView.setText("");
        endTimeView.setText("");
        scannedCardInfo.setText("");
        scannedCardInfo.setVisibility(View.GONE);
        destinationSelectionLayout.setVisibility(View.VISIBLE);
        destinationSpinner.setSelection(0);
        destinationSpinner.setEnabled(true);

        selectedDestinationTitle.setVisibility(View.GONE);
    }

    private void endTrip(String macAddress) {
        Call<Void> call = apiService.endTrip(macAddress);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Trajet terminé avec succès", Toast.LENGTH_SHORT).show();
                    endTimeView.setText("Fin du trajet : " + endTime);
                    newTripButton.setVisibility(View.VISIBLE);
                    saveBusHistory();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Erreur : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale.FRENCH);
        return sdf.format(new Date());
    }

    private void sendBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int niveauBatterie = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        boolean isCharging = BatteryUtils.isCharging(this);

        String terminalType = android.os.Build.MODEL;
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        apiService.updateBusBatteryLevel(macAddress, niveauBatterie, isCharging, androidId, terminalType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Niveau de batterie mis à jour", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors de la mise à jour du niveau de batterie", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Erreur: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm, Runnable onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirmer", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Annuler", (dialog, which) -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                });

        builder.show();
    }

    private void startTrip(String macAddress, String destination, String chauffeurNom, String chauffeurUniqueNumber) {
        String terminalType = android.os.Build.MODEL;
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        int batteryLevel = ((BatteryManager) getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        Call<Void> call = apiService.startTrip(macAddress, destination, chauffeurNom, chauffeurUniqueNumber, batteryLevel, androidId, terminalType);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BusTripActivity.this, "Trajet démarré avec succès pour la destination : " + destination, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors du démarrage du trajet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Échec de la connexion au serveur", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDestinations() {
        Call<List<LigneTrajetDTO>> call = apiService.getAllLignes();
        call.enqueue(new Callback<List<LigneTrajetDTO>>() {
            @Override
            public void onResponse(Call<List<LigneTrajetDTO>> call, Response<List<LigneTrajetDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LigneTrajetDTO> lignes = response.body();
                    List<String> destinationNames = new ArrayList<>();
                    destinationNames.add("Sélectionner une destination");
                    for (LigneTrajetDTO ligne : lignes) {
                        destinationNames.add(ligne.getNomLigne());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(BusTripActivity.this,
                            android.R.layout.simple_spinner_item, destinationNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    destinationSpinner.setAdapter(adapter);
                } else {
                    Toast.makeText(BusTripActivity.this, "Erreur lors de la récupération des destinations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<LigneTrajetDTO>> call, Throwable t) {
                Toast.makeText(BusTripActivity.this, "Échec de la récupération des destinations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Méthode pour enregistrer l'historique du trajet
    private void saveBusHistory() {
        String destination = selectedDestination;
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String connectionTime = getFormattedDate();

        int batteryLevel = ((BatteryManager) getSystemService(BATTERY_SERVICE)).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String terminalType = android.os.Build.MODEL;

        BusHistoryDTO busHistoryDTO = new BusHistoryDTO(
                macAddress,
                destination,
                userName,
                chauffeurUniqueNumber,
                batteryLevel,
                terminalType,
                androidId,
                connectionTime
        );

        Call<Void> call = apiService.saveBusHistory(busHistoryDTO);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Historique du trajet enregistré avec succès.");
                } else {
                    Log.e(TAG, "Erreur lors de l'enregistrement de l'historique.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Erreur lors de l'enregistrement de l'historique.", t);
            }
        });
    }
}
