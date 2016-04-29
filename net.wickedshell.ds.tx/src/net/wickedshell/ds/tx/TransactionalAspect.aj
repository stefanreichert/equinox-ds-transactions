package net.wickedshell.ds.tx;

import net.wickedshell.ds.tx.internal.Activator;

public aspect TransactionalAspect {
	
	pointcut transactionalMethod() : @annotation(Transactional) && execution(* *(..));

	Object around() : transactionalMethod() {
		TransactionManager transactionManager = Activator.getTransactionManager();
		Object returnValue;
		transactionManager.begin();
		try{
			returnValue = proceed();
			transactionManager.commit();
			return returnValue;
		}
		catch(RuntimeException exception){
			transactionManager.rollback(exception);
			throw exception;
		}
		finally{
			transactionManager.close();
		}
	}
	
}
