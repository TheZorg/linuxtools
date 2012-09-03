/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.profiling.provider.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.linuxtools.internal.profiling.provider.ProviderOptionsTab;
import org.eclipse.linuxtools.profiling.launch.ProfileLaunchConfigurationDelegate;

public abstract class ProviderLaunchConfigurationDelegate extends
		ProfileLaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration config, String mode,
			ILaunch launch, IProgressMonitor monitor) {
		try {

			if (config != null) {
				// get provider id from configuration.
				String providerId = config.getAttribute(
						ProviderOptionsTab.PROVIDER_CONFIG_ATT, "");
				if (providerId.equals("")) {
					// No provider available in the configuration.
					return;
				}
				// get configuration delegate associated with provider id.
				ProfileLaunchConfigurationDelegate delegate = getConfigurationDelegateFromId(providerId);
				if (delegate != null) {
					delegate.launch(config, mode, launch, monitor);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return;
	}

	@Override
	public String generateCommand(ILaunchConfiguration config) {
		return null;
	}
}