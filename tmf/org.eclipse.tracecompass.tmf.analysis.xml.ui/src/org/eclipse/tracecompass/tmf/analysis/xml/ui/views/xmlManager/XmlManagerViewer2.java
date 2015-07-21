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

    private Tree xmlFilesTree;

    private Composite actionsComposite;
        private Button importXmlFile;
        private Button removeXmlFile;
        private Button editFile;

    /** Xml file folder and files */
    private File activeXMLFolder = new File(XmlUtils.getXmlFilesPath().toString());
    private File[] activeXMLs = activeXMLFolder.listFiles();

    /**
     *
     * @param parent
     *              The parent's composite
     */
    public XmlManagerViewer2(Composite parent) {
        fparent = parent;

        createContents();
    }

    /**
     *
     * */
    private void createContents() {
        fparent.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesAndActions = new SashForm(fparent, SWT.HORIZONTAL);
        xmlFilesAndActions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesTree = new Tree(xmlFilesAndActions, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
        for(int i = 0; i < activeXMLs.length; i++) {
            TreeItem xmlFileItem = new TreeItem(xmlFilesTree, SWT.NONE);
            xmlFileItem.setText(activeXMLs[i].getName());
            xmlFileItem.setData(XmlManagerStrings.fileKey, activeXMLs[i]);
        }
        xmlFilesTree.addListener(SWT.Modify, XmlManagerListeners.xmlFilesTreeListener(xmlFilesTree));

        actionsComposite = new Composite(xmlFilesAndActions, SWT.NONE);
        actionsComposite.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        actionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesAndActions.setWeights(new int[] {3, 1});

        importXmlFile = new Button(actionsComposite, SWT.PUSH);
        importXmlFile.setText("Import");
        importXmlFile.addSelectionListener(XmlManagerListeners.importXmlFileSL(fparent, xmlFilesTree));

        removeXmlFile = new Button(actionsComposite, SWT.PUSH);
        removeXmlFile.setText("Remove");
        removeXmlFile.addSelectionListener(XmlManagerListeners.removeXmlFileSL(xmlFilesTree));

        editFile = new Button(actionsComposite, SWT.PUSH);
        editFile.setText("Edit");
        editFile.addSelectionListener(XmlManagerListeners.editXmlFileSL(xmlFilesTree));
    }
}
