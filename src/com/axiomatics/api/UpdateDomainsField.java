package com.axiomatics.api;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.mail.internet.MimeMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.axiomatics.data.Contact;
import com.axiomatics.data.Customer;
import com.axiomatics.gmail.ComposeEmail;
import com.google.api.services.gmail.Gmail;

public class UpdateDomainsField {
	
	private static final String UPDATE_DOMAINS_LOG_FILE 	= "./logs/UpdateDomains";
	
	public static void main(String[] args) throws Exception {
		
		
		List<Contact>	contactsPerCust 				= new ArrayList<>();
		List<Customer> 	allCustomerInfo 				= new ArrayList<>();
		List<Customer>  updatedCustomers_OK				= new ArrayList<>();
		List<Customer>  updatedCustomers_NOK			= new ArrayList<>();
		List<String>	allPartnerDomains				= new ArrayList<>();
		JSONArray		custJson						= null;
		String 			putUrl							= null;
		String 			newDomains						= null;
		String			currentTSDomains				= null;
		int 			putStatus						= 0;
		int 			nbUpdatedDomainsCustomers		= 0;
		int				nbUnableUpdatedDomainsCustomers = 0;
		int 			nbAPICall						= 0;


		
		UpdateDomainsField http = new UpdateDomainsField();

		System.out.println("Processing - Send Http requests");
		logWriter("Processing - Send Http requests", UPDATE_DOMAINS_LOG_FILE);
		
		/*******************************************************************************/
		/****************************  Test Instance Connection ************************/
		/*******************************************************************************/
	    // Connection parameters
	    /*String getAllCustomersUrl = "https://app.na2.teamsupport.com/api/json/customers";
		String username = "1205149";
		String password = "041cf131-7d7a-4285-a674-fb906a513b6e";*/
		
		
		/*******************************************************************************/
		/*************************  Production Instance Connection *********************/
		/*******************************************************************************/
		String getAllCustomersUrl = "https://app.teamsupport.com/api/json/customers";
		String username = "1102306";
		String password = "34eb26b7-d195-4d70-94b6-0fdeb9634b51";

		
		StringBuffer httpResponse = http.sendGet(getAllCustomersUrl, username, password);
		custJson = http.parseResponse(httpResponse, "Customers");
		allCustomerInfo = http.getAllCustomersInfo(httpResponse);
		allPartnerDomains	= http.getAllPartnerDomains(httpResponse);
		
		for (int i=0; i < allCustomerInfo.size(); i++) 
		{
			if (excludedCustomer(allCustomerInfo.get(i).getCustName())) continue;
			
			String customerID = allCustomerInfo.get(i).getCustID();
			String getAllContactsPerCustomerUrl = getAllCustomersUrl + "/" + customerID + "/contacts";
			httpResponse = http.sendGet(getAllContactsPerCustomerUrl, username, password);
			nbAPICall++;
			System.out.println("API Calls# : " + nbAPICall);	
			logWriter("API Calls# : " + nbAPICall, UPDATE_DOMAINS_LOG_FILE);
			
			contactsPerCust = http.getAllContactsInfo(httpResponse);
			
			// check if Customer have contacts; if yes -> find customer domains else -> go to the next customer
			if ( !contactsPerCust.isEmpty())
				http.findCustomerDomains(contactsPerCust, allCustomerInfo.get(i), allPartnerDomains);
			else 
				continue;
			
			/************************* Compare new domains with current Domains *******************************
			 * if new domains (not empty) is equal to current domains, do not update current domains in TS
			 */
			newDomains = allCustomerInfo.get(i).getDomains();			
			if (newDomains.equalsIgnoreCase("")) continue;
			
			if (!(custJson.getJSONObject(i).get("Domains").equals(null)))
			{ 
				currentTSDomains 	= (String)custJson.getJSONObject(i).get("Domains");
				if (currentTSDomains.equalsIgnoreCase(newDomains)) continue;
			}
			/**************************************************************************************************/
			
			putUrl = getAllCustomersUrl + "/" + customerID;
			putStatus = http.sendPut( putUrl, username, password, custJson.getJSONObject(i), newDomains , "Domains", "Customer");
			nbAPICall++;
			System.out.println("API Calls# : " + nbAPICall);
			logWriter("API Calls# : " + nbAPICall, UPDATE_DOMAINS_LOG_FILE);
			
			if ( putStatus == 200 ) 
			{
				Customer cust = new Customer();
				cust.setCustName(allCustomerInfo.get(i).getCustName());
				cust.setCustID(allCustomerInfo.get(i).getCustID());
				cust.setDomains(allCustomerInfo.get(i).getDomains());
				updatedCustomers_OK.add(cust);
				nbUpdatedDomainsCustomers++;
			}
			else
			{
				Customer cust = new Customer();
				cust.setCustName(allCustomerInfo.get(i).getCustName());
				cust.setCustID(allCustomerInfo.get(i).getCustID());
				cust.setDomains(allCustomerInfo.get(i).getDomains());
				updatedCustomers_NOK.add(cust);
				nbUnableUpdatedDomainsCustomers++;
			}			
			
			System.out.println("Response Code : " + putStatus);							
			System.out.println();
			
			logWriter("Response Code : " + putStatus, UPDATE_DOMAINS_LOG_FILE);
			logWriter("\n", UPDATE_DOMAINS_LOG_FILE);
			
		}
		
		System.out.println("/--------------------------------------------------------/");
		System.out.println("Number of Customers with updated Domains = \t" + nbUpdatedDomainsCustomers);
		System.out.println("Number of Customers with updated Domains issues = \t" + nbUnableUpdatedDomainsCustomers);
		System.out.println("Processing - End");
		logWriter("/--------------------------------------------------------/", UPDATE_DOMAINS_LOG_FILE);
		logWriter("Number of Customers with updated Domains = " + nbUpdatedDomainsCustomers, UPDATE_DOMAINS_LOG_FILE);
		logWriter("Number of Customers with updated Domains issues = " + nbUnableUpdatedDomainsCustomers, UPDATE_DOMAINS_LOG_FILE);
		logWriter("Processing - End", UPDATE_DOMAINS_LOG_FILE);
		
		String 	pathCredentialFile 	= "client_secret.json";
		String 	userId				= "me";
		String 						to							= "customer-relations@axiomatics.com";
		//String 						to							= "philippe.ferron@axiomatics.com";
		String 						from						= "philippe.ferron@axiomatics.com";
		String 						subject						= "Update TeamSupport Customers Domains";
		String						html						= null;
		
		if (updatedCustomers_OK.size() > 0)
		{	
			html = "<p>Updated Customer Domains List</p>";
			html+= "<div><table><tr bgcolor='#aaaaff'><TH>Customer Name</TH><TH>Customer ID</TH><TH>Customer Domains</TH></tr>";
		}
		
		for (int j=0; j < updatedCustomers_OK.size(); j++) 
		{
			html+="<tr>";
			html+="<td>" + updatedCustomers_OK.get(j).getCustName()						+ "</td>";
			html+="<td align=\"center\">" + updatedCustomers_OK.get(j).getCustID()		+ "</td>";
			html+="<td align=\"center\">" + updatedCustomers_OK.get(j).getDomains() 	+ "</td>";
		}
		
		html+= "</tr></table></div>";
		
		
		if (updatedCustomers_NOK.size() > 0)
		{
			html+= "<p>Update Failure Customer Domains List</p>";
			html+= "<div><table><tr bgcolor='#aaaaff'><TH>Customer Name</TH><TH>Customer ID</TH><TH>Customer Domains</TH></tr>";
		}
			
		
		for (int k=0; k < updatedCustomers_NOK.size(); k++) 
		{
			html+="<tr>";
			html+="<td>" + updatedCustomers_NOK.get(k).getCustName()					+ "</td>";
			html+="<td align=\"center\">" + updatedCustomers_NOK.get(k).getCustID()		+ "</td>";
			html+="<td align=\"center\">" + updatedCustomers_NOK.get(k).getDomains() 	+ "</td>";
		}
		
		html+= "</tr></table></div>";
		
		if (updatedCustomers_OK.size() > 0 || updatedCustomers_NOK.size() > 0)
		{
			Gmail service = ComposeEmail.getGmailService(pathCredentialFile);
			MimeMessage email = ComposeEmail.createHTMLEmail(to, from, subject, html);
			ComposeEmail.sendMessage(service, userId, email);
			MimeMessage myEmail = ComposeEmail.createHTMLEmail("philippe.ferron@axiomatics.com", from, subject, html);
			ComposeEmail.sendMessage(service, userId, myEmail);
			System.out.println("Email being sent !!!");
		}

	}
	
