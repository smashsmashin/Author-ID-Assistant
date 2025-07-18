package app.smashsmashin.authoridassistant;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.Intent;

public class MyAccessibilityService extends AccessibilityService {

  private static final String TAG = "AuthorIDAssistant";
  private static final String TOGGLE_BUTTON_ID = "com.dma.author.authorid:id/button";
  private static final String TARGET_CLASS = "com.dma.author.authorid.view.TagActivity";

  private final Handler timerHandler = new Handler(Looper.getMainLooper());

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    if (!AppState.isKeyFobActionPending) {
      return;
    }
    // Log.d(TAG, "Accessibility event received: " + event.toString());
    if (event == null || event.getClassName() == null)
      return;

    if (TARGET_CLASS.equals(event.getClassName().toString())) {
      AppState.isKeyFobActionPending = false;

      AccessibilityNodeInfo rootNode = getRootInActiveWindow();
      if (rootNode != null) {
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(TOGGLE_BUTTON_ID);
        if (nodes != null && !nodes.isEmpty()) {
          AccessibilityNodeInfo toggleButton = nodes.get(0);
          Log.d(TAG, "ToggleButton found. Is checked: " + toggleButton.isChecked());

          if (AppState.shouldActivate && !toggleButton.isChecked()) {
            Log.d(TAG, "ToggleButton is not checked. Performing click to activate.");
            toggleButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);

            // Start a 1-minute timer
            timerHandler.postDelayed(() -> {
              Log.d(TAG, "1-minute timer finished. Attempting to unccheck button.");

              AppState.isKeyFobActionPending = true;
              AppState.shouldActivate = false;

              Intent intent = new Intent();
              intent.setClassName("com.dma.author.authorid", "com.dma.author.authorid.view.SplashActivity");
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            }, 45000);
            Log.d(TAG, "Started 1-minute timer.");
          } else if (!AppState.shouldActivate && toggleButton.isChecked()) {
            Log.d(TAG, "ToggleButton is checked. Performing click to deactivate.");
            toggleButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
          } else {
            Log.d(TAG, "ToggleButton is already in the desired state. No action needed.");
          }

          // Important: recycle when done
          toggleButton.recycle();
        } else {
          Log.w(TAG, "ToggleButton with ID " + TOGGLE_BUTTON_ID + " not found.");
        }

        // Important: recycle when done
        rootNode.recycle();
      }

      // Close the Author ID app
      performGlobalAction(GLOBAL_ACTION_BACK);
    }
  }

  @Override
  public void onInterrupt() {
    Log.d(TAG, "Accessibility service interrupted.");
  }

  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    Log.d(TAG, "Accessibility service connected (or restarted after boot).");
  }
}
