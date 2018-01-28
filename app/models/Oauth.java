package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Oauth {
	@Id @Column public String oauthConsumerKey;
	@Column public String sharedSecret;
}