	public JSONArray parseResponse(StringBuffer httpResponse, String entity) throws JSONException {
		
		JSONObject responseObject = new JSONObject(httpResponse.toString());
		JSONArray resultsArray = responseObject.getJSONArray(entity);
		return resultsArray;
		
	}
	
	public void findCustomerDomains(List<Contact> contactsList, Customer customer, List<String> allPartnerDomains) throws IOException, JSONException
	{
		String allDomains		= "";
		List<String> domains 	= new ArrayList<>();
		String email			= null;
		String uniqueDomain		= null;
		boolean bfound 			= false;
		
		for (int i=0; i < contactsList.size(); i++) 
		{
			email = contactsList.get(i).getEmail();
			
			if(email.indexOf("@")!=-1)
				uniqueDomain = email.substring(email.lastIndexOf("@") + 1, email.length());
			else 
				continue;
			
			if (!customer.getCustID().equalsIgnoreCase("1141661")) // if customer is different than Accenture
			{
				if (excludedDomain(uniqueDomain))
					continue;
			}
			
			if (excludedCustomer(customer.getCustName()))
				continue;
			
			if (customer.getCustType().equalsIgnoreCase("Customer")) // if Organization type is Customer, check if the unique domain is a partner domain
			{
				if (excludedPartnerDomains(uniqueDomain, allPartnerDomains))
					continue; //if the unique domain is a partner domain, do not add it
			}

					
			for (int j=0; j < domains.size(); j++) 
			{
				if ( !domains.get(j).equalsIgnoreCase(uniqueDomain))
					bfound = false;
				else 
				{ 
					bfound = true;
					break;
				}
			}
			
			domains.add(uniqueDomain);
			
			if (bfound == false)
				if (allDomains.equalsIgnoreCase(""))
					allDomains = uniqueDomain;
				else 
					allDomains = allDomains + ", " + uniqueDomain;
		}
		
		customer.setDomains(allDomains);
		System.out.println("Domains			: " + allDomains);
		System.out.println("Customer		: " + customer.getCustName());
		logWriter("Domains       : " + allDomains, UPDATE_DOMAINS_LOG_FILE);
		logWriter("Customer		: " + customer.getCustName(), UPDATE_DOMAINS_LOG_FILE);

	};
	
	
	private boolean excludedDomain(String uniqueDomain) {
		
		boolean bfound = false;
		String[] emailProviderDomain = {
				"yahoo.no", "yahoo.com", "gmail.com", "google.com", "aol.com", "hotmail.com", "msn.com", "comcast.net", "hotmail.co.uk",
				"sbcglobal.net", "yahoo.co.uk", "yahoo.co.in", "bellsouth.net", "verizon.net", "earthlink.net", "cox.net", 
				"rediffmail.com", "yahoo.ca", "btinternet.com", "charter.net", "shaw.ca", "ntlworld.com", "capgemini.com", "accenture.com",
				"outlook.com","axiomatics.com","axiomaticsfederal.com","outsidegc.com","ibm.com","vassit.co.uk"
				};
		
		for (int i=0; i < emailProviderDomain.length; i++) 
		{
			if (uniqueDomain.equalsIgnoreCase(emailProviderDomain[i]))
			{
					bfound = true;
					break;
			}
		}
			
		return bfound;
	}

