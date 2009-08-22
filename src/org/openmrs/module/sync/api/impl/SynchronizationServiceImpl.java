/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync.api.impl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncRecord;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncServerClass;
import org.openmrs.module.sync.SyncStatistic;
import org.openmrs.module.sync.api.SynchronizationService;
import org.openmrs.module.sync.api.db.SynchronizationDAO;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.SyncServerRecord;

public class SynchronizationServiceImpl implements SynchronizationService {

    private SynchronizationDAO dao;
    private final Log log = LogFactory.getLog(getClass());

    private SynchronizationDAO getSynchronizationDAO() {
        return dao;
    }
    
    public void setSynchronizationDAO(SynchronizationDAO dao) {
        this.dao = dao;
    }
    
    /**
     * @see org.openmrs.api.SynchronizationService#createSyncRecord(org.openmrs.module.sync.SyncRecord)
     */

    public void createSyncRecord(SyncRecord record) throws APIException {
        this.createSyncRecord(record, record.getOriginalUuid());
    }
    
    public void createSyncRecord(SyncRecord record, String originalUuidPassed) throws APIException {

        if ( record != null ) {
            // here is a hack to get around the fact that hibernate decides to commit transaction when it feels like it
            // otherwise, we could run this in the ingest methods
            RemoteServer origin = null;
            int idx = originalUuidPassed.indexOf("|"); 
            if ( idx > -1 ) {
                log.warn("originalPassed is " + originalUuidPassed);
                String originalUuid = originalUuidPassed.substring(0, idx);
                String serverUuid = originalUuidPassed.substring(idx + 1);
                log.warn("serverUuid is " + serverUuid + ", and originalUuid is " + originalUuid);
                record.setOriginalUuid(originalUuid);
                origin = Context.getService(SynchronizationService.class).getRemoteServer(serverUuid);
                if ( origin != null ) {
                    if ( origin.getServerType().equals(RemoteServerType.PARENT) ) {
                        record.setState(SyncRecordState.COMMITTED);
                    }
                } else {
                    log.warn("Could not get remote server by uuid: " + serverUuid);
                }
            }
            
            // before creation, we need to make sure that we create matching entries for each server (server-record relationship)
            Set<SyncServerRecord> serverRecords = record.getServerRecords();
            if ( serverRecords == null ) {
                log.warn("IN createSyncRecord(), SERVERRECORDS ARE NULL, SO SETTING DEFAULTS");
                serverRecords = new HashSet<SyncServerRecord>();
                List<RemoteServer> servers = this.getRemoteServers();
                if ( servers != null ) {
                    for ( RemoteServer server : servers ) {
                        // we only need to create extra server-records for servers that are NOT the parent - the parent state is kept in the actual sync record
                        if ( !server.getServerType().equals(RemoteServerType.PARENT)) {
                            SyncServerRecord serverRecord = new SyncServerRecord(server, record);
                            if ( server.equals(origin) ) {
                                log.warn("this record came from server " + origin.getNickname() + ", so we will set its status to commmitted");
                                serverRecord.setState(SyncRecordState.COMMITTED);
                            }
                            serverRecords.add(serverRecord);
                        }
                    }
                }
                record.setServerRecords(serverRecords);
                //server.setServerClasses(serverClasses);
            }
            
            getSynchronizationDAO().createSyncRecord(record);
        }
    }

