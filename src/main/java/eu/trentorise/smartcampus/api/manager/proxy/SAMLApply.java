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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import eu.trentorise.smartcampus.api.manager.model.SAML;

public class SAMLApply implements PolicyDatastoreApply{
	/**
	 * Instance of {@Logger}.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SAMLApply.class);
	
	//global variables
	private String apiId;
	private String resourceId;
	private String appId;
	private SAML p;
	
	private String samlAssertion;
	
	public SAMLApply(String apiId, String resourceId, String appId, SAML p, String samlAssertion){
		this.apiId = apiId;
		this.resourceId = resourceId;
		this.appId = appId;
		this.p = p;
		
		this.samlAssertion = samlAssertion;
	}

	@Override
	public void apply() {
		// TODO Auto-generated method stub
		decision();
	}
	
	private void decision(){
		// both api and resource id cannot be null
		if (apiId == null && resourceId == null) {
			throw new IllegalArgumentException("Api or Resource id cannot be null.");
		} else {
			
			boolean decision = SAMLDecision(samlAssertion, p.isValSigner(), apiId, resourceId);

			if (decision)
				logger.info("SAML policy --> GRANT");
			else {
				logger.info("SAML policy --> DENY");
				throw new SecurityException(
						"DENY - SAML policy DENIES access.");
			}
		}
		
	}
	
	private boolean SAMLDecision(String samlResponse, boolean validateSigner, String apiId, String resourceId) {	
	  	
		boolean result=false;
		
		if (samlResponse==null){
			throw new IllegalArgumentException(
					"SAML assertion is required.");
		}else{		
			
			byte[] decodedsamlResponse = Base64.decode(samlResponse);
			String decodedResponse= new String(decodedsamlResponse);
			System.out.print(decodedResponse);
	
			try {
				DefaultBootstrap.bootstrap(); // default configuration of OpenSaml library

				ByteArrayInputStream is = new ByteArrayInputStream(decodedsamlResponse);

				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
				documentBuilderFactory.setNamespaceAware(true);

				DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
				/* parsing returns a Document object, which represents
				 * tree of XML document.
				 * (DOM, every element is a node of tree org.w3.dom.Node.)
				 */
				Document document = docBuilder.parse(is);

				Element element = document.getDocumentElement();

				//unmarshalling to retrieve data
				UnmarshallerFactory unmarshallerFactory = Configuration
						.getUnmarshallerFactory();
				Unmarshaller unmarshaller = unmarshallerFactory
						.getUnmarshaller(element);

				XMLObject responseXmlObj = unmarshaller.unmarshall(element);

				Response response = (Response) responseXmlObj;
				String statusCode = response.getStatus().getStatusCode()
						.getValue();
				System.out.print("StatusCode:" + statusCode);

				if (statusCode.contains("status:Success") == false) {
					System.out.print("Error: User is not authenticated.");
				} else {

					/*
					 * TODO validate assertion
					 * 1. Validating the Status OK 
					 * 2. Looking for an Authentication Statement
					 * [describes a statement by the SAML authority asserting
					 * that the assertion subject was authenticated by a
					 * particular means at a particular time][the relying party
					 * may require information additional to the assertion
					 * itself in order to assess the level of confidence they
					 * can place in that assertion] 
					 * 3. Looking for a Conditions statement 
					 * 4. Checking that the timestamps in the assertion are valid OK 
					 * 5. Checking that the Attribute namespace matches, if provided 
					 * 6. Miscellaneous format confirmations 
					 * 7. Confirming Issuer matches 
					 * 8. Confirming a Subject Confirmation was provided and contains valid timestamps
					 * 9. Checking that the Audience matches, if provided 
					 * 10. Checking the Recipient 
					 * 11. Validating the Signature Is the response signed? OK Is the assertion
					 * signed? Is the correct certificate supplied in the
					 * keyinfo? 
					 * 12. Checking that the Site URL Attribute contains a valid site url, if provided 
					 * 13. Looking for portal and organization id, if provided
					 */

					/* TODO 
					 * check AudienceRestriction means: equals resource uri?
					 * which is issuer?
					 */

					List<Assertion> assList = response.getAssertions();

					Assertion assertion = assList.get(0);

					/* TODO
					 * If assertion is encrypted:
					 * EncryptedAssertion encryptedAssertion=response.getEncryptedAssertions().get(0);
					 * Assertion assertion2 = decrypt(encryptedAssertion,credential);
					 * I need to have credential of certificate.
					 */

					/* check the current timestamp against the NotBefore and
					 * NotOnOrAfter elements in the assertion
					 */
					validateTimes(assertion);

					// check signatures
					if (!validateSigner) {
						System.out.print("Grant: post request with SAMLAssertion or retrieve user info.");
						//TODO what about Issuer field?
						result = true;
						
					} else {
						
						Signature sig = response.getSignature();
						int num_firme = 0;

						// check signature response
						if (response.isSigned()) {
							num_firme++;
							result = validateSignature(sig, p.getTruststore());
						} else {
							System.out.print("Signature response is not here.");
						}

						// check signature assertion
						Signature signature = assertion.getSignature();
						if (assertion.isSigned()) {
							num_firme++;
							result = validateSignature(signature, p.getTruststore());
						} else {
							System.out.print("Signature assertion is not here.");
						}

						if (num_firme == 0) {
							System.out.print("Error: No validation signature");
						}
					}

				}

			} catch (ConfigurationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnmarshallingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		}	
		return result;
	}

	private static void validateTimes(Assertion assertion){
		if (assertion.getConditions().getNotBefore() != null
				&& assertion.getConditions().getNotBefore().isAfterNow()) {
			throw new SecurityException(
					"SAML 2.0 Message is outdated (too early) !");
		}

		if (assertion.getConditions().getNotOnOrAfter() != null
				&& (assertion.getConditions().getNotOnOrAfter().isBeforeNow() || assertion
						.getConditions().getNotOnOrAfter().isEqualNow())) {
			throw new SecurityException(
					"SAML 2.0 Message is outdated (too late) !");
		}
	}



	private static boolean validateSignature(Signature sig, String truststore) {
		boolean result = false;
		
		//Convert String to Uri
		URI endpoint = URI.create(truststore);

		SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
		try {
			profileValidator.validate(sig);
		} catch (ValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//TODO validate signature with info trustStore.

		//Read certificate
		// read public key
		FileInputStream inStream;
		try {
			
			//Certificate file (.cer) is needed
			inStream = new FileInputStream(new File(endpoint));
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) cf
					.generateCertificate(inStream);
			// NOTE: value of cert is in response, but I cannot use it due to cast problem.
			inStream.close();
			BasicX509Credential credential = new BasicX509Credential();
			credential.setUsageType(UsageType.SIGNING);
			credential.setEntityCertificate(cert);
			SignatureValidator validator = new SignatureValidator(credential);
			validator.validate(sig);
			result = true;
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ValidationException e) {
			System.out.print("Error keys");
			e.printStackTrace();
			return false;
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

}
