package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.SiteNotice;
import ir.TAHub.TAHub.model.SiteNoticeDismissal;
import ir.TAHub.TAHub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SiteNoticeDismissalRepository extends JpaRepository<SiteNoticeDismissal, Long> {

    boolean existsByUserAndNotice(User user, SiteNotice notice);

    // Everyone sees the top bar of this notice again.
    @Transactional
    long deleteByNotice(SiteNotice notice);
}
