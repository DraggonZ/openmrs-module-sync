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
package org.openmrs.module.sync.api;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.sync.SyncRecordState;
import org.openmrs.module.sync.SyncStatistic;
import org.openmrs.module.sync.Synchronizable;
import org.openmrs.module.sync.engine.SyncRecord;
import org.openmrs.module.sync.filter.SyncClass;
import org.openmrs.module.sync.ingest.SyncImportRecord;
import org.openmrs.module.sync.server.RemoteServer;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface SynchronizationService {

    /**
     * Create a new SyncRecord
     * @param SyncRecord The SyncRecord to create
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void createSyncRecord(SyncRecord record) throws APIException;

    /**
     * Auto generated method comment
     * 
     * @param record
     * @param originalUuid
     */
    public void createSyncRecord(SyncRecord record, String originalUuid);

    /**
     * Update a SyncRecord
     * @param SyncRecord The SyncRecord to update
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void updateSyncRecord(SyncRecord record) throws APIException;
    
    /**
     * Delete a SyncRecord
     * @param SyncRecord The SyncRecord to delete
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void deleteSyncRecord(SyncRecord record) throws APIException;

    /**
     * 
     * @param uuid of the SyncRecord to retrieve
     * @return SyncRecord The SyncRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public SyncRecord getSyncRecord(String uuid) throws APIException;

    @Transactional(readOnly=true)
    public SyncRecord getSyncRecordByOriginalUuid(String originalUuid) throws APIException;

    /**
     * 
     * @return SyncRecord The latest SyncRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public SyncRecord getLatestRecord() throws APIException;

    /**
     * Create a new SyncImportRecord
     * @param SyncImportRecord The SyncImportRecord to create
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void createSyncImportRecord(SyncImportRecord record) throws APIException;
    
    /**
     * Update a SyncImportRecord
     * @param SyncImportRecord The SyncImportRecord to update
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void updateSyncImportRecord(SyncImportRecord record) throws APIException;
    
    /**
     * Delete a SyncImportRecord
     * @param SyncImportRecord The SyncImportRecord to delete
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void deleteSyncImportRecord(SyncImportRecord record) throws APIException;

    /**
     * 
     * @param uuid of the SyncImportRecord to retrieve
     * @return SyncRecord The SyncImportRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public SyncImportRecord getSyncImportRecord(String uuid) throws APIException;
    
    /**
     * Returns the first SyncRecord in either the PENDING SEND or the NEW state
     * @return SyncRecord The first SyncRecord matching the criteria, or null if none matches
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public SyncRecord getFirstSyncRecordInQueue() throws APIException;
    
    /**
     * Get all SyncRecords
     * @return SyncRecord A list containing all SyncRecords
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public List<SyncRecord> getSyncRecords() throws APIException;
    
    /**
     * Get all SyncRecords in a specific SyncRecordState
     * @param state SyncRecordState for the SyncRecords to be returned
     * @return SyncRecord A list containing all SyncRecords with the given state
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public List<SyncRecord> getSyncRecords(SyncRecordState state) throws APIException;

    /**
     * Get all SyncRecords in a specific SyncRecordStates
     * @param states SyncRecordStates for the SyncRecords to be returned
     * @return SyncRecord A list containing all SyncRecords with the given states
     * @throws APIException
     */
    @Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states) throws APIException;

    /**
     * Get all SyncRecords in a specific SyncRecordStates, that the server allows sending for (per-server basis). Filters out records
     * with classes that are not sync-able (see RemoteServr.getClassesSent() for more info on how this works). Updates status of
     * filtered out classes to 'not_supposed_to_sync'.
     * @param states SyncRecordStates for the SyncRecords to be returned
     * @param server Server these records will be sent to, so we can filter on Class
     * @return SyncRecord A list containing all SyncRecords with the given states
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states, RemoteServer server) throws APIException;

    
    /**
     * Get all SyncRecords in a specific SyncRecordStates
     * @param states SyncRecordStates for the SyncRecords to be returned
     * @return SyncRecord A list containing all SyncRecords with the given states
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public List<SyncRecord> getSyncRecords(SyncRecordState[] states, boolean inverse) throws APIException;

    /**
     * Get all SyncRecords after a given timestamp
     * @param from Timestamp specifying lower bound, not included.
     * @return SyncRecord A list containing all SyncRecords with a timestamp after the given timestamp
     * @throws APIException
     */
    //Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public List<SyncRecord> getSyncRecordsSince(Date from) throws APIException;
    
    /**
     * Get all SyncRecords between two timestamps, including the to-timestamp.
     * @param from Timestamp specifying lower bound, not included.
     * @param to Timestamp specifying upper bound, included.
     * @return SyncRecord A list containing all SyncRecords with a timestamp between the from timestamp and up to and including the to timestamp
     * @throws APIException
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)
    public List<SyncRecord> getSyncRecordsBetween(Date from, Date to) throws APIException;

    
    /**
     * 
     * Retrieve value of given global property using synchronization data access mechanisms.
     * 
     * @param propertyName
     * @return
     */
    //@Authorized({"View Synchronization Records"})
    @Transactional(readOnly=true)    
    public String getGlobalProperty(String propertyName) throws APIException;
    
    /**
     * Set global property related to synchronization; notably bypasses any changeset recording mechanisms.
     * @param propertyName String specifying property name which value is to be set.
     * @param propertyValue String specifying property value to be set.
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Records"})
    public void setGlobalProperty(String propertyName, String propertyValue) throws APIException;


    /**
     * Create a new SyncRecord
     * @param SyncRecord The SyncRecord to create
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Servers"})
    public void createRemoteServer(RemoteServer server) throws APIException;
    
    /**
     * Update a SyncRecord
     * @param SyncRecord The SyncRecord to update
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Servers"})
    public void updateRemoteServer(RemoteServer server) throws APIException;
    
    /**
     * Delete a SyncRecord
     * @param SyncRecord The SyncRecord to delete
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization Servers"})
    public void deleteRemoteServer(RemoteServer server) throws APIException;

    /**
     * 
     * @param uuid of the SyncRecord to retrieve
     * @return SyncRecord The SyncRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Servers"})
    @Transactional(readOnly=true)
    public RemoteServer getRemoteServer(Integer serverId) throws APIException;

    /**
     * 
     * @param uuid of the SyncRecord to retrieve
     * @return SyncRecord The SyncRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Servers"})
    @Transactional(readOnly=true)
    public RemoteServer getRemoteServer(String uuid) throws APIException;

    /**
     * 
     * @param username child_username of the RemoteServer to retrieve
     * @return SyncRecord The SyncRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Servers"})
    @Transactional(readOnly=true)
    public RemoteServer getRemoteServerByUsername(String username) throws APIException;

    /**
     * 
     * @param uuid of the SyncRecord to retrieve
     * @return SyncRecord The SyncRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Servers"})
    @Transactional(readOnly=true)
    public List<RemoteServer> getRemoteServers() throws APIException;

    /**
     * 
     * @param uuid of the SyncRecord to retrieve
     * @return SyncRecord The SyncRecord or null if not found
     * @throws APIException
     */
    //@Authorized({"View Synchronization Servers"})
    @Transactional(readOnly=true)
    public RemoteServer getParentServer() throws APIException;
    
    /**
     *  Retrieves globally unique id of the server.
     *  
     * @return uuid of the server. String representation of java.util.UUID.
     * @throws APIException
     */
    @Transactional(readOnly=true)
    public String getServerUuid() throws APIException;

    /**
     * Sets globally unique id of the server. WARNING: Use only during initial server setup.
     * 
     * WARNING: DO NOT CALL this method unless you fully understand the implication of
     * this action. Specifically, changing already assigned GUID for a server will cause
     * it to loose its link to history of changes that may be designated for this server.
     * 
     * @param uuid unique GUID of the server. String representation of java.util.UUID.
     * @throws APIException
     */
    public void setServerUuid(String uuid) throws APIException;
    
    /**
     *  Retrieve user friendly nickname for the server that is (by convention) unique for the given sync network of servers.
     * @return name of the server.
     * @throws APIException
     */
    @Transactional(readOnly=true)
    public String getServerName() throws APIException;

    /**
     * Sets friendly server name. WARNING: Use only during initial server setup.
     * 
     * WARNING: DO NOT CALL this method unless you fully understand the implication of
     * this action. Similarly to {@link #setServerUuid(String)} some data loss may occur if called
     * while server is functioning as part of the sync network.
     * 
     * @param name new server name
     * @throws APIException
     */
    public void setServerName(String name) throws APIException;

    /**
     *  Retrieve user friendly nickname for the server that is (by convention) unique for the given sync network of servers.
     * @return name of the server.
     * @throws APIException
     */
    @Transactional(readOnly=true)
    public String getServerId() throws APIException;

    /**
     * Sets server id for sync network. WARNING: Use only during initial server setup.
     * 
     * WARNING: DO NOT CALL this method unless you fully understand the implication of
     * this action. Similarly to {@link #setServerUuid(String)} some data loss may occur if called
     * while server is functioning as part of the sync network.
     * 
     * @param id new server id for the network of sync servers
     * @throws APIException
     */
    public void setServerId(String id) throws APIException;
    
    
    /**
     * Create a new SyncClass
     * @param SyncClass The SyncClass to create
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization"})
    public void createSyncClass(SyncClass syncClass) throws APIException;
    
    /**
     * Update a SyncClass
     * @param SyncClass The SyncClass to update
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization"})
    public void updateSyncClass(SyncClass syncClass) throws APIException;
    
    /**
     * Delete a SyncClass
     * @param SyncClass The SyncClass to delete
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization"})
    public void deleteSyncClass(SyncClass syncClass) throws APIException;

    /**
     * 
     * @param uuid of the SyncClass to retrieve
     * @return SyncClass The SyncClass or null if not found
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization"})
    @Transactional(readOnly=true)
    public SyncClass getSyncClass(Integer syncClassId) throws APIException;

    /**
     * 
     * @return SyncClass The latest SyncClass or null if not found
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization"})
    @Transactional(readOnly=true)
    public List<SyncClass> getSyncClasses() throws APIException;


    
    
    /**
     * Dumps the entire database, much like what you'd get from the mysqldump command, and
     * adds a few lines to set the child's GUID, and delete sync history 
     * 
     * @param uuidForChild if not null, use this as the uuid for the child server, otherwise autogenerate one 
     * @param out write the sql here
     * @throws APIException
     */
    //@Authorized({"Backup Entire Database"})
    @Transactional(readOnly=true)
    public void createDatabaseForChild(String uuidForChild, OutputStream out) throws APIException;

    /**
     * Deletes instance of synchronizable from data storage.
     * 
     * @param o instance to delete
     * @throws APIException
     */
    //@Authorized({"Manage Synchronization"})
    @Transactional
    public void deleteSynchronizable(Synchronizable o) throws APIException;


    /**
     * Exposes ability to change persistence flush semantics.
     * 
     * @throws APIException
     * 
     * @see org.openmrs.module.sync.api.db.SynchronizationDAO#setFlushModeManual()
     */
    public void setFlushModeManual() throws APIException;
    
    /**
     * Exposes ability to change persistence flush semantics.
     * 
     * @throws APIException
     * 
     * @see org.openmrs.module.sync.api.db.SynchronizationDAO#setFlushModeAutomatic()
     */
    public void setFlushModeAutomatic() throws APIException;
    
    public void flushSession() throws APIException;

    /**
     * Processes save/update to instance of Synchronizable by persisting it into local persistance store.
     * 
     * @param object instance of Synchronizable to be processed.
     * @return
     * @throws APIException
     */
    public void saveOrUpdate(OpenmrsObject object)  throws APIException;
    

    /**
     * Performs generic save of openmrs object using persistance api.
     * 
     * @param fromDate start date
     * @param toDate end date
     * @return
     * @throws DAOException
     */
    public Map<RemoteServer,Set<SyncStatistic>> getSyncStatistics(Date fromDate, Date toDate) throws DAOException;
    
    public boolean checkUuidsForClass(Class clazz) throws APIException;
}
