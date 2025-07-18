package app.smashsmashin.authoridassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PowerReceiver extends BroadcastReceiver {
    private static final String TAG = "AuthorIDAssistant";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            Log.d(TAG, "Power connected");
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            Log.d(TAG, "Power disconnected");
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            Log.d(TAG, "Screen ON");
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Log.d(TAG, "Screen OFF");
        } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            Log.d(TAG, "User unlocked");
        }
    }
}
