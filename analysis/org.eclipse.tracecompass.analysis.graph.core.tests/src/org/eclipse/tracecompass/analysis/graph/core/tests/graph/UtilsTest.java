/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial API and implementation
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.tests.graph;

import java.nio.file.Paths;

import org.eclipse.core.runtime.IPath;
import org.eclipse.tracecompass.analysis.graph.core.tests.Activator;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Check the Utils class for proper functioning.
 *
 * */
public class UtilsTest {

    private static final String TRACESET = "traces/traceset/localhost/lttng-traceset-20150713";
    private static IPath fTracesetDir;

    @BeforeClass
    public void setup() {

        // FIXME: This is ulgy, let's just use strings instead of enum

        fTracesetDir = Paths.get(Activator.getAbsoluteFilePath(TRACESET).toOSString();
    }


    @Test
    public void testFindCtfTrace() {
        IPath path = fTracesetDir.append(Utils.Traceset.TRACE_RPC.getName());
        path.
    }

}
