package app.smashsmashin.authoridassistant;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

  private static final String TAG = "AuthorIDAssistant";
  private static final String TOGGLE_BUTTON_ID = "com.dma.author.authorid:id/button";
  private static final String TARGET_CLASS = "com.dma.author.authorid.view.TagActivity";

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
      findAndClickToggleButton(rootNode);

      // Close the Author ID app
      performGlobalAction(GLOBAL_ACTION_BACK);
    }
  }

  private void findAndClickToggleButton(AccessibilityNodeInfo rootNode) {
    if (rootNode != null) {
      List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(TOGGLE_BUTTON_ID);
      if (nodes != null && !nodes.isEmpty()) {
        AccessibilityNodeInfo toggleButton = nodes.get(0);
        Log.d(TAG, "ToggleButton found. Is checked: " + toggleButton.isChecked());

        if (AppState.shouldActivate && !toggleButton.isChecked()) {
          Log.d(TAG, "ToggleButton is not checked. Performing click to activate.");
          toggleButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
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
