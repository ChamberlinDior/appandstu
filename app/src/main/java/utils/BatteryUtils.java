package utils;


import android.content.Context;
import android.os.BatteryManager;

public class BatteryUtils {

    // Méthode pour obtenir le niveau de la batterie
    public static int getBatteryLevel(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }
}
