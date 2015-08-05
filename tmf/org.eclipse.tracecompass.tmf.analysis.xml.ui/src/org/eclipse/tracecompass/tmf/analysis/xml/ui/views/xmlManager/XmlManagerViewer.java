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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.TmfXmlUiStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.ui.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectModelElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The view for the XML Manager.
 * TODO make the XML manager more MVC (Model, View, Controller)
 * @author Jonathan Sauvé
 * @since 1.0
 *
 */
@SuppressWarnings("restriction")
public class XmlManagerViewer {

    /** Global composite and sash */
    private Composite fparent;
    private SashForm sash;

    /** Xml files and folder */
    private File activeXMLFolder = new File(XmlUtils.getXmlFilesPath().toString());
    private File[] activeXMLs = activeXMLFolder.listFiles();

    /** Buffer the the copy in the XML file tree */
    private TreeItem copyBuffer;

    /** Variables for the file tree column */
    private Group group1;
    private Tree ftree;
    /* Menu for subtree elements */
    private Menu ftreeMenu;
    private MenuItem editItem;
    @SuppressWarnings("unused")
    private MenuItem separatorItem1;
    private MenuItem cloneItem;
    private MenuItem removeItem;
    @SuppressWarnings("unused")
    private MenuItem separatorItem2;
    private MenuItem copyItem;
    private MenuItem pasteItem;
    /* Map to keep the checked items */
    private static Map<Integer, TreeItem> selectedItems = new HashMap<>();
    /* Table to keep the current drag item */
    private static final Object[] currentDragSource = new Object[1];

    /** Variables for the properties column */
    private Group group2;
    private ScrolledComposite sc;
    private Composite groupProperties;

    /** Variables for the actions column */
    private Group group3;
    private Composite selectedFilesComposite;
    private Composite removeMergeComposite;
    private Button mergeFiles;
    private Button removeXml;

    /** Keys to retrieve saved objects */
    private static final String userDataFileKey = "userFile"; //$NON-NLS-1$
    private static final String nodeKey = "node"; //$NON-NLS-1$
    private static final String fileKey = "file"; //$NON-NLS-1$
    private static final String analysisIdKey = "analysisId"; //$NON-NLS-1$

