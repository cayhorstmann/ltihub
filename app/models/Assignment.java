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
     
        @Column
        public String contextId;

        @Column
        public String resourceLinkId;
    
        @Column String toolConsumerInstanceGuId;
 
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

	public String getResourceLinkId(){
		return this.resourceLinkId;
	}

	public void setResourceLinkId(String resourceLinkId){
		this.resourceLinkId = resourceLinkId;
	}

	public String getContextId() {
		return this.contextId;
	}

	public void setContextId(String contextId) {
		this.contextId = contextId;
	}
		
	public String getToolConsumerInstanceGuId() {
		return this.toolConsumerInstanceGuId;
	}

	public void setToolConsumerInstanceGuId(String toolConsumerInstanceGuId) {
		this.toolConsumerInstanceGuId = toolConsumerInstanceGuId;
	}
   }


