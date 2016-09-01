package models;

import java.util.*;

import play.db.ebean.*;
import play.data.validation.Constraints.*;

import javax.persistence.*;

@Entity
public class Assignment extends Model {

    @Id
    public Long id;

    public String title;

    public Assignment( String title) {
    	super();
    	this.title = title;
    }
    
    public Assignment() {
		super();
	}

  public static Finder<Long,Assignment> find = new Finder<>(Assignment.class);
    
    public static List<Assignment> all() {
        return find.all();
    }

    public static Assignment findById(Long id) {
        return find.ref(id);
    }

    public static void addProblem(Assignment assignment, Problem problem) {
	assignment.insert(problem.url);
	}

    public String toString() {
        return title;
    }

    public static void addProblems(Assignment assignment, String problemlist) {
		Collection<Problem> problems = null;
		if(null != problemlist || !problemlist.equals("")) {
			String [] problemArr = problemlist.split(",");
			problems = new ArrayList<Problem>();
			for(String problemstr : problemArr) {
				if(null != problemstr && !problemstr.equals("")) {
					Assignment.addProblem(assignment, Problem.createProblem(problemstr));
				}

			}
		}
	}
}

