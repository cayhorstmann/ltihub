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
	public static List<Assignment> findByProblem(Problem problem) {
		List<AssignmentProblem> assignmentProblems =  all().filter("problem", problem).fetch();
		List<Assignment> assignments = new ArrayList<Assignment>();
		for(AssignmentProblem assignmentProblem : assignmentProblems) {
			assignments.add(Assignment.findById(assignmentProblem.assignment.id));
		}

		return assignments;
	}

	public String toString() {
		return assignment.toString() + " : " + problem.toString();
	}

	public static void deleteByAssignment(Assignment assignment) {

		List<AssignmentProblem> assignmentProblems = all().filter("assignment", assignment).fetch();
		if(null != assignmentProblems) {
			for(AssignmentProblem assignmentProblem : assignmentProblems) {
					assignmentProblem.delete();
			}
		}
	}
}

