package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Jonathan Sauvé
 *
 */
public class AddNewElementViewer extends Dialog {

    /**
     * @param parentShell
     *              The parent shell
     * @param nodeName
     *              The name of the new node. Must be one present in
     *              <code>TmfXmlStrings</code> et <code>TmfXmlUiStrings</code>
     */
    public AddNewElementViewer(Shell parentShell, String nodeName) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        return container;
    }

    @SuppressWarnings("nls")
    @Override
    protected void configureShell(Shell newShell) {
        newShell.setText("Add New Element to XML File");
        super.configureShell(newShell);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(400, 700);
    }

    @Override
    public int open() {
        return super.open();
    }

}
