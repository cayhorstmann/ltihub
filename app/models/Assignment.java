package models;

import java.util.*;
import siena.*;

public class Assignment extends Model {

    @Id(Generator.AUTO_INCREMENT)
    public Long id;

    public String title;

    @Filter("assignment")
    public Query<AssignmentProblem> assignmentProblems;


    @Index("user_index")
    public User user;

    public Assignment(User user, String title) {
    	super();
    	this.title = title;
        this.user = user;
    }
	public Assignment() {
		super();
	}
    static Query<Assignment> all() {
        return Model.all(Assignment.class);
    }

    public static Assignment findById(Long id) {
        return all().filter("id", id).get();
    }

    public static List<Assignment> findByUser(User user) {

        //return all().filter("user", user).order("-created").fetch();
		return all().filter("user", user).fetch();
    }


	public static void addProblem(Assignment assignment, Problem problem) {
			AssignmentProblem assignmentProblem = new AssignmentProblem(assignment, problem);
			assignmentProblem.insert();
	}

    public String toString() {
        return title;
    }

	public List<Problem> findProblemsByAssignment() {

		List<Problem> problems = AssignmentProblem.findByAssignment(this);

		return problems;

	}

	/*public static void addTagsFromCSV(Link link, String tagcsv, User user) {
		Collection<Tag> tags = null;
		if(null != tagcsv || !tagcsv.equalsIgnoreCase("")) {
			String [] tagArr = tagcsv.split(",");
			tags = new ArrayList<Tag>();
			for(String tagstr : tagArr) {
				tagstr = play.templates.JavaExtensions.slugify(tagstr.trim()).trim();
				if(null != tagstr && !tagstr.equals("")) {
					Link.addTag(link, Tag.findOrCreateByName(tagstr,user));
				}

			}
		}
	}*/
}

