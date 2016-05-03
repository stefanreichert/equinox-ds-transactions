package net.wickedshell.ds.tx.sample.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import net.wickedshell.ds.tx.sample.domain.Greeting;

public class GreetingServiceTest {

	private ServiceReference<GreetingService> reference;
	private GreetingService service;

	@Before
	public void setup() {
		BundleContext bundleContext = FrameworkUtil.getBundle(GreetingServiceTest.class).getBundleContext();
		reference = bundleContext.getServiceReference(GreetingService.class);
		service = bundleContext.getService(reference);
	}

	public void tearDown() {
		BundleContext bundleContext = FrameworkUtil.getBundle(GreetingServiceTest.class).getBundleContext();
		bundleContext.ungetService(reference);
		service = null;
	}

	@Test
	public void shouldFindTwoGreetings() {
		// given
		service.persistNewGreeting("Hello DemoCamp crowd!");
		service.persistNewGreeting("This is Equinox DS together with Equinox Aspects!");

		// when
		Collection<Greeting> greetings = service.findAll();

		// then
		assertThat(greetings.size(), is(equalTo(2)));
		
		for (Greeting greeting : greetings) {
			System.out.println(greeting.getGreeting());
		}
	}

}
