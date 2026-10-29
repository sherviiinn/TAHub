package ir.TAHub.TAHub.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a TA may do in one offering. The professor (and admins) can always do everything;
 * a TA can only do what is in their own set. More permissions are added with later phases
 * (assignments, grading, appeals). The page shows the text of "ta.permission.<NAME>".
 */
public enum TaPermission {
    /** Post announcements, and edit or delete their own. */
    POST_ANNOUNCEMENTS,
    /** Edit or delete everybody's announcements, the professor's too. */
    MANAGE_ANNOUNCEMENTS,
    /** Set, change or remove the Telegram group link. */
    EDIT_TELEGRAM,
    /** See the list of students with their student numbers. */
    VIEW_STUDENTS,
    /** Paste the class list, remove and restore students, and manage the join code. */
    MANAGE_STUDENTS,
    /** Add and remove TAs (the head TA has this). */
    MANAGE_TEAM,
    /** Give other TAs the permissions they have themselves (the head TA has this). */
    GRANT_PERMISSIONS;

    /** What a TA gets when the professor makes them a TA (the professor can change it right after). */
    public static Set<TaPermission> defaults() {
        return EnumSet.of(POST_ANNOUNCEMENTS, EDIT_TELEGRAM);
    }
}