    /**
     * @see org.openmrs.api.SynchronizationService#createSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void createSyncImportRecord(SyncImportRecord record) throws APIException {
        getSynchronizationDAO().createSyncImportRecord(record);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getNextSyncRecord()
     */
    public SyncRecord getFirstSyncRecordInQueue() throws APIException {
        return getSynchronizationDAO().getFirstSyncRecordInQueue();
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecord(java.lang.String)
     */
    public SyncRecord getSyncRecord(String uuid) throws APIException {
        return getSynchronizationDAO().getSyncRecord(uuid);
    }

    public SyncRecord getSyncRecordByOriginalUuid(String originalUuid) throws APIException {
        return getSynchronizationDAO().getSyncRecordByOriginalUuid(originalUuid);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getLatestRecord()
     */
    public SyncRecord getLatestRecord() throws APIException {
        return getSynchronizationDAO().getLatestRecord();
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecord(java.lang.String)
     */
    public SyncImportRecord getSyncImportRecord(String uuid) throws APIException {
        return getSynchronizationDAO().getSyncImportRecord(uuid);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecords()
     */
    public List<SyncRecord> getSyncRecords() throws APIException {
        return getSynchronizationDAO().getSyncRecords();
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
     */
    public List<SyncRecord> getSyncRecords(SyncRecordState state)
            throws APIException {
        return getSynchronizationDAO().getSyncRecords(state);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
     */
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states)
            throws APIException {
        return this.getSyncRecords(states, false);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
     */
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states, RemoteServer server)
            throws APIException {
    	List<SyncRecord> temp = null;
        List<SyncRecord> ret = null; 
        
        if ( server != null ) {
            if ( server.getServerType().equals(RemoteServerType.PARENT ) ) {
                ret = this.getSyncRecords(states);
            } else {
                ret = getSynchronizationDAO().getSyncRecords(states, false, server);
            }
        }
        
        // filter out classes that are not supposed to be sent to the specified server
        // and update their status
        if ( ret != null ) {
            temp = new ArrayList<SyncRecord>();
            for ( SyncRecord record : ret ) {
                if ( server.getClassesSent().containsAll(record.getContainedClassSet()) ) {
                    record.setForServer(server);
                    temp.add(record);
                    
                } else {
                    log.warn("Omitting record with " + record.getContainedClasses() + " for server: " + server.getNickname() + " with server type: " + server.getServerType());
                    if ( server.getServerType().equals(RemoteServerType.PARENT)) {
                        record.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
                    } else {
                        // if not the parent, we have to update the record for this specific server
                        Set<SyncServerRecord> records = record.getServerRecords();
                        for ( SyncServerRecord serverRecord : records ) {
                            if ( serverRecord.getSyncServer().equals(server) ) {
                                serverRecord.setState(SyncRecordState.NOT_SUPPOSED_TO_SYNC);
                            }
                        }
                        record.setServerRecords(records);
                    }
                    this.updateSyncRecord(record);
                }
            }
            ret = temp;
        }
        
        return ret;
    }

    
    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecords(org.openmrs.module.sync.engine.SyncRecordState)
     */
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse)
            throws APIException {
        return getSynchronizationDAO().getSyncRecords(states, inverse);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#updateSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void updateSyncRecord(SyncRecord record) throws APIException {
        getSynchronizationDAO().updateSyncRecord(record);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#deleteSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void deleteSyncRecord(SyncRecord record) throws APIException {
        getSynchronizationDAO().deleteSyncRecord(record);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#updateSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void updateSyncImportRecord(SyncImportRecord record) throws APIException {
        getSynchronizationDAO().updateSyncImportRecord(record);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#deleteSyncRecord(org.openmrs.module.sync.SyncRecord)
     */
    public void deleteSyncImportRecord(SyncImportRecord record) throws APIException {
        getSynchronizationDAO().deleteSyncImportRecord(record);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecordsSince(java.util.Date)
     */
    public List<SyncRecord> getSyncRecordsSince(Date from) throws APIException {
        return getSynchronizationDAO().getSyncRecordsSince(from);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getSyncRecordsBetween(java.util.Date, java.util.Date)
     */
    public List<SyncRecord> getSyncRecordsBetween(Date from, Date to)
            throws APIException {
        return getSynchronizationDAO().getSyncRecordsBetween(from, to);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#getGlobalProperty(java.lang.String)
     */
    public String getGlobalProperty(String propertyName) throws APIException {
        return getSynchronizationDAO().getGlobalProperty(propertyName);
    }
    
    /**
     * @see org.openmrs.api.SynchronizationService#setGlobalProperty(String propertyName, String propertyValue)
     */
    public void setGlobalProperty(String propertyName, String propertyValue) 
        throws APIException {
        getSynchronizationDAO().setGlobalProperty(propertyName, propertyValue);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#createRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
     */
    public void createRemoteServer(RemoteServer server) throws APIException {
        if ( server != null ) {
            Set<SyncServerClass> serverClasses = server.getServerClasses();
            if ( serverClasses == null ) {
                log.warn("IN CREATEREMOTESERVER(), SERVERCLASSES ARE NULL, SO SETTING DEFAULTS");
                serverClasses = new HashSet<SyncServerClass>();
                List<SyncClass> classes = this.getSyncClasses();
                if ( classes != null ) {
                    for ( SyncClass syncClass : classes ) {
                        SyncServerClass serverClass = new SyncServerClass(server, syncClass);
                        serverClasses.add(serverClass);
                    }
                }
                server.setServerClasses(serverClasses);
            }
            
            getSynchronizationDAO().createRemoteServer(server);
        }
    }
    
    /**
     * @see org.openmrs.api.SynchronizationService#updateRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
     */
    public void updateRemoteServer(RemoteServer server) throws APIException {
        getSynchronizationDAO().updateRemoteServer(server);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#deleteRemoteServer(org.openmrs.module.sync.engine.RemoteServer)
     */
    public void deleteRemoteServer(RemoteServer server) throws APIException {
        getSynchronizationDAO().deleteRemoteServer(server);
    }
    
    public RemoteServer getRemoteServer(Integer serverId) throws APIException {
        return getSynchronizationDAO().getRemoteServer(serverId);
    }

    public RemoteServer getRemoteServer(String uuid) throws APIException {
        return getSynchronizationDAO().getRemoteServer(uuid);
    }

    public RemoteServer getRemoteServerByUsername(String username) throws APIException {
        return getSynchronizationDAO().getRemoteServerByUsername(username);
    }

    public List<RemoteServer> getRemoteServers() throws APIException {
        return getSynchronizationDAO().getRemoteServers();
    }

    public RemoteServer getParentServer() throws APIException {
        return getSynchronizationDAO().getParentServer();
    }

    /**
     * Returns globally unique identifier of the local server. This value uniquely indentifies server in all
     * data exchanges with other servers.
     */
    public String getServerUuid() throws APIException{   
        return Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_SERVER_GUID);
    }

    /**
     * Updates globally unique identifier of the local server.
     */
    public void setServerUuid(String uuid) throws APIException{   
    	Context.getService(SynchronizationService.class).setGlobalProperty(SyncConstants.PROPERTY_SERVER_GUID, uuid);
    }

    /**
     * Returns server friendly name for sync purposes. It should be assigned by convention to be unique in the synchronization network
     * of servers. This value can be used to scope values that are otherwise unique only locally (such as integer primary keys).
     */
    public String getServerName() throws APIException{   
        return Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_SERVER_NAME);
    }

    /**
     * Updates/saves the friendly server name for sync purposes. 
     */
    public void setServerName(String name) throws APIException{   
    	Context.getService(SynchronizationService.class).setGlobalProperty(SyncConstants.PROPERTY_SERVER_NAME, name);
    }

    /**
     * Returns unique id of the server within its sync network. 
     * 
     * Note: the id is used to scope values and local identifier that are unique locally only. 
     * Thus the provided server id must be unique within a server sync network by convention.   
     */
    public String getServerId() throws APIException{   
        return Context.getAdministrationService().getGlobalProperty(SyncConstants.PROPERTY_SERVER_ID);
    }


    /**
     * Updates sync server id.
     * 
     * Note: the id is used to scope values and local identifier that are unique locally only. 
     * Thus the provided server id must be unique within a server sync network by convention.  
     * 
     * @param id ID to update. Short numerics are recommented (01, 02, etc.)
     * @throws APIException
     */
    public void setServerId(String id) throws APIException{   
    	Context.getService(SynchronizationService.class).setGlobalProperty(SyncConstants.PROPERTY_SERVER_ID, id);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#createSyncClass(org.openmrs.module.sync.SyncClass)
     */
    public void createSyncClass(SyncClass syncClass) throws APIException {
        getSynchronizationDAO().createSyncClass(syncClass);
    }
    
    /**
     * @see org.openmrs.api.SynchronizationService#updateSyncClass(org.openmrs.module.sync.SyncClass)
     */
    public void updateSyncClass(SyncClass syncClass) throws APIException {
        getSynchronizationDAO().updateSyncClass(syncClass);
    }

    /**
     * @see org.openmrs.api.SynchronizationService#deleteSyncClass(org.openmrs.module.sync.SyncClass)
     */
    public void deleteSyncClass(SyncClass syncClass) throws APIException {
        getSynchronizationDAO().deleteSyncClass(syncClass);
    }
    
    public SyncClass getSyncClass(Integer syncClassId) throws APIException {
        return getSynchronizationDAO().getSyncClass(syncClassId);
    }
    
    public List<SyncClass> getSyncClasses() throws APIException {
        return getSynchronizationDAO().getSyncClasses();
    }

    /**
     * @see org.openmrs.api.SynchronizationService#createDatabaseForChild(java.lang.String, java.io.Writer)
     */
    public void createDatabaseForChild(String uuidForChild, OutputStream out) throws APIException {
       getSynchronizationDAO().createDatabaseForChild(uuidForChild, out); 
    }
    
    /**
     * @see org.openmrs.api.SynchronizationService#deleteOpenmrsObject(org.openmrs.synchronization.OpenmrsObject)
     */
    public void deleteOpenmrsObject(OpenmrsObject o) throws APIException {
    	getSynchronizationDAO().deleteOpenmrsObject(o);
    }
 
    /**
     * Changes flush sematics, delegating directly to the corresponsing DAO method.
     * 
     * @see org.openmrs.api.SynchronizationService#setFlushModeManual()
     * @see org.openmrs.api.db.hibernate.HibernateSynchronizationDAO#setFlushModeManual()
     */
    public void setFlushModeManual() throws APIException {
    	getSynchronizationDAO().setFlushModeManual();
    }
    
    /**
     * Changes flush sematics, delegating directly to the corresponsing DAO method.
     * 
     * @see org.openmrs.api.SynchronizationService#setFlushModeAutomatic()
     * @see org.openmrs.api.db.hibernate.HibernateSynchronizationDAO#setFlushModeAutomatic()
     */
    public void setFlushModeAutomatic() throws APIException {
    	getSynchronizationDAO().setFlushModeAutomatic();
    }
    
	/**
     * Performs peristence layer flush, delegating directly to the corresponsing DAO method.
     * 
     * @see org.openmrs.api.SynchronizationService#flushSession()
     * @see org.openmrs.api.db.hibernate.HibernateSynchronizationDAO#flushSession()
     */    
    public void flushSession() throws APIException {
    	getSynchronizationDAO().flushSession();
    }

    /**
     * Processes save/update to instance of OpenmrsObject by persisting it into local persistance store.
     * 
     * @param object instance of OpenmrsObject to be processed.
     * @return
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void saveOrUpdate(OpenmrsObject object)  throws APIException {
    	getSynchronizationDAO().saveOrUpdate(object);
    }

    /**
     * Gets stats for the server:
     * 1. Sync Records count by server by state
     * 2. If any sync records are in 'pending'/failed state and it has been > 24hrs, add statistic for it
     * 3. count of 'pending' sync records (i.e. the ones that are not in complete or error state
     * 
     * @param fromDate start date
     * @param toDate end date
     * @return
     * @throws DAOException
     */
    public Map<RemoteServer,Set<SyncStatistic>> getSyncStatistics(Date fromDate, Date toDate) throws DAOException {
    	
    	Map<RemoteServer,Set<SyncStatistic>> stats = getSynchronizationDAO().getSyncStatistics(fromDate, toDate);
    	
    	//check out the info for the servers: if any records are pending and are older than 1 day, add flag to stats
    	for(Map.Entry<RemoteServer,Set<SyncStatistic>> entry1 : stats.entrySet()) {
    		Long pendingCount = 0L;
    		for(SyncStatistic entry2 : entry1.getValue()) {
    			if (entry2.getType() == SyncStatistic.Type.SYNC_RECORD_COUNT_BY_STATE) {
    				if (entry2.getName() != SyncRecordState.ALREADY_COMMITTED.toString() 
    						&& entry2.getName() != SyncRecordState.COMMITTED.toString()
    						&& entry2.getName() != SyncRecordState.NOT_SUPPOSED_TO_SYNC.toString()
    						) {
    					pendingCount = pendingCount + ((entry2.getValue() == null) ? 0L : Long.parseLong(entry2.getValue().toString()));
    				}
    			}
    		}
    		
    		//add pending count
    		entry1.getValue().add(new SyncStatistic(SyncStatistic.Type.SYNC_RECORDS_PENDING_COUNT,SyncStatistic.Type.SYNC_RECORDS_PENDING_COUNT.toString(),pendingCount)); //careful, manipulating live collection
    		
    		//if some 'stale' records found see if it has been 24hrs since last sync
    		if (pendingCount > 0 && entry1.getKey().getLastSync() != null) {
    			Long lastSyncPlus24hrs = entry1.getKey().getLastSync().getTime() + 24*60*60*1000;
    			if (lastSyncPlus24hrs < new Date().getTime()) {
    				entry1.getValue().add(new SyncStatistic(SyncStatistic.Type.SYNC_RECORDS_OLDER_THAN_24HRS,SyncStatistic.Type.SYNC_RECORDS_OLDER_THAN_24HRS.toString(),true)); //careful, manipulating live collection
    			}
    		}
    	}
    		
    	return stats;
    }
    public boolean checkUuidsForClass(Class clazz) throws APIException {
    	return getSynchronizationDAO().checkUuidsForClass(clazz);
    }
}


