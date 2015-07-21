package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author Jonathan Sauvé
 *
 */
@SuppressWarnings("nls")
public class XmlFilePropertiesViewer {

    private File fxmlFile;

    private Shell fshell;
    private Composite fparent;

    private SashForm fsash;
        private Tree ftree;
        private Composite fproperties;

    /**
     * Public constructor
     * @param xmlFile
     *              The xmlFile
     * */
    public XmlFilePropertiesViewer(File xmlFile) {
        fxmlFile = xmlFile;

        fshell = new Shell(SWT.SHELL_TRIM);
        fshell.setText("Properties - " + fxmlFile.getName());
        fshell.setMinimumSize(400, 400);
        fshell.setSize(400, 400);
        fshell.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));

        fparent = new Composite(fshell, SWT.NONE);
        fparent.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createContents();
    }

    private void createContents() {
        fsash = new SashForm(fparent, SWT.HORIZONTAL);
        fsash.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fsash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        ftree = new Tree(fsash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        ftree.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        ftree.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, true));

        TmfXmlManagerParser parser = null;
        try {
            parser = new TmfXmlManagerParser(fxmlFile.getPath());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        if(parser != null) {
            List<Node> roots = parser.getRoots();
            for(Node root : roots) {
                TreeItem item = new TreeItem(ftree, SWT.NONE);
                item.setText(root.getNodeName());
                item.setData(XmlManagerStrings.nodeKey, root);
            }
        }


        fproperties = new Composite(fsash, SWT.NONE);
        fproperties.setLayout(XmlManagerUtils.createGridLayout(1, 0, 0));
        fproperties.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        fsash.setWeights(new int[] {1, 3});
    }

    /**
     *
     * */
    public void open() {
        fshell.open();
    }
}
