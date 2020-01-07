package com.axiomatics.data;

public class Contact {
	
	private String firstName;
	private String lastName;
	private String email;
	private String contactID;
	private String title;
	private String isActive;
	private String IsPortalUser;

	public String getFirstName()
	{
	    return this.firstName;
	}
	public void setFirstName(String firstName)
	{
	     this.firstName = firstName;
	}
	
	public String getLastName()
	{
	    return this.lastName;
	}
	public void setLastName(String lastName)
	{
	     this.lastName = lastName;
	}
	
	public String getEmail()
	{
	    return this.email;
	}
	public void setEmail(String email)
	{
	     this.email = email;
	}
	
	public String getContactID()
	{
	    return this.contactID;
	}
	public void setContactID(String contactID)
	{
	     this.contactID = contactID;
	}
	
	public String getTitle()
	{
	    return this.title;
	}
	public void setTitle(String title)
	{
	     this.title = title;
	}
	
	public String getIsActive()
	{
	    return this.isActive;
	}
	public void setIsActive(String isActive)
	{
	     this.isActive = isActive;
	}
	
	public String getIsPortalUser()
	{
	    return this.IsPortalUser;
	}
	public void setIsPortalUser(String IsPortalUser)
	{
	     this.IsPortalUser = IsPortalUser;
	}
}
