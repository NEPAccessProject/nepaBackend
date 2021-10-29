package nepaBackend.pojo;

import nepaBackend.model.ApplicationUser;

public class AppUserDetails {
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private String affiliation;
	private String organization;
	private String jobTitle;
	
	public AppUserDetails() {
		
	}
	
	/** Create new AppUserDetails based on the fields we care about in given ApplicationUser user */
	public AppUserDetails(ApplicationUser user) {
		this.setAffiliation(user.getAffiliation());
		this.setEmail(user.getEmail());
		this.setFirstName(user.getFirstName());
		this.setLastName(user.getLastName());
		this.setOrganization(user.getOrganization());
		this.setJobTitle(user.getJobTitle());
		this.setUsername(user.getUsername());
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getAffiliation() {
		return affiliation;
	}
	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	public String getJobTitle() {
		return jobTitle;
	}
	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}
}