	private static boolean excludedPartnerDomains(String uniqueDomain, List<String> allPartnerDomains) {
		
		boolean bfound = false;
		
		for (int i=0; i < allPartnerDomains.size(); i++) 
		{
			if (uniqueDomain.equalsIgnoreCase(allPartnerDomains.get(i)))
			{
					bfound = true;
					break;
			}
		}
			
		return bfound;
	}
	
	private static boolean excludedCustomer(String customerName) {
			
			boolean bfound = false;
			String[] excludedCustomer = {
					"_Unknown Company"
					};
			
			for (int i=0; i < excludedCustomer.length; i++) 
			{
				if (customerName.equalsIgnoreCase(excludedCustomer[i]))
				{
						bfound = true;
						break;
				}
			}
				
			return bfound;
		}
	
	public List<String> getAllPartnerDomains(StringBuffer httpResult) throws JSONException {
		
		List<String> 	allPartnerDomains	= new ArrayList<>();
		String 			customerType		= null;	
		String 			customerDomains		= null;
	
		
		String entity = "Customers";
		JSONObject responseObject = new JSONObject(httpResult.toString());
		JSONArray resultsArray = responseObject.getJSONArray(entity);
		
		for (int i=0; i<resultsArray.length(); i++) 
		{
			try 
			{
				JSONObject object = resultsArray.getJSONObject(i);		
	
				customerType 	= object.getString("Type");
				customerDomains	= object.getString("Domains");
				
				if (customerType.equalsIgnoreCase("Partner"))
				{
					if (customerDomains.contains(",")) // If there is more than one domain per organization
					{
							// split domains
							String stringSplit[] = customerDomains.split(",");
							for (int j=0; j < stringSplit.length; j++) 
								allPartnerDomains.add(stringSplit[j].replaceAll(" ", ""));

					}
					else // only one domain per organization
						allPartnerDomains.add(customerDomains);
				}
			}
			catch (org.json.JSONException exception)
			{
				System.out.println("No Type or No Domains");
				System.out.println();
				continue;
			}
		}
		
		return allPartnerDomains;
	}	
	
