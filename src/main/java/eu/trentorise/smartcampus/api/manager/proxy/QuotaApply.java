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


import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.smartcampus.api.manager.model.Api;
import eu.trentorise.smartcampus.api.manager.model.ApiData;
import eu.trentorise.smartcampus.api.manager.model.App;
import eu.trentorise.smartcampus.api.manager.model.Quota;
import eu.trentorise.smartcampus.api.manager.model.Status;
import eu.trentorise.smartcampus.api.manager.model.proxy.AccessApi;
import eu.trentorise.smartcampus.api.manager.model.proxy.PolicyQuota;
import eu.trentorise.smartcampus.api.manager.persistence.PersistenceManager;
import eu.trentorise.smartcampus.api.manager.persistence.PersistenceManagerProxy;

/**
 * Class that apply Quota logic.
 * 
 * @author Giulia Canobbio
 *
 */
public class QuotaApply implements PolicyDatastoreApply{
	
	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger logger = LoggerFactory.getLogger(QuotaApply.class);
	//@Autowired
	private PersistenceManagerProxy pmanager;
	private PersistenceManager manager;
	//global variables
	private Quota p;
	private String apiId;
	private String resourceId;
	private String appId;
	
	int interval;
	String timeUnit;
	/*int count;
	int countSilver;
	int countGold;
	int countPlatinum;
	*/
	/**
	 * Constructor.
	 * 
	 * @param p : instance of {@link Quota}
	 */
	public void setQuotaApply(String apiId, String resourceId, String appId, Quota p){
		this.p = p;
		this.apiId = apiId;
		this.resourceId = resourceId;
		this.appId = appId;
	}
	
	public void setPManager(PersistenceManagerProxy pmanager){
		this.pmanager = pmanager;
	}
	
	public void setManager(PersistenceManager manager){
		this.manager = manager;
	}

	@Override
	public void apply() {
		// TODO Auto-generated method stub
		logger.info("Applying quota policy..");
		logger.info("Api id: {}, ",apiId);
		logger.info("Resource id: {}, ",resourceId);
		logger.info("App id: {}, ",appId);
		logger.info("Policy: {}. ",p.getName());
		
		this.interval = p.getInterval();
		this.timeUnit = p.getTimeUnit();
		//boolean decision = decisionApiAppStatus();
		//if(decision){
			decision(appId, resourceId, apiId);
		//}

	}
	
	private int getQuotaValue(){
		int quota = 0;

		// get Status list
		Api api = manager.getApiById(apiId);
		List<Status> status = api.getStatus();

		// get app apiData
		App app = manager.getAppById(appId);
		List<ApiData> list = app.getApis();
		String appApiStatus = "DEFAULT";

		// if apiId is in list - retrieve status
		if (list != null && list.size() > 0) {
			if (list.contains(apiId)) {
				// retrieve status from app data
				for (int i = 0; i < list.size(); i++) {
					// find apiId
					if (list.get(i).getApiId().equalsIgnoreCase(apiId)) {
						appApiStatus = list.get(i).getApiStatus();
					}
				}
			}
		}

		// from api status list retrieves quota
		if (status != null && status.size() > 0 && !appApiStatus.equalsIgnoreCase("DEFAULT")) {
			// retrieves quota
			for (int i = 0; i < status.size(); i++) {
				if (status.get(i).getName().equalsIgnoreCase(appApiStatus)) {
					quota = status.get(i).getQuota();
				}
			}
		}

		return quota;
	}
	
