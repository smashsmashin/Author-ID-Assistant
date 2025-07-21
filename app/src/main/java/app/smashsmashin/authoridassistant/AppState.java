package app.smashsmashin.authoridassistant;

import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.util.Log;
import android.content.Context;

public class AppState {
    private static final String TAG = "AuthorIDAssistant";

    public static boolean isKeyFobActionPending = false;
    public static boolean shouldActivate = false;
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static int timers = 0;

    public static void cancelAllTimers() {
        handler.removeCallbacksAndMessages(AppState.class);
        if (timers > 0) {
            Log.d(TAG, "Canceled " + timers + " timers.");
            timers = 0;
        }
    }

    private static void startTimer(Runnable r, long millis) {
        handler.postDelayed(r, AppState.class, millis);
        Log.d(TAG, "Started " + (millis / 1000) + " seconds timer.");
        timers++;
    }

    public static void triggerAuthorIDActivity(Context context) {
        isKeyFobActionPending = true;
        proceedWithTheActivity(context);

        // ensure to stop it later
        if (shouldActivate) {
            cancelAllTimers();

            // Start a 30-seconds timer
            startTimer(() -> {
                timers--;
                Log.d(TAG, "30-seconds timer finished. Attempting to unccheck button.");
                isKeyFobActionPending = true;
                shouldActivate = false;
                proceedWithTheActivity(context);
            }, 30000);
        }
    }

    private static void proceedWithTheActivity(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.dma.author.authorid", "com.dma.author.authorid.view.SplashActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}