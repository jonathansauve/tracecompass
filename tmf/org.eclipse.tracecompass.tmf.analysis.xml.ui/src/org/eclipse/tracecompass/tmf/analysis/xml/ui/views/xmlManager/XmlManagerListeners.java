package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.TmfXmlUiStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.ui.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectModelElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author Jonathan Sauvé
 *
 */
@SuppressWarnings({"restriction", "nls"})
public class XmlManagerListeners {

    /** Make this class non-instantiable */
    private XmlManagerListeners(){ }

    /** XmlManagerViewer2 listeners */

    /**
     * This listener implements multiple cases for the xmlFilesTree
     * @param xmlFilesTree
     *              The xmlFilesTree
     * @param remove
     *              The remove button
     * @param edit
     *              The edit button
     * @return
     *              The listener for this tree
     * */
    public static Listener xmlFilesTreeListener(final Tree xmlFilesTree, final Button remove, final Button edit) {
        return new Listener() {

            @Override
            public void handleEvent(Event event) {
                switch(event.type) {
                    case SWT.Modify:
                        @SuppressWarnings("unchecked")
                        Pair<Boolean, File> data = (Pair<Boolean, File>) event.data;
                        boolean toDelete = data.getFirst();
                        File xmlFile = data.getSecond();
                        if(!toDelete) {
                            File runtimeXmlFile = getRuntimeXmlFile(xmlFile);
                            if(runtimeXmlFile == null) {
                                return;
                            }
                            TreeItem newItem = new TreeItem(xmlFilesTree, SWT.NONE);
                            newItem.setText(xmlFile.getName());
                            newItem.setData(XmlManagerStrings.fileKey, runtimeXmlFile);
                        } else {
                            int index = -1;
                            TreeItem[] children = xmlFilesTree.getItems();
                            for(int i = 0; i < children.length; i++) {
                                if(children[i].getText().equals(xmlFile.getName())) {
                                    index = i;
                                    break;
                                }
                            }
                            IStatus status = XmlUtils.removeXmlFile(xmlFile);
                            if(status.isOK() && index != -1) {
                                children[index].dispose();
                            }
                        }
                        break;
                    case SWT.Selection:
                        Event newEvent = new Event();
                        if(event.detail == SWT.CHECK) {
                            newEvent.widget = remove;
                            remove.notifyListeners(SWT.Activate, event);
                        }
                        newEvent.widget = edit;
                        edit.notifyListeners(SWT.Activate, newEvent);
                        break;
                    default:
                        break;
                }
            }

            private File getRuntimeXmlFile(File xmlFile) {
                File activeXMLFolder = new File(XmlUtils.getXmlFilesPath().toString());
                File[] activeXMLs = activeXMLFolder.listFiles();

                if (activeXMLs != null) {
                    for (int i = 0; i < activeXMLs.length; i++) {
                            if (activeXMLs[i].getName().equals(xmlFile.getName())) {
                                return activeXMLs[i];
                            }
                    }
                }
                return null;
            }
        };
    }

    /**
     * This listener opens a fileDialog to choose an XML file.
     * @param parent
     *              The parent's composite
     * @param xmlFilesTree
     *              The tree to be notify when a new file is added
     * @return
     *              The selection listener
     * */
    public static SelectionListener importXmlFileSL(final Composite parent, final Tree xmlFilesTree) {
        return new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(parent.getShell());
                dialog.setText("Import XML Analysis");
                String[] extensions = { "*.xml" };
                String[] extNames = { "Import XML Analysis File (*.xml)" };
                dialog.setFilterExtensions(extensions);
                dialog.setFilterNames(extNames);

                String filePath = dialog.open();
                if (filePath != null)
                {
                    File xml = new File(filePath);
                    /* Check if the file is already active */
                    File xmlFilesFolder = XmlUtils.getXmlFilesPath().toFile();
                    File[] activeFiles = xmlFilesFolder.listFiles();
                    for(int i = 0; i < activeFiles.length; i++) {
                        if(activeFiles[i].getName().equals(xml.getName())) {
                            ErrorDialog.openError(parent.getShell(), "Import error", "Error when adding the file",
                                    new Status(IStatus.ERROR, Activator.PLUGIN_ID, "An analysis file with the same name is already active"));
                            return;
                        }
                    }
                    IStatus status = XmlUtils.xmlValidate(xml);
                    if (status.isOK())
                    {
                        IStatus addStatus = XmlUtils.addXmlFile(xml);
                        if (!addStatus.isOK())
                        {
                            ErrorDialog.openError(parent.getShell(), "Import error", "Error when addind the file", addStatus);
                        }
                        else
                        {
                            XmlAnalysisModuleSource.notifyModuleChange();
                            /*
                             * FIXME: It refreshes the list of analysis under a
                             * trace, but since modules are instantiated when
                             * the trace opens, the changes won't apply to an
                             * opened trace, it needs to be closed then reopened
                             */
                            refreshProject();
                            Event newFile = new Event();
                            newFile.data = new Pair<>(false, xml);
                            xmlFilesTree.notifyListeners(SWT.Modify, newFile);
                        }
                    }
                    else
                    {
                        ErrorDialog.openError(parent.getShell(), "Import error", "The file is not a valid XML file", status);                     }
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
            public void widgetDefaultSelected(SelectionEvent e) { }
        };
    }

