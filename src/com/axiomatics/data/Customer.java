package com.axiomatics.data;

public class Customer {
	
	private String custID;
	private String custName;
	private String isActive;
	private String hasPortalAcess;
	private String domains;
	private String custType;
	
	public String getCustType()
	{
	    return this.custType;
	}
	public void setCustType(String custType)
	{
	     this.custType = custType;
	}
	
	public String getCustID()
	{
	    return this.custID;
	}
	public void setCustID(String custID)
	{
	     this.custID = custID;
	}
	
	public String getCustName()
	{
	    return this.custName;
	}
	public void setCustName(String custName)
	{
	     this.custName = custName;
	}
	
	public String getIsActive()
	{
	    return this.isActive;
	}
	public void setIsActive(String isActive)
	{
	     this.isActive = isActive;
	}
	
	public String getHasPortalAccess()
	{
	    return this.hasPortalAcess;
	}
	public void setHasPortalAccess(String hasPortalAcess)
	{
	     this.hasPortalAcess = hasPortalAcess;
	}
	
	public String getDomains()
	{
	    return this.domains;
	}
	public void setDomains(String domains)
	{
	     this.domains = domains;
	}

}
