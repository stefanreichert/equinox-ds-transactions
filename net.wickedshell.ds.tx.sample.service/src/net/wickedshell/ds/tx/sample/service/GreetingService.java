package net.wickedshell.ds.tx.sample.service;

import java.util.Collection;

import net.wickedshell.ds.tx.sample.domain.Greeting;

public interface GreetingService {

	Greeting createAndPersistNewGreeting();
	
	Collection<Greeting> findAll();

}