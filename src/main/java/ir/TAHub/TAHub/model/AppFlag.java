package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Remembers that a one-time job already ran (for example "give every TA the new permission once"),
 * so it does not run again at the next start and undo what people changed later.
 */
@Entity
@Table(name = "app_flags")
public class AppFlag {

    @Id
    @Column(length = 100)
    private String name;

    @Column(nullable = false)
    private Instant doneAt;

    public AppFlag() {
    }

    public AppFlag(String name, Instant doneAt) {
        this.name = name;
        this.doneAt = doneAt;
    }

    public String getName() { return name; }

    public Instant getDoneAt() { return doneAt; }
}
