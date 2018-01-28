package models;

import java.util.*;
import javax.persistence.*;
import play.data.validation.*;
import io.ebean.Model;
		
@Entity
public class Submission extends Model {
	public Submission(){
		this.submittedAt = new Date();
	}

    @Id public Long id;	
    @Column @Constraints.Required public String studentId; 
	@Column	@Constraints.Required public String toolConsumerId;
	@Column	@Constraints.Required public String contextId;
    @ManyToOne public Problem problem;
    @Column public double score;    
    @Column public Date submittedAt;        	
    @Column(columnDefinition = "TEXT") public String script;    
    @OneToOne public Submission previous;     

	public String toString() {
		return getClass().getName() + "[submissionId=" + id 
				+ ",studentId=" + studentId + ",toolConsumerId=" + toolConsumerId
				+ ",contextId=" + contextId + ",score=" + score   
				+ ",submittedAt=" + submittedAt + ",script=" + script + ",previous=" + previous.id 
				+ ",problemId=" + problem.id + "]";
	}
}


