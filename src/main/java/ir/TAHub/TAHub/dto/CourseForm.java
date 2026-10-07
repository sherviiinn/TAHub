package ir.TAHub.TAHub.dto;

/**
 * Data submitted by the "new course" form.
 * professorId is only used when an admin creates the course.
 */
public class CourseForm {

    private String code;
    private String name;
    private String semester;
    private Long professorId;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }
}