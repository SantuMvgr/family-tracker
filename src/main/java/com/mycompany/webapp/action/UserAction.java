package com.mycompany.webapp.action;

import com.opensymphony.xwork2.Preparable;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import org.apache.struts2.ServletActionContext;
import com.mycompany.Constants;
import com.mycompany.dao.SearchException;
import com.mycompany.model.Role;
import com.mycompany.model.User;
import com.mycompany.service.UserExistsException;
import com.mycompany.webapp.util.RequestUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Action for facilitating User Management feature.
 */
public class UserAction extends BaseAction implements Preparable {
    private static final long serialVersionUID = 6776558938712115191L;
    private List<User> users;
    private User user;
    private String id;
    private String query;
    private String weatherInfo;
    private String topNewsFeed;
    private String businessNewsFeed;
    private String sportsNewsFeed;

    public String getTopNewsFeed() {
		return topNewsFeed;
	}

	public void setTopNewsFeed(String topNewsFeed) {
		this.topNewsFeed = topNewsFeed;
	}

	public String getBusinessNewsFeed() {
		return businessNewsFeed;
	}

	public void setBusinessNewsFeed(String businessNewsFeed) {
		this.businessNewsFeed = businessNewsFeed;
	}

	public String getSportsNewsFeed() {
		return sportsNewsFeed;
	}

	public void setSportsNewsFeed(String sportsNewsFeed) {
		this.sportsNewsFeed = sportsNewsFeed;
	}

	public String getWeatherInfo() {
		return weatherInfo;
	}

	public void setWeatherInfo(String weatherInfo) {
		this.weatherInfo = weatherInfo;
	}

	/**
     * Grab the entity from the database before populating with request parameters
     */
    public void prepare() {
        // prevent failures on new
        if (getRequest().getMethod().equalsIgnoreCase("post") && (!"".equals(getRequest().getParameter("user.id")))) {
            user = userManager.getUser(getRequest().getParameter("user.id"));
        }
    }

