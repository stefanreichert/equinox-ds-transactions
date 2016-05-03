package net.wickedshell.ds.tx.sample.service.internal;

import java.util.Collection;

import net.wickedshell.ds.tx.Transactional;
import net.wickedshell.ds.tx.sample.domain.Greeting;
import net.wickedshell.ds.tx.sample.repository.GreetingRepository;
import net.wickedshell.ds.tx.sample.service.GreetingService;

public class GreetingServiceImpl implements GreetingService {

	private GreetingRepository repository;

	public void setGreetingRepository(GreetingRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public Greeting persistNewGreeting(String greetingString) {
		Greeting greeting = new Greeting();
		greeting.setGreeting(greetingString);
		repository.persist(greeting);
		return greeting;
	}

	@Override
	@Transactional
	public Collection<Greeting> findAll() {
		return repository.findAll();
	}

}
