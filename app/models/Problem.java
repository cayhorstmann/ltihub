package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import io.ebean.Model;
import play.data.validation.Constraints;

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
    public double weight;
    
	public Problem() {
	}
		
	public Problem(Assignment assignment, String url, int problemGroup, Double weight) {
		this.assignment = assignment;
		this.url = url;
		this.problemGroup = problemGroup;
		this.weight = weight;
	}
	
	public Long getId() {
		return id;
	}
		
	public int getProblemGroup() {
		return problemGroup;
	}
}
