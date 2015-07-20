package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

    /** Xml file folder and files */
    private File activeXMLFolder = new File(XmlUtils.getXmlFilesPath().toString());
    private File[] activeXMLs = activeXMLFolder.listFiles();

    /** Keys to retrieve saved objects */
    private static final String nodeKey = "node"; //$NON-NLS-1$
    private static final String fileKey = "file"; //$NON-NLS-1$

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
        fparent.setLayout(createGridLayout(1, 0, 0));
        fparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesAndActions = new SashForm(fparent, SWT.HORIZONTAL);
        xmlFilesAndActions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesTree = new Tree(xmlFilesAndActions, SWT.CHECK);
        for(int i = 0; i < activeXMLs.length; i++) {
            TreeItem xmlFileItem = new TreeItem(xmlFilesTree, SWT.NONE);
            xmlFileItem.setText(activeXMLs[i].getName());
            xmlFileItem.setData(fileKey, activeXMLs[i]);
        }

        actionsComposite = new Composite(xmlFilesAndActions, SWT.NONE);
        actionsComposite.setLayout(createGridLayout(1, 0, 0));
        actionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        xmlFilesAndActions.setWeights(new int[] {3, 1});

        importXmlFile = new Button(actionsComposite, SWT.PUSH);
        importXmlFile.setText("Import");
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
