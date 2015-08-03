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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.TmfXmlUiStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Jonathan Sauvé
 *
 */
@SuppressWarnings({"nls"})
public class XmlFilePropertiesViewer extends Dialog {

    private static File fxmlFile;

    private static Shell fshell;
    private static Composite fparent;

    private static SashForm fsash;
        private static Tree ftree;
        /**
         * The last selected item in the tree
         */
        public static TreeItem lastSelectedItem;
        /**
         * The index of {@link XmlFilePropertiesViewer#lastSelectedItem}
         * 0 by default
         */
        public static int lastSelectedItemIndex = 0;
        private static Composite fcomposite;
            private static ScrolledComposite sc;
                private static Composite fproperties;
                    /**
                     * A boolean to know if a table as been already create
                     */
                    public static boolean createAnotherTable = false;
                    private static final Table[] currentTable = new Table[1];
                    private static Composite fchanges;
                        private static Button restoreDefaults;
                        private static Button saveChanges;

    private static Map<Integer, Runnable> unappliedModif = new HashMap<>();
    private static Map<Integer, Runnable> originalValuesOfModifs = new HashMap<>();
    private static List<Node> initialValues = new ArrayList<>();

    /**
     * Public constructor
     * @param parentShell
     *              The parent's shell
     * @param xmlFile
     *              The xmlFile to show the properties
     */
    public XmlFilePropertiesViewer(Shell parentShell, File xmlFile) {
        super(parentShell);
        super.setShellStyle(super.getShellStyle() | SWT.SHELL_TRIM);
        fxmlFile = xmlFile;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        fparent = (Composite)super.createDialogArea(parent);
        fparent.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createContents();

        return fparent;
    }

