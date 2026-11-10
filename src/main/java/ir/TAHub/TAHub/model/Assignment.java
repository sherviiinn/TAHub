package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An assignment of one offering. The questions are a PDF (questionFile); their number and points are defined
 * later, while the first answer is graded (phase 5). A draft is seen only by the professor and the TAs.
 * The deadline can be extended for everyone, many times (AssignmentExtension); firstDeadline stays the original,
 * so answers sent after it can get the factor of the extension (phase 5). Deleting only hides.
 */
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Column(nullable = false, length = 150)
    private String title;

    /** Used in file names ("name - student number - Ch2.pdf"), so it is short. */
    @Column(nullable = false, length = 30)
    private String chapter;

    @Column(length = 2000)
    private String description;

    /** The deadline now (the last extension, or the first deadline). */
    @Column(nullable = false)
    private Instant deadline;

    /** The deadline when the assignment was published. Answers after it are "extended". */
    @Column(nullable = false)
    private Instant firstDeadline;

    @ManyToOne
    @JoinColumn(name = "question_file_id")
    private StoredFile questionFile;

    @Column(nullable = false)
    private boolean published;

    private Instant publishedAt;

    @Column(nullable = false)
    private boolean deleted;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Assignment() {
    }

    /** Has the deadline been extended at least once? */
    public boolean isExtended() {
        return published && deadline.isAfter(firstDeadline);
    }

    public boolean isOpenForSubmissions(Instant now) {
        return published && !deleted && now.isBefore(deadline);
    }

    public Long getId() { return id; }

    public CourseOffering getOffering() { return offering; }
    public void setOffering(CourseOffering offering) { this.offering = offering; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }

    public Instant getFirstDeadline() { return firstDeadline; }
    public void setFirstDeadline(Instant firstDeadline) { this.firstDeadline = firstDeadline; }

    public StoredFile getQuestionFile() { return questionFile; }
    public void setQuestionFile(StoredFile questionFile) { this.questionFile = questionFile; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
