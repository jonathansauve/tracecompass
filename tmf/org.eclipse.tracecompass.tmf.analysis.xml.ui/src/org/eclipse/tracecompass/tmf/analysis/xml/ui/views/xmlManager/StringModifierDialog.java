package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author jonathansauve
 *
 */
public class StringModifierDialog extends Dialog {

    private String ftitle;
    private String fname;
    private String foldValue;
    private String freason;

    private String currentValue;

    /**
     * @param parentShell
     *              The parent's shell
     * @param title
     *              The title of the Dialog
     * @param name
     *              The name of the string the
     *              user have to modify
     * @param oldValue
     *              The old value (to change)
     * @param reason
     *              The reason to modify this string
     */
    public StringModifierDialog(Shell parentShell, String title, String name, String oldValue, String reason) {
        super(parentShell);
        super.setShellStyle(super.getShellStyle() | SWT.SHELL_TRIM);
        ftitle = title;
        fname = name;
        foldValue = oldValue;
        freason = reason;
        currentValue = oldValue;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        Label modifyReason = new Label(container, SWT.NONE);
        modifyReason.setText(freason);

        Label enterValueText = new Label(container, SWT.NONE);
        enterValueText.setText("Enter a new value for " + fname + ":"); //$NON-NLS-1$ //$NON-NLS-2$

        Text enterValue = new Text(container, SWT.NONE);
        enterValue.setLayoutData(new GridData(400, 19));
        enterValue.setText(foldValue);
        enterValue.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                currentValue = ((Text)e.widget).getText();
            }
        });

        return container;
    }

    @Override
    public int open() {
        super.setBlockOnOpen(false);
        super.open();
        super.setBlockOnOpen(true);

        final Button okButton = super.getButton(IDialogConstants.OK_ID);
        okButton.setText("Finish"); //$NON-NLS-1$

        Display display;
        Shell shell = super.getShell();
        if (shell == null) {
            display = Display.getCurrent();
        } else {
            display = Display.getCurrent();
        }

        while (shell != null && !shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
                if(!okButton.isDisposed()) {
                    if(currentValue.equals(foldValue)) {
                        okButton.setEnabled(false);
                    } else {
                        okButton.setEnabled(true);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (!display.isDisposed())
         {
            display.update();
         }

        return super.getReturnCode();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(ftitle);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(600, 300);
    }

    /**
     * @return
     *              The modified string
     */
    public String getModifiedString() {
        return currentValue;
    }
}
