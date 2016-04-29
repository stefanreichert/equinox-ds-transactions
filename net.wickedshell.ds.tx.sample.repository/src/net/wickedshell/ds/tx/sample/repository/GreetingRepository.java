package net.wickedshell.ds.tx.sample.repository;

import java.util.Collection;

import net.wickedshell.ds.tx.sample.domain.Greeting;

public interface GreetingRepository {

	Collection<Greeting> findAll();
	
	void persist(Greeting grreting);
}
