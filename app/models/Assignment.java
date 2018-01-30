package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import io.ebean.Model;

@Entity
public class Assignment extends Model {
	@Id	public long id;

	/*
	 * The context ID and resource link ID are ONLY for finding the assignment
	 * in Moodle. The toolConsumerId is required because those other IDs 
	 * might not be unique.
	 * 
	 * The assignment stays valid outside the context. (We want to
	 * be able to copy courses from one semester to the next.) 
	 */
	
	@Column	public String contextId;
	@Column	public String resourceLinkId;
	@Column	public String toolConsumerId;

	@Column	public int duration;

	@OneToMany(mappedBy = "assignment")
	public List<Problem> problems = new ArrayList<Problem>();

	public List<Problem> getProblems() {
		return this.problems;
	}
	
	public String toString() { return "Assignment[id=" + id + ",contextId=" + contextId +
			",resourceLinkId=" + resourceLinkId + ",toolConsumerId=" + toolConsumerId +
			",duration=" + duration; 
	}
}
