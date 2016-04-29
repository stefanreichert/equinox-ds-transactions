package net.wickedshell.ds.tx;

import javax.persistence.EntityManager;

public interface TransactionContext {
	
	EntityManager getEntityManager();

}
