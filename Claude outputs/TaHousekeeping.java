package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.model.TaApplication;
import ir.TAHub.TAHub.model.TaApplicationStatus;
import ir.TAHub.TAHub.model.TaInvitation;
import ir.TAHub.TAHub.model.TaInvitationStatus;
import ir.TAHub.TAHub.model.TaLimitRequest;
import ir.TAHub.TAHub.model.TaLimitRequestStatus;
import ir.TAHub.TAHub.repository.TaApplicationRepository;
import ir.TAHub.TAHub.repository.TaInvitationRepository;
import ir.TAHub.TAHub.repository.TaLimitRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Closes TA things that waited too long, or whose semester is over:
 *  - every night: TA invitations and team-limit requests older than 7 days expire
 *    (an invitation takes a place in the team, so it must not wait forever);
 *  - when a new semester starts: the old semester's waiting TA requests, invitations and limit requests close.
 * Students' own TA requests do not expire by time: the professor closes them, or the semester ends.
 */
@Service
public class TaHousekeeping {

    /** How long an invitation or a limit request waits before it expires. Change it here. */
    public static final Duration WAIT_LIMIT = Duration.ofDays(7);

    private static final Logger log = LoggerFactory.getLogger(TaHousekeeping.class);

    private final TaInvitationRepository invitationRepository;
    private final TaLimitRequestRepository limitRequestRepository;
    private final TaApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    public TaHousekeeping(TaInvitationRepository invitationRepository,
                          TaLimitRequestRepository limitRequestRepository,
                          TaApplicationRepository applicationRepository,
                          NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.limitRequestRepository = limitRequestRepository;
        this.applicationRepository = applicationRepository;
        this.notificationService = notificationService;
    }

    /** Every night at 00:20 Tehran time. */
    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Tehran")
    @Transactional
    public void expireOldOnes() {
        Instant before = Instant.now().minus(WAIT_LIMIT);

        int invitations = 0;
        for (TaInvitation invitation : invitationRepository.findByStatusAndCreatedAtBefore(TaInvitationStatus.PENDING, before)) {
            // There is no separate "expired" state for invitations; CANCELLED with an "expired" message.
            close(invitation);
            notificationService.notifyTaInvitationExpired(invitation);
            invitations++;
        }

        int requests = 0;
        for (TaLimitRequest request : limitRequestRepository.findByStatusAndCreatedAtBefore(TaLimitRequestStatus.PENDING, before)) {
            close(request, TaLimitRequestStatus.EXPIRED);
            notificationService.notifyLimitRequestAnswered(request, NotificationService.TA_LIMIT_EXPIRED_KEY);
            requests++;
        }

        if (invitations + requests > 0) {
            log.info("Expired {} TA invitations and {} team limit requests", invitations, requests);
        }
    }

    /**
     * Call this when a semester ends (a new one starts). Its waiting TA requests become WITHDRAWN,
     * its waiting invitations and limit requests are closed, and the unread notifications about them go away.
     */
    @Transactional
    public void closeSemester(Semester semester) {
        Set<CourseOffering> touched = new HashSet<>();

        for (TaApplication application : applicationRepository.findByStatusOrderByIdAsc(TaApplicationStatus.PENDING)) {
            if (inSemester(application.getOffering(), semester)) {
                application.setStatus(TaApplicationStatus.WITHDRAWN);
                application.setDecidedAt(Instant.now());
                applicationRepository.save(application);
                touched.add(application.getOffering());
            }
        }
        touched.forEach(notificationService::clearTaRequestNotifications);

        for (TaInvitation invitation : invitationRepository.findByStatus(TaInvitationStatus.PENDING)) {
            if (inSemester(invitation.getOffering(), semester)) {
                close(invitation);
            }
        }

        for (TaLimitRequest request : limitRequestRepository.findByStatus(TaLimitRequestStatus.PENDING)) {
            if (inSemester(request.getOffering(), semester)) {
                close(request, TaLimitRequestStatus.CANCELLED);
                notificationService.clearLimitRequestNotification(request);
            }
        }
    }

    // ---------- helpers ----------

    private void close(TaInvitation invitation) {
        invitation.setStatus(TaInvitationStatus.CANCELLED);
        invitation.setDecidedAt(Instant.now());
        invitationRepository.save(invitation);
        notificationService.onTaInvitationCancelled(invitation); // the invitee's unread notice goes away
    }

    private void close(TaLimitRequest request, TaLimitRequestStatus status) {
        request.setStatus(status);
        request.setDecidedAt(Instant.now());
        limitRequestRepository.save(request);
    }

    private boolean inSemester(CourseOffering offering, Semester semester) {
        return offering.getSemester().getId().equals(semester.getId());
    }
}
