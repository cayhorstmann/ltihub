package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.data.validation.Constraints;

import io.ebean.Model;

@Entity
public class Problem extends Model {
          
    @Id
    public Long id;
      
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
    
	public Problem() {
	}
		
	public Problem(Assignment assignment, String url, int problemGroup, Double weight) {
		this.assignment = assignment;
		this.url = url;
		this.problemGroup = problemGroup;
		this.weight = weight;
	}
		
	public Long getId() {
		return this.id;
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
}
