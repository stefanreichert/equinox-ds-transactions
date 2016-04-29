package net.wickedshell.ds.tx.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.spi.PersistenceProvider;

import org.osgi.service.component.ComponentContext;

import net.wickedshell.ds.tx.TransactionContext;
import net.wickedshell.ds.tx.TransactionManager;

public class TransactionManagerImpl implements TransactionManager, TransactionContext {

	private EntityManagerFactory entityManagerFactory;

	private PersistenceProvider persistenceProvider;

	private ThreadLocal<EntityManager> localEntityManager = new ThreadLocal<>();

	public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

	public void activate(ComponentContext context) throws IOException {
		try (InputStream propertyStream = context.getBundleContext().getBundle()
				.getResource("transactionmanager.properties").openStream()) {
			Properties properties = new Properties();
			properties.load(propertyStream);
			String persistenceUnitName = properties.getProperty("persistenceUnitName");
			entityManagerFactory = persistenceProvider.createEntityManagerFactory(persistenceUnitName, new HashMap<>());
			if (entityManagerFactory == null) {
				throw new RuntimeException(
						"failed to create entity manager factory for persistence unit " + persistenceUnitName);
			}
		}
	}

	@Override
	public EntityManager getEntityManager() {
		EntityManager entityManager = localEntityManager.get();
		if (entityManager == null) {
			throw new RuntimeException("no entity manager available; did you forget to begin a transaction?");
		}
		return entityManager;
	}

	@Override
	public void begin() {
		System.out.println("begin transaction");
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.setFlushMode(FlushModeType.AUTO);
		entityManager.getTransaction().begin();
		localEntityManager.set(entityManager);
	}

	@Override
	public void commit() {
		System.out.println("commit transaction");
		EntityManager entityManager = getEntityManager();
		if (entityManager.isOpen() && entityManager.getTransaction().isActive()) {
			getEntityManager().getTransaction().commit();
		}
	}

	@Override
	public void rollback(RuntimeException exception) {
		System.err.println("rollback transaction: " + exception.getMessage());
		EntityManager entityManager = getEntityManager();
		if (entityManager.isOpen() && entityManager.getTransaction().isActive()) {
			entityManager.getTransaction().rollback();
		}
	}

	@Override
	public void close() {
		System.out.println("close transaction");
		getEntityManager().close();
		localEntityManager.set(null);
	}

}
