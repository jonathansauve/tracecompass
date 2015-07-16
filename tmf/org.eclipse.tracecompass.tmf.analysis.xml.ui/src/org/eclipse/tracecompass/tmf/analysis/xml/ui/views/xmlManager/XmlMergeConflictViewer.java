package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * @author jonathansauve
 *
 */
public class XmlMergeConflictViewer extends Dialog {

    /**
     * @param parentShell The parent shell
     */
    protected XmlMergeConflictViewer(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);

      Label conflict = new Label(container, SWT.NONE);
      conflict.setText("There's a conflict"); //$NON-NLS-1$

      return container;
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Merge conflict"); //$NON-NLS-1$
    }

    @Override
    protected Point getInitialSize() {
      return new Point(450, 250);
    }

}
