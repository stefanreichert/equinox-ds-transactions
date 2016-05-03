package net.wickedshell.ds.tx.sample.service;

import java.util.Collection;

import net.wickedshell.ds.tx.sample.domain.Greeting;

public interface GreetingService {

	Greeting persistNewGreeting(String greeting);
	
	Collection<Greeting> findAll();

}