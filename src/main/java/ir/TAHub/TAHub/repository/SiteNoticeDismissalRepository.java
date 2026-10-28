package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.SiteNotice;
import ir.TAHub.TAHub.model.SiteNoticeDismissal;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteNoticeDismissalRepository extends JpaRepository<SiteNoticeDismissal, Long> {

    boolean existsByUserAndNotice(User user, SiteNotice notice);
}
