package models;

import siena.*;
import java.util.*;

public class Problem extends Model {

    @Id(Generator.AUTO_INCREMENT)
    public Long id;

    public String url;

	@Index("user_index")
	public User user;


    public static Query<Problem> all() {
        return Model.all(Problem.class);
    }

	public static Problem findOrCreateByUrl(String url, User user) {
        Problem problem = all().filter("user", user).filter("url", url).get();
        if(null == problem) {
			problem = new Problem();
			problem.url = url;
			problem.user = user;
			problem.insert();
		}
		return problem;
	}

	public static Problem findById(Long id) {
		return  all().filter("id", id).get();
	}

    public static Problem findByUrl(String url, User user) {
        return all().filter("user", user).filter("url", url).get();
    }

    public static List<Problem> findByUser(User user) {
        return all().filter("user", user).fetch();
    }

    public String toString() {
        return url;
    }
}

