package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.ui.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectModelElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @author Jonathan Sauvé
 *
 */
public class XmlManagerListeners {

    /** Make this class non-instantiable */
    private XmlManagerListeners(){ }

    /** XmlManagerViewer2 listeners */

    /**
     * This listener implements multiple cases for the xmlFilesTree
     * @param xmlFilesTree
     *              The xmlFilesTree
     * @return
     *              The listener for this tree
     * */
    public static Listener xmlFilesTreeListener(final Tree xmlFilesTree) {
        return new Listener() {

            @Override
            public void handleEvent(Event event) {
                switch(event.type) {
                    case SWT.Modify:
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
                    default:
                }
            }

            private File getRuntimeXmlFile(File xmlFile) {
                File activeXMLFolder = new File(XmlUtils.getXmlFilesPath().toString());
                File[] activeXMLs = activeXMLFolder.listFiles();
                boolean present = false;
                int count = 0;
                TreeItem[] items = xmlFilesTree.getItems();

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
                            ErrorDialog.openError(parent.getShell(), "Import error", "Error when addind the file", addStatus); //$NON-NLS-1$ //$NON-NLS-2$
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
                        ErrorDialog.openError(parent.getShell(), "Import error", "The file is not a valid XML file", status); //$NON-NLS-1$ //$NON-NLS-2$
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

            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] files = xmlFilesTree.getItems();
                for (int i = 0; i < files.length; i++)
                {
                    if (files[i].getChecked() == true)
                    {
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
}
