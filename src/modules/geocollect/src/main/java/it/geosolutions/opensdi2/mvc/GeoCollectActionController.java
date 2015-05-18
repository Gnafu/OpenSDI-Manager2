/*
 *  OpenSDI Manager 2
 *  Copyright (C) 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.opensdi2.mvc;

import it.geosolutions.geocollect.model.http.CommitResponse;
import it.geosolutions.geocollect.model.http.Status;
import it.geosolutions.opensdi2.configurations.controller.OSDIModuleController;
import it.geosolutions.opensdi2.configurations.exceptions.OSDIConfigurationException;
import it.geosolutions.opensdi2.configurations.model.OSDIConfigurationKVP;
import it.geosolutions.opensdi2.workflow.ActionBlock;
import it.geosolutions.opensdi2.workflow.ActionSequence;
import it.geosolutions.opensdi2.workflow.BaseAction;
import it.geosolutions.opensdi2.workflow.BlockConfiguration;
import it.geosolutions.opensdi2.workflow.WorkflowContext;
import it.geosolutions.opensdi2.workflow.WorkflowException;
import it.geosolutions.opensdi2.workflow.WorkflowStatus;
import it.geosolutions.opensdi2.workflow.action.DataStoreConfiguration;
import it.geosolutions.opensdi2.workflow.action.FeatureUpdater;
import it.geosolutions.opensdi2.workflow.action.FeatureUpdaterConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.opengis.filter.identity.FeatureId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
/**
 * Controller for a GeoCollect Action 
 * 
 * @author Lorenzo Natali (lorenzo.natali at geo-solutions.it)
 * 
 */
@Controller
@RequestMapping("/geocollect/action")
@PreAuthorize("!hasRole('ROLE_ANONYMOUS')")
public class GeoCollectActionController extends OSDIModuleController {
	
    @Autowired
    private ApplicationContext appContext;
    
	/**
	 * Context object to store data to
	 */
	private WorkflowContext ctx;
	
	public static String GEOCOLLECT_SCOPEID = "geocollect";
	
	/**
	 * DataStoreConfiguration to use
	 */
	@Autowired
	private DataStoreConfiguration action3Config;
	
	/**
	 * Custom IDs for the workflow
	 */
	private static final String INPUT_ID = "input";
	private static final String WRITER_ID = "writer";
	private static final String OUTPUTIDS = "outputids";

	/**
	 * Available actions
	 */
	public static final String STORE_FEATURE_ACTION = "store";

	/**
	 * Mapping of the various configured actions to run
	 */
	@Autowired
	private Map<String, ActionSequence> actionsMapping;
	
	/**
	 * Mapping of the various configurable actions
	 */
	@Autowired
	private Map<String, BaseAction> configurableAction;
	
	/**
	 * Logger
	 */
	private final static Logger LOGGER = Logger.getLogger(GeoCollectActionController.class);
	
	@RequestMapping(value = "/{action}/configuration", method = { RequestMethod.GET })
	public @ResponseBody Object getConfigurationList(
	       @PathVariable("action") String action){

	    
	    BlockConfiguration ret = null;
	    LOGGER.info("Received call for Action Configuration:" + action);
	    
	    if(configurableAction.get(action) != null){
    	    ret =  configurableAction.get(action).getConfiguration();
    	    
    	    if(ret != null){
    	        return ret;
    	    }
	    }
	    
	    for(String ba_idx : configurableAction.keySet()){
	        BaseAction ba = configurableAction.get(ba_idx);
	        if(ba != null && ba instanceof FeatureUpdater){
	            
	            ret = ba.getConfiguration();
	            if(ret != null){
	                
	                LOGGER.info("Retrieved BlockConfiguration of "+ba_idx);

    	            try {
    	                
                        OSDIConfigurationKVP actionConfig = (OSDIConfigurationKVP) depot.loadExistingConfiguration(GEOCOLLECT_SCOPEID, ba.getId());
                        
                        if(ret instanceof FeatureUpdaterConfiguration){
                            
                            FeatureUpdaterConfiguration fconfig = (FeatureUpdaterConfiguration) ret;
                            
                            Map<String,String> rules = new HashMap<String, String>();
                            
                            for(String rule_key : actionConfig.getAllKeys()){
                                
                                if(actionConfig.getValue(rule_key) instanceof String){
                                
                                    rules.put(rule_key, (String) actionConfig.getValue(rule_key));
                                }
                                
                            }
                            
                            fconfig.setRules(rules);
                            return fconfig;
                        }
                        
                        LOGGER.info("weeel done");
                        
                    } catch (OSDIConfigurationException e) {
                        LOGGER.warn("Error Loading configuration", e);
                    }
    	            
    	            return ret;
	            }
            } 
	    }
	    
	    return ret;
    }
	
