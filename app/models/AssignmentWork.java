package models;

import java.util.Date;

import io.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * The work of a student on an assignment.
 */
@Entity
public class AssignmentWork extends Model {
	@Id public long id;
	@Column public String studentId;
	@Column	public String toolConsumerId;
	@Column	public String contextId;
	@Column public Assignment assignment;
	@Column public Date startTime = new Date();
	@Column public int problemGroup;
}