package app.smashsmashin.authoridassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "AuthorIDAssistant";

    @Override
    public void onReceive(Context context, Intent intent) {
	Log.d(TAG, "Boot!");
        // Register dynamic receiver for screen events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);

        context.getApplicationContext().registerReceiver(new PowerReceiver(), filter);
    }
}