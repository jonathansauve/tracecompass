package org.eclipse.tracecompass.tmf.analysis.xml.ui.tests.xmlManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

/**
 * Test suite for the XmlManagerViewer class.
 */
@SuppressWarnings("javadoc")
//@RunWith(SWTBotJunit4ClassRunner.class)
public class XmlManagerViewerTest {

    private static final String PROJECT_NAME = "XmlManagerViewerTest";

    private static SWTWorkbenchBot fBot;

    private static final Logger fLogger = Logger.getRootLogger();

    @BeforeClass
    public static void setUp() {
        SWTBotUtils.failIfUIThread();

        SWTBotPreferences.TIMEOUT = 20000;
        fLogger.removeAllAppenders();
        fLogger.addAppender(new NullAppender());
        fBot = new SWTWorkbenchBot();
    }

    @AfterClass
    public static void tearDown() {
        fBot = null;
    }
}
