package org.eclipse.tracecompass.tmf.ui.views.xmlManager;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;

/**
 * This class is a SelectionAdapter used by the mergeFiles button in
 * the XML manager view. It uses the static method {@link XmlManagerViewer#getTreeSelection()}
 * to retrieve the selected files to merge. These files are passed to the {@link XmlMergeProgressViewer}.
 * @author Jonathan Sauvé
 *
 */
@SuppressWarnings("restriction")
public class XmlMergeSelectionAdapter extends SelectionAdapter {
    private static Shell myShell;
    private Object[] items;
    private static File file1;
    private static File file2;

    /**
     * @param shell The parent shell
     */
    public XmlMergeSelectionAdapter(Shell shell) {
      myShell = shell;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        this.items = XmlManagerViewer.getTreeSelection();
        if(items.length != 2)
        {
            ErrorDialog.openError(myShell, "Merge error", "The merge failed",  //$NON-NLS-1$ //$NON-NLS-2$
                    new Status(IStatus.ERROR, Activator.PLUGIN_ID, "You can't merge more than 2 files"));  //$NON-NLS-1$
            return;
        }

        file1 = (File)((TreeItem)items[0]).getData();
        file2 = (File)((TreeItem)items[1]).getData();

        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                XmlMergeProgressViewer mergeProgressViewer = new XmlMergeProgressViewer(myShell, file1, file2);
                mergeProgressViewer.open();
            }
        });
    }
}
