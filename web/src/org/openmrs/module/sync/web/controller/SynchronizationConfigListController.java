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
package org.openmrs.module.sync.web.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.sync.SyncClass;
import org.openmrs.module.sync.SyncConstants;
import org.openmrs.module.sync.SyncSource;
import org.openmrs.module.sync.SyncSourceJournal;
import org.openmrs.module.sync.SyncTransmission;
import org.openmrs.module.sync.SyncUtil;
import org.openmrs.module.sync.SyncUtilTransmission;
import org.openmrs.module.sync.api.SynchronizationService;
import org.openmrs.module.sync.serialization.TimestampNormalizer;
import org.openmrs.module.sync.server.RemoteServer;
import org.openmrs.module.sync.server.RemoteServerType;
import org.openmrs.module.sync.server.ServerConnectionState;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.web.WebConstants;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

public class SynchronizationConfigListController extends SimpleFormController {

    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
     *      org.springframework.web.bind.ServletRequestDataBinder)
     */
    protected void initBinder(HttpServletRequest request,
            ServletRequestDataBinder binder) throws Exception {
        super.initBinder(request, binder);
    }
    
	@Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj, BindException errors) throws Exception {

		log.debug("in processFormSubmission");
		
    	ModelAndView result = new ModelAndView(new RedirectView(getSuccessView()));
    	
        if (!Context.isAuthenticated())
            throw new APIAuthenticationException("Not authenticated!");
        
        HttpSession httpSession = request.getSession();
        String success = "";
        String error = "";
        MessageSourceAccessor msa = getMessageSourceAccessor();
        
        String action = ServletRequestUtils.getStringParameter(request, "action", "");
        
        log.debug("action is " + action);
        
        if ( "saveParent".equals(action) ) {
        	//System.out.println("\n\nSTARTED IS " + request.getParameterValues("started")[0]);
        	
        	String address = ServletRequestUtils.getStringParameter(request, "address", "");
        	String nickname = ServletRequestUtils.getStringParameter(request, "nickname", "");
        	String username = ServletRequestUtils.getStringParameter(request, "username", "");
        	String password = ServletRequestUtils.getStringParameter(request, "password", "");
            String uuid = ServletRequestUtils.getStringParameter(request, "parentUuid", "");
    		String[] startedParams = request.getParameterValues("started");
    		boolean started = false;
    		if ( startedParams != null ) {
	    		for ( String startedParam : startedParams ) {
	    			if ( startedParam.equals("true") ) started = true;
	    		}
    		}
        	Integer repeatInterval = ServletRequestUtils.getIntParameter(request, "repeatInterval", 0) * 60;  // interval really is in seconds, to * 60 to convert to minutes 
        	
        	if ( password.length() == 0 ) error = msa.getMessage("SynchronizationConfig.parent.error.passwordRequired");
        	if ( username.length() == 0 ) error = msa.getMessage("SynchronizationConfig.parent.error.usernameRequired");
        	if ( address.length() == 0 ) error = msa.getMessage("SynchronizationConfig.parent.error.addressRequired");
        	if ( started && repeatInterval == 0 ) error = msa.getMessage("SynchronizationConfig.parent.error.invalidRepeat");
        	
        	if ( error.length() == 0 ) {
            	RemoteServer parent = Context.getService(SynchronizationService.class).getParentServer();

            	if ( parent == null ) {
            		parent = new RemoteServer();
            	}
        		parent.setAddress(address);
                // this is special for parent - will always be "Parent"
        		parent.setNickname(RemoteServerType.PARENT.toString());
        		parent.setUsername(username);
        		parent.setPassword(password);
        		parent.setServerType(RemoteServerType.PARENT);
                if ( uuid.length() > 0 ) parent.setUuid(uuid);

        		if ( parent.getServerId() == null ) {
            		Context.getService(SynchronizationService.class).createRemoteServer(parent);
        		} else {
            		Context.getService(SynchronizationService.class).updateRemoteServer(parent);
        		}
        		
    	        // also set TaskConfig for scheduling
    	        if ( parent.getServerId() != null ) {
            		TaskDefinition parentSchedule = null;
        	        Collection<TaskDefinition> tasks = Context.getSchedulerService().getRegisteredTasks();
	        		
        	        String serverId = parent.getServerId().toString();
    	        	if ( tasks != null ) {
    	            	for ( TaskDefinition task : tasks ) {
    	            		if ( task.getTaskClass().equals(SyncConstants.SCHEDULED_TASK_CLASS) ) {
    	            			if ( serverId.equals(task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID)) ) {
    	            				parentSchedule = task;
    	            			}
    	            		}
    	            	}
    	        	}

    	        	Map<String,String> props = new HashMap<String,String>();
    	        	props.put(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID, serverId);
    	        	if ( parentSchedule != null ) {
    	        		Context.getSchedulerService().shutdownTask(parentSchedule);
    	        		parentSchedule.setStarted(started);
    	        		parentSchedule.setRepeatInterval((long)repeatInterval);
    	        		parentSchedule.setStartOnStartup(started);
    	        		parentSchedule.setProperties(props);
    	        		if ( started ) {
    	        			parentSchedule.setStartTime(new Date());
    	        		}
    	        		Context.getSchedulerService().saveTask(parentSchedule);
    	        		if ( started ) {
    	        			Context.getSchedulerService().scheduleTask(parentSchedule);
    	        		}
    	        	} else {
    	        		if ( started ) {
        	        		parentSchedule = new TaskDefinition();
        	        		parentSchedule.setName(msa.getMessage(SyncConstants.DEFAULT_PARENT_SCHEDULE_NAME));
        	        		parentSchedule.setDescription(msa.getMessage(SyncConstants.DEFAULT_PARENT_SCHEDULE_DESCRIPTION));
        	        		parentSchedule.setRepeatInterval((long)repeatInterval);
        	        		parentSchedule.setStartTime(new Date());
        	        		parentSchedule.setTaskClass(SyncConstants.SCHEDULED_TASK_CLASS);
        	        		parentSchedule.setStarted(started);
        	        		parentSchedule.setStartOnStartup(started);
        	        		parentSchedule.setProperties(props);
        	        		Context.getSchedulerService().saveTask(parentSchedule);
       	        			Context.getSchedulerService().scheduleTask(parentSchedule);
    	        		}
    	        	}
    	        }
        		
        		success = msa.getMessage("SynchronizationConfig.parent.saved");        		
        	}
        } else if ( "saveClasses".equals(action) ) {
        	// save uuid, server name, and admin email first
        	String serverUuid = ServletRequestUtils.getStringParameter(request, "serverUuid", "");
        	String serverId = ServletRequestUtils.getStringParameter(request, "serverId", "");
        	String serverName = ServletRequestUtils.getStringParameter(request, "serverName", "");
        	String adminEmail = ServletRequestUtils.getStringParameter(request, "serverAdminEmail", "");
        	
        	if ( serverUuid.length() > 0 ) Context.getService(SynchronizationService.class).setServerUuid(serverUuid);
        	if ( serverId.length() > 0 ) Context.getService(SynchronizationService.class).setServerId(serverId);
        	if (serverName.length() > 0 ) Context.getService(SynchronizationService.class).setServerName(serverName);
        	if (adminEmail.length() > 0 ) SyncUtil.setAdminEmail(adminEmail);
        	
            String[] classIdsTo = ServletRequestUtils.getRequiredStringParameters(request, "toDefault");
            String[] classIdsFrom = ServletRequestUtils.getRequiredStringParameters(request, "fromDefault");
            Set<String> idsTo = new HashSet<String>();
            Set<String> idsFrom = new HashSet<String>();
            if ( classIdsTo != null ) idsTo.addAll(Arrays.asList(classIdsTo));
            if ( classIdsFrom != null ) idsFrom.addAll(Arrays.asList(classIdsFrom));
            
            List<SyncClass> syncClasses = Context.getService(SynchronizationService.class).getSyncClasses();
            if ( syncClasses != null ) {
                //log.warn("SYNCCLASSES IS SIZE: " + syncClasses.size());
                for ( SyncClass syncClass : syncClasses ) {
                    if ( idsTo.contains(syncClass.getSyncClassId().toString()) ) syncClass.setDefaultTo(true);
                    else syncClass.setDefaultTo(false);
                    if ( idsFrom.contains(syncClass.getSyncClassId().toString()) ) syncClass.setDefaultFrom(true);
                    else syncClass.setDefaultFrom(false);
                    Context.getService(SynchronizationService.class).updateSyncClass(syncClass);
                }
            }

            success = msa.getMessage("SynchronizationConfig.classes.saved");             
        } else if ( "deleteServer".equals(action) ) {
            // check to see if the user is trying to delete a server, react accordingly
            Integer serverId = ServletRequestUtils.getIntParameter(request, "serverId", 0);
            String serverName = "Server " + serverId.toString();

            SynchronizationService ss = Context.getService(SynchronizationService.class);
            
            if ( serverId > 0 ) {
            	RemoteServer deleteServer = ss.getRemoteServer(serverId);
            	serverName = deleteServer.getNickname();

            	try {
            		ss.deleteRemoteServer(deleteServer);
            		Object[] args = {serverName};
                    success = msa.getMessage("SynchronizationConfig.server.deleted", args);             
            	} catch (Exception e) {
            		Object[] args = {serverName};
            		error = msa.getMessage("SynchronizationConfig.server.deleteFailed", args);
            	}
            } else {
            	error = msa.getMessage("SynchronizationConfig.server.notDeleted");
            }

        } else if ( "manualTx".equals(action ) ) {
            try {
                Integer serverId = ServletRequestUtils.getIntParameter(request, "serverId", 0);
                RemoteServer server = Context.getService(SynchronizationService.class).getRemoteServer(serverId);
                
                log.warn("IN MANUAL-TX WITH SERVERID: " + serverId);
                
                // we are creating a sync-transmission, so start by generating a SyncTransmission object
                SyncTransmission tx = SyncUtilTransmission.createSyncTransmission(server);
                String toTransmit = tx.getFileOutput();

                // Record last attempt
                server.setLastSync(new Date());
                Context.getService(SynchronizationService.class).updateRemoteServer(server);
                
                // Write sync transmission to response
                InputStream in = new ByteArrayInputStream(toTransmit.getBytes());
                response.setContentType("text/xml; charset=utf-8");
                response.setHeader("Content-Disposition", "attachment; filename=" + tx.getFileName() + ".xml");
                OutputStream out = response.getOutputStream();
                IOUtils.copy(in, out);
                out.flush();
                out.close();

                // don't return a model/view - we'll need to return a file instead.
                result = null;
            } catch(Exception e) {
                error = msa.getMessage("SynchronizationStatus.createTx.error");  
                e.printStackTrace();
            }

        }
        
        if (!success.equals(""))
            httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, success);
        
        if (!error.equals(""))
            httpSession.setAttribute(WebConstants.OPENMRS_ERROR_ATTR, error);
        
		return result;
	}

    /**
     * 
     * This is called prior to displaying a form for the first time. It tells
     * Spring the form/command object to load into the request
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    protected Object formBackingObject(HttpServletRequest request)
            throws ServletException {
        Map<String,Object> obj = new HashMap<String,Object>();
        List<RemoteServer> serverList = new ArrayList<RemoteServer>();

        // only fill the Object if the user has authenticated properly
        if (Context.isAuthenticated()) {
            SynchronizationService ss = Context.getService(SynchronizationService.class);

            serverList.addAll(ss.getRemoteServers());
            obj.put("serverList", serverList);
            
            SyncSource source = new SyncSourceJournal();
            obj.put("localServerUuid",source.getSyncSourceUuid());
            obj.put("localServerSyncStatus", source.getSyncStatus());           
        }

        return obj;
    }

	@SuppressWarnings("unchecked")
    @Override
    protected Map referenceData(HttpServletRequest request, Object obj, Errors errors) throws Exception {
		Map<String,Object> ret = new HashMap<String,Object>();

		if ( Context.isAuthenticated() ) {
	        //cast
	        Map<String,Object> ref = (Map<String,Object>)obj; 
	        
			// the parent server
	        List<RemoteServer> serverList = (ArrayList<RemoteServer>)ref.get("serverList");
	        RemoteServer parent = null;
	        
	        for ( RemoteServer server : serverList ) {
	        	if ( server.getServerType().equals(RemoteServerType.PARENT)) {
	        		parent = server;
	        	}
	        }
			
	        // testConnection error messages
	        MessageSourceAccessor msa = getMessageSourceAccessor();
	        Map<String,String> connectionState = new HashMap<String,String>();
	        connectionState.put(ServerConnectionState.OK.toString(), msa.getMessage("SynchronizationConfig.server.connection.status.ok"));
	        connectionState.put(ServerConnectionState.AUTHORIZATION_FAILED.toString(), msa.getMessage("SynchronizationConfig.server.connection.status.noAuth"));
	        connectionState.put(ServerConnectionState.CONNECTION_FAILED.toString(), msa.getMessage("SynchronizationConfig.server.connection.status.noConnection"));
	        connectionState.put(ServerConnectionState.CERTIFICATE_FAILED.toString(), msa.getMessage("SynchronizationConfig.server.connection.status.noCertificate"));
	        connectionState.put(ServerConnectionState.MALFORMED_URL.toString(), msa.getMessage("SynchronizationConfig.server.connection.status.badUrl"));
	        connectionState.put(ServerConnectionState.NO_ADDRESS.toString(), msa.getMessage("SynchronizationConfig.server.connection.status.noAddress"));
	        
	        // taskConfig for automated syncing
	        TaskDefinition parentSchedule = new TaskDefinition();
	        String repeatInterval = "";
	        if ( parent != null ) {
	        	Collection<TaskDefinition> tasks = Context.getSchedulerService().getRegisteredTasks();
	        	if ( tasks != null ) {
	        		String serverId = parent.getServerId().toString();
	            	for ( TaskDefinition task : tasks ) {
	            		if ( task.getTaskClass().equals(SyncConstants.SCHEDULED_TASK_CLASS) ) {
	            			if ( serverId.equals(task.getProperty(SyncConstants.SCHEDULED_TASK_PROPERTY_SERVER_ID)) ) {
	            				parentSchedule = task;
	            				Long repeat = parentSchedule.getRepeatInterval() / 60;
	            				repeatInterval = repeat.toString();
	            				if ( repeatInterval.indexOf(".") > -1 ) repeatInterval = repeatInterval.substring(0, repeatInterval.indexOf("."));
	            			}
	            		}
	            	}
	        	}
	        }
            
            Map<String,List<SyncClass>> syncClassGroups = new HashMap<String,List<SyncClass>>();
            Map<String,List<SyncClass>> syncClassGroupsLeft = new HashMap<String,List<SyncClass>>();
            Map<String,List<SyncClass>> syncClassGroupsRight = new HashMap<String,List<SyncClass>>();
            Map<String,Boolean> syncClassGroupTo = new HashMap<String,Boolean>();
            Map<String,Boolean> syncClassGroupFrom = new HashMap<String,Boolean>();

            List<SyncClass> syncClasses = Context.getService(SynchronizationService.class).getSyncClasses();
            if ( syncClasses != null ) {
                //log.warn("SYNCCLASSES IS SIZE: " + syncClasses.size());
                for ( SyncClass syncClass : syncClasses ) {
                    String type = syncClass.getType().toString();
                    List<SyncClass> currList = syncClassGroups.get(type);
                    if ( currList == null ) {
                        currList = new ArrayList<SyncClass>();
                        syncClassGroupTo.put(type, false);
                        syncClassGroupFrom.put(type, false);
                    }
                    currList.add(syncClass);
                    syncClassGroups.put(type, currList);
                    if ( syncClass.getDefaultTo() ) syncClassGroupTo.put(type, true); 
                    if ( syncClass.getDefaultFrom() ) syncClassGroupFrom.put(type, true); 
                    //log.warn("Added type " + type + " to list, size is now " + currList.size());
                }

                /*
                 * This algorithm is nicer in theory
                int countLeft = 0;
                int countRight = 0;
                for ( Iterator<Map.Entry<String, List<SyncClass>>> it = syncClassGroups.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, List<SyncClass>> entry = it.next();
                    if ( countLeft > countRight ) {
                        syncClassGroupsRight.put(entry.getKey(), entry.getValue());
                        countRight += entry.getValue().size();
                    } else {
                        syncClassGroupsLeft.put(entry.getKey(), entry.getValue());
                        countLeft += entry.getValue().size();
                    }
                }
                */
                // but this one simply is a better end-product
                for ( Iterator<Map.Entry<String, List<SyncClass>>> it = syncClassGroups.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, List<SyncClass>> entry = it.next();
                    if ( entry.getKey().equals("REQUIRED") || entry.getKey().equals("PATIENT") ) {
                        syncClassGroupsLeft.put(entry.getKey(), entry.getValue());
                    } else {
                        syncClassGroupsRight.put(entry.getKey(), entry.getValue());
                    }
                }

            } else {
                //log.warn("SYNCCLASSES CAME BACK NULL");
            }
	        
            ret.put("syncClassGroups", syncClassGroups);
            ret.put("syncClassGroupsLeft", syncClassGroupsLeft);
            ret.put("syncClassGroupsRight", syncClassGroupsRight);
            ret.put("syncClassGroupTo", syncClassGroupTo);
            ret.put("syncClassGroupFrom", syncClassGroupFrom);
	        ret.put("connectionState", connectionState.entrySet());
			ret.put("parent", parent);
	        ret.put("parentSchedule", parentSchedule);
	        ret.put("repeatInterval", repeatInterval);
            ret.put("syncDateDisplayFormat", TimestampNormalizer.DATETIME_DISPLAY_FORMAT);
            
            //sync status staff
            ret.put("localServerSyncStatusValue",SyncUtil.getSyncStatus());
	        ret.put("localServerSyncStatusText", msa.getMessage("SynchronizationConfig.syncStatus.status." + ref.get("localServerSyncStatus").toString()));
            ret.put("localServerSyncStatusMsg", msa.getMessage("SynchronizationConfig.syncStatus.status." + ref.get("localServerSyncStatus").toString() + ".info" , new String[] {SyncConstants.RUNTIMEPROPERTY_SYNC_STATUS}));
	        ret.put("localServerUuid", ref.get("localServerUuid"));
	        ret.put("localServerId", Context.getService(SynchronizationService.class).getServerId());
	        ret.put("localServerName", Context.getService(SynchronizationService.class).getServerName());           
	        ret.put("localServerAdminEmail", SyncUtil.getAdminEmail());           
		}
        
	    return ret;
    }

}