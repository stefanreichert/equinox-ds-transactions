package net.wickedshell.ds.tx.sample.service.internal;

import java.text.DateFormat;
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
	public Greeting createAndPersistNewGreeting() {
		Greeting greeting = new Greeting();
		greeting.setGreeting("Hello DemoCamp crowd, nice to meet you!");
		repository.persist(greeting);
		return greeting;
	}

	@Override
	@Transactional
	public Collection<Greeting> findAll() {
		return repository.findAll();
	}

	public void activate() {
		Greeting greeting = createAndPersistNewGreeting();
		System.out.printf("greeting created: %s%n", greeting.getGreeting());
	}

	public void deactivate() {
		for (Greeting persistedGreeting : findAll()) {
			System.out.printf("[%d] greeting [%s]: %s%n", persistedGreeting.getId(),
					DateFormat.getDateTimeInstance().format(persistedGreeting.getCreationDate()),
					persistedGreeting.getGreeting());
		}
	}

}
