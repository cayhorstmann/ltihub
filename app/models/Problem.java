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

        public static Finder<Long, Problem> find = new Finder<Long, Problem>(
          Long.class, Problem.class
        );

        public Long getId() {
          return id;
        }
        public void setId(Long id) {
          this.id = id;
        }
        public String getUrl() {
          return url;
        }
        public void setUrl(String url) {
          this.url = url;
        }
		
		public static List<Problem> getProblems() {
			return find.all();
		}
   }


