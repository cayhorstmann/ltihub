package models;

        import java.util.*;
        import javax.persistence.*;
        import play.db.ebean.*;
        import play.data.format.*;
        import play.data.validation.*;
	import com.avaje.ebean.Model;
		
        @Entity
        public class Assignment extends Model {
          
        @Id
        public Long assignmentId;
      
        @OneToMany(mappedBy="assignment")
	public List<Problem> problems = new ArrayList<Problem>();	

        public static Finder<Long, Assignment> find = new Finder<Long, Assignment>(
          Long.class, Assignment.class
        );
	
        public Assignment() {
		}

	public Assignment(List<Problem> problems) {
		
		this.problems = problems;
	}

	public Long getAssignmentId() {
		return this.assignmentId;
	}

	public void setAssignmentId(Long assignmentId) {
		this.assignmentId = assignmentId;
	}  
	
	public List<Problem> getProblems(){
			return this.problems;
	}
	
	public void setProblems(List<Problem> problems) {
		this.problems = problems;
	}
		
   }