    /**
     * Holder for users to display on list screen
     *
     * @return list of users
     */
    public List<User> getUsers() {
        return users;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setQ(String q) {
        this.query = q;
    }

    /**
     * Delete the user passed in.
     *
     * @return success
     */
    public String delete() {
        userManager.removeUser(user.getId().toString());
        List<Object> args = new ArrayList<Object>();
        args.add(user.getFullName());
        saveMessage(getText("user.deleted", args));

        return SUCCESS;
    }

    /**
     * Grab the user from the database based on the "id" passed in.
     *
     * @return success if user found
     * @throws IOException can happen when sending a "forbidden" from response.sendError()
     */
    public String edit() throws IOException {
        HttpServletRequest request = getRequest();
        boolean editProfile = request.getRequestURI().contains("editProfile");

        // if URL is "editProfile" - make sure it's the current user
        if (editProfile && ((request.getParameter("id") != null) || (request.getParameter("from") != null))) {
            ServletActionContext.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            log.warn("User '" + request.getRemoteUser() + "' is trying to edit user '" +
                    request.getParameter("id") + "'");
            return null;
        }

        // if a user's id is passed in
        if (id != null) {
            // lookup the user using that id
            user = userManager.getUser(id);
        } else if (editProfile) {
            user = userManager.getUserByUsername(request.getRemoteUser());
        } else {
            user = new User();
            user.addRole(new Role(Constants.USER_ROLE));
        }

        if (user.getUsername() != null) {
            user.setConfirmPassword(user.getPassword());

            // if user logged in with remember me, display a warning that they can't change passwords
            log.debug("checking for remember me login...");

            AuthenticationTrustResolver resolver = new AuthenticationTrustResolverImpl();
            SecurityContext ctx = SecurityContextHolder.getContext();

            if (ctx != null) {
                Authentication auth = ctx.getAuthentication();

                if (resolver.isRememberMe(auth)) {
                    getSession().setAttribute("cookieLogin", "true");
                    saveMessage(getText("userProfile.cookieLogin"));
                }
            }
        }

        return SUCCESS;
    }

    /**
     * Default: just returns "success"
     *
     * @return "success"
     */
    public String execute() {
        return SUCCESS;
    }

    /**
     * Sends users to "mainMenu" when !from.equals("list"). Sends everyone else to "cancel"
     *
     * @return "mainMenu" or "cancel"
     */
    public String cancel() {
        if (!"list".equals(from)) {
            return "mainMenu";
        }
        return "cancel";
    }

    /**
     * Save user
     *
     * @return success if everything worked, otherwise input
     * @throws Exception when setting "access denied" fails on response
     */
    public String save() throws Exception {

        Integer originalVersion = user.getVersion();

        boolean isNew = ("".equals(getRequest().getParameter("user.version")));
        // only attempt to change roles if user is admin
        // for other users, prepare() method will handle populating
        if (getRequest().isUserInRole(Constants.ADMIN_ROLE)) {
            user.getRoles().clear(); // APF-788: Removing roles from user doesn't work
            String[] userRoles = getRequest().getParameterValues("userRoles");

            for (int i = 0; userRoles != null && i < userRoles.length; i++) {
                String roleName = userRoles[i];
                try {
                    user.addRole(roleManager.getRole(roleName));
                } catch (DataIntegrityViolationException e) {
                    return showUserExistsException(originalVersion);
                }
            }
        }

        try {
            userManager.saveUser(user);
        } catch (AccessDeniedException ade) {
            // thrown by UserSecurityAdvice configured in aop:advisor userManagerSecurity
            log.warn(ade.getMessage());
            getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        } catch (UserExistsException e) {
            return showUserExistsException(originalVersion);
        }

        if (!"list".equals(from)) {
            // add success messages
            saveMessage(getText("user.saved"));
            return "mainMenu";
        } else {
            // add success messages
            List<Object> args = new ArrayList<Object>();
            args.add(user.getFullName());
            if (isNew) {
                saveMessage(getText("user.added", args));
                // Send an account information e-mail
                mailMessage.setSubject(getText("signup.email.subject"));
                try {
                    sendUserMessage(user, getText("newuser.email.message", args), RequestUtil.getAppURL(getRequest()));
                } catch (MailException me) {
                    addActionError(me.getCause().getLocalizedMessage());
                }
                return SUCCESS;
            } else {
                user.setConfirmPassword(user.getPassword());
                saveMessage(getText("user.updated.byAdmin", args));
                return INPUT;
            }
        }
    }

    private String showUserExistsException(Integer originalVersion) {
        List<Object> args = new ArrayList<Object>();
        args.add(user.getUsername());
        args.add(user.getEmail());
        addActionError(getText("errors.existing.user", args));

        // reset the version # to what was passed in
        user.setVersion(originalVersion);
        // redisplay the unencrypted passwords
        user.setPassword(user.getConfirmPassword());
        return INPUT;
    }

    /**
     * Fetch all users from database and put into local "users" variable for retrieval in the UI.
     *
     * @return "success" if no exceptions thrown
     */
    public String list() {
        try {
            users = userManager.search(query);
        } catch (SearchException se) {
            addActionError(se.getMessage());
            users = userManager.getUsers();
        }
        return SUCCESS;
    }
    
    public String showUserTracker() {
    	
    	user = userManager.getUser(id);
    	
    	Client client = Client.create();
    	WebResource webResource = client.resource("http://openweathermap.org/data/2.5/weather");
    	MultivaluedMap queryParams = new MultivaluedMapImpl();
    	queryParams.add("q", user.getAddress().getCity());
    	queryParams.add("APPID", "8c737ea99f3578ff6483971b4df34bd5");
    	queryParams.add("type", "json");
    	queryParams.add("units", "metric");
    	ClientResponse response = webResource.queryParams(queryParams).accept("application/json").get(ClientResponse.class);
    	
    	if(response.getStatus() != 200) {
    		throw new RuntimeException("Failed with HTTP Code - " + response.getStatus());
    	}
    	
    	weatherInfo  = response.getEntity(String.class);
    	
    	getNewsFeeds();
    	
    	System.out.println(weatherInfo);
    	
    	return SUCCESS;
    }
    
    private void getNewsFeeds() {
    	
    	Client client = Client.create();
    	//Category - 'Top News', id - 26 
    	WebResource webResource = client.resource("http://api.feedzilla.com/v1/categories/26/articles.json?culture_code=en_in&count=3");
    	ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
    	
    	if(response.getStatus() != 200) {
    		throw new RuntimeException("Failed with HTTP Code - " + response.getStatus());
    	}
    	topNewsFeed = response.getEntity(String.class);
    	
    	//Category - 'Business', id - 22
    	webResource = client.resource("http://api.feedzilla.com/v1/categories/22/articles.json?culture_code=en_in&count=3");
    	response = webResource.accept("application/json").get(ClientResponse.class);
    	
    	if(response.getStatus() != 200) {
    		throw new RuntimeException("Failed with HTTP Code - " + response.getStatus());
    	}
    	businessNewsFeed = response.getEntity(String.class);
    	
    	//Category - 'Sports', id - 27
    	webResource = client.resource("http://api.feedzilla.com/v1/categories/27/articles.json?culture_code=en_in&count=3");
    	response = webResource.accept("application/json").get(ClientResponse.class);
    	
    	if(response.getStatus() != 200) {
    		throw new RuntimeException("Failed with HTTP Code - " + response.getStatus());
    	}
    	sportsNewsFeed = response.getEntity(String.class);
    	
    }

}
