package net.wickedshell.ds.tx.sample.domain;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public final class Greeting {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Basic
	private String greeting;

	@Basic
	private Date creationDate = new Date();

	public String getGreeting() {
		return greeting;
	}

	public void setGreeting(String greeting) {
		this.greeting = greeting;
	}

	public Long getId() {
		return id;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof Greeting)) {
			return false;
		}
		Greeting otherGreeting = (Greeting) object;
		Long otherId = otherGreeting.getId();
		return id == null ? otherId == null : id.equals(otherGreeting.getId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
}
