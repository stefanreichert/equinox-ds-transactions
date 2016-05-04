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

import javax.persistence.EntityManager;

/**
 * A object that provides access to transactional resources bound to a specific
 * context (usually the executing thread).
 * 
 * @author Stefan Reichert
 */
public interface TransactionContext {

	/**
	 * Returns the {@link EntityManager} for the current context. The instance
	 * is managed in terms of transaction handling.<br>
	 * In case there is no instance available in the context, the method throws
	 * an {@link IllegalStateException}.
	 * 
	 * @return an entity manager instance
	 * @throws IllegalStateException
	 *             in case there is no entity manager available in the context
	 */
	EntityManager getEntityManager();

}
