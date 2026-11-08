package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.CourseOffering;
import ir.TAHub.TAHub.model.TaLimitRequest;
import ir.TAHub.TAHub.model.TaLimitRequestStatus;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaLimitRequestRepository extends JpaRepository<TaLimitRequest, Long> {

    // The waiting request of an offering (there is at most one).
    Optional<TaLimitRequest> findFirstByOfferingAndStatus(CourseOffering offering, TaLimitRequestStatus status);

    // For the nightly job: waiting requests older than a moment.
    List<TaLimitRequest> findByStatusAndCreatedAtBefore(TaLimitRequestStatus status, Instant before);

    List<TaLimitRequest> findByStatus(TaLimitRequestStatus status);

    // When a student number is released: the waiting requests that person sent.
    List<TaLimitRequest> findByRequestedByAndStatus(User requestedBy, TaLimitRequestStatus status);
}
