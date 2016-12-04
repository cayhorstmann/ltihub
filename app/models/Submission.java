package models;

import java.util.*;
import javax.persistence.*;
import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;
import com.avaje.ebean.Model;
		
    @Entity
    //@Table(name = "Submissions")
    public class Submission extends Model {
          
    @Id 
    public Long submissionId;
		
	//@ManyToOne(fetch = FetchType.LAZY) Line commented since last successful run
	//@JoinColumn(name = "assignment_id") Line commented since last successful run
	@ManyToOne
	Problem problem;

    public static Finder<Long, Submission> find = new Finder<Long, Submission>(
        Long.class, Submission.class);
        
	public Submission(){
	}
		
	public Long SubmissionId() {
		return this.submissionId;
	}

	public void setSubmissionId(Long submissionId) {
		this.submissionId = submissionId;
	}

	public Problem getProblem() {
		return this.problem;
	}

	public void setProblem(Problem problem) {
		this.problem = problem;
	}

}


