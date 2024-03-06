package org.geoserver.monitor.monitorPsql;

import java.util.logging.Level;

/*
 * author: Sangeetha Shankar (DLR-TS)
 */

import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

public class PsqlMonitorInitializer implements GeoServerInitializer{
	
	static Logger LOGGER = Logging.getLogger(PsqlMonitorInitializer.class);

    Monitor monitor;

    public PsqlMonitorInitializer(Monitor monitor) {
        
        /* Note: PSQL Monitor modules seems to get loaded after the initialization of monitor module. 
         * Due to this, PsqlMonitorDAO is not recognized/available during monitor initialization and 
         * it ends up using memory storage. Hence, when this module is initialized, 
         * we recreate the Monitor object, if 'postgres' is the storage method specified in monitor.properties.
         * Best approach would be to load these modules before monitor initialization. */
        
        String storage = monitor.getConfig().getStorage();        
        String monitorDAO = monitor.getDAO().getName();        
        if(storage.equals(monitorDAO)) {
        	this.monitor = monitor;
        }
        else {
        	try {
	        	LOGGER.info("Monitor DAO is not "+storage+" even though specified so in monitor.properties. Recreating Monitor object..."); 
	        	
	        	GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
	        	MonitorConfig config = new MonitorConfig(loader);
	        	this.monitor = new Monitor(config);
        	}
        	catch (Exception e) {
        		LOGGER.log(Level.SEVERE,e.toString()); 
        	}
        }
        
        LOGGER.info("Monitor DAO = "+this.monitor.getDAO().getName());        
        LOGGER.info("Storage Config = "+this.monitor.getConfig().getStorage());   
    }

	@Override
	public void initialize(GeoServer geoServer) throws Exception {
		//check if monitor extension is enabled
		if (!monitor.isEnabled()) {
			return;
		}
		
        LOGGER.info("PsqlMonitor extension enabled.");		
	}
}
