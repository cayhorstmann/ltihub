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

	public Assignment() {
	}

	public Assignment(List<Problem> problems) {

		this.problems = problems;
	}

	public long getId() {
		return this.id;
	}

	public List<Problem> getProblems() {
		return this.problems;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getDuration() {
		return this.duration;
	}

	public void setProblems(List<Problem> problems) {
		this.problems = problems;
	}

	public String getResourceLinkId() {
		return this.resourceLinkId;
	}

	public void setResourceLinkId(String resourceLinkId) {
		this.resourceLinkId = resourceLinkId;
	}

	public String getContextId() {
		return this.contextId;
	}

	public void setContextId(String contextId) {
		this.contextId = contextId;
	}

	public String getToolConsumerId() {
		return this.toolConsumerId;
	}

	public void setToolConsumerId(String toolConsumerId) {
		this.toolConsumerId = toolConsumerId;
	}
}
