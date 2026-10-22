package ir.TAHub.TAHub.model;

/** How a student got into an offering. */
public enum EnrollmentSource {
    /** The student used the join code and is not on the professor's class list. */
    JOIN_CODE,
    /** The student is on the class list the professor provided. */
    ROSTER
}