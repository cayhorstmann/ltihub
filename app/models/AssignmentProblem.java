package models;

import java.util.*;
import siena.*;

public class AssignmentProblem extends Model {

    @Id
    public Long id;

    @NotNull
    @Column("assignment_id")
    @Index("assignment_index")
    public Assignment assignment;

    @NotNull
    @Column("problem_id")
    @Index("problem_index")
    public Problem problem;

	public AssignmentProblem(Assignment assignment, Problem problem) {
		super();
		this.assignment = assignment;
		this.problem = problem;
	}

	public static Query<AssignmentProblem> all() {
		return Model.all(AssignmentProblem.class);
	}

	public static List<Problem> findByAssignment(Assignment assignment) {
		List<AssignmentProblem> assignmentProblems =  all().filter("assignment", assignment).fetch();
		List<Problem> problems = new ArrayList<Problem>();
		for(AssignmentProblem assignmentProblem : assignmentProblems) {
			problems.add(Problem.findById(assignmentProblem.problem.id));
		}

		return problems;
	}
	
	public String toString() {
		return assignment.toString() + " : " + problem.toString();
	}

}

