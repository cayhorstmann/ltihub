package models;

import java.util.*;
import javax.persistence.*;
import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;
import com.avaje.ebean.Model;
		
    @Entity
    public class Submission extends Model {
          
    @Id 
    public Long submissionId;
	
    @Column
    @Constraints.Required
    public String studentId;

    @Column
    @Constraints.Required
    public Long assignmentId;

    @Column
    @Constraints.Required
    public Long correct;
    
    @Column
    public Long maxscore;	
    
    @Column
    public String activity;

    @Column
    public Date submittedAt;        
	
    @Column
    public String content;
    
    @ManyToOne
    public Problem problem;

    public static Finder<Long, Submission> find = new Finder<Long, Submission>(
        Long.class, Submission.class);
        
	public Submission(){
	}
		
	public Long getSubmissionId() {
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
	
	public String getStudentId(){
		return this.studentId;
	}

	public void setStudentId(String studentId){
		this.studentId = studentId;
	}

	public Long getAssignmentId(){
		return this.assignmentId;
	}

	public void setAssignmentId(Long assignmentId){
		this.assignmentId = assignmentId;
	}

	public Long getCorrect(){
		return this.correct;
	}

	public void setCorrect(Long correct){
		this.correct = correct;
	}
	
        public Long getMaxScore(){
		return this.maxscore;
	}
	
	public void setMaxScore(Long maxscore){
		this.maxscore = maxscore;
	}
	public String getActivity(){
		return this.activity;
	}

	public void setActivity(String activity){
		this.activity = activity;
	}

	@Override
	public void save(){
		submittedAt();
		super.save();
	}

	@PrePersist
	void submittedAt(){
		this.submittedAt = new Date();
	}
}


