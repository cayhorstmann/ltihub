package models;

        import java.util.*;
        import javax.persistence.*;
        import play.db.ebean.*;
        import play.data.format.*;
        import play.data.validation.*;
	import com.avaje.ebean.Model;
		
        @Entity
        @Table(name = "Problems")
        public class Problem extends Model {
          
        @Id
        public Long id;
      
        @Column
        @Constraints.Required
        public String url;

        @ManyToOne(fetch=FetchType.LAZY)
        @JoinColumn(name = "assignment_id")
        Assignment assignment;

        public static Finder<Long, Problem> find = new Finder<Long, Problem>(
          Long.class, Problem.class
        );
		
	public static List<Problem> getProblems() {
		return find.all();
	}
   }


