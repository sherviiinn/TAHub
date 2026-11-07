package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.Enrollment;
import ir.TAHub.TAHub.model.EnrollmentStatus;
import ir.TAHub.TAHub.model.TaApplication;
import ir.TAHub.TAHub.model.TaApplicationStatus;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaInvitation;
import ir.TAHub.TAHub.model.TaInvitationStatus;
import ir.TAHub.TAHub.model.TaLimitRequest;
import ir.TAHub.TAHub.model.TaLimitRequestStatus;
import ir.TAHub.TAHub.model.User;
import ir.TAHub.TAHub.repository.EnrollmentRepository;
import ir.TAHub.TAHub.repository.TaApplicationRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import ir.TAHub.TAHub.repository.TaInvitationRepository;
import ir.TAHub.TAHub.repository.TaLimitRequestRepository;
import ir.TAHub.TAHub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Disabling accounts and releasing a student number that someone else took.
 * Nothing is ever deleted: a disabled account keeps all its history; a released one too, under a new
 * internal username, so the real owner of the number can register.
 */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaAssignmentRepository taRepository;
    private final TaInvitationRepository invitationRepository;
    private final TaApplicationRepository applicationRepository;
    private final TaLimitRequestRepository limitRequestRepository;

    public AccountService(UserRepository userRepository,
                          EnrollmentRepository enrollmentRepository,
                          TaAssignmentRepository taRepository,
                          TaInvitationRepository invitationRepository,
                          TaApplicationRepository applicationRepository,
                          TaLimitRequestRepository limitRequestRepository) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taRepository = taRepository;
        this.invitationRepository = invitationRepository;
        this.applicationRepository = applicationRepository;
        this.limitRequestRepository = limitRequestRepository;
    }

    /** The account cannot sign in any more; if it is signed in now, it is signed out on its next click. */
    @Transactional
    public void disable(User user) {
        if (user.getDisabledAt() == null) {
            user.setDisabledAt(Instant.now());
            userRepository.save(user);
        }
    }

    /** Undo "disable". Not possible after the number was released. */
    @Transactional
    public void enable(User user) {
        if (user.getReleasedNumber() == null) {
            user.setDisabledAt(null);
            userRepository.save(user);
        }
    }

    /**
     * Frees the student number of a DISABLED account: the account gets the internal username
     * "released-<id>" (the old number is kept in releasedNumber), and it is taken out of every course:
     * enrollments become REMOVED, TA roles end, waiting TA invitations, TA requests and
     * team-limit requests are closed.
     */
    @Transactional
    public void release(User user) {
        if (user.getDisabledAt() == null || user.getReleasedNumber() != null) {
            throw new IllegalStateException("Only a disabled, not yet released account can be released");
        }
        Instant now = Instant.now();

        for (Enrollment enrollment : enrollmentRepository.findByStudentAndStatus(user, EnrollmentStatus.ACTIVE)) {
            enrollment.setStatus(EnrollmentStatus.REMOVED);
            enrollmentRepository.save(enrollment);
        }
        for (TaAssignment assignment : taRepository.findByUserAndActiveTrue(user)) {
            assignment.setActive(false);
            taRepository.save(assignment);
        }
        for (TaInvitation invitation : invitationRepository
                .findByInviteeAndStatusOrderByCreatedAtDesc(user, TaInvitationStatus.PENDING)) {
            invitation.setStatus(TaInvitationStatus.CANCELLED);
            invitation.setDecidedAt(now);
            invitationRepository.save(invitation);
        }
        for (TaApplication application : applicationRepository.findByStudentOrderByIdDesc(user)) {
            if (application.getStatus() == TaApplicationStatus.PENDING) {
                application.setStatus(TaApplicationStatus.WITHDRAWN);
                application.setDecidedAt(now);
                applicationRepository.save(application);
            }
        }

        for (TaLimitRequest request : limitRequestRepository.findByRequestedByAndStatus(user, TaLimitRequestStatus.PENDING)) {
            request.setStatus(TaLimitRequestStatus.CANCELLED);
            request.setDecidedAt(now);
            limitRequestRepository.save(request);
        }

        user.setReleasedNumber(user.getStudentNumber());
        user.setStudentNumber("released-" + user.getId());
        userRepository.save(user);
    }
}
