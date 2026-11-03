package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.repository.SemesterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final TaHousekeeping taHousekeeping;

    public SemesterService(SemesterRepository semesterRepository, TaHousekeeping taHousekeeping) {
        this.semesterRepository = semesterRepository;
        this.taHousekeeping = taHousekeeping;
    }

    /**
     * Archives the current semester and activates a new one.
     * Runs in one transaction: either both changes happen or none.
     * The old semester's waiting TA requests, invitations and team-limit requests are closed too.
     */
    @Transactional
    public Semester startNewSemester(String code) {
        // The loaded entity is tracked by Hibernate, so this change is saved automatically at commit.
        semesterRepository.findByActiveTrue().ifPresent(current -> {
            current.setActive(false);
            taHousekeeping.closeSemester(current);
        });

        Semester next = new Semester();
        next.setCode(code);
        next.setActive(true);
        return semesterRepository.save(next);
    }
}