	/**
	 * 
	 * @param action
	 * @return
	 */
    @RequestMapping(value = "/{action}/configuration", method = { RequestMethod.POST })
    public @ResponseBody Object setActionConfiguration(
           @PathVariable("action") String action,
           @RequestBody JSONObject body){

        LOGGER.info(body.toJSONString());
        
        Map<String, Object> rulesObj = (Map<String, Object>) body.get("rules");
       
        if(rulesObj == null){
            
            // Rules not found
            CommitResponse r = new CommitResponse();
            r.setMessage("Missing \"rules\" object");
            r.setStatus(Status.ERROR);
            return r;
        }
        
        
        BlockConfiguration ret = null;
        LOGGER.info("Received call to set Action Configuration:" + action);
        
        if(configurableAction.get(action) != null){
            ret =  configurableAction.get(action).getConfiguration();
            
            if(ret == null){
                
                for(String ba_idx : configurableAction.keySet()){
                    BaseAction ba = configurableAction.get(ba_idx);
                    if(ba != null && ba instanceof FeatureUpdater){
                        ret = ba.getConfiguration();
                        
                        OSDIConfigurationKVP newConfig = new OSDIConfigurationKVP(GEOCOLLECT_SCOPEID, ba.getId());
                        
                        for(Object s : rulesObj.keySet()){
                            if(s != null){
                                newConfig.addNew(s.toString(), rulesObj.get(s));
                            }
                        } 
                        
                        try {
                            depot.addNewConfiguration(newConfig);
                        } catch (OSDIConfigurationException e) {
                            try {
                                depot.updateExistingConfiguration(newConfig);
                            } catch (OSDIConfigurationException e1) {
                                // ignore
                            }
                        }
                        
                        CommitResponse r = new CommitResponse();
                        r.setStatus(Status.SUCCESS);
                        return r;
                        
                    }
                }
                
            }
            
            if(ret != null){
                
                LOGGER.info("Retrieved BlockConfiguration of "+action);
                
                if(ret instanceof FeatureUpdaterConfiguration){
                    
                    LOGGER.info("Got a FeatureUpdaterConfiguration");
                }

                CommitResponse r = new CommitResponse();
                r.setStatus(Status.SUCCESS);
                return r;
            }
            
        }
        
        // Action not found
        CommitResponse r = new CommitResponse();
        r.setStatus(Status.ERROR);
        return r;
    }
	
	
	@RequestMapping(value = "/{action}", method = { RequestMethod.GET,	RequestMethod.POST })
	public @ResponseBody Object performAction(
			@PathVariable("action") String action,
			@RequestBody JSONObject body){
		/* get user details
		 * UserDetails userDetails =
				 (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			
		*/

		// TODO: action "in carico" : set the corresponding feature "working_user" to the username
		//LOGGER.info(body);
		LOGGER.info("Received call for Action " + action);
		/*
		for(String s : actionsMapping.keySet()){
			LOGGER.info(s);
		}
		*/

		if(actionsMapping.containsKey(action)){
			LOGGER.info("Action Exists");
		
			// store the feature using a chain of Actions
			// create a SimpleFeature
			// Manipulate it (do nothing)
			// Store to the DataStore
			// Send mail
			
			try {
			
				// setup the workflow
				ctx = new WorkflowContext();
				
				LOGGER.info(body.toJSONString());
				
				// add the input
				ctx.addContextElement(INPUT_ID, body.toJSONString());
				
				// execute the action sequence
				ActionSequence sequence = actionsMapping.get(action);

				
		        for(ActionBlock actionBlock : sequence.getActions()){
		            
		            if(actionBlock != null && actionBlock instanceof FeatureUpdater){
		                
		                BlockConfiguration ret = actionBlock.getConfiguration();
		                if(ret != null){
		                    
		                    LOGGER.info("Retrieved BlockConfiguration of "+actionBlock.getId());

		                    try {
		                        
		                        OSDIConfigurationKVP actionConfig = (OSDIConfigurationKVP) depot.loadExistingConfiguration(GEOCOLLECT_SCOPEID, actionBlock.getId());
		                        
		                        if(ret instanceof FeatureUpdaterConfiguration){
		                            
		                            FeatureUpdaterConfiguration fconfig = (FeatureUpdaterConfiguration) ret;
		                            
		                            Map<String,String> rules = new HashMap<String, String>();
		                            
		                            for(String rule_key : actionConfig.getAllKeys()){
		                                
		                                if(actionConfig.getValue(rule_key) instanceof String){
		                                
		                                    rules.put(rule_key, (String) actionConfig.getValue(rule_key));
		                                }
		                                
		                            }
		                            
		                            fconfig.setRules(rules);

		                        }
		                        
		                        LOGGER.info("weeel done");
		                        
		                    } catch (OSDIConfigurationException e) {
		                        LOGGER.warn("Error Loading configuration", e);
		                    }

		                }
		            } 
		        }

				
				sequence.execute(ctx);
				
				if(WorkflowStatus.Status.COMPLETED == ctx.getStatusElements().get(WRITER_ID).getCurrentStatus()){
					
					// SUCCESS
					CommitResponse r = new CommitResponse();
					
					// datastorewriter stores the output feature ids in the "outputid" context element
					Object outputids = ctx.getContextElements().get(OUTPUTIDS);
					if(outputids != null && outputids instanceof List){
						
						List<FeatureId> outList = (List<FeatureId>)outputids;
						if(!outList.isEmpty()){
							// There should be only one ID
							r.setId(outList.get(0).getID());
						}

						r.setStatus(Status.SUCCESS);
						
						return r;
					}
					
				}else{
					// FAIL!
					
					// this will go through and the ERROR to be returned
				}
				
			} catch (WorkflowException wfe) {
				// TODO Auto-generated catch block
				wfe.printStackTrace();
			}
			
		}else{
			LOGGER.info("Action DOES NOT Exists");
		}
		
		CommitResponse r = new CommitResponse();
		r.setId("2");
		
		r.setStatus(Status.ERROR);
		return r;
	}

	public Map<String, ActionSequence> getActionsMapping() {
		return actionsMapping;
	}

	public void setActionsMapping(Map<String, ActionSequence> actionsMapping) {
		this.actionsMapping = actionsMapping;
	}

    @Override
    public String getInstanceID(HttpServletRequest req) {
        return "";
    }
}
