package ir.TAHub.TAHub.config;

import ir.TAHub.TAHub.model.Announcement;
import ir.TAHub.TAHub.model.CafeteriaMeal;
import ir.TAHub.TAHub.model.CafeteriaSettings;
import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.Food;
import ir.TAHub.TAHub.model.RosterEntry;
import ir.TAHub.TAHub.model.SiteNotice;
import ir.TAHub.TAHub.model.SiteNoticeDismissal;
import ir.TAHub.TAHub.model.TaApplication;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.service.NotificationStream;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Live pages: after a change is COMMITTED to the database, every open tab is told which "topic" changed
 * (like "offering:5"). A page that shows that topic reloads its content quietly (static/js/live.js).
 * This listens to Hibernate for all tables at once, so no controller has to remember to do it.
 *
 * Topics:  offering:<id>  course page and its announcements
 *          ta-requests    the TA request pages
 *          notices        site notices page;  banner  the yellow top bar
 *          cafeteria      cafeteria pages
 *          (notifications pages are told separately, only to their owner, by NotificationStream)
 */
@Component
public class LiveTopicListener
        implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

    private final EntityManagerFactory entityManagerFactory;
    private final NotificationStream stream;

    public LiveTopicListener(EntityManagerFactory entityManagerFactory, NotificationStream stream) {
        this.entityManagerFactory = entityManagerFactory;
        this.stream = stream;
    }

    @PostConstruct
    void register() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sessionFactory.getEventEngine().getListenerRegistry();
        registry.appendListeners(EventType.POST_COMMIT_INSERT, this);
        registry.appendListeners(EventType.POST_COMMIT_UPDATE, this);
        registry.appendListeners(EventType.POST_COMMIT_DELETE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        changed(event.getEntity());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        changed(event.getEntity());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        changed(event.getEntity());
    }

    // Nothing to tell if the commit failed.
    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
    }

    /** Hibernate 7 only sends "after commit" events to listeners that say they want them. */
    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return true;
    }

    private void changed(Object entity) {
        Set<String> topics = new LinkedHashSet<>();
        if (entity instanceof CourseOffering offering) {
            topics.add(offeringTopic(offering));
        } else if (entity instanceof Enrollment enrollment) {
            topics.add(offeringTopic(enrollment.getOffering()));
        } else if (entity instanceof RosterEntry entry) {
            topics.add(offeringTopic(entry.getOffering()));
        } else if (entity instanceof TaAssignment assignment) {
            topics.add(offeringTopic(assignment.getOffering()));
            topics.add("ta-requests");
        } else if (entity instanceof TaApplication application) {
            topics.add(offeringTopic(application.getOffering()));
            topics.add("ta-requests");
        } else if (entity instanceof Announcement announcement) {
            topics.add(offeringTopic(announcement.getOffering()));
        } else if (entity instanceof SiteNotice) {
            topics.add("notices");
            topics.add("banner");
        } else if (entity instanceof SiteNoticeDismissal) {
            topics.add("banner");
        } else if (entity instanceof Food || entity instanceof CafeteriaMeal || entity instanceof CafeteriaSettings) {
            topics.add("cafeteria");
        }
        topics.forEach(stream::broadcast);
    }

    private String offeringTopic(CourseOffering offering) {
        return "offering:" + offering.getId();
    }
}
