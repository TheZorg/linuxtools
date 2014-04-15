/*******************************************************************************
 * Copyright (c) 2004 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Roland Grunberg <rgrunber@redhat.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.oprofile.core.linux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.linuxtools.internal.oprofile.core.OpcontrolException;
import org.eclipse.linuxtools.internal.oprofile.core.Oprofile;
import org.eclipse.linuxtools.internal.oprofile.core.OprofileCorePlugin;
import org.eclipse.linuxtools.internal.oprofile.core.OprofileProperties;
import org.eclipse.linuxtools.profiling.launch.IRemoteFileProxy;
import org.eclipse.linuxtools.profiling.launch.RemoteProxyManager;
import org.eclipse.osgi.util.NLS;

/**
 * Handle errors generated by the opcontrol command giving users a better
 * idea of what went wrong.
 */
public final class OpControlErrorHandler {

	private static OpControlErrorHandler singleton = new OpControlErrorHandler();
	private String errorFilePath = OprofileCorePlugin.getDefault().getPluginLocation() + "op_error_key"; //$NON-NLS-1$
	private Map<String, String> errorMap = new HashMap<> ();

	private OpControlErrorHandler (){
	}

	public static OpControlErrorHandler getInstance (){
		return singleton;
	}

	/**
	 * @param stdout Standard output text collected from opcontrol
	 * @param stderr Standard error text collected from opcontrol
	 */
	public OpcontrolException handleError (String stdout, String stderr) {
		String type = "";
		String fullErr = "";
		IRemoteFileProxy proxy = null;

		// Figure out which stream has the error
		// If both have errors, then stderr takes priority
		// Set this initially in case we don't match against any errors
		if (!stderr.trim().equals("")) { //$NON-NLS-1$
			fullErr = stderr;
			type = "process.log.stderr"; //$NON-NLS-1$
		}else if (!stdout.trim().equals("")){ //$NON-NLS-1$
			fullErr = stdout;
			type = "process.log.stdout"; //$NON-NLS-1$
		}else{
			// The error is unkown
						return new OpcontrolException(OprofileCorePlugin.createErrorStatus(
								"opcontrolNonZeroExitCode", null)); //$NON-NLS-1$
		}

		// Read in the errors
		try {
			proxy = RemoteProxyManager.getInstance().getFileProxy(Oprofile.OprofileProject.getProject());
			IFileStore fileStore = proxy.getResource(errorFilePath);
			InputStream is = fileStore.openInputStream(EFS.NONE, new NullProgressMonitor());

			String line = "";
			BufferedReader buff = new BufferedReader(new InputStreamReader(is));

			// Populate the mapping
			while ((line = buff.readLine()) != null){
				String [] parts = line.split("="); //$NON-NLS-1$
				errorMap.put(parts[0], parts[1]);
			}

			buff.close();

		} catch (IOException|CoreException e) {
			e.printStackTrace();
		}

		String error = null;
		// Determine if stdout or stderr contains an error pattern
		for (String key : errorMap.keySet()){
			if (stderr.contains(key)){
				fullErr = stderr;
				type = "process.log.stderr"; //$NON-NLS-1$
				error = errorMap.get(key);
				break;
			}else if (stdout.contains(key)){
				fullErr = stdout;
				type = "process.log.stdout"; //$NON-NLS-1$
				error = errorMap.get(key);
				break;
			}
		}

		// We have something to log
		OprofileCorePlugin.log(IStatus.ERROR, NLS.bind(
				OprofileProperties.getString(type), "opcontrol", fullErr)); //$NON-NLS-1$

		if (error != null) {
			return new OpcontrolException(OprofileCorePlugin.createErrorStatus(
					error, null));
		} else {
			// There is an error, but we did not match anything
			return new OpcontrolException(OprofileCorePlugin.createErrorStatus(
					"opcontrolNonZeroExitCodeExtraInfo", null)); //$NON-NLS-1$
		}

	}

}
