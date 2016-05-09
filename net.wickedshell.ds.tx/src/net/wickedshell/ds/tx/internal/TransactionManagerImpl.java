/*******************************************************************************
* Copyright (c) 2016 Stefan Reichert
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Stefan Reichert - initial API and implementation
*******************************************************************************/

package net.wickedshell.ds.tx.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.spi.PersistenceProvider;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.wickedshell.ds.tx.TransactionContext;

/**
 * {@link TransactionManager} and {@link TransactionContext} implementation that
 * manages an {@link EntityManagerFactory}. {@link #begin()} will create a new
 * {@link EntityManager} that is attached to the current thread.<br>
 * This class expects a <code>transactionmanager.properties</code> file on the
 * classpath. The file needs to define the <i>persistenceUnitName</i> property.
 * The value is used for the creation of the factory.
 * 
 * @author Stefan Reichert
 *
 */
public class TransactionManagerImpl implements TransactionManager, TransactionContext {

	/** the logger instance to use. **/
	private static final Logger LOG = LoggerFactory.getLogger(TransactionManager.class);

	/** the factory that is created on activation of the manager. */
	private EntityManagerFactory entityManagerFactory;

	/**
	 * the JPA persistence provider reference to create the factory instance.
	 */
	private PersistenceProvider persistenceProvider;

	/** the container for the context data */
	private ThreadLocal<TransactionContextData> localContextData = new ThreadLocal<>();

	/**
	 * Sets the {@link PersistenceProvider} reference.<br>
	 * <i>Intended to be used by the DI framework only.</i>
	 * 
	 * @param persistenceProvider
	 *            The {@link PersistenceProvider} instance from the DI container
	 */
	public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

	/**
	 * Initializes the {@link #entityManagerFactory} field using the injected
	 * {@link #persistenceProvider}. <i>Intended to be used by the DI framework
	 * on activation only.</i>
	 * 
	 * @param context
	 *            The component context required for evaluating the configured
	 *            persistence unit
	 * @throws IllegalStateException
	 *             in case the properties file is not available or property is
	 *             missing
	 */
	public void initializeEntityManagerFactory(ComponentContext context) {
		String persistenceUnitName = evaluatePersistenceUnitName(context);
		entityManagerFactory = persistenceProvider.createEntityManagerFactory(persistenceUnitName, new HashMap<>());
		if (entityManagerFactory == null) {
			throw new RuntimeException(
					"failed to create entity manager factory for persistence unit " + persistenceUnitName);
		}
	}

	/**
	 * Closes the {@link #entityManagerFactory} field .<br>
	 * <i>Intended to be used by the DI framework on deactivation only.</i>
	 */
	public void closeEntityManagerFactory() {
		entityManagerFactory.close();
	}

	/**
	 * Evaluates the name of the persistence unit that is configured a
	 * transactional resource.<br>
	 * The method expects a <code>transactionmanager.properties</code> file on
	 * the classpath. The file needs to define the <i>persistenceUnitName</i>
	 * property. The value is used for the creation of the factory.
	 * 
	 * @param context
	 *            The component context in order to resolve the properties file
	 *            from the bundle's classpath
	 * @return the name of the configured persistence unit
	 * @throws IllegalStateException
	 *             in case the properties file is not available or property is
	 *             missing
	 */
	private String evaluatePersistenceUnitName(ComponentContext context) {
		URL propertiesURL = context.getBundleContext().getBundle().getResource("transactionmanager.properties");
		if (propertiesURL == null) {
			throw new IllegalStateException("missing file transactionmanager.properties on classpath");
		}
		try (InputStream propertyStream = propertiesURL.openStream()) {
			Properties properties = new Properties();
			properties.load(propertyStream);
			String persistenceUnitName = properties.getProperty("persistenceUnitName");
			if (persistenceUnitName == null) {
				throw new IllegalStateException("missing property persistenceUnitName in properties file");
			}
			return persistenceUnitName;
		} catch (IOException exception) {
			throw new IllegalStateException(exception.getMessage(), exception);
		}
	}

	private TransactionContextData getContextData() {
		TransactionContextData contextData = localContextData.get();
		if (contextData == null) {
			throw new IllegalStateException(
					"no entity manager available; did you forget to begin a transaction with @Transactional?");
		}
		return contextData;
	}

	@Override
	public EntityManager getEntityManager() {
		return getContextData().getEntityManager();
	}

	@Override
	public void begin() {
		if (localContextData.get() == null) {
			doBegin();
		} else {
			TransactionContextData contextData = getContextData();
			contextData.incrementNestingDepth();
			LOG.debug("%s # reuse transaction", contextData.getTransactionId());
		}
	}

	private void doBegin() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.setFlushMode(FlushModeType.AUTO);
			entityManager.getTransaction().begin();
			TransactionContextData contextData = new TransactionContextData(entityManager);
			localContextData.set(contextData);
			LOG.debug("%s # begin transaction", contextData.getTransactionId());
		} catch (RuntimeException exception) {
			entityManager.close();
			localContextData.remove();
			throw exception;
		}
	}

	@Override
	public void commit() {
		if (!getContextData().isInNestedState()) {
			doCommit(getContextData());
		} else {
			LOG.trace("%s # skipped committing transaction - nested state", getContextData().getTransactionId());
		}
	}

	private void doCommit(TransactionContextData contextData) {
		EntityManager entityManager = contextData.getEntityManager();
		if (entityManager.isOpen() && entityManager.getTransaction().isActive()) {
			getEntityManager().getTransaction().commit();
			LOG.debug("%s # committed transaction", contextData.getTransactionId());
		} else {
			LOG.trace("%s # skipped committing transaction - not active anymore", contextData.getTransactionId());
		}
	}

	@Override
	public void rollback(RuntimeException exception) {
		if (!getContextData().isInNestedState()) {
			doRollback(getContextData(), exception);
		} else {
			LOG.trace("%s # skipped rolling back transaction - nested state", getContextData().getTransactionId());
		}
	}

	private void doRollback(TransactionContextData contextData, RuntimeException exception) {
		EntityManager entityManager = contextData.getEntityManager();
		if (entityManager.isOpen() && entityManager.getTransaction().isActive()) {
			entityManager.getTransaction().rollback();
			LOG.debug("%s # rolled back transaction : ", contextData.getTransactionId(), exception.getMessage());
		} else {
			LOG.trace("%s # skipped rolling back transaction - not active anymore", contextData.getTransactionId());
		}
	}

	@Override
	public void close() {
		if (!getContextData().isInNestedState()) {
			doClose(getContextData());
		} else {
			getContextData().decrementNestingDepth();
			LOG.trace("%s # skipped closing transaction - nested state", getContextData().getTransactionId());
		}
	}

	private void doClose(TransactionContextData contextData) {
		contextData.getEntityManager().close();
		localContextData.set(null);
		LOG.debug("%s # closed transaction", contextData.getTransactionId());
	}

}
