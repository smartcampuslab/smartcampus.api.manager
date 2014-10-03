/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.api.manager.proxy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import eu.trentorise.smartcampus.api.manager.model.Api;
import eu.trentorise.smartcampus.api.manager.model.IPAccessControl;
import eu.trentorise.smartcampus.api.manager.model.Policy;
import eu.trentorise.smartcampus.api.manager.model.Quota;
import eu.trentorise.smartcampus.api.manager.model.RequestHandlerObject;
import eu.trentorise.smartcampus.api.manager.model.Resource;
import eu.trentorise.smartcampus.api.manager.model.SpikeArrest;
import eu.trentorise.smartcampus.api.manager.model.VerifyAppKey;
import eu.trentorise.smartcampus.api.manager.model.proxy.PolicyQuota;
import eu.trentorise.smartcampus.api.manager.persistence.PersistenceManager;
import eu.trentorise.smartcampus.api.manager.persistence.PersistenceManagerProxy;

/**
 * Class that retrieves policies of api and its resources.
 * 
 * @author Giulia Canobbio
 *
 */
@Component
@Transactional
public class PolicyDecisionPoint {
	
	/**
	 * Instance of {@link Logger}.
	 */
	private static final Logger logger = LoggerFactory.getLogger(PolicyDecisionPoint.class);
	/**
	 * Instance of {@link PersistenceManager}.
	 */
	@Autowired
	private PersistenceManager manager;
	/**
	 * Instance of {@link PersistenceManagerProxy}.
	 */
	@Autowired
	private PersistenceManagerProxy proxyManager;
	
	/**
	 * Retrieves api policies and resource policies.
	 * 
	 * @return list of instance {@link Policy}
	 */
	private List<Policy> policiesList(String apiId, String resourceId) {
		// logger.info("policiesList() - ApiId: {}", apiId);

		List<Policy> pToApply = new ArrayList<Policy>();
		boolean qfound = false, spfound = false, ipfound = false, vappfound = false;
		
		// api policies
		try {
			Api api = manager.getApiById(apiId);

			// resource policies
			if (resourceId != null) {

				Resource r = manager.getResourceApiByResourceId(apiId,
						resourceId);
				// retrieve policy resource
				List<Policy> rplist = r.getPolicy();
				if (rplist != null && rplist.size() > 0) {
					pToApply.addAll(rplist);
				}
				
				//check which type of policy are added to list for resource
				if (listContainsClass(pToApply,"Quota")) {
					qfound=true;
				}
				if (listContainsClass(pToApply,"Spike Arrest")) {
					spfound=true;
				}
				if (listContainsClass(pToApply,"IP Access Control")) {
					ipfound=true;
				}
				if (listContainsClass(pToApply,"Verify App Key")) {
					vappfound=true;
				}

			}
			// api policies

			List<Policy> policies = api.getPolicy();
			if (policies != null && policies.size() > 0) {
				for (int i = 0; i < policies.size(); i++) {
					if (policies.get(i) instanceof Quota && !qfound) {
						pToApply.add(policies.get(i));
					} 
					if (policies.get(i) instanceof SpikeArrest && !spfound) {
						pToApply.add(policies.get(i));
					}
					if (policies.get(i) instanceof IPAccessControl && !ipfound) {
						pToApply.add(policies.get(i));
					}
					if (policies.get(i) instanceof VerifyAppKey && !vappfound) {
						pToApply.add(policies.get(i));
					}
				}

			}
	
		} catch (NullPointerException n) {
			logger.info("No policies for this api {}", apiId);
			//throw new IllegalArgumentException("No policies for this api "+ apiId);
		}

		return pToApply;
	}
	
