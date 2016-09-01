package models;

import java.util.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;
import com.avaje.ebean.Model;
import javax.persistence.*;

@Entity
public class Problem extends Model {

    @Id
    public Long id;

    @Constraints.Required
    public String url;

    public static Finder<Long,Problem> find = new Finder<>(Problem.class);

   public static List<Problem> all() {
  	  return find.all();
    }

    public static Problem createProblem(String url) {
                Problem problem = new Problem();
		problem.url = url;
		problem.save();
		return problem;
	}

    public static Problem findById(Long id) {
	return find.ref(id);
	}

    public String toString() {
        return url;
    }
}

