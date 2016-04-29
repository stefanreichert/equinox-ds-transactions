/*******************************************************************************
 * Copyright (c) 2010 Oracle.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at 
 *     http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     mkeith - Gemini JPA work 
 ******************************************************************************/
package org.eclipse.gemini.jpa.datasource;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.eclipse.gemini.jpa.GeminiManager;
import org.eclipse.gemini.jpa.GeminiPersistenceUnitProperties;
import org.eclipse.gemini.jpa.GeminiUtil;
import org.eclipse.gemini.jpa.PUnitInfo;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;

import static org.eclipse.gemini.jpa.GeminiUtil.debug;
import static org.eclipse.gemini.jpa.GeminiUtil.fatalError;
import static org.eclipse.gemini.jpa.GeminiUtil.warning;
import static org.osgi.service.jdbc.DataSourceFactory.*;

/** 
 * Utility class with datasource helper methods.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class DataSourceUtil {

    // Pointer back to manager
    GeminiManager mgr;
    
    public DataSourceUtil(GeminiManager mgr) {
        this.mgr = mgr;
    }
    
    public DataSource acquireDataSource(PUnitInfo pUnitInfo, Map<?,?> properties) {

        debug("DataSourceUtil.acquireDataSource - for punit ", pUnitInfo.getUnitName());
        // If an actual data source object was passed in then just return it and 
        // let it be re-added to the properties map by the caller  
        // ### Enhancement for bug 335983 - Contributed by Eduard Bartsch ###
        Object ds = properties.get(PersistenceUnitProperties.NON_JTA_DATASOURCE);
        if (ds instanceof DataSource) {
            return (DataSource) ds;
        }
        
        // Support the case of a provider-connected data source. In this case the provider
        // will be responsible for connecting to a data source (using specified properties)
        // ### Enhancement for bug 369029 - Ideas contributed by Gunnar W ###
        if (properties.containsKey(GeminiPersistenceUnitProperties.PROVIDER_CONNECTED_DATA_SOURCE)) {
            return null;
        }
        
        Driver driver = null;

        // Sort out which named driver we are dealing with
        String driverName = (String)properties.get(GeminiUtil.JPA_JDBC_DRIVER_PROPERTY);
        String driverVersion = (String)properties.get(GeminiUtil.OSGI_JDBC_DRIVER_VERSION_PROPERTY);
        if (driverName == null) {
            driverName = pUnitInfo.getDriverClassName();
            if (driverName == null) {
                // We at least need a driver name
                fatalError("No driver was specified", null);
            } else {
                // No driver was specified in props so take the version from p-unit as well, if it's there
                if (driverVersion == null)
                    driverVersion = pUnitInfo.getDriverVersion();
            }
        }

        // Try using a DSF service if we have one stored away and the one asked for is the same
        ServiceReference dsfRef = pUnitInfo.getDsfService();
        if ((dsfRef != null) && (specifiesSameDriver(driverName, driverVersion, pUnitInfo))) {
            debug("DataSourceUtil - Using existing DSF service ref from punit ", pUnitInfo.getUnitName());
            DataSourceFactory dsf = (DataSourceFactory) mgr.getBundleContext().getService(dsfRef);
            try {
                // There is no standard way of getting JDBC properties from JPA props
                // (apart from the url/user/pw that are converted using getJdbcProperties)
                driver = dsf.createDriver(null);
            } catch (SQLException sqlEx) {
                // Service was registered but seems to be busted
                fatalError("Could not create data source for " + driverName, sqlEx);
            }
        }
        // If we still have no driver then try doing a dynamic lookup
        if (driver == null) {
            debug("Trying dynamic lookup of DSF for ", driverName, " for p-unit ", pUnitInfo.getUnitName());
            String filter = filterForDSFLookup(driverName, driverVersion);
            ServiceReference[] dsfRefs = lookupDSF(pUnitInfo.getBundle().getBundleContext(), filter);
            if (dsfRefs != null) {
                debug("DataSourceUtil - Found DSF, props: ", GeminiUtil.serviceProperties(dsfRefs[0]));
                DataSourceFactory dsf = (DataSourceFactory) mgr.getBundleContext().getService(dsfRefs[0]);
                try {
                    driver = dsf.createDriver(null);
                } catch (SQLException sqlEx) {
                    fatalError("Could not create data source for " + driverName, sqlEx);
                }
            }
        }
        // Finally, try loading it locally
        if (driver == null) {
            debug("DataSourceUtil - Trying to load driver ", driverName, " locally from p-unit bundle ", pUnitInfo.getUnitName());
            try {
                Class<?> driverClass = pUnitInfo.getBundle().loadClass(driverName);
                driver = (Driver) driverClass.newInstance();
                debug("DataSourceUtil - JDBC driver ", driverName, " loaded locally from p-unit bundle ", pUnitInfo.getUnitName());
            } catch (Exception ex) {
                fatalError("Could not create data source for " + driverName, ex);
            }
        }
        Properties props = getJdbcProperties(pUnitInfo, properties);
        
        return new PlainDriverDataSource(driver, props);
    }
    
    // Return whether the same driver is specified as in the PUnitInfo.
    // Driver names can both be assumed to be non-null and must be equal.
    // One, or both, of the versions may be null, but if neither are null then they must match.
    protected boolean specifiesSameDriver(String driverName, String driverVersion, PUnitInfo unitInfo) {
        String infoDriverName = unitInfo.getDriverClassName();
        String infoDriverVersion = unitInfo.getDriverVersion();
        return (driverName.equals(infoDriverName)) &&
                 ((driverVersion == null) || 
                  (infoDriverVersion == null) || 
                  (driverVersion.equals(infoDriverVersion)));
    }
    
    /*
     * Return the current JDBC url, user and password properties. The props set in the
     * XML file (from pUnitInfo) will be overridden by any properties of the same name
     * in the runtime properties Map passed in. The resulting properties will be passed
     * to a JDBC driver.
     */
    public Properties getJdbcProperties(PUnitInfo pUnitInfo, Map<?,?> properties) {

        Properties props = new Properties();
        
        // Get the 3 driver properties, if they exist (url, user, password)
        debug("DataSourceUtil.getJDBCProperties");
        debug("  fromMap: ", properties);
        debug("  fromDescriptor: ", pUnitInfo);

        String url = (String)properties.get(GeminiUtil.JPA_JDBC_URL_PROPERTY);
        if (url == null)
            url = pUnitInfo.getDriverUrl();        
        if (url != null) 
            props.put(JDBC_URL, url);
        
        String user = (String)properties.get(GeminiUtil.JPA_JDBC_USER_PROPERTY);
        if (user == null)
            user = pUnitInfo.getDriverUser();        
        if (user != null) 
            props.put(JDBC_USER, user);

        String pw = (String)properties.get(GeminiUtil.JPA_JDBC_PASSWORD_PROPERTY);
        if (pw == null)
            pw = pUnitInfo.getDriverPassword();        
        if (pw != null) 
            props.put(JDBC_PASSWORD, pw);

        debug("DataSourceUtil.getJDBCProperties - returning: ", props);
        return props;
    }
    
    /*==============================================*/
    /* Data source factory service tracking methods */
    /*==============================================*/
    
    /** 
     * Look up the data source factory service for the specified
     * persistence unit and start a data source factory tracker. 
     * One of two trackers will be created:
     * 
     * a) If the DSF was registered then start a tracker to track when it goes away
     * so that we can remove the dependent EMF service
     * 
     * b) If the DSF was not registered then start a tracker to detect when it comes online
     * 
     * @param pUnitInfo The metadata for this p-unit
     * @return true if the data source factory service was registered, false if it wasn't
     */
    public boolean trackDataSourceFactory(PUnitInfo pUnitInfo) {
        
        debug("ServicesUtil trackDataSourceFactory for p-unit ", pUnitInfo.getUnitName());
        ServiceReference[] dsfRefs = null;
        ServiceTracker tracker = null;

        // See if the data source factory service for the driver is registered
        try {
            String filter = filterForDSFLookup(pUnitInfo.getDriverClassName(), pUnitInfo.getDriverVersion());
            dsfRefs = lookupDSF(pUnitInfo.getBundle().getBundleContext(), filter);
            if (dsfRefs != null) {
                // We found at least one -- track the first one
                // *** Note: Race condition still exists where service could disappear before being tracked
                ServiceReference dsfRef = dsfRefs[0];
                debug("ServicesUtil starting tracker on existing DSF for ", pUnitInfo.getUnitName());
                tracker = new ServiceTracker(mgr.getBundleContext(), 
                                             dsfRef,
                                             new DSFOfflineTracker(pUnitInfo, this));
                pUnitInfo.setDsfService(dsfRef);
                debug("DSF service props: ", GeminiUtil.serviceProperties(dsfRef));
            } else {
                // No service was found, track for a service that may come in the future 
                debug("ServicesUtil starting tracker to wait for DSF for ", pUnitInfo.getUnitName());
                tracker = new ServiceTracker(mgr.getBundleContext(), 
                                             mgr.getBundleContext().createFilter(filter),
                                             new DSFOnlineTracker(pUnitInfo, this));
            }
        } catch (Exception ex) {
            fatalError("Unexpected failure creating DSF service tracker", ex);
        }
        pUnitInfo.setTracker(tracker);
        tracker.open();
        return dsfRefs != null;
    }

    /** 
     * Stop tracking the data source factory for the given p-unit
     */
    public void stopTrackingDataSourceFactory(PUnitInfo pUnitInfo) {
        // Clean up the tracker
        debug("ServicesUtil stopTrackingDataSourceFactory", 
              " for p-unit ", pUnitInfo.getUnitName());
        if (pUnitInfo.getTracker() != null) {
            debug("ServicesUtil stopping tracker for p-unit ", 
                    pUnitInfo.getUnitName());
            pUnitInfo.getTracker().close();
            pUnitInfo.setTracker(null);
        }
    }

    /** 
     * This method will be invoked by the OnlineTracker when a data source factory 
     * service comes online. This occurs when the p-unit has been processed before the
     * JDBC service has had a chance to be activated or register its DSF services.
     */
    public void dataSourceFactoryOnline(PUnitInfo pUnitInfo, ServiceReference ref) {
        // TODO async handling of data source adding
        debug("dataSourceFactoryOnline, ref=", ref, " for p-unit ", pUnitInfo.getUnitName());
        if (pUnitInfo.getEmf() != null) {
            // EMF has already been created by the user (using EMFBuilder svc). Too late for a DSF service
            warning("DSF " + ref + " came online when EMF for p-unit " + pUnitInfo.getUnitName() +
                    " already existed - ignoring DSF");
        } else {
            // If we already have a DSF service, for some reason, then ignore this one
            if (pUnitInfo.getDsfService() != null) { 
                warning("DSF service already exists for p-unit " + pUnitInfo.getUnitName() + " - ignoring new DSF service");
            } else {
                // We registered a tracker and don't have a DSF service so this one must be of interest to us.
                // Unregister and go through the entire registration process again, assuming we will find this new DSF
                debug("dataSourceFactoryOnline, unregistering and reregistering EMF services for p-unit ", pUnitInfo.getUnitName());
                mgr.getServicesUtil().unregisterEMFServices(pUnitInfo);
                mgr.getServicesUtil().registerEMFServices(pUnitInfo);
            }
        }
    }

    /** 
     * This method will be invoked by the OfflineTracker when the data source factory 
     * that we are relying on goes offline. 
     */
    public void dataSourceFactoryOffline(PUnitInfo pUnitInfo, ServiceReference removedRef) {
        // TODO async handling of data source removal
        ServiceReference dsServiceRef = pUnitInfo.getDsfService();
        debug("dataSourceFactoryOffline, p-unit=", pUnitInfo.getUnitName(), "removedRef=", removedRef,
              "storedRef=", dsServiceRef);
        // Verify that this is the dsf service that we care about
        if (dsServiceRef == null) {
            warning("DataSourceFactory " + removedRef + " went offline but no record of it was stored in p-unit " + pUnitInfo.getUnitName());
        } else {
            if (dsServiceRef.compareTo(removedRef) != 0) { 
                warning("DataSourceFactory " + removedRef + " went offline but a different DSF was stored in p-unit " + pUnitInfo.getUnitName());
            }
        }
        // Unregister the EMF service but leave the Builder
        debug("dataSourceFactoryOffline - unregistering EMF service ", "for p-unit ", pUnitInfo.getUnitName());
        pUnitInfo.setDsfService(null);
        mgr.getServicesUtil().unregisterEMFService(pUnitInfo);
    }
    
    /** 
     * This method will be invoked by trackDataSourceFactory() or by the EclipseLinkProvider when 
     * DSF service is being looked up
     * Note that driverVersion may be null, driverName should not be
     */
    public String filterForDSFLookup(String driverName, String driverVersion) {
        String driverNameFilter = "(" + DataSourceFactory.OSGI_JDBC_DRIVER_CLASS + "=" + driverName + ")";
        if (driverVersion == null) {
            return driverNameFilter;
        } else {
            String driverVersionFilter = "(" + DataSourceFactory.OSGI_JDBC_DRIVER_VERSION + "=" + driverVersion + ")";
            return "(&" + driverNameFilter + driverVersionFilter + ")";
        }
    }
    
    /** 
     * This method will be invoked by trackDataSourceFactory() or by the EclipseLinkProvider 
     * to look up a DSF service
     */
    public ServiceReference[] lookupDSF(BundleContext ctx, String filter) {
        try {
            return ctx.getServiceReferences(DataSourceFactory.class.getName(), filter);
        } catch (InvalidSyntaxException isEx) {
            fatalError("Bad filter syntax (likely because of missing driver class name)", isEx);
            return null; // For the compiler...
        }
    }    
}