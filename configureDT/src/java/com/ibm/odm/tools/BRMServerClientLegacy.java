/*
 * IBM Confidential
 * OCO Source Materials
 * 5725-B69 5655-Y31 5655-Y17
 * Copyright IBM Corp. 1987, 2023
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */

package com.ibm.odm.tools;

import java.util.ArrayList;
import java.util.List;

import ilog.rules.teamserver.client.IlrRemoteSessionFactory;
import ilog.rules.teamserver.client.internal.BRMServerClient;
import ilog.rules.teamserver.model.IlrConnectException;
import ilog.rules.teamserver.model.IlrSession;

public abstract class BRMServerClientLegacy extends BRMServerClient {

	private String username;
	private String password;
	private String url;
	private String dataSource;
	
	private boolean legacy = false;
	private IlrSession session;

	protected abstract void execute(String[] otherArgs);

	/*
	 *  legacy entry point 
	 *  called by older versions of BRMServerClient 
	 *  and kept for compatibility
	 *  
	 *   the new version of BRMServerClient 
	 *   - opens the session
	 *   - and calls the method execute(String[] otherArgs)  (implemented in subclasses of BRMServerClientLegacy) 
	 */
	protected void execute(String username, String password, String url, String dataSource, String[] otherArgs) {
		if (username == null || password == null || url == null ) {
			usage();
			return;
		}
		this.username = username;
		this.password = password;
		this.url = url;
		this.dataSource = dataSource;

		legacy = true;

		try {
            openSession();
			execute(removeConnectionArgs(otherArgs));
		} catch (IlrConnectException e) {
			e.printStackTrace() ;
		} finally {
			if (getSession() != null) {
				getSession().close();
				session = null;
			}
		}
	}

	protected IlrSession getSession() {
		if (legacy) {
			if (session == null) {
				try {
					openSession();
				} catch (IlrConnectException e) {
					e.printStackTrace() ;
				} finally {
						session = null;
				}
			}
			return session;
		} else {	
		  return super.session;
		}
	}
	
	private IlrSession openSession()
			throws IlrConnectException {
		IlrRemoteSessionFactory factory = new IlrRemoteSessionFactory();
		factory.connect(username, password, url, dataSource);
		session = factory.getSession();
		factory = null;
		return session;
	}

	/*
	 *  this methods removes the token and zenApiKey from the arguments
	 *  because some subclasses would abort when receiving more arguments than expected 
	 */
	private String[] removeConnectionArgs(String[] otherArgs) {
		List<String> filteredArgs = new ArrayList<String>();

		for (int i = 0; i < otherArgs.length; i++) {
			if (i % 2 == 0) {
				// the arguments are in the form: ["-option1", "<value>", "-option2", "<value>", ...]
				// this argument is an option
				String option = otherArgs[i];
				if ("-token".equals(option) || "-zenApiKey".equals(option)) {
					// ignore this argument and the next (the value)
					i++;
					continue;
				}
			}
			filteredArgs.add(otherArgs[i]);
		}
		return filteredArgs.toArray(new String[0]);
	}
	
	/*
	 *  the username, password, url and dataSource are available 
	 *  1) in BRMServerClientLegacy  if an older version of BRMServerClient is used
	 *  2) in BRMServerClient        otherwise
	 *  
	 *  in the second case, those fields in BRMServerClient are private and can be accessed through getters
	 */
	
	protected String getUsername() {
		return legacy ? username : super.getUsername();
	}

	protected String getPassword() {
		return legacy ? password : super.getPassword();
	}

	protected String getUrl() {
		return legacy ? url : super.getUrl();
	}

	protected String getDataSource() {
		return legacy ? dataSource : super.getDataSource();
	}
	protected boolean isLegacy() {
		return legacy;
	}	
}