	/*private boolean decisionApiAppStatus(){
		//get Status list
		Api api = manager.getApiById(apiId);
		List<Status> status = api.getStatus();
		
		//get app apiData
		App app = manager.getAppById(appId);
		List<ApiData> list = app.getApis();
		String appApiStatus = "DEFAULT";
		int quota = 0;
		
		//if apiId is in list - retrieve status
		if(list!=null && list.size()>0){
			if(list.contains(apiId)){
				//retrieve status from app data
				for(int i=0;i<list.size();i++){
					//find apiId
					if(list.get(i).getApiId().equalsIgnoreCase(apiId)){
						appApiStatus = list.get(i).getApiStatus();
					}
				}
			}
		}
			
		// from api status list retrieves quota
		if (status != null && status.size() > 0) {
			// retrieves quota
			for (int i = 0; i < status.size(); i++) {
				if (status.get(i).getName().equalsIgnoreCase(appApiStatus)) {
					quota = status.get(i).getQuota();
				}
			}
		}
			
		if (quota != 0) {
			Date today = new Date();
			// retrieve AccessApi data
			AccessApi accessApi = pmanager.retrieveAccessApiByIdParams(apiId,
					appId);
			if (accessApi != null) {
				Date savedDate = accessApi.getTime();
				// check Date - quota is per day
				if (DatesDiff(savedDate, today) >= 86400) {
					// start counter from zero : 1 - update
					accessApi.setCount(1);
					accessApi.setTime(today);
					pmanager.updateAccessApi(accessApi);
					// GRANT
					logger.info("Access api --> GRANT ");
					return true;
				} else {
					// check counter
					int savedCounter = accessApi.getCount();
					if (savedCounter < quota) {
						// update
						accessApi.setCount(savedCounter + 1);
						accessApi.setTime(today);
						pmanager.updateAccessApi(accessApi);
						// GRANT
						logger.info("Access api --> GRANT ");
						return true;
					}
				}
			} else {
				// update
				accessApi = new AccessApi();
				accessApi.setApiId(apiId);
				accessApi.setAppId(appId);
				accessApi.setCount(1);
				accessApi.setTime(today);
				pmanager.updateAccessApi(accessApi);
				logger.info("Access api --> GRANT ");
				return true;
			}

		}
		logger.info("Access api --> DENY ");
		return false;
	}*/
	
