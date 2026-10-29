package ir.TAHub.TAHub.model;

/** What happened to an invitation to be a TA. */
public enum TaInvitationStatus {
    /** Waiting for the invited student to answer. */
    PENDING,
    /** The student said yes and became a TA. */
    ACCEPTED,
    /** The student said no. They can be invited again later. */
    DECLINED,
    /** The person who invited (or the professor) took the invitation back. */
    CANCELLED
}
