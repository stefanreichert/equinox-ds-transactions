package net.wickedshell.ds.tx;

public interface TransactionManager {
	
	void begin();
	
	void commit();
	
	void rollback(RuntimeException exception);
	
	void close();

}