    @Override
    protected void configureShell(Shell newShell) {
        newShell.setText("Properties - " + fxmlFile.getName());
        newShell.setMinimumSize(700, 800);
        newShell.setSize(700, 800);
        newShell.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));

        super.configureShell(newShell);

        fshell = newShell;
    }

    @Override
    protected void cancelPressed() {
        Button cancelButton = super.getButton(IDialogConstants.CANCEL_ID);
        if(cancelButton != null) {
            cancelButton.addListener(SWT.Selection, XmlManagerListeners.closeShellSL(fshell));
        }
    }

    private static void createContents() {
        fsash = new SashForm(fparent, SWT.HORIZONTAL | SWT.NONE);
        fsash.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fsash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        ftree = new Tree(fsash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        ftree.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        ftree.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, true));
        ftree.addSelectionListener(XmlManagerListeners.propertiesTreeSL(ftree));

        TmfXmlManagerParser parser = null;
        try {
            parser = new TmfXmlManagerParser(fxmlFile.getPath());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        if(parser != null) {
            List<Node> roots = parser.getRoots();
            for(Node root : roots) {
                root.setUserData(XmlManagerStrings.fileKey, fxmlFile, null);
                initialValues.add(root);
                TreeItem item = new TreeItem(ftree, SWT.NONE);
                item.setText(root.getNodeName());
                item.setData(XmlManagerStrings.nodeKey, root);
            }
        }

        fcomposite = new Composite(fsash, SWT.BORDER);
        fcomposite.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fcomposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        sc = new ScrolledComposite(fcomposite, SWT.V_SCROLL | SWT.H_SCROLL);
        sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        fproperties = new Composite(sc, SWT.NONE);
        fproperties.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fproperties.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        fchanges = new Composite(fcomposite, SWT.NONE);
        GridLayout changesCompositeLayout = new GridLayout(2, false);
        changesCompositeLayout.marginHeight = 20; changesCompositeLayout.marginWidth = 20;
        fchanges.setLayout(changesCompositeLayout);
        fchanges.setLayoutData(new GridData(SWT.END, SWT.END, true, false));

        restoreDefaults = new Button(fchanges, SWT.PUSH);
        restoreDefaults.setText("Restore Defaults");
        restoreDefaults.setLayoutData(new GridData(140, 30));
        restoreDefaults.addSelectionListener(XmlManagerListeners.restoreDefaultsSL());

        saveChanges = new Button(fchanges, SWT.PUSH);
        saveChanges.setText("Save Changes");
        saveChanges.setLayoutData(new GridData(120, 30));
        saveChanges.addSelectionListener(XmlManagerListeners.saveChangesSL());
        saveChanges.setEnabled(false);

        fsash.setWeights(new int[] {1, 3});
    }

    /**
     * @param root
     *              The root node
     */
    public static void fillComposite(final Node root) {
        /* Clear parent's children*/
        Control[] parentChildren = fproperties.getChildren();
        for(int i = 0; i < parentChildren.length; i++) {
            parentChildren[i].dispose();
        }
        createAnotherTable = true;
        fillCompositeWithRoot(root);
        currentTable[0] = null;

        sc.setContent(fproperties);
        sc.setExpandHorizontal(true);
        sc.setExpandVertical(true);
        sc.setMinSize(fproperties.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    /**
    *
    * @param root
    *              The root node
    */
   public static void fillCompositeWithRoot(final Node root) {
       fproperties.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
       fproperties.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

       File xmlFile = (File) root.getUserData(XmlManagerStrings.fileKey);
       String nodeName = root.getNodeName();

       switch(nodeName) {
       case TmfXmlUiStrings.TIME_GRAPH_VIEW:
           Composite associateFileComp = new Composite(fproperties, SWT.NONE);
           associateFileComp.setLayout(XmlManagerUtils.createGridLayout(1, 5, 5));
           associateFileComp.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

           Label associateFile = new Label(associateFileComp, SWT.NONE);
           associateFile.setText("File: " + xmlFile.getName()); //$NON-NLS-1$

           Composite IDComposite = new Composite(fproperties, SWT.NONE);
           IDComposite.setLayout(XmlManagerUtils.createGridLayout(1, 5, 5));
           IDComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

           Label ID = new Label(IDComposite, SWT.NONE);
           ID.setText("ID: "); //$NON-NLS-1$

           Composite IDValueComposite = new Composite(fproperties, SWT.NONE);
           IDValueComposite.setLayout(XmlManagerUtils.createGridLayout(1, 15, 5));
           IDValueComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

           Text IDValue = new Text(IDValueComposite, SWT.BORDER);
           IDValue.setLayoutData(new GridData(300, 40));
           final String initialTitle = root.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue();
           IDValue.setText(initialTitle);
           IDValue.setData(XmlManagerStrings.nodeKey, root);
           IDValue.addModifyListener(XmlManagerListeners.textML(IDValue, initialTitle, root, TmfXmlStrings.ID));
           originalValuesOfModifs.put(IDValue.hashCode(), new Runnable() {

                @Override
                public void run() {
                    try {
                        XmlUtils.setNewAttribute(fxmlFile, XmlUtils.getOriginalXmlFile(fxmlFile), root,
                                TmfXmlStrings.ID, initialTitle);
                    } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            });

           XmlManagerUtils.addBasicMenuToText(IDValue);

           @SuppressWarnings("unused")
           MenuItem separator = new MenuItem(IDValue.getMenu(), SWT.SEPARATOR);

           MenuItem resetText = new MenuItem(IDValue.getMenu(), SWT.NONE);
           resetText.setText("Reset");
           resetText.setData(XmlManagerStrings.nodeKey, root);
           resetText.addSelectionListener(XmlManagerListeners.resetTextSL(IDValue, initialTitle, root));
           break;
       case TmfXmlUiStrings.XY_VIEW:
           Composite associateFileComp2 = new Composite(fproperties, SWT.NONE);
           associateFileComp2.setLayout(XmlManagerUtils.createGridLayout(1, 5, 5));
           associateFileComp2.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

           Label associateFile2 = new Label(associateFileComp2, SWT.NONE);
           associateFile2.setText("File: " + xmlFile.getName()); //$NON-NLS-1$

           Composite IDComposite2 = new Composite(fproperties, SWT.NONE);
           IDComposite2.setLayout(XmlManagerUtils.createGridLayout(1, 5, 5));
           IDComposite2.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

           Label ID2 = new Label(IDComposite2, SWT.NONE);
           ID2.setText("ID: "); //$NON-NLS-1$

           Composite IDValueComposite2 = new Composite(fproperties, SWT.NONE);
           IDValueComposite2.setLayout(XmlManagerUtils.createGridLayout(1, 15, 5));
           IDValueComposite2.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

           Text IDValue2 = new Text(IDValueComposite2, SWT.BORDER);
           IDValue2.setLayoutData(new GridData(300, 40));
           final String initialTitle2 = root.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue();
           IDValue2.setText(initialTitle2);
           IDValue2.setData(XmlManagerStrings.nodeKey, root);
           IDValue2.addModifyListener(XmlManagerListeners.textML(IDValue2, initialTitle2, root, TmfXmlStrings.ID));
           originalValuesOfModifs.put(IDValue2.hashCode(), new Runnable() {

               @Override
               public void run() {
                   try {
                       XmlUtils.setNewAttribute(fxmlFile, XmlUtils.getOriginalXmlFile(fxmlFile), root,
                               TmfXmlStrings.ID, initialTitle2);
                   } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                       e.printStackTrace();
                       return;
                   }
               }
           });

           break;
       case TmfXmlStrings.STATE_PROVIDER:
           break;
       default:
           break;
       }

       NodeList rootChildren = root.getChildNodes();
       for(int i = 0; i < rootChildren.getLength(); i++) {
           fillCompositeWithRootChildren(root, rootChildren.item(i));
       }

       List<Table> tables = new ArrayList<>();
       Control[] parentChildren = fproperties.getChildren();
       for(int i = 0; i < parentChildren.length; i++) {
           if(parentChildren[i] instanceof Table) {
               tables.add((Table)parentChildren[i]);
               break;
           }
       }
       if(!tables.isEmpty()) {
           for(int i = 0; i < tables.size(); i++) {
               Table table = tables.get(i);
               for(int j = 0; j < table.getColumnCount(); j++) {
                   table.getColumn(i).pack();
               }
               table.pack();
           }

       }

       fproperties.layout(true, true);
   }

   /**
    * @param root
    *              The root of the tree. Three possibilities:
    *              {@link TmfXmlUiStrings#TIME_GRAPH_VIEW},
    *              {@link TmfXmlUiStrings#XY_VIEW} or
    *              {@link TmfXmlStrings#STATE_PROVIDER}
    * @param child
    *              The child node
    */
   public static void fillCompositeWithRootChildren(final Node root, final Node child) {
       String nodeName = child.getNodeName();

       switch(nodeName) {
       case TmfXmlUiStrings.ENTRY_ELEMENT:
           Label entryTitle = new Label(fproperties, SWT.NONE);
           entryTitle.setText("Shown information:"); //$NON-NLS-1$

           FontData fontData3 = entryTitle.getFont().getFontData()[0];
           Font font3 = new Font(fproperties.getDisplay(), new FontData(fontData3.getName(), fontData3
                   .getHeight(), SWT.BOLD));
           entryTitle.setFont(font3);

           Composite currentPathComposite = new Composite(fproperties, SWT.NONE);
           currentPathComposite.setLayout(XmlManagerUtils.createGridLayout(3, 5, 5));

           Label currentPath = new Label(currentPathComposite, SWT.NONE);
           currentPath.setText("Current entry path: "); //$NON-NLS-1$

           Label currentPathValue = new Label(currentPathComposite, SWT.BORDER);
           final String initialValue = child.getAttributes().getNamedItem(TmfXmlUiStrings.PATH).getNodeValue();
           currentPathValue.setText(initialValue);
           currentPathValue.setLayoutData(new GridData(200, 20));
           originalValuesOfModifs.put(currentPathValue.hashCode(), new Runnable() {

               @Override
               public void run() {
                   try {
                       XmlUtils.setNewAttribute(fxmlFile, XmlUtils.getOriginalXmlFile(fxmlFile), child,
                               TmfXmlUiStrings.PATH, initialValue);
                   } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                       e.printStackTrace();
                       return;
                   }
               }
           });

           Button buildPath = new Button(currentPathComposite, SWT.PUSH);
           buildPath.setText("Build path"); //$NON-NLS-1$

           buildPath.addSelectionListener(XmlManagerListeners.buildPathSL(fproperties, currentPathValue, root));

           createEntryTable(root, child);
           break;
       case TmfXmlStrings.HEAD:
           break;
       case TmfXmlStrings.TRACETYPE:
           break;
       case TmfXmlStrings.ID:
           break;
       case TmfXmlStrings.LABEL:
           Composite labelComposite = new Composite(fproperties, SWT.NONE);
           labelComposite.setLayout(XmlManagerUtils.createGridLayout(1, 5, 5));
           labelComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

           Label graphTitle = new Label(labelComposite, SWT.NONE);
           graphTitle.setText("Graph title: "); //$NON-NLS-1$

           Composite textComposite = new Composite(fproperties, SWT.NONE);
           textComposite.setLayout(XmlManagerUtils.createGridLayout(1, 15, 5));
           textComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

           final Text text = new Text(textComposite, SWT.BORDER);
           final String initialTitle = child.getAttributes().getNamedItem(TmfXmlStrings.VALUE).getNodeValue();
           text.setLayoutData(new GridData(300, 40));
           text.setText(initialTitle);
           text.setData(XmlManagerStrings.nodeKey, child);
           text.addModifyListener(XmlManagerListeners.textML(text, initialTitle, root, TmfXmlStrings.VALUE));
           XmlManagerUtils.addBasicMenuToText(text);
           originalValuesOfModifs.put(text.hashCode(), new Runnable() {

               @Override
               public void run() {
                   try {
                       XmlUtils.setNewAttribute(fxmlFile, XmlUtils.getOriginalXmlFile(fxmlFile), child,
                               TmfXmlStrings.VALUE, initialTitle);
                   } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                       e.printStackTrace();
                       return;
                   }
               }
           });

           @SuppressWarnings("unused")
           MenuItem separator = new MenuItem(text.getMenu(), SWT.SEPARATOR);

           MenuItem resetText = new MenuItem(text.getMenu(), SWT.NONE);
           resetText.setText("Reset");
           resetText.setData(XmlManagerStrings.nodeKey, child);
           resetText.addSelectionListener(XmlManagerListeners.resetTextSL(text, initialTitle, root));
           break;
       case TmfXmlStrings.ANALYSIS:
           Composite analysisIDComposite = new Composite(fproperties, SWT.NONE);
           analysisIDComposite.setLayout(XmlManagerUtils.createGridLayout(1, 5, 5));
           analysisIDComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

           Label analysisID = new Label(analysisIDComposite, SWT.NONE);
           analysisID.setText("Analysis ID: ");  //$NON-NLS-1$

           Composite analysisIDValueComposite = new Composite(fproperties, SWT.NONE);
           analysisIDValueComposite.setLayout(XmlManagerUtils.createGridLayout(1, 15, 5));
           analysisIDValueComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

           Text analysisIDValue = new Text(analysisIDValueComposite, SWT.BORDER);
           analysisIDValue.setLayoutData(new GridData(300, 40));
           final String initialTitle2 = child.getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue();
           analysisIDValue.setText(initialTitle2);
           analysisIDValue.setData(XmlManagerStrings.nodeKey, child);
           analysisIDValue.addModifyListener(XmlManagerListeners.textML(analysisIDValue, initialTitle2, root, TmfXmlStrings.ID));
           originalValuesOfModifs.put(analysisIDValue.hashCode(), new Runnable() {

               @Override
               public void run() {
                   try {
                       XmlUtils.setNewAttribute(fxmlFile, XmlUtils.getOriginalXmlFile(fxmlFile), child,
                               TmfXmlStrings.VALUE, initialTitle2);
                   } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                       e.printStackTrace();
                       return;
                   }
               }
           });

           XmlManagerUtils.addBasicMenuToText(analysisIDValue);

           @SuppressWarnings("unused")
           MenuItem separator2 = new MenuItem(analysisIDValue.getMenu(), SWT.SEPARATOR);

           MenuItem resetText2 = new MenuItem(analysisIDValue.getMenu(), SWT.NONE);
           resetText2.setText("Reset");
           resetText2.setData(XmlManagerStrings.nodeKey, root);
           resetText2.addSelectionListener(XmlManagerListeners.resetTextSL(analysisIDValue, initialTitle2, root));
           break;
       case TmfXmlStrings.DEFINED_VALUE:
           if(createAnotherTable) {
               Label processStatusTitle = new Label(fproperties, SWT.NONE);
               processStatusTitle.setText("Process Status Colors:"); //$NON-NLS-1$

               FontData fontData2 = processStatusTitle.getFont().getFontData()[0];
               Font font2 = new Font(fproperties.getDisplay(), new FontData(fontData2.getName(), fontData2
                       .getHeight(), SWT.BOLD));
               processStatusTitle.setFont(font2);

               Composite definedValueTableComposite = new Composite(fproperties, SWT.NONE);
               definedValueTableComposite.setLayout(XmlManagerUtils.createGridLayout(1, 15, 5));
               definedValueTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

               Table definedValueTable = new Table(definedValueTableComposite, SWT.SINGLE | SWT.BORDER);
               definedValueTable.setLinesVisible(true);
               definedValueTable.setHeaderVisible(true);
               definedValueTable.setLayout(new TableLayout());
               definedValueTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

               TableColumn nameColumn = new TableColumn(definedValueTable, SWT.NONE);
               nameColumn.setText(TmfXmlStrings.NAME);
               nameColumn.setWidth(300);
               nameColumn.setResizable(true);

               TableColumn colorColumn = new TableColumn(definedValueTable, SWT.NONE);
               colorColumn.setText(TmfXmlStrings.COLOR);
               colorColumn.setWidth(100);
               colorColumn.setResizable(true);

               createDefinedValueTableEditor(definedValueTable, root);
               currentTable[0] = definedValueTable;
               createAnotherTable = false;
           }

           if(currentTable[0] != null) {
               addRowDefinedValueTable(currentTable[0], child);
               currentTable[0].setSize(currentTable[0].computeSize(SWT.DEFAULT, SWT.DEFAULT));
           }

           break;
       case TmfXmlStrings.LOCATION:
           break;
       case TmfXmlStrings.EVENT_HANDLER:
           break;
       case TmfXmlStrings.STATE_ATTRIBUTE:
           break;
       case TmfXmlStrings.STATE_VALUE:
           break;
       case TmfXmlStrings.STATE_CHANGE:
           break;
       case TmfXmlStrings.ELEMENT_FIELD:
           break;
       case TmfXmlStrings.HANDLER_EVENT_NAME:
           break;
       default:
           break;
       }

       NodeList childChildren = child.getChildNodes();
       for(int i = 0; i < childChildren.getLength(); i++) {
           fillCompositeWithRootChildren(root, childChildren.item(i));
       }
   }

   private static void createDefinedValueTableEditor(Table definedValueTable, Node root) {
    final TableEditor editor = new TableEditor(definedValueTable);
    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;
    definedValueTable.addListener(SWT.MouseDoubleClick, XmlManagerListeners.definedValueTableListener(definedValueTable, editor, root));
}

