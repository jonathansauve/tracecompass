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

package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;

/**
 * @author Jonathan Sauvé
 *
 */
@SuppressWarnings("nls")
public class XmlManagerViewer2 {

    /** The parent's composite */
    private Composite fparent;

    /** Other composites and controls */
    private SashForm xmlFilesAndActions;

    private static Tree xmlFilesTree;

    private Composite actionsComposite;
        private Button importXmlFile;
        private Button removeXmlFile;
        private Button editFile;

    /** Variables for the XmlFile*/

    /** Xml file folder and files */
    private static File activeXMLFolder = new File(XmlUtils.getXmlFilesPath().toString());
    private static File[] activeXMLs = activeXMLFolder.listFiles();

    /**
     *
     * @param parent
     *              The parent's composite
     */
    public XmlManagerViewer2(Composite parent) {
        fparent = parent;

        createContents();
        addListeners();
    }

    /**
     *
     * */
    private void createContents() {
        fparent.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesAndActions = new SashForm(fparent, SWT.HORIZONTAL);
        xmlFilesAndActions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesTree = new Tree(xmlFilesAndActions, SWT.V_SCROLL | SWT.H_SCROLL);
        for(int i = 0; i < activeXMLs.length; i++) {
            TreeItem xmlFileItem = new TreeItem(xmlFilesTree, SWT.NONE);
            xmlFileItem.setText(activeXMLs[i].getName());
            xmlFileItem.setData(XmlManagerStrings.fileKey, activeXMLs[i]);
        }


        actionsComposite = new Composite(xmlFilesAndActions, SWT.NONE);
        actionsComposite.setLayout(XmlManagerUtils.createGridLayout(1, 0, 5));
        actionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesAndActions.setWeights(new int[] {2, 1});

        importXmlFile = new Button(actionsComposite, SWT.PUSH);
        importXmlFile.setLayoutData(new GridData(70, 30));
        importXmlFile.setText("Import");

        removeXmlFile = new Button(actionsComposite, SWT.PUSH);
        removeXmlFile.setText("Remove");
        removeXmlFile.setLayoutData(new GridData(70, 30));

        editFile = new Button(actionsComposite, SWT.PUSH);
        editFile.setText("Edit");
        editFile.setLayoutData(new GridData(70, 30));
    }

    private void addListeners() {
        xmlFilesTree.addListener(SWT.Modify, XmlManagerListeners.xmlFilesTreeListener(xmlFilesTree, removeXmlFile, editFile));
        xmlFilesTree.addListener(SWT.Selection, XmlManagerListeners.xmlFilesTreeListener(xmlFilesTree, removeXmlFile, editFile));

        importXmlFile.addSelectionListener(XmlManagerListeners.importXmlFileSL(fparent, xmlFilesTree));

        removeXmlFile.addSelectionListener(XmlManagerListeners.removeXmlFileSL(xmlFilesTree));

        editFile.addSelectionListener(XmlManagerListeners.editXmlFileSL(fparent, xmlFilesTree));
    }

    /**
     * This function update the files in the tree.
     */
    public static void update() {
        TreeItem[] items = xmlFilesTree.getItems();
        activeXMLs = activeXMLFolder.listFiles();
        for(int i = 0; i < items.length; i++) {
            TreeItem item = items[i];
            for(int j = 0; j < activeXMLs.length; j++) {
                File file = activeXMLs[j];
                if(item.getText().equals(file.getName())) {
                    item.setData(XmlManagerStrings.fileKey, file);
                    break;
                }
            }
        }
    }
}
