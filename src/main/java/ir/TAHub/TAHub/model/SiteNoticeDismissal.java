package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/** A user closed the top bar of one notice, so it is not shown to them again (on any device). */
@Entity
@Table(name = "site_notice_dismissals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notice_id"}))
public class SiteNoticeDismissal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private SiteNotice notice;

    @Column(nullable = false)
    private Instant dismissedAt;

    public SiteNoticeDismissal() {
    }

    public SiteNoticeDismissal(User user, SiteNotice notice) {
        this.user = user;
        this.notice = notice;
        this.dismissedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public SiteNotice getNotice() { return notice; }
    public Instant getDismissedAt() { return dismissedAt; }
}