    /**
     * This listener removes all selected file in the tree
     * @param xmlFilesTree
     *              The xmlFilesTree
     * @return
     *              The selection listener
     * */
    public static SelectionListener removeXmlFileSL(final Tree xmlFilesTree) {
        return new SelectionListener() {

            @SuppressWarnings("null")
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] files = xmlFilesTree.getSelection();
                for (int i = 0; i < files.length; i++)
                {
                    MessageBox messageBox = new MessageBox(xmlFilesTree.getShell(), SWT.ICON_QUESTION
                            | SWT.YES | SWT.NO);
                    messageBox.setMessage("Do you really want to remove this XML analysis?");
                    messageBox.setText("Removing XML analysis - " + files[i].getText());
                    int response = messageBox.open();
                    if(response == SWT.YES) {
                        /* Notify the tree to delete this item, and the associate file */
                        Event newEvent = new Event();
                        newEvent.data = new Pair<>(true, files[i].getData(XmlManagerStrings.fileKey));
                        xmlFilesTree.notifyListeners(SWT.Modify, newEvent);
                    }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) { }
        };
    }

    /**
     * This listener open a new window to allow the modification
     * of the properties of the file
     * @param xmlFilesTree
     *              The XML file tree
     * @return
     *              The selection listener
     *
     * */
    public static SelectionListener editXmlFileSL(final Tree xmlFilesTree) {
        return new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selection = xmlFilesTree.getSelection();
                if(selection.length != 0) {
                    XmlFilePropertiesViewer pv = new XmlFilePropertiesViewer((File)selection[0].getData(XmlManagerStrings.fileKey));
                    pv.open();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) { }
        };
    }

    /**
     * @return
     *              The selection listener
     */
    public static SelectionListener propertiesTreeSL() {
        return new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem selectedItem = (TreeItem) e.item;
                Node root = (Node) selectedItem.getData(XmlManagerStrings.nodeKey);

                XmlFilePropertiesViewer.fillComposite(root);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) { }
        };
    }

    /**
     * @param parent
     *              The parent composite
     * @param currentPathValue
     *              The Label to update when a new path is builded
     * @param root
     *              The root Node
     * @return
     *              The selection listener
     */
    public static SelectionListener buildPathSL(final Composite parent, final Label currentPathValue, final Node root) {
        return new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Button button = (Button) e.widget;
                Node oldNode = (Node) button.getData(XmlManagerStrings.nodeKey);
                File xmlFile = (File)root.getUserData(XmlManagerStrings.fileKey);

                StateSystemPathBuilderViewer path = new StateSystemPathBuilderViewer(parent.getShell());
                // Check if the user have an active trace before
                // opening
                ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();

                if (trace != null) {
                    int returnCode = path.open();
                    if (returnCode == Window.OK) {
                        currentPathValue.setText(path.getBuildPath());
                        try {
                            XmlUtils.setNewAttribute(xmlFile, XmlUtils.getOriginalXmlFile(xmlFile), oldNode, TmfXmlUiStrings.PATH, path.getBuildPath());
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
                    ErrorDialog.openError(parent.getShell(), "Open error",
                            "An error occured when opening the State System Path Builder",
                            new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No active trace"));
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) { }
        };
    }

    /**
     * @param text
     *              The text to set the new attribute
     * @param initialTitle
     *              The initialTitle
     * @param root
     *              The root node
     * @return
     *              The selection listener
     */
    public static SelectionListener resetTextSL(final Text text, final String initialTitle, final Node root) {
        return new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                MenuItem menuItem = (MenuItem) e.widget;
                Node oldNode = (Node) menuItem.getData(XmlManagerStrings.nodeKey);
                File xmlFile = (File)root.getUserData(XmlManagerStrings.fileKey);

                if (!initialTitle.equals(text.getText())) {
                    text.setText(initialTitle);
                    try {
                        XmlUtils.setNewAttribute(xmlFile, XmlUtils.getOriginalXmlFile(xmlFile), oldNode, TmfXmlStrings.VALUE, text.getText());
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
            public void widgetDefaultSelected(SelectionEvent e) { }
        };
    }

    /**
     * @param text
     *              The Text where occured the listener
     * @param initialTitle
     *              The initial text value of the Text
     * @param root
     *              The root node
     * @return
     *              The modify listener
     */
    public static ModifyListener textML(final Text text, final String initialTitle, final Node root) {
        return new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                Node oldNode = (Node) text.getData(XmlManagerStrings.nodeKey);
                File xmlFile = (File)root.getUserData(XmlManagerStrings.fileKey);

                if (!initialTitle.equals(text.getText()))
                {
                    try {
                        XmlUtils.setNewAttribute(xmlFile, XmlUtils.getOriginalXmlFile(xmlFile), oldNode, TmfXmlStrings.VALUE, text.getText());
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
        };
    }
}