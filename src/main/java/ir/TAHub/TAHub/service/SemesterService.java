package ir.TAHub.TAHub.service;

import ir.TAHub.TAHub.model.Semester;
import ir.TAHub.TAHub.repository.SemesterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SemesterService {

    private final SemesterRepository semesterRepository;

    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    /**
     * Archives the current semester and activates a new one.
     * Runs in one transaction: either both changes happen or none.
     */
    @Transactional
    public Semester startNewSemester(String code) {
        // The loaded entity is tracked by Hibernate, so this change is saved automatically at commit.
        semesterRepository.findByActiveTrue().ifPresent(current -> current.setActive(false));

        Semester next = new Semester();
        next.setCode(code);
        next.setActive(true);
        return semesterRepository.save(next);
    }
}