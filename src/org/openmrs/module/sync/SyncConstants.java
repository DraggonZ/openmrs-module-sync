package org.openmrs.module.sync;

import java.text.SimpleDateFormat;

import org.openmrs.module.sync.SynchronizationTask;


/**
 * Common sync consts.
 *
 */
public class SyncConstants {

    public static final SimpleDateFormat SYNC_FILENAME_MASK = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_S"); //used to format file names
	public static final String LAST_SYNC_LOCAL = "synchronization.last_sync_local";
	public static final String LAST_SYNC_REMOTE = "synchronization.last_sync_remote";
	
	public static final String DATA_IMPORT_SERVLET = "/admin/synchronization/synchronizationImport.list";
	
	public static final String TEST_MESSAGE = "test";
	
    public static final int CONNECTION_TIMEOUT_MS = 10000; //http connection timeout in milliseconds
	
	public static final SyncRecordState[] SYNC_TO_PARENT_STATES = {
		SyncRecordState.NEW,	
		SyncRecordState.PENDING_SEND,
		SyncRecordState.SEND_FAILED,
		SyncRecordState.SENT,
		SyncRecordState.SENT_AGAIN,
		SyncRecordState.FAILED,
		SyncRecordState.FAILED_AND_STOPPED //this is here so that we can display the failed record on UI
	};

	// error message codes
	public static final String ERROR_NO_RESPONSE = "SynchronizationStatus.transmission.noResponseError";
	public static final String ERROR_TRANSMISSION_CREATION = "SynchronizationStatus.transmission.createError";
	public static final String ERROR_NO_PARENT_DEFINED = "SynchronizationStatus.transmission.noParentError";
	public static final String ERROR_SEND_FAILED = "SynchronizationStatus.transmission.sendError";
	public static final String ERROR_RESPONSE_NOT_UNDERSTOOD = "SynchronizationStatus.transmission.corruptResponseError";
	public static final String ERROR_AUTH_FAILED = "SynchronizationStatus.transmission.noAuthError";
	public static final String ERROR_TX_NOT_UNDERSTOOD = "SynchronizationStatus.transmission.corruptTxError";
	public static final String ERROR_NO_CONNECTION = "SynchronizationStatus.transmission.noConnectionError";
	public static final String ERROR_INVALID_SERVER = "SynchronizationStatus.transmission.invalidServer";

	// error message codes - at the item/record level
	public static final String ERROR_ITEM_NOT_COMMITTED = "SynchronizationStatus.item.notCommitted";
	public static final String ERROR_ITEM_NOCLASS = "SynchronizationStatus.item.noClassFound";
	public static final String ERROR_ITEM_BADXML_MISSING = "SynchronizationStatus.item.badXml.missing";
	public static final String ERROR_ITEM_UNSET_PROPERTY = "SynchronizationStatus.item.unsetProperty";
	public static final String ERROR_ITEM_UNEXPECTED = "SynchronizationStatus.item.unexpected";
	public static final String ERROR_RECORD_UNEXPECTED = "SynchronizationStatus.record.unexpected";
	
	// error-induced filenames
	public static final String FILENAME_NO_RESPONSE = "no_response_from_server";
	public static final String FILENAME_NOT_CREATED = "unable_to_create_transmission";
	public static final String FILENAME_NO_PARENT_DEFINED = "no_parent_defined";
	public static final String FILENAME_SEND_FAILED = "send_failed";
	public static final String FILENAME_RESPONSE_NOT_UNDERSTOOD = "response_not_understood";
	public static final String FILENAME_AUTH_FAILED = "not_authenticated";
	public static final String FILENAME_TX_NOT_UNDERSTOOD = "transmission_not_understood";
	public static final String FILENAME_NO_CONNECTION = "no_connection";
	public static final String FILENAME_INVALID_SERVER = "invalid_server";
	public static final String FILENAME_TEST = "test";
	
	public static final String GUID_UNKNOWN = "";

	public static final String UTF8 = "UTF-8";
	public static final String POST_METHOD = "POST";
    
	public static final String SYNC_DATA_FILE_PARAM = "syncDataFile";
	
	//global props
    public static final String PROPERTY_SERVER_GUID = "synchronization.server_uuid"; //used internally in sync
    public static final String PROPERTY_SERVER_ID = "synchronization.server_id"; //numeric value used for scoping any local values (such as user_ids)
    public static final String PROPERTY_SERVER_NAME = "synchronization.server_name"; //used for display purposes, nickname
    public static final String PROPERTY_NAME_MAX_RETRY_COUNT = "synchronization.max_retry_count";
    public static final String PROPERTY_SYNC_ADMIN_EMAIL = "synchronization.admin_email";
    public static final String PROPERTY_NAME_MAX_RECORDS = "synchronization.max_records";
    public static final String PROPERTY_NAME_MAX_RECORDS_DEFAULT = "50";
    public static final String PROPERTY_ENABLE_COMPRESSION = "synchronization.enable_compression";
    public static final String PROPERTY_VERSION = "synchronization.version";

    //runtime prop
    public static final String RUNTIMEPROPERTY_SYNC_STATUS = "synchronization.sync_status"; //SyncStatusState enum

	public static final String RESPONSE_SUFFIX = "_response";

	public static final String DIR_IMPORT = "import";
	public static final String DIR_JOURNAL = "journal";

	public static final String SCHEDULED_TASK_CLASS = SynchronizationTask.class.getName();
	public static final String SCHEDULED_TASK_PROPERTY_SERVER_ID = "serverId";

	public static final String DEFAULT_PARENT_SCHEDULE_NAME = "SynchronizationStatus.parent.schedule.default.name";
	public static final String DEFAULT_PARENT_SCHEDULE_DESCRIPTION = "SynchronizationStatus.parent.schedule.default.description";

    public static final String DEFAULT_CHILD_SERVER_USER_GENDER = "M";
    public static final String DEFAULT_CHILD_SERVER_USER_NAME = "SynchronizationConfig.child.user.name";
}
