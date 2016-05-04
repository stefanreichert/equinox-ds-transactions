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

import net.wickedshell.ds.tx.TransactionContext;

/**
 * A interface for a transaction manager. The manager is <i>stateful</i>
 * regarding the chosen context (e.g. the executing thread). The appropriate
 * order of use must be
 * <ul>
 * <li>{@link #begin()}</li>
 * <li>{@link #commit()} or {@link #rollback(RuntimeException)}</li>
 * <li>{@link #close()}</li>
 * </ul>
 * 
 * @author Stefan Reichert
 */
public interface TransactionManager {

	/**
	 * Begins a transaction by initializing all required transactional data for
	 * the context. The data may be accessed by the {@link TransactionContext}
	 * object from the container.
	 * 
	 * @throws IllegalStateException
	 *             in case the already is a context
	 */
	void begin();

	/**
	 * Executes a commit on all existing transactional resources for the
	 * context.
	 * 
	 * @throws IllegalStateException
	 *             in case the is no context
	 */
	void commit();

	/**
	 * Executes a rollback on all existing transactional resources for the
	 * context.
	 * 
	 * @throws IllegalStateException
	 *             in case the is no context
	 */
	void rollback(RuntimeException exception);

	/**
	 * Closes and releases all existing transactional resources for the context.
	 * 
	 * @throws IllegalStateException
	 *             in case the is no context
	 */
	void close();

}
