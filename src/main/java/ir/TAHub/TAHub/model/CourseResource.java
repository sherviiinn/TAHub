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
 * A learning resource of one offering: a video, a folder of slides, a PDF, any https link.
 * Resources belong to the offering (one semester), so every semester starts empty; the next semester
 * can copy the ones it wants from the last semester's list. "deleted" only hides a resource.
 */
@Entity
@Table(name = "course_resources")
public class CourseResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 300)
    private String description;

    /** Optional group, like "Chapter 1" or "Solved exercises". Resources without one are not grouped. */
    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean deleted;

    public CourseResource() {
    }

    /** What kind of link this is, for its icon: youtube, aparat, drive, telegram, github, pdf or link. */
    public String getKind() {
        return ResourceKind.of(url);
    }

    public Long getId() { return id; }

    public CourseOffering getOffering() { return offering; }
    public void setOffering(CourseOffering offering) { this.offering = offering; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
