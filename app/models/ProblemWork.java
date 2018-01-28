package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import io.ebean.Model;
import play.data.validation.Constraints;

/**
 * The work of a student on a problem.
 */
@Entity
public class ProblemWork extends Model {
	@Id public long id;
	@Column @Constraints.Required public String studentId;
	@Column	@Constraints.Required public String toolConsumerId;
	@Column	@Constraints.Required public String contextId;
    @Column public double highestScore;
    @Column(columnDefinition = "TEXT") public String state;
    @Column public long clientStamp; // monotonically increasing
    @ManyToOne public Problem problem;
    @OneToOne public Submission lastDetail;
}