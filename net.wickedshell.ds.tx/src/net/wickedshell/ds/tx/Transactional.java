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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for transactional methods. If a method is annotated as
 * <i>transactional</i> it will be provided with a context. The context data may
 * be retrieved by injecting the {@link TransactionContext} where required.
 * 
 * @author Stefan Reichert
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Transactional {

}