	public List<Customer> getAllCustomersInfo(StringBuffer httpResult) throws JSONException {
		
		List<Customer> 	allCustomersInfo	= new ArrayList<>();
		String 			customerID 			= null;	
		String 			customerName		= null;
		String 			customerType		= null;
	
		
		String entity = "Customers";
		JSONObject responseObject = new JSONObject(httpResult.toString());
		JSONArray resultsArray = responseObject.getJSONArray(entity);
		
		for (int i=0; i<resultsArray.length(); i++) 
		{
			
			JSONObject object = resultsArray.getJSONObject(i);		

			customerID 		= object.getString("ID");
			customerName 	= object.getString("Name");
			
			try
			{
			customerType	= object.getString("Type");
			}
			catch (org.json.JSONException exception)
			{
				System.out.println("No Type");
				System.out.println();
			}
			
			Customer cust = new Customer();
			cust.setCustID(customerID);
			cust.setCustName(customerName);
			cust.setCustType(customerType);
			allCustomersInfo.add(cust);
			
		}
		
		return allCustomersInfo;
	}
	
	public List<Contact> getAllContactsInfo(StringBuffer httpResult) throws JSONException, IOException {
			
			List<Contact> 	allContactsInfo	= new ArrayList<>();
			String 			contactID 			= null;
			String 			email				= null;
			
			try 
			{
				String entity = "Contacts";
				JSONObject responseObject = new JSONObject(httpResult.toString());
				JSONArray resultsArray = responseObject.getJSONArray(entity);
				
				for (int i=0; i<resultsArray.length(); i++) 
				{
					JSONObject object = resultsArray.getJSONObject(i);		
		
					contactID 	= object.getString("ID");
					email 		= object.getString("Email");
					
					if(email.indexOf("@")==-1) continue;
					
					Contact contact = new Contact();
					contact.setContactID(contactID);
					contact.setEmail(email);
					allContactsInfo.add(contact);	
				}
			}
			catch (org.json.JSONException exception)
			{
				System.out.println("No Contacts");
				System.out.println();
				
				logWriter("No Contacts", UPDATE_DOMAINS_LOG_FILE);
				logWriter("\n", UPDATE_DOMAINS_LOG_FILE);

			}
			
			return allContactsInfo;
		}
	
	
	public int sendPut( String putUrl, String username, String password, JSONObject custJson, String domainsPerCustomer, String custField, String entity) throws Exception {
		
		int status = 0;
		
		custJson.put(custField, domainsPerCustomer);
		HttpClient putClient = HttpClientBuilder.create().build();
		
		HttpPut putRequest = new HttpPut(putUrl);
		
		String auth = username + ":" + password;
		String encodedAuth = Base64.encodeBase64String(auth.getBytes());				
		putRequest.addHeader("Authorization", "Basic " + encodedAuth);
		
		//String strContact = "{\"Contact\":" + custJson.toString() + "}";
		String strContact = "{\"" + entity + "\":" + custJson.toString() + "}";
		StringEntity params = new StringEntity(strContact ,"UTF-8");
        params.setContentType("application/json");
        putRequest.addHeader("content-type", "application/json");
        putRequest.setEntity(params);
		
		HttpResponse putResponse = putClient.execute(putRequest);
		
		System.out.println("\nSending 'PUT' request to URL : " + putUrl);
		logWriter("\nSending 'PUT' request to URL : " + putUrl, UPDATE_DOMAINS_LOG_FILE);
		status = putResponse.getStatusLine().getStatusCode();
		
		if ( status == 200)
		{
			System.out.println("Domains being updated	: " + domainsPerCustomer);
			logWriter("Domains being updated : " + domainsPerCustomer, UPDATE_DOMAINS_LOG_FILE);
		}
		else
		{
			System.out.println("Domains not being updated	: " + domainsPerCustomer);
			logWriter("Domains not being updated : " + domainsPerCustomer, UPDATE_DOMAINS_LOG_FILE);
		}	
		
		return status;
	}
	
