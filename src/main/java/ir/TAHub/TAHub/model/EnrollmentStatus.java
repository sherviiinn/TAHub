package ir.TAHub.TAHub.model;

/** The state of one student's enrollment in one course offering. */
public enum EnrollmentStatus {
    /** The student is taking the course. */
    ACTIVE,
    /** The student left by themselves. They may enroll again. */
    DROPPED,
    /** The professor removed the student. Only the professor can restore them. */
    REMOVED
}