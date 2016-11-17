package models;

import java.util.*;
import javax.persistence.*;
        import play.db.ebean.*;
        import play.data.format.*;
        import play.data.validation.*;
	import com.avaje.ebean.Model;
		
        @Entity
        public class Problem extends Model {
          
        @Id
        public Long problemId;
      
        @Column
        @Constraints.Required
        public String url;

        @ManyToOne
        Assignment assignment;

        public static Finder<Long, Problem> find = new Finder<Long, Problem>(
          Long.class, Problem.class
        );
		
	public Problem(){
		}
		
	public Problem(Assignment assignment, String url){
		this.assignment = assignment;
		this.url = url;
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
       // public static List<Problem> getProblems() {
	//	return find.all();
	//}
   }
