package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.NotificationRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live unread counts with Server-Sent Events (SSE).
 * Every open page (tab) of a logged-in user keeps one connection here. When that user's notifications change,
 * the new unread count is pushed to all their tabs, so the menu updates without reloading the page.
 * The same connection also carries "changed" events (live pages): which topic changed, like "offering:5"
 * (see LiveTopicListener). A page showing that topic quietly reloads its content.
 * A connection is idle almost all the time: it costs a few KB and no thread while nothing happens.
 */
@Service
public class NotificationStream {

    /** After this the browser simply reconnects (EventSource does that by itself). */
    private static final long CONNECTION_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    /** The key under which a transaction keeps the users whose counts changed. */
    private static final Object PENDING_KEY = NotificationStream.class.getName() + ".pending";

    /** Set once at startup, so the entity listener (which is not a Spring bean) can reach this service. */
    private static NotificationStream instance;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** user id -> the open connections of that user (one per tab). */
    private final Map<Long, List<SseEmitter>> connections = new ConcurrentHashMap<>();

    public NotificationStream(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    void remember() {
        instance = this;
    }

    /** Opens a live connection for this user and sends the current count right away. */
    public SseEmitter connect(User user) {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
        Long userId = user.getId();
        connections.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable forget = () -> forget(userId, emitter);
        emitter.onCompletion(forget);
        emitter.onTimeout(forget);
        emitter.onError(error -> forget.run());

        send(userId, emitter, notificationRepository.countByUserAndSeenFalse(user));
        return emitter;
    }

    /**
     * Called by NotificationLiveListener. Inside a transaction, the push waits until the commit
     * (so the page never sees a count that was rolled back); outside one, it is sent now.
     */
    public static void countChanged(Long userId) {
        NotificationStream stream = instance;
        if (stream == null || !stream.connections.containsKey(userId)) {
            return; // nobody has a page open for this user
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            stream.push(Set.of(userId));
            return;
        }

        @SuppressWarnings("unchecked")
        Set<Long> pending = (Set<Long>) TransactionSynchronizationManager.getResource(PENDING_KEY);
        if (pending == null) {
            Set<Long> users = new HashSet<>();
            TransactionSynchronizationManager.bindResource(PENDING_KEY, users);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // On another thread, fully outside the finished transaction (it only reads counts).
                    CompletableFuture.runAsync(() -> stream.push(users));
                }

                @Override
                public void afterCompletion(int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(PENDING_KEY);
                }
            });
            pending = users;
        }
        pending.add(userId);
    }

    /**
     * Tells every open tab that a topic changed. Only the topic name is sent (like "offering:5"),
     * never data: each page then reloads itself with its own permissions.
     */
    public void broadcast(String topic) {
        connections.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                sendChanged(userId, emitter, topic);
            }
        });
    }

    /** Every 25 seconds a tiny comment keeps idle connections open (some networks close silent ones). */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        connections.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    forget(userId, emitter);
                }
            }
        });
    }

    // ---------- helpers ----------

    private void push(Set<Long> userIds) {
        for (Long userId : userIds) {
            List<SseEmitter> emitters = connections.get(userId);
            if (emitters == null || emitters.isEmpty()) {
                continue;
            }
            userRepository.findById(userId).ifPresent(user -> {
                long count = notificationRepository.countByUserAndSeenFalse(user);
                for (SseEmitter emitter : emitters) {
                    send(userId, emitter, count);
                    // The user's notifications page (if open) reloads its list too.
                    sendChanged(userId, emitter, "notifications");
                }
            });
        }
    }

    private void send(Long userId, SseEmitter emitter, long count) {
        try {
            emitter.send(SseEmitter.event().name("unread").data(count));
        } catch (IOException | IllegalStateException e) {
            forget(userId, emitter); // the tab was closed
        }
    }

    private void sendChanged(Long userId, SseEmitter emitter, String topic) {
        try {
            emitter.send(SseEmitter.event().name("changed").data(topic));
        } catch (IOException | IllegalStateException e) {
            forget(userId, emitter);
        }
    }

    private void forget(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = connections.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                connections.remove(userId, emitters);
            }
        }
    }
}
