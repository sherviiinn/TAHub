package ir.TAHub.TAHub.dto;

/**
 * Data submitted by the "new course" form.
 * The semester is not part of the form: offerings always go to the active semester.
 * professorId is only used when an admin creates the offering.
 */
public class CourseForm {

    private String code;
    private String name;
    private Long professorId;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }
}