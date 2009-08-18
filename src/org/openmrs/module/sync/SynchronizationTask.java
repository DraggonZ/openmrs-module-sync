package org.openmrs.module.sync;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.api.SynchronizationService;
import org.openmrs.module.sync.ingest.SyncTransmissionResponse;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.tasks.AbstractTask;

/**
 * Represents scheduled task to perform full data synchronization with a remote server as identified during the task setup.
 *
 */
public class SynchronizationTask extends AbstractTask {

	// Logger
	private static Log log = LogFactory.getLog(SynchronizationTask.class);
	
	// Instance of configuration information for task
	private Integer serverId = 0;

	/**
	 * Default Constructor (Uses SchedulerConstants.username and
	 * SchedulerConstants.password
	 */
	public SynchronizationTask() {
		// do nothing for now
	}

	/**
	 * Runs 'full' data synchronization (i.e. both send local changes and receive changes from the remote server as identified 
	 * in the task setup). 
	 * <p> NOTE: Any exception (outside of session open/close) is caughted and reported in the error log thus creating retry
	 * behavior based on the scheduled frequency.
	 */
	public void execute() {
		Context.openSession();
		try {
			log.debug("Synchronizing data to a server.");
			if (Context.isAuthenticated() == false && serverId > 0)
				authenticate();
			
			// test to see if sync is enabled before trying to sync
			SyncStatusState syncStatus = SyncUtil.getSyncStatus();
			
			if ( syncStatus.equals(SyncStatusState.ENABLED_CONTINUE_ON_ERROR) || syncStatus.equals(SyncStatusState.ENABLED_STRICT) ) {
			
				RemoteServer server = Context.getService(SynchronizationService.class).getRemoteServer(serverId);
				if ( server != null ) {
					SyncTransmissionResponse response = SyncUtilTransmission.doFullSynchronize(server);
					try {
						response.createFile(false, SyncConstants.DIR_JOURNAL);
					} catch ( Exception e ) {
	    				log.error("Unable to create file to store SyncTransmissionResponse: " + response.getFileName(), e);
	    				e.printStackTrace();
					}
				}
			} else {
				log.info("Not going to sync because Syncing is not ENABLED");
			}
		} catch (Exception e) {
			log.error("Scheduler error while trying to synchronize data. Will retry per schedule.", e);
		} finally {
			Context.closeSession();
			log.debug("Synchronization complete.");
		}
	}
	
	/**
	 * Initializes task. Note serverId is in most cases an Id (as stored in sync server table) of parent. As such, parent Id
	 * does not need to be stored separately with the task as it can always be determined from sync server table. 
	 * serverId is stored here as we envision using this feature to also 'export' data to another server -- esentially 
	 * 'shadow' copying data to a separate server for other uses such as reporting.   
	 * 
	 * @param config
	 */
	@Override
	public void initialize(final TaskDefinition definition) { 
		super.initialize(definition);
		try {
			this.serverId = Integer.valueOf(definition.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID));
        } catch (Exception e) {
        	this.serverId = 0;
        	log.error("Could not find serverId for this sync scheduled task.",e);
        }
	}

}
