/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.eclipse.wst.jsdt.integration.tests;

import org.eclipse.wst.jsdt.integration.tests.nodejs.NodeJSDebuggerTest;
import org.eclipse.wst.jsdt.integration.tests.nodejs.NodeJSLauncherTest;
import org.jboss.reddeer.junit.runner.RedDeerSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({	
	//NodeJS Debugger Tests
	NodeJSLauncherTest.class,
	NodeJSDebuggerTest.class
	
	// ...
	
})
public class JSDTIntegrationTests {

}
