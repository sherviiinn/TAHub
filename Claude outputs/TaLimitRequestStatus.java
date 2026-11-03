package ir.TAHub.TAHub.model;

/** What happened to a head TA's request to raise the team limit. */
public enum TaLimitRequestStatus {
    /** Waiting for the professor. Only one per offering at a time. */
    PENDING,
    /** The professor said yes (or raised the limit far enough by hand). */
    APPROVED,
    /** The professor said no. */
    REJECTED,
    /** Nobody answered within 7 days. */
    EXPIRED,
    /** Closed because the semester ended. */
    CANCELLED
}
