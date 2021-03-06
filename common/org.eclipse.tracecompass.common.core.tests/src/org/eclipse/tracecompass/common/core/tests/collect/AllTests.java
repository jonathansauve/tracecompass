/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.common.core.tests.collect;

import org.eclipse.tracecompass.common.core.tests.collect.BufferedBlockingQueueTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Master test suite for Common Core.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    BufferedBlockingQueueTest.class
})
public class AllTests {

}
