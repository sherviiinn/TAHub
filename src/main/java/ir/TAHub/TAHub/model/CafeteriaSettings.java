package ir.TAHub.TAHub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;

/**
 * When reservation for one day opens and closes at the university cafeteria, for example
 * "opens 6 days before at 00:00, closes 2 days before at 23:59". Times are Tehran time.
 * There is only one row (id 1). Until an admin saves it, the defaults below are used.
 */
@Entity
@Table(name = "cafeteria_settings")
public class CafeteriaSettings {

    public static final long SINGLE_ID = 1L;

    @Id
    private Long id = SINGLE_ID;

    @Column(nullable = false)
    private int openDaysBefore = 6;

    @Column(nullable = false)
    private LocalTime openTime = LocalTime.of(0, 0);

    @Column(nullable = false)
    private int closeDaysBefore = 2;

    @Column(nullable = false)
    private LocalTime closeTime = LocalTime.of(23, 59);

    private Instant updatedAt;

    public CafeteriaSettings() {
    }

    public Long getId() { return id; }

    public int getOpenDaysBefore() { return openDaysBefore; }
    public void setOpenDaysBefore(int openDaysBefore) { this.openDaysBefore = openDaysBefore; }

    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }

    public int getCloseDaysBefore() { return closeDaysBefore; }
    public void setCloseDaysBefore(int closeDaysBefore) { this.closeDaysBefore = closeDaysBefore; }

    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