/**
    *
    * @param parent
    * @param definedValueTable
    * @param child
    */
   private static void addRowDefinedValueTable(Table definedValueTable, final Node child) {
       TableItem item = new TableItem(definedValueTable, SWT.NONE);
       item.setData(XmlManagerStrings.nodeKey, child);
       final String initialValue = child.getAttributes().getNamedItem(TmfXmlStrings.NAME).getNodeValue();
       item.setText(0, initialValue);

       final Node colorNode = child.getAttributes().getNamedItem(TmfXmlStrings.COLOR);
       if(colorNode != null) {
           String stringColor = colorNode.getNodeValue();
           final RGB oldRGB = new RGB(XmlManagerUtils.hexaToRed(stringColor), XmlManagerUtils.hexaToGreen(stringColor),
                   XmlManagerUtils.hexaToBlue(stringColor));
           item.setBackground(1, new Color(fproperties.getDisplay(), oldRGB));
       }

       definedValueTable.layout(true, true);
       originalValuesOfModifs.put(item.hashCode(), new Runnable() {

           @Override
           public void run() {
               try {
                   XmlUtils.setNewAttribute(fxmlFile, XmlUtils.getOriginalXmlFile(fxmlFile), child,
                           TmfXmlStrings.VALUE, initialValue);
                   if(colorNode != null) {
                    XmlUtils.setNewAttribute(fxmlFile, XmlUtils.getOriginalXmlFile(fxmlFile), child,
                               TmfXmlStrings.COLOR, colorNode.getNodeValue());
                }
               } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                   e.printStackTrace();
                   return;
               }
           }
       });
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
   private static void createEntryTable(final Node root, final Node entry) {
       if (!entry.getNodeName().equals(TmfXmlUiStrings.ENTRY_ELEMENT)) {
           return;
       }

       Composite entryAttributeComposite = new Composite(fproperties, SWT.NONE);
       entryAttributeComposite.setLayout(XmlManagerUtils.createGridLayout(1, 5, 5));
       entryAttributeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

       Label entryAttributeTitle = new Label(entryAttributeComposite, SWT.NONE);
       entryAttributeTitle.setText("Entry attributes:"); //$NON-NLS-1$

       Composite entryAttributeTableComposite = new Composite(fproperties, SWT.NONE);
       entryAttributeTableComposite.setLayout(XmlManagerUtils.createGridLayout(1, 15, 5));
       entryAttributeTableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

       final Table entryAttributeTable = new Table(entryAttributeTableComposite, SWT.SINGLE | SWT.BORDER);
       entryAttributeTable.setLinesVisible(true);
       entryAttributeTable.setHeaderVisible(true);
       entryAttributeTable.setLayout(new TableLayout());
       //entryAttributeTable.setLayoutData(new GridData(300, 150));
       entryAttributeTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

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
               row.setData(XmlManagerStrings.nodeKey, entryChildren.item(j));

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
                                           addModif(item.hashCode(), new Runnable() {

                                                @Override
                                                public void run() {
                                                    try {
                                                        String newText = types.getText();
                                                        Node entryChild = (Node) item.getData(XmlManagerStrings.nodeKey);
                                                        File copyFile = (File) root.getUserData(XmlManagerStrings.fileKey);

                                                        XmlUtils.setNewAttribute(copyFile, XmlUtils.getOriginalXmlFile(copyFile), entryChild,
                                                                entryAttributeTable.getColumn(column).getText(), newText);
                                                    } catch (ParserConfigurationException | SAXException | IOException | TransformerException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                }
                                            });
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
                                           addModif(item.hashCode(), new Runnable() {

                                                @Override
                                                public void run() {
                                                    try {
                                                        String newText = text.getText();
                                                        Node entryChild = (Node) item.getData(XmlManagerStrings.nodeKey);
                                                        File copyFile = (File) root.getUserData(XmlManagerStrings.fileKey);

                                                        XmlUtils.setNewAttribute(copyFile, XmlUtils.getOriginalXmlFile(copyFile), entryChild,
                                                                entryAttributeTable.getColumn(column).getText(), newText);
                                                    } catch (ParserConfigurationException | SAXException | IOException | TransformerException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                }
                                            });
                                           break;
                                       default:
                                           break;
                                       }
                                   }
                               };
                               XmlManagerUtils.addBasicMenuToText(text);

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
                                           addModif(item.hashCode(), new Runnable() {

                                                @Override
                                                public void run() {
                                                    try {
                                                        String newText = types.getText();
                                                        Node entryChild = (Node) item.getData(XmlManagerStrings.nodeKey);
                                                        File copyFile = (File) root.getUserData(XmlManagerStrings.fileKey);

                                                        XmlUtils.setNewAttribute(copyFile, XmlUtils.getOriginalXmlFile(copyFile), entryChild,
                                                                entryAttributeTable.getColumn(column).getText(), newText);
                                                    } catch (ParserConfigurationException | SAXException | IOException | TransformerException e1) {
                                                        e1.printStackTrace();
                                                    }
                                                }
                                           });
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
     * @param id
     *              The ID associate with this method. In facts, this ID must be
     *              the one of the widget on which the event occured
     * @param newValueMethod
     *              The method to be called later
     * @return
     *              Whether a method with this ID is actually present or not
     */
   public static boolean addModif(int id, Runnable newValueMethod) {
       Runnable ret = unappliedModif.put(id, newValueMethod);
       if(unappliedModif.size() == 1) {
           saveChanges.setEnabled(true);
       }
       return ret == null ? false:true;
   }

   /**
     * This method execute all the changes on the XML file at one time.
     * It clears the map after the execution.
     */
   public static void applyAllModifs() {
       for(Runnable method : unappliedModif.values()) {
           method.run();
       }
       // update the roots node
       TmfXmlManagerParser parser = null;
       try {
           parser = new TmfXmlManagerParser(fxmlFile.getPath());
       } catch (ParserConfigurationException | SAXException | IOException e) {
           e.printStackTrace();
       }
       if(parser != null) {
           List<Node> roots = parser.getRoots();
           TreeItem[] treeItems = ftree.getItems();
           for(int i = 0; i < roots.size() && i < treeItems.length; i++) {
               Node root = roots.get(i); root.setUserData(XmlManagerStrings.fileKey, fxmlFile, null);
               treeItems[i].setData(XmlManagerStrings.nodeKey, roots.get(i));
           }
       }
       unappliedModif.clear();
       saveChanges.setEnabled(false);
   }

   /**
     * @return
     *              True if there are changes in standby, false otherwise
     */
    public static boolean modifsInStandby() {
           return !unappliedModif.isEmpty();
       }

    /**
     * Clear all the changes in standby, without applying them
     */
    public static void clearModifs() {
        unappliedModif.clear();
        saveChanges.setEnabled(false);
    }

    /**
     *
     */
    public static void restoreDefaults() {
        for(Runnable method : originalValuesOfModifs.values()) {
            method.run();
        }

        TreeItem[] items = ftree.getItems();
        for(int i = 0; i < items.length; i++) {
            items[i].dispose();
        }

        for(Node root : initialValues) {
            TreeItem item = new TreeItem(ftree, SWT.NONE);
            item.setText(root.getNodeName());
            item.setData(XmlManagerStrings.nodeKey, root);
        }

        unappliedModif.clear();

        Control[] children = fproperties.getChildren();
        for(int i = 0; i < children.length; i++) {
            children[i].dispose();
        }
        ftree.select(ftree.getItem(lastSelectedItemIndex));
        Event event = new Event();
        event.item = ftree.getItem(lastSelectedItemIndex);
        ftree.notifyListeners(SWT.Selection, event);
    }
}