    /**
     * Public constructor
     *
     * @param parent
     *            The current composite where the view is showed
     */
    public XmlManagerViewer(final Composite parent) {

        fparent = parent;

        Composite fComposite = new Composite(parent, SWT.NONE);
        fComposite.setLayout(createGridLayout(1, 0, 0));

        sash = new SashForm(fComposite, SWT.HORIZONTAL);
        sash.setLayout(createGridLayout(1, 0, 0));
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        /** Initialization of group 1 - Active XMLs Analysis */

        group1 = new Group(sash, SWT.SHADOW_ETCHED_IN);
        group1.setText("Active XMLs Analysis"); //$NON-NLS-1$
        group1.setLayout(createGridLayout(1, 0, 0));
        group1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        ftree = new Tree(group1, SWT.CHECK);
        ftree.setLayout(createGridLayout(1, 0, 0));
        ftree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        /* Initialize the tree */
        if (activeXMLs != null)
        {
            for (int i = 0; i < activeXMLs.length; i++)
            {
                addTreeItem(activeXMLs[i]);
            }
        }

        /*
         * Add a selection listener. This listener update the properties column
         * when the user selects a file or a sub-part
         */
        ftree.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem) event.item;
                if (event.detail == SWT.CHECK)
                {
                    checkItems(item, item.getChecked());
                    if (item.getChecked())
                    {
                        selectedItems.put(item.hashCode(), item);
                        if (item.getItemCount() != 0) // The entire file, the
                                                      // parent
                        {
                            boolean present = false;
                            for (int i = 0; i < item.getItemCount(); i++)
                            {
                                Control[] properties = groupProperties.getChildren();

                                for (int j = 0; j < properties.length; j++)
                                {
                                    if (((Node) properties[j].getData(nodeKey)).isEqualNode(((Node) (item.getItem(i).getData(nodeKey)))))
                                    {
                                        present = true;
                                    }
                                }
                                if (!present) {
                                    try {
                                        updateTreeData();
                                    } catch (ParserConfigurationException e) {
                                        e.printStackTrace();
                                    } catch (SAXException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    updatePropertiesOnInsert((Node) item.getItem(i).getData(nodeKey));
                                }
                                present = false;
                            }
                            updateActionsOnInsert((File) item.getData(fileKey));
                        }
                        else // the child
                        {
                            try {
                                updateTreeData();
                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                            } catch (SAXException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            updatePropertiesOnInsert((Node) item.getData(nodeKey));
                        }

                        sc.setContent(groupProperties);
                        sc.setExpandHorizontal(true);
                        sc.setExpandVertical(true);
                        sc.setMinSize(groupProperties.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                    }
                    else
                    {
                        selectedItems.remove(item.hashCode());
                        Control[] elementsActions = selectedFilesComposite.getChildren();
                        if (item.getItemCount() != 0) // the selected file is a
                                                      // parent
                        {
                            updatePropertiesOnRemove((File) item.getData(fileKey));

                            File selectFile = (File) item.getData(fileKey);

                            // remove selectedFile from actions column
                            if (elementsActions != null)
                            {
                                List<File> toDelete = new ArrayList<>();
                                for (int j = 0; j < elementsActions.length; j++)
                                {
                                    String fileName = ((File) elementsActions[j].getData(fileKey)).getName();

                                    if (fileName.equals(selectFile.getName()))
                                    {
                                        toDelete.add((File) elementsActions[j].getData(fileKey));
                                    }
                                }
                                if (toDelete.size() != 0)
                                {
                                    updateActionsOnRemove(toDelete);
                                }
                            }
                        }
                        else // we remove the properties of the child
                        {
                            try {
                                updateTreeData();
                            } catch (ParserConfigurationException e) {
                                e.printStackTrace();
                            } catch (SAXException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            updatePropertiesOnRemove((Node) item.getData(nodeKey));
                        }
                    }
                }
            }
        });

        /* Set the tree to allow drag and drop between sub-file elements */
        setTreeDragDrop(ftree);

        /* Create a menu for the tree :
         * Edit, clone, remove, copy and paste actions are implemented */

        ftreeMenu = new Menu(ftree);
        editItem = new MenuItem(ftreeMenu, SWT.NONE);
        editItem.setText("Edit"); //$NON-NLS-1$

        separatorItem1 = new MenuItem(ftreeMenu, SWT.SEPARATOR);

        cloneItem = new MenuItem(ftreeMenu, SWT.NONE);
        cloneItem.setText("Clone"); //$NON-NLS-1$
        cloneItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (ftree.getSelectionCount() == 0) {
                    return;
                }
                TreeItem sel = ftree.getSelection()[0];
                Object data = sel.getData(nodeKey);
                if (data != null) {
                    Node selData = (Node) data;
                    Node idNode = selData.getAttributes().getNamedItem(TmfXmlStrings.ID);
                    if (idNode != null) {
                        String idValue = idNode.getNodeValue();
                        StringModifierDialog sModDialog = new StringModifierDialog(fparent.getShell(), "Clone manager", selData.getNodeName(), //$NON-NLS-1$
                                idValue, "Two part of the same XML cannot have the same ID and the same type."); //$NON-NLS-1$
                        int ret = sModDialog.open();
                        if (ret != Window.OK) {
                            return;
                        }

                        selData.getAttributes().getNamedItem(TmfXmlStrings.ID).setNodeValue(sModDialog.getModifiedString());

                        IStatus appendStatus = XmlUtils.appendElementInFile(selData, selData.getParentNode(), (File) sel.getParentItem().getData(fileKey), false);
                        if (appendStatus.isOK()) {
                            TreeItem clone = new TreeItem(sel.getParentItem(), SWT.NONE);
                            clone.setText(selData.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue());
                            // clone.setData(sel.getData());
                            clone.setData(nodeKey, selData);
                            Node cloneData = (Node) clone.getData(nodeKey);
                            XmlUtils.appendElementInFile(cloneData, cloneData.getParentNode(), (File) clone.getParentItem().getData(fileKey), true);
                        }
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        removeItem = new MenuItem(ftreeMenu, SWT.NONE);
        removeItem.setText("Remove"); //$NON-NLS-1$
        removeItem.addSelectionListener(new SelectionListener(
                ) {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (ftree.getSelectionCount() == 0) {
                            return;
                        }
                        TreeItem sel = ftree.getSelection()[0];
                        Object data = sel.getData(nodeKey);
                        if (data != null) {
                            Node selData = (Node) data;

                            IStatus removeStatus = XmlUtils.removeElementFromFile(selData, selData.getParentNode(), (File) sel.getParentItem().getData(fileKey), false);
                            if (removeStatus.isOK()) {
                                removeStatus = XmlUtils.removeElementFromFile(selData, selData.getParentNode(), (File) sel.getParentItem().getData(fileKey), true);
                                if (removeStatus.isOK()) {
                                    Control[] groupPropertiesChildren = groupProperties.getChildren();
                                    for (int i = 0; i < groupPropertiesChildren.length; i++) {
                                        if (selData.isEqualNode((Node) groupPropertiesChildren[i].getData(nodeKey))) {
                                            groupPropertiesChildren[i].dispose();
                                            break;
                                        }
                                    }
                                    sel.dispose();
                                }
                            }
                        }
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });

        separatorItem2 = new MenuItem(ftreeMenu, SWT.SEPARATOR);

        copyItem = new MenuItem(ftreeMenu, SWT.NONE);
        copyItem.setText("Copy"); //$NON-NLS-1$
        copyItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (ftree.getSelectionCount() == 0) {
                    return;
                }
                TreeItem sel = ftree.getSelection()[0];
                Object data = sel.getData(nodeKey);
                if (data != null) {
                    copyBuffer = ftree.getSelection()[0];
                    if (!pasteItem.isEnabled()) {
                        pasteItem.setEnabled(true);
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        pasteItem = new MenuItem(ftreeMenu, SWT.NONE);
        pasteItem.setText("Paste"); //$NON-NLS-1$
        pasteItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (ftree.getSelectionCount() == 0) {
                    return;
                }
                TreeItem pasteTreeItem = ftree.getSelection()[0];
                String newText = copyBuffer.getText();

                Node copyBufferData = (Node) copyBuffer.getData(nodeKey);
                Object pasteFileData = pasteTreeItem.getData(fileKey);
                Object pasteNodeData = pasteTreeItem.getData(nodeKey);

                // drop on the file item
                if (pasteFileData != null) {
                    IStatus appendStatus = XmlUtils.appendElementInFile(copyBufferData, copyBufferData.getParentNode(), (File) pasteFileData, false);
                    if (appendStatus.isOK()) {
                        TreeItem newItem = new TreeItem(pasteTreeItem, SWT.NONE);
                        newItem.setText(newText);

                        // newItem.setData(copyBufferData);
                        newItem.setData(nodeKey, copyBufferData);
                        // Add the node to the XML tree (paste)
                        XmlUtils.appendElementInFile(copyBufferData, copyBufferData.getParentNode(), (File) pasteFileData, true);
                    }
                }
                // drop on the a node of a file
                else if (pasteNodeData != null) {
                    IStatus appendStatus = XmlUtils.appendElementInFile(copyBufferData, copyBufferData.getParentNode(), (File) pasteTreeItem.getParentItem().getData(fileKey), false);
                    if (appendStatus.isOK()) {
                        TreeItem newItem = new TreeItem(pasteTreeItem.getParentItem(), SWT.NONE);
                        newItem.setText(newText);

                        // newItem.setData(copyBufferData);
                        newItem.setData(nodeKey, copyBufferData);
                        // Add the node to the XML tree (paste)
                        XmlUtils.appendElementInFile(copyBufferData, copyBufferData.getParentNode(), (File) pasteTreeItem.getParentItem().getData(fileKey), true);
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        pasteItem.setEnabled(false);

        ftree.setMenu(ftreeMenu);

        /** Initialization of group 2 - Properties */
        group2 = new Group(sash, SWT.V_SCROLL);
        group2.setText("Properties"); //$NON-NLS-1$
        group2.setLayout(createGridLayout(1, 0, 0));
        group2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        sc = new ScrolledComposite(group2, SWT.V_SCROLL | SWT.H_SCROLL);
        sc.setLayout(createGridLayout(1, 0, 0));
        sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        groupProperties = new Composite(sc, SWT.NONE);
        groupProperties.setLayout(createGridLayout(1, 0, 0));
        groupProperties.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        /** Initialization of group 3 - Actions */

        group3 = new Group(sash, SWT.SHADOW_ETCHED_IN);
        group3.setText("Actions"); //$NON-NLS-1$
        group3.setLayout(createGridLayout(1, 0, 0));
        group3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite importCreate = new Composite(group3, SWT.NONE);
        importCreate.setLayout(createGridLayout(2, 0, 0));

        Button importXml = new Button(importCreate, SWT.PUSH);
        importXml.setText("Import XML Analysis"); //$NON-NLS-1$

        /* Listener to import a new xml analysis */
        importXml.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(fparent.getShell());
                dialog.setText("Import XML Analysis"); //$NON-NLS-1$
                String[] extensions = { "*.xml" }; //$NON-NLS-1$
                String[] extNames = { "Import XML Analysis File (*.xml)" }; //$NON-NLS-1$
                dialog.setFilterExtensions(extensions);
                dialog.setFilterNames(extNames);

                String filePath = dialog.open();
                if (filePath != null)
                {
                    File xml = new File(filePath);
                    IStatus status = XmlUtils.xmlValidate(xml);
                    if (status.isOK())
                    {
                        IStatus addStatus = XmlUtils.addXmlFile(xml);
                        if (!addStatus.isOK())
                        {
                            ErrorDialog.openError(fparent.getShell(), "Import error", "Error when addind the file", addStatus); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        else
                        {
                            XmlAnalysisModuleSource.notifyModuleChange();
                            updateActiveXMLsOnImport();
                            /*
                             * FIXME: It refreshes the list of analysis under a
                             * trace, but since modules are instantiated when
                             * the trace opens, the changes won't apply to an
                             * opened trace, it needs to be closed then reopened
                             */
                            refreshProject();
                        }
                    }
                    else
                    {
                        ErrorDialog.openError(fparent.getShell(), "Import error", "The file is not a valid XML file", status); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }

            private void refreshProject() {
                // Check if we are closing down
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (window == null) {
                    return;
                }

                // Get the selection
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                IWorkbenchPart part = page.getActivePart();
                if (part == null) {
                    return;
                }
                ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
                if (selectionProvider == null) {
                    return;
                }
                ISelection selection = selectionProvider.getSelection();

                if (selection instanceof TreeSelection) {
                    TreeSelection sel = (TreeSelection) selection;
                    // There should be only one item selected as per the
                    // plugin.xml
                    Object element = sel.getFirstElement();
                    if (element instanceof TmfProjectModelElement) {
                        ((TmfProjectModelElement) element).getProject().refresh();
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        Button newXml = new Button(importCreate, SWT.PUSH);
        newXml.setText("Create new XML Analysis"); //$NON-NLS-1$

        /*
         * Listener to create a new xml analysis. TODO Open an XML editor in
         * Eclipse instead of gedit
         */
        newXml.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                XmlTemplateChooserViewer tc = new XmlTemplateChooserViewer(parent.getShell());
                int ret = tc.open();

                List<String> templates = tc.getChoosedTemplates();

                Runtime runTime = Runtime.getRuntime();
                try {
                    String[] cmd = new String[templates.size() + 1];
                    cmd[0] = "gedit"; //$NON-NLS-1$
                    for (int i = 0; i < templates.size(); i++)
                    {
                        cmd[i + 1] = templates.get(i);
                    }
                    if (ret == 0)
                    {
                        runTime.exec(cmd);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        @SuppressWarnings("unused")
        Label separator = new Label(group3, SWT.HORIZONTAL | SWT.SEPARATOR | SWT.FILL);

        Label selectedFiles = new Label(group3, SWT.NONE);
        selectedFiles.setText("Selected files:"); //$NON-NLS-1$

        selectedFilesComposite = new Composite(group3, SWT.NONE);
        selectedFilesComposite.setLayout(createGridLayout(1, 15, 5));

        removeMergeComposite = new Composite(group3, SWT.NONE);
        removeMergeComposite.setLayout(createGridLayout(2, 0, 0));

        removeXml = new Button(removeMergeComposite, SWT.PUSH);
        removeXml.setText("Remove XML Analysis"); //$NON-NLS-1$
        removeXml.setEnabled(false);

        /* Listener to remove a selected xml file */
        removeXml.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // get the selected files
                List<Object> selectFiles = new ArrayList<>();
                TreeItem[] files = ftree.getItems();
                for (int i = 0; i < files.length; i++)
                {
                    if (files[i].getChecked() == true)
                    {
                        selectFiles.add(files[i].getData(fileKey));
                    }
                }

                for (int i = 0; i < selectFiles.size(); i++)
                {
                    Control[] elementsActions = selectedFilesComposite.getChildren();
                    XmlUtils.removeXmlFile((File) selectFiles.get(i));

                    File selectedFile = (File) selectFiles.get(i);

                    updatePropertiesOnRemove(selectedFile);

                    // remove selectedFile from actions column
                    if (elementsActions != null)
                    {
                        List<File> toDelete = new ArrayList<>();
                        for (int j = 0; j < elementsActions.length; j++)
                        {
                            String fileName = ((File) elementsActions[j].getData(fileKey)).getName();

                            if (fileName.equals(selectedFile.getName()))
                            {
                                toDelete.add((File) elementsActions[j].getData(fileKey));
                            }
                        }
                        if (toDelete.size() != 0)
                        {
                            updateActionsOnRemove(toDelete);
                        }
                    }

                }

                updateActiveXMLsOnRemove();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        mergeFiles = new Button(removeMergeComposite, SWT.PUSH);
        mergeFiles.setText("Merge XMLs Analysis"); //$NON-NLS-1$
        mergeFiles.setToolTipText("Click to merge selected files. You can only merge 2 files at a time."); //$NON-NLS-1$
        mergeFiles.setEnabled(false);

        // TODO merge files view - not finished
        mergeFiles.addSelectionListener(new XmlMergeSelectionAdapter(fparent.getShell()));

        /* Disable this functionnality */
        mergeFiles.setEnabled(false);
        // TODO Find a way to close the trace tab and reoppened it.
    }

    /**
     * This function update the <code>ftree</code> of active XML files. It
     * should be called when a new analysis is added.
     */
    private void updateActiveXMLsOnImport()
    {
        activeXMLs = activeXMLFolder.listFiles();
        boolean present = false;
        int count = 0;
        TreeItem[] items = ftree.getItems();

        if (activeXMLs != null)
        {
            for (int i = 0; i < activeXMLs.length; i++) // We insert files into
                                                        // the tree
            {
                for (int j = 0; j < items.length; j++)
                {
                    if (activeXMLs[i].getName().equals(((File) (items[j].getData(fileKey))).getName())) // if
                                                                                                        // the
                                                                                                        // files
                                                                                                        // is
                                                                                                        // already
                                                                                                        // there,
                    { // we don't had it
                        present = true;
                        count++;
                        break;
                    }
                }
                if (!present) // Add the new file
                {
                    addTreeItem(activeXMLs[i]);
                }
                present = false;
            }
            if (count == activeXMLs.length)
            {
                ErrorDialog.openError(fparent.getShell(), "Import error", "Impossible action", new Status(IStatus.ERROR, Activator.PLUGIN_ID, "This XML analysis is already active")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
    }

    /**
     * This function update the <code>ftree</code> of active XML files. It
     * should be called when an XML analysis is removed.
     */
    private void updateActiveXMLsOnRemove()
    {
        activeXMLs = activeXMLFolder.listFiles();
        boolean present = false;
        TreeItem[] items = ftree.getItems();

        if (activeXMLs != null)
        {
            for (int i = 0; i < items.length; i++)
            {
                for (int j = 0; j < activeXMLs.length; j++)
                {
                    if (((File) items[i].getData(fileKey)).getName().equals(activeXMLs[j].getName()))
                    {
                        present = true;
                        break;
                    }
                }
                if (!present)
                {
                    items[i].dispose();
                }
                present = false;
            }
        }
    }

    /**
     * This function append a new group to the <code>groupProperties</code>
     * composite. It does not check if the group associate to the
     * <code>root</code> is already present.
     *
     * @param root
     *            The file we want to show properties
     */
    private void updatePropertiesOnInsert(final Node root)
    {
        Composite group = new Composite(groupProperties, SWT.NONE);
        group.setLayout(createGridLayout(1, 5, 5));
        group.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true));
        group.setData(nodeKey, root);

        if (root.getNodeName() == TmfXmlUiStrings.TIME_GRAPH_VIEW)
        {
            Composite titleComposite = new Composite(group, SWT.NONE);
            titleComposite.setLayout(createGridLayout(1, 5, 5));
            titleComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

            Label title = new Label(titleComposite, SWT.NONE);
            title.setText("-- TIME GRAPH VIEW SECTION --"); //$NON-NLS-1$

            Composite assFileComp = new Composite(group, SWT.NONE);
            assFileComp.setLayout(createGridLayout(1, 5, 5));
            assFileComp.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true));

            Label associateFile = new Label(assFileComp, SWT.NONE);

            associateFile.setText("File: " + ((File)(root.getUserData(userDataFileKey))).getName()); //$NON-NLS-1$

            Composite IDAndIDValue = new Composite(group, SWT.NONE);
            IDAndIDValue.setLayout(createGridLayout(2, 5, 5));

            Label ID = new Label(IDAndIDValue, SWT.NONE);
            ID.setText("ID: "); //$NON-NLS-1$

            Label IDValue = new Label(IDAndIDValue, SWT.NONE);
            IDValue.setText(root.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue());

            FontData fontData = IDValue.getFont().getFontData()[0];
            Font font = new Font(fparent.getDisplay(), new FontData(fontData.getName(), fontData
                    .getHeight(), SWT.ITALIC));
            IDValue.setFont(font);

            Label processStatusTitle = null;
            Composite definedValue = null;

            NodeList childs = root.getChildNodes();

            for (int i = 0; i < childs.getLength(); i++)
            {
                if (childs.item(i).getNodeName() == TmfXmlStrings.HEAD)
                {
                    NodeList headChilds = childs.item(i).getChildNodes();

                    for (int j = 0; j < headChilds.getLength(); j++)
                    {
                        if (headChilds.item(j).getNodeName().equals(TmfXmlStrings.ANALYSIS))
                        {
                            Composite analysisIDComposite = new Composite(group, SWT.NONE);
                            analysisIDComposite.setLayout(createGridLayout(1, 5, 5));

                            Label analysisID = new Label(analysisIDComposite, SWT.NONE);
                            analysisID.setText("Analysis ID: " + headChilds.item(j).getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue()); //$NON-NLS-1$
                        }
                        if (headChilds.item(j).getNodeName().equals(TmfXmlStrings.LABEL))
                        {
                            Composite label = new Composite(group, SWT.NONE);
                            label.setLayout(createGridLayout(4, 5, 5));

                            Label graphTitle = new Label(label, SWT.NONE);
                            graphTitle.setText("Graph title: "); //$NON-NLS-1$

                            final Text text = new Text(label, SWT.WRAP | SWT.BORDER);
                            final String initialTitle = headChilds.item(j).getAttributes().getNamedItem(TmfXmlStrings.VALUE).getNodeValue();
                            text.setLayoutData(new GridData(150, 19));
                            text.setText(initialTitle);

                            Button resetText = new Button(label, SWT.WRAP | SWT.PUSH);
                            resetText.setText("Reset"); //$NON-NLS-1$
                            resetText.setData(nodeKey, headChilds.item(j));

                            resetText.addSelectionListener(new SelectionListener() {

                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    Button button = (Button) e.widget;
                                    Node oldNode = (Node) button.getData(nodeKey);

                                    if (!initialTitle.equals(text.getText()))
                                    {
                                        text.setText(initialTitle);
                                        try {
                                            File copyFile = (File) root.getUserData(userDataFileKey);
                                            XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlStrings.VALUE, text.getText());
                                        } catch (ParserConfigurationException e1) {
                                            e1.printStackTrace();
                                        } catch (SAXException e1) {
                                            e1.printStackTrace();
                                        } catch (IOException e1) {
                                            e1.printStackTrace();
                                        } catch (TransformerException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void widgetDefaultSelected(SelectionEvent e) {
                                    widgetSelected(e);
                                }
                            });

                            Button saveText = new Button(label, SWT.WRAP | SWT.PUSH);
                            saveText.setText("Save title"); //$NON-NLS-1$
                            saveText.setData(nodeKey, headChilds.item(j));

                            saveText.addSelectionListener(new SelectionListener() {

                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    Button button = (Button) e.widget;
                                    Node oldNode = (Node) button.getData(nodeKey);

                                    if (!initialTitle.equals(text.getText()))
                                    {
                                        try {
                                            File copyFile = (File) root.getUserData(userDataFileKey);
                                            XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlStrings.VALUE, text.getText());
                                        } catch (ParserConfigurationException e1) {
                                            e1.printStackTrace();
                                        } catch (SAXException e1) {
                                            e1.printStackTrace();
                                        } catch (IOException e1) {
                                            e1.printStackTrace();
                                        } catch (TransformerException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void widgetDefaultSelected(SelectionEvent e) {
                                    widgetSelected(e);
                                }
                            });
                        }
                    }
                }

                if (childs.item(i).getNodeName() == TmfXmlStrings.DEFINED_VALUE)
                {
                    RowLayout rowl = new RowLayout();
                    rowl.spacing = 0;
                    rowl.marginBottom = 0;
                    rowl.marginTop = 0;
                    rowl.wrap = false;
                    rowl.marginLeft = 0;
                    rowl.marginRight = 0;

                    /* Process status labels */
                    if (processStatusTitle == null)
                    {
                        processStatusTitle = new Label(group, SWT.NONE);
                        processStatusTitle.setText("Process Status Colors:"); //$NON-NLS-1$

                        FontData fontData2 = processStatusTitle.getFont().getFontData()[0];
                        Font font2 = new Font(fparent.getDisplay(), new FontData(fontData2.getName(), fontData2
                                .getHeight(), SWT.BOLD));
                        processStatusTitle.setFont(font2);
                    }

                    if (definedValue == null)
                    {
                        definedValue = new Composite(group, SWT.NONE);
                        definedValue.setLayout(createGridLayout(2, 5, 5));
                    }

                    Composite definedValueLabel = new Composite(definedValue, SWT.NONE);
                    definedValueLabel.setLayout(rowl);

                    Label label = new Label(definedValueLabel, SWT.NONE);
                    label.setText(childs.item(i).getAttributes().getNamedItem(TmfXmlStrings.NAME).getNodeValue() + " : "); //$NON-NLS-1$

                    final Label c = new Label(definedValueLabel, SWT.READ_ONLY | SWT.BORDER);
                    c.setData(nodeKey, childs.item(i));
                    String stringColor = childs.item(i).getAttributes().getNamedItem(TmfXmlStrings.COLOR).getNodeValue();
                    final RGB oldRgb = new RGB(XmlManagerUtils.hexaToRed(stringColor), XmlManagerUtils.hexaToGreen(stringColor),
                            XmlManagerUtils.hexaToBlue(stringColor));
                    Color color = new Color(fparent.getDisplay(), oldRgb);

                    /* Color of the process status */
                    c.setText("          "); //$NON-NLS-1$
                    c.setBackground(color);

                    Composite definedValueActions = new Composite(definedValue, SWT.NONE);
                    definedValueActions.setLayout(rowl);

                    /* Reset - get the original color */
                    Button buttonReset = new Button(definedValueActions, SWT.PUSH);
                    buttonReset.setText("Reset"); //$NON-NLS-1$
                    buttonReset.setData(nodeKey, childs.item(i));

                    buttonReset.addSelectionListener(new SelectionListener() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            Button button = (Button) e.widget;
                            Node oldNode = (Node) button.getData(nodeKey);

                            if (!c.getBackground().getRGB().equals(oldRgb))
                            {
                                c.setBackground(new Color(fparent.getDisplay(), oldRgb));
                                try {
                                    File copyFile = (File) root.getUserData(userDataFileKey);
                                    XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlStrings.COLOR,
                                            XmlManagerUtils.rgbToHexa(oldRgb.red, oldRgb.green, oldRgb.blue));
                                } catch (ParserConfigurationException e1) {
                                    e1.printStackTrace();
                                } catch (SAXException e1) {
                                    e1.printStackTrace();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (TransformerException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void widgetDefaultSelected(SelectionEvent e) {
                            widgetSelected(e);
                        }
                    });

                    /* Button to change color */
                    Button buttonColor = new Button(definedValueActions, SWT.PUSH);
                    buttonColor.setText("Change color"); //$NON-NLS-1$
                    buttonColor.setData(nodeKey, childs.item(i)); // The node
                                                                  // associate
                                                                  // with the
                                                                  // button

                    buttonColor.addSelectionListener(new SelectionListener() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            Button button = (Button) e.widget;
                            Node oldNode = (Node) button.getData(nodeKey);

                            ColorDialog dialog = new ColorDialog(fparent.getDisplay().getActiveShell());
                            dialog.setRGB(oldRgb);
                            dialog.setText("Choose a new color"); //$NON-NLS-1$

                            RGB newRgb = dialog.open();
                            if (newRgb != null)
                            {
                                c.setBackground(new Color(fparent.getDisplay(), newRgb));
                                try {
                                    File copyFile = (File) root.getUserData(userDataFileKey);
                                    XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlStrings.COLOR,
                                            XmlManagerUtils.rgbToHexa(newRgb.red, newRgb.green, newRgb.blue));
                                } catch (ParserConfigurationException e1) {
                                    e1.printStackTrace();
                                } catch (SAXException e1) {
                                    e1.printStackTrace();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (TransformerException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void widgetDefaultSelected(SelectionEvent e) {
                            widgetSelected(e);
                        }
                    });
                }

                if (childs.item(i).getNodeName() == TmfXmlUiStrings.ENTRY_ELEMENT)
                {
                    Label entryTitle = new Label(group, SWT.NONE);
                    entryTitle.setText("Shown information:"); //$NON-NLS-1$

                    FontData fontData3 = entryTitle.getFont().getFontData()[0];
                    Font font3 = new Font(fparent.getDisplay(), new FontData(fontData3.getName(), fontData3
                            .getHeight(), SWT.BOLD));
                    entryTitle.setFont(font3);

                    Composite currentPathComposite = new Composite(group, SWT.NONE);
                    currentPathComposite.setLayout(createGridLayout(3, 5, 5));

                    Label currentPath = new Label(currentPathComposite, SWT.NONE);
                    currentPath.setText("Current entry path: "); //$NON-NLS-1$

                    final Label currentPathValue = new Label(currentPathComposite, SWT.BORDER);
                    currentPathValue.setText(childs.item(i).getAttributes().getNamedItem(TmfXmlUiStrings.PATH).getNodeValue());
                    currentPathValue.setLayoutData(new GridData(200, 20));

                    Button buildPath = new Button(currentPathComposite, SWT.PUSH);
                    buildPath.setText("Build path"); //$NON-NLS-1$
                    buildPath.setData(nodeKey, childs.item(i));

                    buildPath.addSelectionListener(new SelectionListener() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            Button button = (Button) e.widget;
                            Node oldNode = (Node) button.getData(nodeKey);

                            StateSystemPathBuilderViewer path = new StateSystemPathBuilderViewer(fparent.getShell());
                            // Check if the user have an active trace before
                            // opening
                            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();

                            if (trace != null) {
                                int returnCode = path.open();
                                if (returnCode == Window.OK) {
                                    currentPathValue.setText(path.getBuildPath());
                                    File copyFile = (File) root.getUserData(userDataFileKey);
                                    try {
                                        XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlUiStrings.PATH, path.getBuildPath());
                                    } catch (ParserConfigurationException e1) {
                                        e1.printStackTrace();
                                    } catch (SAXException e1) {
                                        e1.printStackTrace();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    } catch (TransformerException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                            else {
                                ErrorDialog.openError(fparent.getShell(), "Open error", //$NON-NLS-1$
                                        "An error occured when opening the State System Path Builder", //$NON-NLS-1$
                                        new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No active trace")); //$NON-NLS-1$
                            }
                        }

                        @Override
                        public void widgetDefaultSelected(SelectionEvent e) {
                            widgetSelected(e);
                        }
                    });

                    createEntryTable(root, group, childs.item(i));
                }
            }
        }

        if (root.getNodeName() == TmfXmlUiStrings.XY_VIEW)
        {
            Composite titleComposite = new Composite(group, SWT.NONE);
            titleComposite.setLayout(createGridLayout(1, 5, 5));
            titleComposite.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));

            Label title = new Label(titleComposite, SWT.NONE);
            title.setText("-- XY VIEW SECTION --"); //$NON-NLS-1$

            Composite associateFileComp = new Composite(group, SWT.NONE);
            associateFileComp.setLayout(createGridLayout(1, 5, 5));

            Label associateFile = new Label(associateFileComp, SWT.NONE);
            associateFile.setText("File: " + ((File) (root.getUserData(userDataFileKey))).getName()); //$NON-NLS-1$

            Composite IDAndValue = new Composite(group, SWT.NONE);
            IDAndValue.setLayout(createGridLayout(2, 5, 5));

            Label ID = new Label(IDAndValue, SWT.NONE);
            ID.setText("ID: "); //$NON-NLS-1$

            Label IDValue = new Label(IDAndValue, SWT.NONE);
            IDValue.setText(root.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue());

            FontData fontData = IDValue.getFont().getFontData()[0];
            Font font = new Font(fparent.getDisplay(), new FontData(fontData.getName(), fontData
                    .getHeight(), SWT.ITALIC));
            IDValue.setFont(font);

            NodeList childs = root.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++)
            {
                // Show head information
                if (childs.item(i).getNodeName().equals(TmfXmlStrings.HEAD))
                {
                    NodeList headChilds = childs.item(i).getChildNodes();
                    for (int j = 0; j < headChilds.getLength(); j++)
                    {
                        if (headChilds.item(j).getNodeName().equals(TmfXmlStrings.ANALYSIS))
                        {
                            Composite analysisIDComposite = new Composite(group, SWT.NONE);
                            analysisIDComposite.setLayout(createGridLayout(1, 5, 5));

                            Label analysisID = new Label(analysisIDComposite, SWT.NONE);
                            analysisID.setText("Analysis ID: " + headChilds.item(j).getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue());  //$NON-NLS-1$
                        }
                        if (headChilds.item(j).getNodeName().equals(TmfXmlStrings.LABEL))
                        {
                            Composite label = new Composite(group, SWT.NONE);
                            label.setLayout(createGridLayout(4, 5, 5));

                            Label graphTitle = new Label(label, SWT.NONE);
                            graphTitle.setText("Graph title: "); //$NON-NLS-1$

                            final Text text = new Text(label, SWT.WRAP | SWT.BORDER);
                            final String initialTitle = headChilds.item(j).getAttributes().getNamedItem(TmfXmlStrings.VALUE).getNodeValue();
                            text.setLayoutData(new GridData(150, 19));
                            text.setText(initialTitle);

                            Button resetText = new Button(label, SWT.WRAP | SWT.PUSH);
                            resetText.setText("Reset"); //$NON-NLS-1$
                            resetText.setData(nodeKey, headChilds.item(j));

                            resetText.addSelectionListener(new SelectionListener() {

                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    Button button = (Button) e.widget;
                                    Node oldNode = (Node) button.getData(nodeKey);

                                    if (!initialTitle.equals(text.getText()))
                                    {
                                        text.setText(initialTitle);
                                        try {
                                            File copyFile = (File) root.getUserData(userDataFileKey);
                                            XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlStrings.VALUE, text.getText());
                                        } catch (ParserConfigurationException e1) {
                                            e1.printStackTrace();
                                        } catch (SAXException e1) {
                                            e1.printStackTrace();
                                        } catch (IOException e1) {
                                            e1.printStackTrace();
                                        } catch (TransformerException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void widgetDefaultSelected(SelectionEvent e) {
                                    widgetSelected(e);
                                }
                            });

                            Button saveText = new Button(label, SWT.WRAP | SWT.PUSH);
                            saveText.setText("Save title"); //$NON-NLS-1$
                            saveText.setData(nodeKey, headChilds.item(j));

                            saveText.addSelectionListener(new SelectionListener() {

                                @Override
                                public void widgetSelected(SelectionEvent e) {
                                    Button button = (Button) e.widget;
                                    Node oldNode = (Node) button.getData(nodeKey);

                                    if (!initialTitle.equals(text.getText()))
                                    {
                                        try {
                                            File copyFile = (File) root.getUserData(userDataFileKey);
                                            XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlStrings.VALUE, text.getText());
                                        } catch (ParserConfigurationException e1) {
                                            e1.printStackTrace();
                                        } catch (SAXException e1) {
                                            e1.printStackTrace();
                                        } catch (IOException e1) {
                                            e1.printStackTrace();
                                        } catch (TransformerException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void widgetDefaultSelected(SelectionEvent e) {
                                    widgetSelected(e);
                                }
                            });
                        }
                    }
                }

                // Show entry information
                if (childs.item(i).getNodeName().equals(TmfXmlUiStrings.ENTRY_ELEMENT))
                {
                    Label entryTitle = new Label(group, SWT.NONE);
                    entryTitle.setText("Shown information:"); //$NON-NLS-1$

                    FontData fontData3 = entryTitle.getFont().getFontData()[0];
                    Font font3 = new Font(fparent.getDisplay(), new FontData(fontData3.getName(), fontData3
                            .getHeight(), SWT.BOLD));
                    entryTitle.setFont(font3);

                    Composite currentPathComposite = new Composite(group, SWT.NONE);
                    currentPathComposite.setLayout(createGridLayout(2, 5, 5));

                    Label currentPath = new Label(currentPathComposite, SWT.NONE);
                    currentPath.setText("Current entry path: "); //$NON-NLS-1$

                    final Label currentPathValue = new Label(currentPathComposite, SWT.BORDER);
                    currentPathValue.setText(childs.item(i).getAttributes().getNamedItem(TmfXmlUiStrings.PATH).getNodeValue());
                    currentPathValue.setLayoutData(new GridData(200, 20));

                    Composite buildPathComposite = new Composite(group, SWT.NONE);
                    buildPathComposite.setLayout(createGridLayout(1, 5, 5));

                    Button buildPath = new Button(buildPathComposite, SWT.PUSH);
                    buildPath.setText("Build path"); //$NON-NLS-1$

                    buildPath.addSelectionListener(new SelectionListener() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            Button button = (Button) e.widget;
                            Node oldNode = (Node) button.getData(nodeKey);

                            StateSystemPathBuilderViewer path = new StateSystemPathBuilderViewer(fparent.getShell());
                            // Check if the user have an active trace before
                            // opening
                            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();

                            if (trace != null) {
                                int returnCode = path.open();
                                if (returnCode == Window.OK) {
                                    currentPathValue.setText(path.getBuildPath());
                                    File copyFile = (File) root.getUserData(userDataFileKey);
                                    try {
                                        XmlUtils.setNewAttribute(copyFile, oldNode, TmfXmlUiStrings.PATH, path.getBuildPath());
                                    } catch (ParserConfigurationException e1) {
                                        e1.printStackTrace();
                                    } catch (SAXException e1) {
                                        e1.printStackTrace();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    } catch (TransformerException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                            else {
                                ErrorDialog.openError(fparent.getShell(), "Open error", //$NON-NLS-1$
                                        "An error occured when opening the State System Path Builder", //$NON-NLS-1$
                                        new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No active trace")); //$NON-NLS-1$
                            }
                        }

                        @Override
                        public void widgetDefaultSelected(SelectionEvent e) {
                            widgetSelected(e);
                        }
                    });

                    createEntryTable(root, group, childs.item(i));
                }

            }
        }

        if (root.getNodeName() == TmfXmlStrings.STATE_PROVIDER)
        {
            // TODO Add the new for the state provider modification (Simon
            // Delisle?)
            Composite titleComposite = new Composite(group, SWT.NONE);
            titleComposite.setLayout(createGridLayout(1, 5, 5));
            titleComposite.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));

            Label title = new Label(titleComposite, SWT.NONE);
            title.setText("-- STATE PROVIDER SECTION --"); //$NON-NLS-1$

            Composite associateFileComp = new Composite(group, SWT.NONE);
            associateFileComp.setLayout(createGridLayout(1, 5, 5));

            Label associateFile = new Label(associateFileComp, SWT.NONE);
            associateFile.setText("File: " + ((File) (root.getUserData(userDataFileKey))).getName()); //$NON-NLS-1$

            Composite IDAndVersionAndValues = new Composite(group, SWT.NONE);
            IDAndVersionAndValues.setLayout(createGridLayout(4, 5, 5));

            Label ID = new Label(IDAndVersionAndValues, SWT.NONE);
            ID.setText("ID: "); //$NON-NLS-1$

            Label IDValue = new Label(IDAndVersionAndValues, SWT.NONE);
            IDValue.setText(root.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue());

            FontData fontData = IDValue.getFont().getFontData()[0];
            Font font = new Font(fparent.getDisplay(), new FontData(fontData.getName(), fontData
                    .getHeight(), SWT.ITALIC));
            IDValue.setFont(font);

            Label version = new Label(IDAndVersionAndValues, SWT.NONE);
            version.setText("Version: "); //$NON-NLS-1$

            Label versionValue = new Label(IDAndVersionAndValues, SWT.NONE);
            versionValue.setText(root.getAttributes().getNamedItem(TmfXmlStrings.VERSION).getNodeValue());
        }
    }

    /**
     * This function update the properties column when the user un-selected an
     * entire file from <code>ftree</code>
     *
     * @param file
     *            The file being remove
     *
     */
    private void updatePropertiesOnRemove(File file)
    {
        Control[] elementsProperties = groupProperties.getChildren();
        if (elementsProperties != null)
        {
            List<Node> toDelete = new ArrayList<>();
            for (int j = 0; j < elementsProperties.length; j++)
            {
                /*
                 * Check if the child is link to the selected file Data of
                 * composite : Node (root) User data of Node : File (associate)
                 * File : Name (String)
                 */
                String fileName = ((File) ((Node) ((Composite) elementsProperties[j]).getData(nodeKey)).getUserData(userDataFileKey)).getName();

                if (file.getName().equals(fileName))
                {
                    // Add the node to remove to toDelete list
                    toDelete.add((Node) elementsProperties[j].getData(nodeKey));
                }
            }
            if (!toDelete.isEmpty())
            {
                // Retrieve node who's not in toDelete list
                Control[] childs = groupProperties.getChildren();
                List<Node> toKeep = new ArrayList<>();
                boolean toBeDeleted = false;
                for (int i = 0; i < childs.length; i++)
                {
                    for (int j = 0; j < toDelete.size(); j++)
                    {
                        if (((Node) childs[i].getData(nodeKey)).equals(toDelete.get(j)))
                        {
                            toBeDeleted = true;
                            break;
                        }
                    }
                    if (!toBeDeleted)
                    {
                        toKeep.add((Node) childs[i].getData(nodeKey));
                    }
                    toBeDeleted = false;
                }

                // Dispose all properties and re-insert nodes
                for (int i = 0; i < childs.length; i++)
                {
                    childs[i].dispose();
                }

                for (int i = 0; i < toKeep.size(); i++)
                {
                    updatePropertiesOnInsert(toKeep.get(i));
                }
                sc.setContent(groupProperties);
                sc.setExpandHorizontal(true);
                sc.setExpandVertical(true);
                sc.setMinSize(groupProperties.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        }
    }

    /**
     * This function update the properties column when the user un-selected a
     * node (file child in <code>ftree</code>
     *
     * @param node
     *            The node to remove
     */
    private void updatePropertiesOnRemove(Node node)
    {
        Control[] elementsProperties = groupProperties.getChildren();
        if (elementsProperties != null)
        {
            List<Node> toDelete = new ArrayList<>();
            for (int j = 0; j < elementsProperties.length; j++)
            {
                String nodeName = ((Node) ((Composite) elementsProperties[j]).getData(nodeKey)).getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue();

                // True if the ID's node are the same
                if (node.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue().equals(nodeName))
                {
                    // Add the node to remove to toDelete list
                    toDelete.add((Node) elementsProperties[j].getData(nodeKey));
                }
            }
            if (!toDelete.isEmpty())
            {
                // Retrieve node who's not in toDelete list
                Control[] childs = groupProperties.getChildren();
                List<Node> toKeep = new ArrayList<>();
                boolean toBeDeleted = false;
                for (int i = 0; i < childs.length; i++)
                {
                    for (int j = 0; j < toDelete.size(); j++)
                    {
                        if (((Node) childs[i].getData(nodeKey)).equals(toDelete.get(j)))
                        {
                            toBeDeleted = true;
                            break;
                        }
                    }
                    if (!toBeDeleted)
                    {
                        toKeep.add((Node) childs[i].getData(nodeKey));
                    }
                    toBeDeleted = false;
                }

                // Dispose all properties and re-insert nodes
                for (int i = 0; i < childs.length; i++)
                {
                    childs[i].dispose();
                }

                for (int i = 0; i < toKeep.size(); i++)
                {
                    updatePropertiesOnInsert(toKeep.get(i));
                }
                sc.setContent(groupProperties);
                sc.setExpandHorizontal(true);
                sc.setExpandVertical(true);
                sc.setMinSize(groupProperties.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        }
    }

    /**
     * This function update the actions column when a new file is selected
     *
     * @param file
     *            The selected file
     */
    private void updateActionsOnInsert(File file)
    {
        Label selectFile = new Label(selectedFilesComposite, SWT.NONE);
        selectFile.setText("- " + file.getName()); //$NON-NLS-1$
        selectFile.setData(fileKey, file);

        /* Disable the merge button, until the merge is finished */
        /*
         * if(selectedFilesComposite.getChildren().length <= 1) {
         * mergeFiles.setEnabled(false); } else { mergeFiles.setEnabled(true); }
         */
        if (selectedFilesComposite.getChildren().length < 1)
        {
            removeXml.setEnabled(false);
        }
        else
        {
            removeXml.setEnabled(true);
        }

        group3.layout();
    }

    /**
     * This function update the actions column when a new file is selected
     *
     * @param toDelete
     *            The list of files associated labels needs to be deleted
     */
    private void updateActionsOnRemove(List<File> toDelete)
    {
        // Retrieve node who's not in toDelete list
        Control[] childs = selectedFilesComposite.getChildren();
        List<File> toKeep = new ArrayList<>();
        boolean toBeDeleted = false;
        for (int i = 0; i < childs.length; i++)
        {
            for (int j = 0; j < toDelete.size(); j++)
            {
                if (((File) childs[i].getData(fileKey)).equals(toDelete.get(j)))
                {
                    toBeDeleted = true;
                    break;
                }
            }
            if (!toBeDeleted)
            {
                toKeep.add((File) childs[i].getData(fileKey));
            }
            toBeDeleted = false;
        }

        // Dispose all properties and re-insert nodes
        for (int i = 0; i < childs.length; i++)
        {
            childs[i].dispose();
        }

        for (int i = 0; i < toKeep.size(); i++)
        {
            updateActionsOnInsert(toKeep.get(i));
        }
        if (toKeep.size() == 0)
        {
            removeXml.setEnabled(false);
        }
    }

    /**
     * This function returned an array of all the checked files in the
     * <code>ftree</code>
     *
     * @return An array of selected items
     */
    public static Object[] getTreeSelection() {
        return selectedItems.values().toArray();
    }

    /**
     * This recursive function check the first <code>item</item> and its
     * childs, if <code>checked</code> is <code>true</code>
     *
     * @param item
     *            The root item
     * @param checked
     *            <code>true</code> to check children, <code>false</code>
     *            otherwise
     * */
    private void checkItems(TreeItem item, boolean checked)
    {
        item.setChecked(checked);
        TreeItem[] items = item.getItems();
        for (int i = 0; i < items.length; i++)
        {
            checkItems(items[i], checked);
        }
    }

    /**
     * This function create a table for an entry node, with the appropriate
     * columns
     *
     * @param root
     *            The root of the entry (timeGraphView or xyView)
     * @param group
     *            The composite of the entry
     * @param entry
     *            The entry node
     * */
    private void createEntryTable(final Node root, final Composite group, final Node entry) {
        if (!entry.getNodeName().equals(TmfXmlUiStrings.ENTRY_ELEMENT)) {
            return;
        }

        Composite entryAttributeComposite = new Composite(group, SWT.NONE);
        entryAttributeComposite.setLayout(createGridLayout(1, 5, 5));
        entryAttributeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label entryAttributeTitle = new Label(entryAttributeComposite, SWT.NONE);
        entryAttributeTitle.setText("Entry attributes:"); //$NON-NLS-1$

        Composite entryAttributeTableComposite = new Composite(group, SWT.NONE);
        entryAttributeTableComposite.setLayout(createGridLayout(1, 15, 5));
        entryAttributeTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Table entryAttributeTable = new Table(entryAttributeTableComposite, SWT.MULTI | SWT.BORDER);
        entryAttributeTable.setLinesVisible(true);
        entryAttributeTable.setHeaderVisible(true);
        entryAttributeTable.setLayout(new TableLayout());
        entryAttributeTable.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true));

        TableColumn attributeColumn = new TableColumn(entryAttributeTable, SWT.NONE);
        attributeColumn.setText("Attribute"); //$NON-NLS-1$
        attributeColumn.setResizable(true);
        attributeColumn.setWidth(100);

        /* Find all the column headers */
        List<String> columnsHeaders = new ArrayList<>();
        NodeList entryChildren = entry.getChildNodes();
        for (int j = 0; j < entryChildren.getLength(); j++) {
            if (!entryChildren.item(j).getNodeName().equals("#text")) { //$NON-NLS-1$
                NamedNodeMap childAttributes = entryChildren.item(j).getAttributes();

                for (int k = 0; k < childAttributes.getLength(); k++) {
                    boolean present = false;
                    for (String columnHeader : columnsHeaders) {
                        if (columnHeader.equals(childAttributes.item(k).getNodeName())) {
                            present = true;
                            break;
                        }
                    }
                    if (!present) {
                        columnsHeaders.add(childAttributes.item(k).getNodeName());
                    }
                }
            }
        }
        /* Create all the columns */
        for (int j = 0; j < columnsHeaders.size(); j++) {
            TableColumn column = new TableColumn(entryAttributeTable, SWT.NONE);
            column.setText(columnsHeaders.get(j));
            column.setResizable(true);
            column.setWidth(100);
        }

        /* Fill the table */
        for (int j = 0; j < entryChildren.getLength(); j++) {
            if (!entryChildren.item(j).getNodeName().equals("#text")) { //$NON-NLS-1$
                TableItem row = new TableItem(entryAttributeTable, SWT.NONE);
                row.setText(0, entryChildren.item(j).getNodeName());
                // row.setData(entryChildren.item(j));
                row.setData(nodeKey, entryChildren.item(j));

                NamedNodeMap childAttributes = entryChildren.item(j).getAttributes();
                for (int k = 0; k < childAttributes.getLength(); k++) {
                    TableColumn[] columns = entryAttributeTable.getColumns();
                    for (int l = 0; l < columns.length; l++) {
                        if (columns[l].getText().equals(childAttributes.item(k).getNodeName())) {
                            row.setText(l, childAttributes.item(k).getNodeValue());
                        }
                    }
                }
            }
        }

        final TableEditor editor = new TableEditor(entryAttributeTable);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        entryAttributeTable.addListener(SWT.MouseDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                Rectangle clientArea = entryAttributeTable.getClientArea();
                Point pt = new Point(event.x, event.y);
                int index = entryAttributeTable.getTopIndex();
                while (index < entryAttributeTable.getItemCount()) {
                    boolean visible = false;
                    final TableItem item = entryAttributeTable.getItem(index);
                    for (int j = 0; j < entryAttributeTable.getColumnCount(); j++) {
                        Rectangle rect = item.getBounds(j);
                        if (rect.contains(pt)) {
                            final int column = j;
                            final TableColumn tableColumn = entryAttributeTable.getColumn(j);
                            if (tableColumn.getText().equals("Attribute")) { //$NON-NLS-1$
                                final Combo types = new Combo(entryAttributeTable, SWT.READ_ONLY);
                                String rootType = root.getNodeName();
                                String[] possibleTypes = null;
                                if (rootType.equals(TmfXmlUiStrings.TIME_GRAPH_VIEW)) {
                                    possibleTypes = new String[4];
                                    possibleTypes[0] = TmfXmlUiStrings.DISPLAY_ELEMENT;
                                    possibleTypes[1] = TmfXmlStrings.ID;
                                    possibleTypes[2] = TmfXmlUiStrings.PARENT_ELEMENT;
                                    possibleTypes[3] = TmfXmlStrings.NAME;
                                }
                                if (rootType.equals(TmfXmlUiStrings.XY_VIEW)) {
                                    possibleTypes = new String[2];
                                    possibleTypes[0] = TmfXmlUiStrings.DISPLAY_ELEMENT;
                                    possibleTypes[1] = TmfXmlStrings.NAME;
                                }
                                types.setItems(possibleTypes);
                                types.setText(item.getText(j));

                                Listener comboListener = new Listener() {

                                    @Override
                                    public void handleEvent(final Event e) {
                                        switch (e.type) {
                                        case SWT.FocusOut:
                                            item.setText(column, types.getText());
                                            types.dispose();
                                            break;
                                        case SWT.Traverse:
                                            switch (e.detail) {
                                            case SWT.TRAVERSE_RETURN:
                                                item.setText(column, types.getText());
                                                //$FALL-THROUGH$
                                            case SWT.TRAVERSE_ESCAPE:
                                                types.dispose();
                                                e.doit = false;
                                                //$FALL-THROUGH$
                                            default:
                                                break;
                                            }
                                            break;
                                        case SWT.Modify:
                                            try {
                                                updateTreeData();

                                                String newText = types.getText();
                                                Node entryChild = (Node) item.getData(nodeKey);
                                                File copyFile = (File) root.getUserData(userDataFileKey);

                                                XmlUtils.setNewAttribute(copyFile, entryChild,
                                                        entryAttributeTable.getColumn(column).getText(), newText);
                                            } catch (ParserConfigurationException | SAXException | IOException | TransformerException e1) {
                                                e1.printStackTrace();
                                            }
                                            break;
                                        default:
                                            break;

                                        }
                                    }
                                };

                                types.addListener(SWT.FocusOut, comboListener);
                                types.addListener(SWT.Traverse, comboListener);
                                types.addListener(SWT.Modify, comboListener);
                                editor.setEditor(types, item, j);
                                types.setText(item.getText(j));
                                types.setFocus();
                                return;
                            }
                            else if (tableColumn.getText().equals(TmfXmlStrings.VALUE)) {
                                final Text text = new Text(entryAttributeTable, SWT.NONE);
                                Listener textListener = new Listener() {
                                    @Override
                                    public void handleEvent(final Event e) {
                                        switch (e.type) {
                                        case SWT.FocusOut:
                                            item.setText(column, text.getText());
                                            text.dispose();
                                            break;
                                        case SWT.Traverse:
                                            switch (e.detail) {
                                            case SWT.TRAVERSE_RETURN:
                                                item.setText(column, text.getText());
                                                //$FALL-THROUGH$
                                            case SWT.TRAVERSE_ESCAPE:
                                                text.dispose();
                                                e.doit = false;
                                                //$FALL-THROUGH$
                                            default:
                                                break;
                                            }
                                            break;
                                        case SWT.Modify:
                                            try {
                                                updateTreeData();

                                                String newText = text.getText();
                                                Node entryChild = (Node) item.getData(nodeKey);
                                                File copyFile = (File) root.getUserData(userDataFileKey);

                                                XmlUtils.setNewAttribute(copyFile, entryChild,
                                                        entryAttributeTable.getColumn(column).getText(), newText);
                                            } catch (ParserConfigurationException | SAXException | IOException | TransformerException e1) {
                                                e1.printStackTrace();
                                            }
                                            break;
                                        default:
                                            break;
                                        }
                                    }
                                };
                                addBasicMenuToText(text);

                                text.addListener(SWT.FocusOut, textListener);
                                text.addListener(SWT.Traverse, textListener);
                                text.addListener(SWT.Modify, textListener);
                                editor.setEditor(text, item, j);
                                text.setText(item.getText(j));
                                text.selectAll();
                                text.setFocus();
                                return;
                            } else if (tableColumn.getText().equals(TmfXmlStrings.TYPE)) {
                                final Combo types = new Combo(entryAttributeTable, SWT.READ_ONLY);
                                String[] possibleTypes = { TmfXmlStrings.TYPE_CONSTANT, TmfXmlStrings.TYPE_LOCATION,
                                        TmfXmlStrings.TYPE_QUERY, TmfXmlStrings.TYPE_SELF };
                                types.setItems(possibleTypes);
                                types.setText(item.getText(j));

                                Listener comboListener = new Listener() {

                                    @Override
                                    public void handleEvent(final Event e) {
                                        switch (e.type) {
                                        case SWT.FocusOut:
                                            item.setText(column, types.getText());
                                            types.dispose();
                                            break;
                                        case SWT.Traverse:
                                            switch (e.detail) {
                                            case SWT.TRAVERSE_RETURN:
                                                item.setText(column, types.getText());
                                                //$FALL-THROUGH$
                                            case SWT.TRAVERSE_ESCAPE:
                                                types.dispose();
                                                e.doit = false;
                                                //$FALL-THROUGH$
                                            default:
                                                break;
                                            }
                                            break;
                                        case SWT.Modify:
                                            try {
                                                updateTreeData();

                                                String newText = types.getText();
                                                Node entryChild = (Node) item.getData(nodeKey);
                                                File copyFile = (File) root.getUserData(userDataFileKey);

                                                XmlUtils.setNewAttribute(copyFile, entryChild,
                                                        entryAttributeTable.getColumn(column).getText(), newText);
                                            } catch (ParserConfigurationException | SAXException | IOException | TransformerException e1) {
                                                e1.printStackTrace();
                                            }
                                            break;
                                        default:
                                            break;

                                        }
                                    }
                                };

                                types.addListener(SWT.FocusOut, comboListener);
                                types.addListener(SWT.Traverse, comboListener);
                                types.addListener(SWT.Modify, comboListener);
                                editor.setEditor(types, item, j);
                                types.setText(item.getText(j));
                                types.setFocus();
                                return;
                            }
                        }
                        if (!visible && rect.intersects(clientArea)) {
                            visible = true;
                        }
                    }
                    if (!visible) {
                        return;
                    }
                    index++;
                }
            }
        });
    }

    /**
     * Enable drag and drop between <code>TreeItems</code> in <code>Tree</code>
     * */
    private static void setTreeDragDrop(final Tree tree) {
        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(tree, operations);
        source.setTransfer(types);
        source.addDragListener(new DragSourceListener() {

        @Override
        public void dragStart(DragSourceEvent event) {
            TreeItem[] selection = tree.getSelection();
            Object node = selection[0].getData(nodeKey);
            if (selection.length > 0 && node != null) {
                event.doit = true;
                currentDragSource[0] = selection[0];
            } else {
                event.doit = false;
            }
        }

        @Override
        public void dragSetData(DragSourceEvent event) {
            event.data = ((TreeItem)currentDragSource[0]).getText();
        }

        @Override
        public void dragFinished(DragSourceEvent event) {
            currentDragSource[0] = null;
        }
        });

        final DropTarget target = new DropTarget(tree, operations);
        target.setTransfer(types);
        target.addDropListener(new DropTargetListener() {

            @Override
            public void dropAccept(DropTargetEvent event) {
            }

            @Override
            public void drop(DropTargetEvent event) {
                boolean success = false;
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                  }

                TreeItem obj = ((TreeItem)event.item);
                String newText = (String)event.data;

                if(currentDragSource[0] instanceof TreeItem && event.item != null) {
                    TreeItem dragSource = (TreeItem) currentDragSource[0];
                    Node dragSourceData = (Node) dragSource.getData(nodeKey);
                    Object dropItemFileData = obj.getData(fileKey);
                    Object dropItemNodeData = obj.getData(nodeKey);

                    // drop on the file item
                    if(dropItemFileData != null) {
                        IStatus appendStatus = XmlUtils.appendElementInFile(dragSourceData, dragSourceData.getParentNode(), (File)dropItemFileData, false);
                        TreeItem dragSourceParent = dragSource.getParentItem();
                        IStatus removeStatus = XmlUtils.removeElementFromFile(dragSourceData, dragSourceData.getParentNode(), (File)dragSourceParent.getData(fileKey), false);
                        if(appendStatus.isOK() && removeStatus.isOK()) {
                            TreeItem newItem = new TreeItem(obj, SWT.NONE);
                            newItem.setText(newText);

                            File dragFile = (File)dragSource.getParentItem().getData(fileKey);
                            String oldId = (String)dragSource.getParentItem().getData(analysisIdKey);
                            String newId = XmlUtils.getAnalysisId(((File)dropItemFileData).getPath());

                            if(oldId != null) {
                                Element analysisNode = XmlUtils.getElementInFile(dragFile.getPath(), TmfXmlStrings.ANALYSIS, oldId);
                                try {
                                    XmlUtils.setNewAttribute(dragFile, analysisNode, TmfXmlStrings.ID, newId);
                                    // Retrieve the parent (dragSourceData) in the modifiated node
                                    Node parent = findParentNodeRecursive(dragSourceData.getNodeName(), analysisNode);

                                } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                                    e.printStackTrace();
                                    return;
                                }
                            } else {
                                return;
                            }

                            newItem.setData(nodeKey, dragSourceData);

                            success = true;

                            // Add the node to the XML tree (drop)
                            XmlUtils.appendElementInFile(dragSourceData, dragSourceData.getParentNode(), (File)dropItemFileData, true);
                            // Remove the node from the dragSource XML tree (drag)
                            XmlUtils.removeElementFromFile(dragSourceData, dragSourceData.getParentNode(), (File)dragSourceParent.getData(fileKey), true);
                        }
                    }
                    // drop on the a node of a file
                    else if(dropItemNodeData != null) {
                        IStatus appendStatus = XmlUtils.appendElementInFile(dragSourceData, dragSourceData.getParentNode(), (File)obj.getParentItem().getData(fileKey), false);
                        TreeItem dragSourceParent = dragSource.getParentItem();
                        IStatus removeStatus = XmlUtils.removeElementFromFile(dragSourceData, dragSourceData.getParentNode(), (File)dragSourceParent.getData(fileKey), false);

                        if(appendStatus.isOK() && removeStatus.isOK()) {
                            TreeItem newItem = new TreeItem(obj.getParentItem(), SWT.NONE);
                            newItem.setText(newText);

                            //newItem.setData(dragSourceData);
                            newItem.setData(nodeKey, dragSourceData);
                            success = true;

                            // Add the node to the XML tree (drop)
                            XmlUtils.appendElementInFile(dragSourceData, dragSourceData.getParentNode(), (File)obj.getParentItem().getData(fileKey), true);
                            // Remove the node from the dragSource XML tree (drag)
                            XmlUtils.removeElementFromFile(dragSourceData, dragSourceData.getParentNode(), (File)dragSourceParent.getData(fileKey), true);
                        }
                    }
                }

                if(success && currentDragSource[0] instanceof TreeItem) {
                    TreeItem dragSource = (TreeItem) currentDragSource[0];
                    dragSource.dispose();
                }
            }

            private Node findParentNodeRecursive(String parentNodeName, Node child) {
                if(child.getParentNode().getNodeName().equals(parentNodeName)) {
                    return child.getParentNode();
                }
                return findParentNodeRecursive(parentNodeName, child.getParentNode());
            }

            @Override
            public void dragOver(DropTargetEvent event) {}

            @Override
            public void dragOperationChanged(DropTargetEvent event) {}

            @Override
            public void dragLeave(DropTargetEvent event) {}

            @Override
            public void dragEnter(DropTargetEvent event) {}
        });
    }

    /**
     * Add a new <code>TreeItem</code>
     *
     * @param file
     *            The new file
     * */
    private void addTreeItem(File file)
    {
        TreeItem item = new TreeItem(ftree, SWT.NONE);
        item.setText(file.getName());
        item.setData(fileKey, file);
        item.setData(analysisIdKey, XmlUtils.getAnalysisId(file.getAbsolutePath()));

        String path = ((File) item.getData(fileKey)).getPath();
        TmfXmlManagerParser parser = null;
        try {
            parser = new TmfXmlManagerParser(path);
            List<Node> roots = parser.getRoots();

            for (int j = 0; j < roots.size(); j++)
            {
                roots.get(j).setUserData(userDataFileKey, file, null); // Associate
                                                                       // the
                                                                       // Node
                TreeItem fileChild = new TreeItem(item, SWT.NONE);
                fileChild.setText(roots.get(j).getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue());
                fileChild.setData(nodeKey, roots.get(j));
            }
        } catch (ParserConfigurationException err) {
            Activator.getDefault();
            Activator.logError("Error when parsing the XML file", err); //$NON-NLS-1$
            err.printStackTrace();
        } catch (SAXException err) {
            Activator.getDefault();
            Activator.logError("SAX error", err); //$NON-NLS-1$
            err.printStackTrace();
        } catch (IOException err) {
            Activator.getDefault();
            Activator.logError("Failed to read file", err); //$NON-NLS-1$
            err.printStackTrace();
        }
        item.setExpanded(true);
    }

    /**
     * This function update the data associate with the <code>TreeItems</code>.
     * FIXME Do in-depth change. For now, only files and roots node are updated.
     * */
    private void updateTreeData() throws ParserConfigurationException, SAXException, IOException {
        // For each child in the tree, re-set his data by parsing again his
        // file.
        File[] upToDateFiles = activeXMLFolder.listFiles();
        TreeItem[] treeFiles = ftree.getItems();

        for (int i = 0; i < upToDateFiles.length; i++) {
            for (int j = 0; j < treeFiles.length; j++) {
                // Compare the file link to the treeItem and those in the folder
                if (((File) treeFiles[j].getData(fileKey)).getName().equals(upToDateFiles[i].getName())) {
                    treeFiles[j].setData(fileKey, upToDateFiles[i]);
                    TreeItem[] treeFileElements = treeFiles[j].getItems();
                    TmfXmlManagerParser parser = new TmfXmlManagerParser(upToDateFiles[i].getPath());
                    List<Node> roots = parser.getRoots();
                    for (int k = 0; k < treeFileElements.length; k++) {
                        roots.get(k).setUserData(userDataFileKey, upToDateFiles[i], null);
                        treeFileElements[k].setData(nodeKey, roots.get(k));
                    }
                }
            }
        }
    }

    /**
     * Add a menu to a <code>Text</code> control. Menu elements : Cut, Copy,
     * Paste & SelectAll
     *
     * @param control
     *            The Text
     * */
    private static void addBasicMenuToText(final Text control) {
        Menu menu = new Menu(control);
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("Cut"); //$NON-NLS-1$
        item.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                control.cut();
            }
        });
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Copy"); //$NON-NLS-1$
        item.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                control.copy();
            }
        });
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Paste"); //$NON-NLS-1$
        item.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                control.paste();
            }
        });
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Select All"); //$NON-NLS-1$
        item.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                control.selectAll();
            }
        });

        control.setMenu(menu);
    }

    /**
     * Create a new GridLayout
     * @param numColumns
     *              The number of columns
     * @param marginWidth
     *              The number of pixels of horizontal margin that will
     *              be placed along the left and right edges of the layout.
     * @param marginHeight
     *              The number of pixels of vertical margin that will
     *              be placed along the top and bottom edges of the layout.
     * */
    private static GridLayout createGridLayout(int numColumns, int marginWidth, int marginHeight) {
        GridLayout grid = new GridLayout(numColumns, false);
        grid.horizontalSpacing = 0; grid.verticalSpacing = 0;
        grid.marginWidth = marginWidth; grid.marginHeight = marginHeight;
        return grid;
    }
}