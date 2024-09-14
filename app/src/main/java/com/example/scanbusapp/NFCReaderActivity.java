package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NFCReaderActivity extends AppCompatActivity {

    private static final String TAG = "NFCReaderActivity";
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private TextView resultView;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcreader);

        resultView = findViewById(R.id.result_view);

        // Initialiser Retrofit pour l'API
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Initialisation du NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            resultView.setText("NFC n'est pas supporté sur cet appareil.");
            return;
        }

        // Initialisation du PendingIntent avec le flag mutabilité
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
            fetchClientInfo(rfid); // Récupérer les informations du client avec ce RFID
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private void fetchClientInfo(String rfid) {
        Log.d(TAG, "Fetching client info for RFID: " + rfid); // Ajout de logs pour déboguer
        Call<ClientDTO> call = apiService.verifyCard(rfid);
        call.enqueue(new Callback<ClientDTO>() {
            @Override
            public void onResponse(Call<ClientDTO> call, Response<ClientDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ClientDTO client = response.body();
                    String clientInfo = "Nom: " + client.getNom() + "\n" +
                            "Prénom: " + client.getPrenom() + "\n" +
                            "Forfait actif: " + (client.isForfaitActif() ? "Oui" : "Non");
                    resultView.setText(clientInfo);
                    Log.d(TAG, "Client info retrieved successfully");
                } else {
                    resultView.setText("Client non trouvé");
                    Log.e(TAG, "Erreur: Client non trouvé ou problème serveur");
                }
            }

            @Override
            public void onFailure(Call<ClientDTO> call, Throwable t) {
                resultView.setText("Erreur de communication avec le serveur");
                Log.e(TAG, "Erreur lors de la récupération des informations du client", t);
            }
        });
    }
}
