package ir.TAHub.TAHub.repository;

import ir.TAHub.TAHub.model.SiteNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteNoticeRepository extends JpaRepository<SiteNotice, Long> {

    // Newest first. The id breaks ties between notices posted in the same moment.
    List<SiteNotice> findByDeletedFalseOrderByCreatedAtDescIdDesc();

    // The notice for the top bar: the newest one that should be shown there.
    Optional<SiteNotice> findFirstByDeletedFalseAndShowInBannerTrueOrderByCreatedAtDescIdDesc();
}
