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

package net.wickedshell.ds.tx;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import net.wickedshell.ds.tx.internal.TransactionManager;

/**
 * Aspect that provides a {@link TransactionContext} for methods annotated with
 * {@link Transactional}. The aspect uses the {@link TransactionManager}
 * instance from the bundle context
 * 
 * @author Stefan Reichert
 *
 */
public aspect TransactionalAspect {

	pointcut transactionalMethod() : execution(@Transactional * *(..));

	Object around() : transactionalMethod() {
		BundleContext bundleContext = FrameworkUtil.getBundle(Transactional.class).getBundleContext();
		ServiceReference<TransactionManager> serviceReference = bundleContext
				.getServiceReference(TransactionManager.class);
		if (serviceReference == null) {
			throw new IllegalStateException("failed to handle transaction; no transaction manager available");
		}
		TransactionManager transactionManager = bundleContext.getService(serviceReference);
		Object returnValue;
		transactionManager.begin();
		try {
			returnValue = proceed();
			transactionManager.commit();
			return returnValue;
		} catch (RuntimeException exception) {
			transactionManager.rollback(exception);
			throw exception;
		} finally {
			transactionManager.close();
			if (serviceReference != null) {
				bundleContext.ungetService(serviceReference);
			}
		}
	}

}
