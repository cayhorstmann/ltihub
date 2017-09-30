package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.data.validation.Constraints;

import com.avaje.ebean.Model;

// TODO: Add position for ordering
// TODO: Add weight

@Entity
public class Problem extends Model {
          
    @Id
    public Long problemId;
      
    @Column
    @Constraints.Required
    public String url;

    @ManyToOne
    public Assignment assignment;
    
    @Column
    @Constraints.Required
    public int problemGroup;

    @OneToMany(mappedBy="problem")
    public List<Submission> submissions = new ArrayList<Submission>();

	public Problem() {
	}
		
	public Problem(Assignment assignment, String url, int problemGroup) {
		this.assignment = assignment;
		this.url = url;
		this.problemGroup = problemGroup;
	}
		
	public Long getProblemId() {
		return this.problemId;
	}

	public void setProblemId(Long problemId) {
		this.problemId = problemId;
	}

	public String getProblemUrl() {
		return this.url;
	}

	public void setProblemUrl(String url) {
		this.url = url;
	}
       
	public Assignment getAssignment() {
		return this.assignment;
	}

	public void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}

	public List<Submission> getSubmissions(){
		return this.submissions;
	}

	public void setSubmission(List<Submission> submissions){
		this.submissions = submissions;
	}
	
	public int getProblemGroup() {
		return problemGroup;
	}
	
	public void setProblemGroup(int problemGroup) {
		this.problemGroup = problemGroup;
	}
}