	// HTTP GET request
	public StringBuffer sendGet( String getUrl, String username, String password) throws Exception {
		
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(getUrl);
		
		String auth = username + ":" + password;
		String encodedAuth = Base64.encodeBase64String(auth.getBytes());				
		request.addHeader("Authorization", "Basic " + encodedAuth);

		HttpResponse response = client.execute(request);

		System.out.println("\nSending 'GET' request to URL : " + getUrl);
		System.out.println("Response Code : " +
	                   response.getStatusLine().getStatusCode());
		logWriter("\nSending 'GET' request to URL : "+ getUrl, UPDATE_DOMAINS_LOG_FILE);
		logWriter("Response Code : " +
                response.getStatusLine().getStatusCode(), UPDATE_DOMAINS_LOG_FILE);

		BufferedReader rd = new BufferedReader(
	                   new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		
		return result;
		
	}
	
	private static void write(String f, String s) throws IOException {
        TimeZone tz = TimeZone.getTimeZone("CST"); // or PST, MID, etc ...
        Date now = new Date();
        DateFormat df = new SimpleDateFormat ("yyyy.MM.dd hh:mm:ss ");
        DateFormat dfLogFile = new SimpleDateFormat ("yyyyMMdd");
        df.setTimeZone(tz);
        String currentTime = df.format(now);
        String extLogFile = dfLogFile.format(now);
        f = f + "_" + extLogFile + ".log";
       
        FileWriter aWriter = new FileWriter(f, true);
        aWriter.write(currentTime + " " + s + "\r\n");
        aWriter.flush();
        aWriter.close();
    }
	
	private static void logWriter(String string, String logfile) throws IOException, JSONException {
			
		write(logfile, string);
	}


}