	private void decision(String appId, String resourceId, String apiId){
		 	 
		Date currentTime= new Date();
		
		boolean decision = false;
		
		//verifica input (resourceId non può essere null...)
		if(resourceId==null) {
			logger.info("Error: resource id is null");
			try {
				throw new Exception("Resource id cannot be null.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }else{
			   PolicyQuota q = pmanager.retrievePolicyQuotaByParams(apiId, resourceId);
			   
			   if(q!=null){
				   logger.info("Request {} ",resourceId);
				   logger.info("App {}",appId);
				 //Da interval e timeUnit calcolo valore timeLimit
					int t=0;
					//time unit
					if(timeUnit.equalsIgnoreCase("second")){
						t = 1;
					}else if(timeUnit.equalsIgnoreCase("minute")){
						t = 60;
					}else if(timeUnit.equalsIgnoreCase("hour")){
						t = 3600;
					}else if(timeUnit.equalsIgnoreCase("day")){
						t = 86400;
					}else if(timeUnit.equalsIgnoreCase("month")){
						t = 2592000;
					}
					
					int timeLimit =interval*t*1000;  //in milliseconds
					int resourceQuota= getQuotaValue();
						
					if(appId==null){
					    //appId=0;
						decision=QuotaDecision(timeLimit, resourceQuota, apiId, resourceId, 
								currentTime);
					}else{
						decision=QuotaDecision(timeLimit, resourceQuota, apiId,appId, resourceId, 
								currentTime);}
			  }else{ 
				  decision=true;
			  
				  //add new entry for policy quota
				  PolicyQuota pq = new PolicyQuota();
				  pq.setApiId(apiId);
				  pq.setAppId(appId);
				  pq.setResourceId(resourceId);
				  pq.setCount(1);
				  pq.setTime(new Date());
				  pmanager.addPolicyQuota(pq);
			  }
		   }
		
		if(decision)
			logger.info("Quota policy --> GRANT ");
		else
			logger.info("Quota policy --> DENY ");
	}
	
	private boolean QuotaDecision(int timeLimit, int resourceQuota,
			String apiId, String appId, String resourceId, Date currentTime) {

		// oppure possiamo assumere che questa tabella del database sia
		// inizializzata (anche con valori default) in seguito all'iscrizione
		// dell'app
		/*
		 * List<QuotaStatus> quotastatus
		 * =quotastatusr.findByApiIdAndResourceIdAndAppId(apiId,
		 * resourceId,appId); DB_QuotaStatus_update(quotastatus, apiId,
		 * resourceId, appId);
		 */
		// STATUS ASSOCIATO SOLO ALL'APP List<QuotaStatus> quotastatus =
		// quotastatusr.findByAppId(appId);
		// DB_QuotaStatus_update(quotastatus,appId);

		PolicyQuota pq = pmanager.retrievePolicyQuotaByParamIds(apiId,
				resourceId, appId);

		if (pq==null) {
			updatePolicyQuota(pq, apiId, resourceId, appId, currentTime,
					timeLimit);
			return true;
		} else {

			updatePolicyQuota(pq, apiId, resourceId, appId,
					currentTime, timeLimit);

			// String status = quotastatus.get(0).getStatus();
			boolean decision = false;
			// oss: se resourceQuota[1]=0 --> che per quella risorsa non è
			// specificato il count silver --> nessuna app può avere quello
			// status per quella risorsa
			// NOO --> viene preso il valore <= del valore status -->[DA FARE -
			// SI TORNA COME PRIMA]
			/*
			 * switch (status) { case "default": if
			 * (pqlist.get(0).getCount()<=resourceQuota[0]) decision=true;
			 * break; case "silver": if
			 * (pqlist.get(0).getCount()<=resourceQuota[1]) decision=true;
			 * break; case "gold": if
			 * (pqlist.get(0).getCount()<=resourceQuota[2]) decision=true;
			 * break; case "platinum": if
			 * (pqlist.get(0).getCount()<=resourceQuota[3]) decision=true;
			 * break; }
			 */
			if (resourceQuota!=0 && pq.getCount() <= resourceQuota) {
				decision = true;
			}
			
			if (pq.getCount() <= p.getAllowCount()) {
				decision = true;
			}

			return decision;

		}

	}
	
	private boolean QuotaDecision(int timeLimit, int resourceQuota, 
			String apiId, String resourceId, Date currentTime) {

		// in resource=i appId=0 salvo i count delle request senza appId
		// counter viene calcolato come somma delle richieste con appId e senza
		// quota viene settato con il valore di default, ne consideriamo un
		// altro?

		PolicyQuota pq = pmanager.retrievePolicyQuotaByParams(apiId,
				resourceId);
		int counter = 1;

		if (DatesDiff(pq.getTime(), currentTime) < timeLimit) {
			counter = +pq.getCount();
		}
		

		System.out.print(counter);
		updatePolicyQuota(pq, apiId, resourceId, null, currentTime,
				timeLimit);

		if (resourceQuota!=0 && counter <= resourceQuota)
			return true;
		else if(resourceQuota==0 && counter <= p.getAllowCount())
			return true;
		else
			return false;

	}

	private long DatesDiff(Date d1, Date d2) {
		long millisDiff = d2.getTime() - d1.getTime();
		return millisDiff;
	}

	private void updatePolicyQuota(PolicyQuota q, String apiId,
			String resourceId, String appId, Date currentTime, int timeLimit) {
		if (q == null) {
			q = new PolicyQuota();
			q.setApiId(apiId);
			q.setResourceId(resourceId);
			q.setAppId(appId);
			q.setTime(currentTime);
			q.setCount(1);
			pmanager.addPolicyQuota(q);
		} else {
			if (DatesDiff(q.getTime(), currentTime) < timeLimit) {
				int quotaCount = q.getCount() + 1;
				q.setCount(quotaCount);
				pmanager.updatePolicyQuota(q);
			} else {
				q.setTime(currentTime);
				q.setCount(1);
				pmanager.updatePolicyQuota(q);
			}
		}
	}
	
	
}
