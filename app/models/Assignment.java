package models;

        import java.util.*;
        import javax.persistence.*;
        import play.db.ebean.*;
        import play.data.format.*;
        import play.data.validation.*;
		import com.avaje.ebean.Model;
		
        @Entity
        @Table(name = "Assignment")
        public class Assignment extends Model {
          
        @Id
	@Column(name = "assignment_id")
        public Long id;
      
        @Column
        @OneToMany(cascade = CascadeType.ALL)
	public List<Problem> problems = new ArrayList<Problem>();	

        public static Finder<Long, Assignment> find = new Finder<Long, Assignment>(
          Long.class, Assignment.class
        );
	
        public List<Problem> getProblems(){
		return problems;
	}
		
   }


