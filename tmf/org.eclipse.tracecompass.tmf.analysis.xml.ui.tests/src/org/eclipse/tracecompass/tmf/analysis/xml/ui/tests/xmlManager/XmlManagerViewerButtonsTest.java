/*******************************************************************************
 * Copyright (c) 2014, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jonathan Sauvé - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.ui.tests.xmlManager;

import static org.junit.Assert.assertNotNull;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test suite for the XmlManagerViewer class.
 */
@SuppressWarnings("javadoc")
@RunWith(SWTBotJunit4ClassRunner.class)
public class XmlManagerViewerButtonsTest {

    private static final String PROJECT_NAME = "XmlManagerViewerTest";
    private static final String RESOURCE_PERSPECTIVE_ID = "org.eclipse.ui.resourcePerspective";
    private static final String RESOURCE_SHELL_TITLE = "Resource";
    private static final String WINDOW_MENU_ITEM = "Window";
    private static final String SHOW_VIEW_MENU_ITEM_AND_SHELL_TITLE = "Show View";
    private static final String TRACING_TREE_ITEM = "Tracing";
    private static final String XML_MANAGER_TREE_ITEM_AND_SHELL_TITLE = "XML Manager";
    private static final String OK_BUTTON_TEXT = "OK";

    private static final String IMPORT_BUTTON_TEXT = "Import";
    private static final String REMOVE_BUTTON_TEXT = "Remove";
    private static final String EDIT_BUTTON_TEXT = "Edit";


    private static SWTWorkbenchBot fBot;

    private static final Logger fLogger = Logger.getRootLogger();

    @BeforeClass
    public static void setUp() {
        SWTBotUtils.failIfUIThread();
        Thread.currentThread().setName("SWTBot Thread"); // for the debugger
        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 20000; /* 20 second timeout */
        fLogger.removeAllAppenders();
        fLogger.addAppender(new ConsoleAppender(new SimpleLayout()));
        fBot = new SWTWorkbenchBot();

        SWTBotUtils.closeView("welcome", fBot);

        SWTBotUtils.switchToPerspective(RESOURCE_PERSPECTIVE_ID);
        /* finish waiting for eclipse to load */
        SWTBotUtils.waitForJobs();
    }

    @AfterClass
    public static void tearDown() {
        fLogger.removeAllAppenders();
    }

    @SuppressWarnings("restriction")
    @Test
    public void testButtonsExists() {
        /* Click on Window --> Show View */
        fBot.shell(RESOURCE_SHELL_TITLE).contextMenu(WINDOW_MENU_ITEM).contextMenu(SHOW_VIEW_MENU_ITEM_AND_SHELL_TITLE).click();

        /* Focus on Show View shell */
        fBot.shell(SHOW_VIEW_MENU_ITEM_AND_SHELL_TITLE).setFocus();
        SWTBotView showViewView = fBot.viewByTitle(SHOW_VIEW_MENU_ITEM_AND_SHELL_TITLE);

        /* Get Tree, find Tracing TreeItem, expand it and select XML Manager TreeItem */
        SWTBotTreeItem tracingItem = showViewView.bot().tree().getTreeItem(TRACING_TREE_ITEM);
        assertNotNull(tracingItem);
        tracingItem.select();
        tracingItem.expand();
        SWTBotTreeItem xmlManagerTreeItem = null;
        for(String node : tracingItem.getNodes()) {
            if(node.equals(XML_MANAGER_TREE_ITEM_AND_SHELL_TITLE)) {
                xmlManagerTreeItem = tracingItem.getNode(node);
                break;
            }
        }
        assertNotNull(xmlManagerTreeItem);
        xmlManagerTreeItem.select();

        showViewView.bot().button(OK_BUTTON_TEXT).click();

        SWTBotView xmlManagerView = fBot.viewByTitle(XML_MANAGER_TREE_ITEM_AND_SHELL_TITLE);
        assertNotNull(xmlManagerView);

        assertNotNull(xmlManagerView.bot().button(IMPORT_BUTTON_TEXT));
        assertNotNull(xmlManagerView.bot().button(REMOVE_BUTTON_TEXT));
        assertNotNull(xmlManagerView.bot().button(EDIT_BUTTON_TEXT));
    }
}
