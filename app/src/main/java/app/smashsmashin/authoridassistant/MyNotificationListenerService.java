package app.smashsmashin.authoridassistant;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import java.util.Optional;

public class MyNotificationListenerService extends NotificationListenerService {

    private static MyNotificationListenerService instance;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        instance = null;
    }

    public static Optional<MyNotificationListenerService> getInstance() {
        return Optional.ofNullable(instance);
    }

    public boolean isAuthorIDNotificationPresent() {
        for (StatusBarNotification sbn : getActiveNotifications()) {
            if ("com.dma.author.authorid".equals(sbn.getPackageName())) {
                Notification notification = sbn.getNotification();
                if (notification != null && notification.contentIntent != null) {
                    String title = notification.extras.getString(Notification.EXTRA_TITLE);
                    String text = notification.extras.getString(Notification.EXTRA_TEXT);
                    if ("Author ID".equals(title) && "Key FOB activated".equals(text)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
