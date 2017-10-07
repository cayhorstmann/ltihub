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
    
    @Column
    public Double weight;
    
    @Column
    public Integer duration;

    @OneToMany(mappedBy="problem")
    public List<Submission> submissions = new ArrayList<Submission>();

	public Problem() {
	}
		
	public Problem(Assignment assignment, String url, int problemGroup, Double weight, Integer duration) {
		this.assignment = assignment;
		this.url = url;
		this.problemGroup = problemGroup;
		this.weight = weight;
		this.duration = duration;
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
	
	public Double getWeight() {
		return weight;
	}
	
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	
	public int getDuration() {
		return duration == null ? 0 : duration;
	}
	
	public void setDuration(Integer duration) {
		this.duration = duration;
	}		
	
	public static double[] getWeights(List<Problem> problems) {
		int unweighted = 0;
        double weightSum = 0;
        double[] weights = new double[problems.size()];
        int i = 0;
        for (Problem problem : problems) {
        	if (problem.getWeight() ==  null) {
        		unweighted++;
        		weights[i] = -1;
        	}
        	else {
        		weights[i] = problem.getWeight();
        		weightSum += weights[i];
        	}
        	i++;
        }
        if (unweighted > 0) {
        	double defaultWeight = (1 - weightSum) / unweighted; 
        	for (i = 0; i < weights.length; i++)
        		if (weights[i] == -1) weights[i] = defaultWeight;
        }
        return weights;
	}	
}
