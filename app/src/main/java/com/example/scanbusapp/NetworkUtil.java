package com.example.scanbusapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkUtil {

    private static final MutableLiveData<Boolean> networkStatus = new MutableLiveData<>();

    // Méthode pour vérifier la disponibilité du réseau
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // Méthode pour observer le statut du réseau
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void registerNetworkCallback(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                networkStatus.postValue(true);  // Réseau disponible
            }

            @Override
            public void onLost(Network network) {
                networkStatus.postValue(false); // Réseau perdu
            }
        });
    }

    // Méthode pour obtenir le statut du réseau en tant que LiveData
    public static LiveData<Boolean> getNetworkStatus() {
        return networkStatus;
    }
}
