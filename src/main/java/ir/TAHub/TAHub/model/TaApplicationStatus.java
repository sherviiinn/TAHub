package ir.TAHub.TAHub.model;

/** The state of a student's request to become a TA. Requests are never deleted, only their state changes. */
public enum TaApplicationStatus {
    /** Waiting for the professor. */
    PENDING,
    /** The professor accepted it; the student became a TA. */
    ACCEPTED,
    /** The professor rejected it. The professor can reconsider it later. */
    REJECTED,
    /** The student took the request back. The student may apply again. */
    WITHDRAWN
}