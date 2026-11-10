package ir.TAHub.TAHub.config;

import ir.TAHub.TAHub.model.AppFlag;
import ir.TAHub.TAHub.model.TaAssignment;
import ir.TAHub.TAHub.model.TaPermission;
import ir.TAHub.TAHub.repository.AppFlagRepository;
import ir.TAHub.TAHub.repository.TaAssignmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * "Manage assignments" is a default permission of every TA (decided in phase 4). TAs whose permissions
 * were never chosen get it automatically (they use the defaults). TAs whose permissions were already
 * saved get it here, ONCE: the flag in app_flags stops it from running again, so if the professor takes
 * the permission away later, the next start does not give it back.
 */
@Component
@Order(3) // After DemoDataSeeder (1) and DataInitializer (2).
public class AssignmentPermissionMigration implements CommandLineRunner {

    static final String FLAG = "grant-manage-assignments";

    private final AppFlagRepository flagRepository;
    private final TaAssignmentRepository taRepository;

    public AssignmentPermissionMigration(AppFlagRepository flagRepository, TaAssignmentRepository taRepository) {
        this.flagRepository = flagRepository;
        this.taRepository = taRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (flagRepository.existsById(FLAG)) {
            return;
        }
        for (TaAssignment ta : taRepository.findAll()) {
            Set<TaPermission> permissions = EnumSet.noneOf(TaPermission.class);
            permissions.addAll(ta.getEffectivePermissions());
            // For a TA on the defaults this changes nothing (the defaults already have it).
            if (permissions.add(TaPermission.MANAGE_ASSIGNMENTS)) {
                ta.replacePermissions(permissions);
                taRepository.save(ta);
            }
        }
        flagRepository.save(new AppFlag(FLAG, Instant.now()));
    }
}