	private boolean listContainsClass(List<Policy> list, String type){
		for(int i=0;i<list.size();i++){
			//Quota
			if(type.equalsIgnoreCase("Quota")){
				if(list.get(i) instanceof Quota){
					return true;
				}
			}
			//Spike Arrest
			if(type.equalsIgnoreCase("Spike Arrest")){
				if(list.get(i) instanceof SpikeArrest){
					return true;
				}
			}
			//IP Access Control
			if(type.equalsIgnoreCase("IP Access Control")){
				if(list.get(i) instanceof IPAccessControl){
					return true;
				}
			}
			//Verify App Key
			if(type.equalsIgnoreCase("Verify App Key")){
				if(list.get(i) instanceof VerifyAppKey){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Apply policy logic to the request.
	 * 
	 * @param obj : instance of {@link RequestHandlerObject}
	 */
	public void applyPoliciesBatch(RequestHandlerObject obj){
		
		String apiId = obj.getApiId();
		String resourceId = obj.getResourceId();
		String appId = obj.getAppId();
		Map<String, String> headers = obj.getHeaders();

		List<Policy> pToApply = policiesList(apiId, resourceId);
		
		PolicyDatastoreBatch batch = new PolicyDatastoreBatch();
		
		if (pToApply != null && pToApply.size() > 0) {
			for (int i = 0; i < pToApply.size(); i++) {
				
				if(pToApply.get(i) instanceof Quota){
					//init a count 0 to avoid concurrency problem
					initQuotaApplyElement(apiId, resourceId, appId);
					
					QuotaApply qa = new QuotaApply(apiId, resourceId, appId,(Quota)pToApply.get(i));
					qa.setPManager(proxyManager);
					qa.setManager(manager);
					batch.add(qa);
				}
				else if(pToApply.get(i) instanceof SpikeArrest){
					SpikeArrestApply saa = new SpikeArrestApply(apiId,resourceId,appId,
							(SpikeArrest)pToApply.get(i));
					saa.setPManager(proxyManager);
					batch.add(saa);
				}
				else if(pToApply.get(i) instanceof IPAccessControl){
					String appIp = headers.get("X-FORWARDED-FOR");
					IPAccessControlApply ipa = new IPAccessControlApply(apiId, resourceId, 
							(IPAccessControl)pToApply.get(i), appIp);
					batch.add(ipa);
				}
				else if(pToApply.get(i) instanceof VerifyAppKey){
					String appSecret = headers.get("appSecret");
					VerifyAppKeyApply vapp = new VerifyAppKeyApply(apiId, resourceId,appId,
							(VerifyAppKey)pToApply.get(i), appSecret);
					vapp.setManager(manager);
					batch.add(vapp);
				}
			}
			//apply policies
			try{
				batch.apply();
			}catch(SecurityException s){
				String msg = s.getMessage();
				logger.info("Cause of security exception: {}",msg);
				//roolback TODO
				if(msg.contains("Quota")){
					logger.info("Quota deny");
				}
				if(msg.contains("Spike Arrest")){
					logger.info("Spike Arrest deny");
				}
				//exception
				if(resourceId==null)
					throw new SecurityException("You are not allowed to access this api. "
							+s.getMessage());
				else
					throw new SecurityException("You are not allowed to access this resource. "
							+s.getMessage());
			}
			
		} else {
			logger.info("Access -> GRANT");
			//throw new IllegalArgumentException("There is no policies to apply");
		}
	}
	
	/**
	 * Initialize table of Quota Apply with a new count.
	 * Count element is set to zero.
	 * 
	 * @param apiId : String
	 * @param resourceId : String
	 * @param appId : String
	 */
	private void initQuotaApplyElement(String apiId, String resourceId, String appId){
		PolicyQuota p = null;
		if(appId!=null){
			p = proxyManager.retrievePolicyQuotaByParamIds(apiId, resourceId, appId);
		}else{
			p = proxyManager.retrievePolicyQuotaByParams(apiId, resourceId);
		}
		if(p==null){
			PolicyQuota pq = new PolicyQuota();
			pq.setApiId(apiId);
			pq.setAppId(appId);
			pq.setResourceId(resourceId);
			pq.setCount(0);
			pq.setTime(new Date());
			//for callback
			//pq.setState("initial");
			proxyManager.addPolicyQuota(pq);
		}
	}
	
	/**
	 * Initialize table of Spike Arrest Apply.
	 * 
	 * @param apiId : String
	 * @param resourceId : String
	 * @param appId : String
	 */
	/*private void initSpikeArrestApplyElement(String apiId, String resourceId, String appId){
		LastTime sp = null;
		if(appId!=null){
			sp = proxyManager.retrievePolicySpikeArrestByApiAndRAndAppId(apiId, resourceId, appId);
		}else{
			sp = proxyManager.retrievePolicySpikeArrestByApiAndResouceId(apiId, resourceId);
		}
		if(sp==null){
			sp = new LastTime();
			sp.setApiId(apiId);
			sp.setAppId(appId);
			sp.setResourceId(resourceId);
			sp.setTime(new Date());
			proxyManager.addPolicySpikeArrest(sp);
		}
	}*/
}
