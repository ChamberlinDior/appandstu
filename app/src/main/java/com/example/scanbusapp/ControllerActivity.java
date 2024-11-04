package com.example.scanbusapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControllerActivity extends AppCompatActivity {
    private static final String TAG = "ControllerActivity";

    private TextView tvRfidInfo;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private AppDatabase db;
    private String scannedRfid;

    // ExecutorService pour gérer les opérations asynchrones en arrière-plan
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller); // Assurez-vous que le layout a le bon fond comme BusTripActivity

        initUI();
        initNfcAdapter();
        initDatabase();
    }

    private void initUI() {
        tvRfidInfo = findViewById(R.id.tvRfidInfo);



    }

    private void initNfcAdapter() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC n'est pas disponible sur cet appareil.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }

    private void initDatabase() {
        db = AppDatabase.getInstance(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                scannedRfid = toHexString(tag.getId());
                handleCardScan(scannedRfid);
            }
        }
    }

    private void enableForegroundDispatch() {
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void handleCardScan(String rfid) {
        executorService.execute(() -> {
            try {
                Carte existingCarte = db.carteDao().findByRfid(rfid);
                if (existingCarte != null) {
                    validateForfait(existingCarte);
                } else {
                    setScreenRed();
                    runOnUiThread(() -> tvRfidInfo.setText("Carte invalide ou forfait non trouvé."));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la validation du forfait", e);
                runOnUiThread(() -> Toast.makeText(this, "Erreur de validation : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void validateForfait(Carte existingCarte) {
        List<Forfait> forfaits = db.forfaitDao().getForfaitsByCarteId(existingCarte.getId());

        if (forfaits == null || forfaits.isEmpty() || !isForfaitActive(forfaits.get(0))) {
            setScreenRed();
            runOnUiThread(() -> tvRfidInfo.setText("Aucun forfait actif pour ce RFID."));
        } else {
            runOnUiThread(() -> tvRfidInfo.setText("Forfait actif : " + forfaits.get(0).getTypeForfait()));
        }
    }

    private boolean isForfaitActive(Forfait forfait) {
        Date now = new Date();
        return now.before(forfait.getDateExpiration());
    }

    private void setScreenRed() {
        runOnUiThread(() -> {
            getWindow().getDecorView().setBackgroundColor(Color.RED);
            new android.os.Handler().postDelayed(() -> {
                getWindow().getDecorView().setBackgroundColor(getResources().getColor(android.R.color.white));
            }, 2000); // Retourne au fond blanc après 2 secondes
        });
    }
}
