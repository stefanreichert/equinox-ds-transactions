package net.wickedshell.ds.tx.internal;

import java.util.UUID;

import javax.persistence.EntityManager;

/**
 * Container object for context data.
 * 
 * @author Stefan Reichert
 *
 */
public class TransactionContextData {

	private final String transactionId;
	private final EntityManager entityManager;
	private int nestingDepth = 0;

	public TransactionContextData(EntityManager entityManager) {
		transactionId = UUID.randomUUID().toString();
		this.entityManager = entityManager;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void incrementNestingDepth() {
		nestingDepth++;
	}

	public void decrementNestingDepth() {
		nestingDepth--;
	}

	public boolean isInNestedState() {
		return nestingDepth > 0;
	}

}
