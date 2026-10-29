package ir.TAHub.TAHub.model;

import ir.TAHub.TAHub.service.NotificationStream;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * Hibernate calls this after a notification row is saved, changed or deleted.
 * It only remembers WHOSE count changed; the new count is sent after the transaction is committed.
 */
public class NotificationLiveListener {

    @PostPersist
    @PostUpdate
    @PostRemove
    void changed(Notification notification) {
        if (notification.getUser() != null) {
            NotificationStream.countChanged(notification.getUser().getId());
        }
    }
}
