package net.wickedshell.ds.tx.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import net.wickedshell.ds.tx.TransactionManager;

public class Activator implements BundleActivator {

	private static ServiceReference<TransactionManager> reference;
	private static BundleContext bundleContext;

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	public static TransactionManager getTransactionManager() {
		return bundleContext.getService(getTransactionManagerReference());
	}

	private synchronized static ServiceReference<TransactionManager> getTransactionManagerReference() {
		if (reference == null) {
			reference = bundleContext.getServiceReference(TransactionManager.class);
		}
		return reference;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (reference != null) {
			context.ungetService(reference);
		}
		reference = null;
		bundleContext = null;
	}
}
