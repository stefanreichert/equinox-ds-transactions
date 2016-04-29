package net.wickedshell.ds.tx.sample.repository.internal;

import java.util.Collection;

import javax.persistence.TypedQuery;

import net.wickedshell.ds.tx.TransactionContext;
import net.wickedshell.ds.tx.sample.domain.Greeting;
import net.wickedshell.ds.tx.sample.repository.GreetingRepository;

public class GreetingRepositoryImpl implements GreetingRepository {

	private TransactionContext transactionContext;

	public void setTransactionContext(TransactionContext transactionContext) {
		this.transactionContext = transactionContext;
	}
	
	@Override
	public Collection<Greeting> findAll() {
		TypedQuery<Greeting> query = transactionContext.getEntityManager().createQuery("SELECT greeting FROM Greeting AS greeting ORDER BY greeting.creationDate", Greeting.class);
		return query.getResultList();
	}

	@Override
	public void persist(Greeting greeting) {
		transactionContext.getEntityManager().persist(greeting);
	}

}
