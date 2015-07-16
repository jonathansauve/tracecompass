package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * This viewer extends dialog to show the progress
 * while merging two XML files.
 * @author Jonathan Sauvé
 */
public class XmlMergeProgressViewer extends Dialog {
    private static volatile ProgressBar bar;
    private static volatile Label percentage;
    private static File file1;
    private static File file2;

    /**
     * @param parentShell The parent shell
     * @param file1 The first file
     * @param file2 The second file
     */
    protected XmlMergeProgressViewer(Shell parentShell, File file1, File file2) {
        super(parentShell);
        XmlMergeProgressViewer.file1 = file1;
        XmlMergeProgressViewer.file2 = file2;
    }

    @Override
    protected Control createDialogArea(Composite parent) {

      Composite container = (Composite) super.createDialogArea(parent);
      // TODO Change layout, add % under progress bar and change his size (setBounds)

      Label mergingFiles = new Label(container, SWT.NONE);
      mergingFiles.setText("Merging files..."); //$NON-NLS-1$

      FontData fontData = mergingFiles.getFont().getFontData()[0];
      fontData.setHeight(18);
      Font font = new Font(container.getDisplay(), new FontData(fontData.getName(), fontData
          .getHeight(), SWT.NORMAL));
      mergingFiles.setFont(font);

      Composite filesComposite = new Composite(container, SWT.NONE);
      GridLayout gl = new GridLayout(1, false);
      gl.marginLeft = 10;
      gl.marginBottom = 10;
      filesComposite.setLayout(gl);

      Label file1Label = new Label(filesComposite, SWT.NONE);
      file1Label.setText(file1.getName());

      Label file2Label = new Label(filesComposite, SWT.NONE);
      file2Label.setText(file2.getName());

      bar = new ProgressBar(container, SWT.HORIZONTAL | SWT.SMOOTH);
      bar.setMinimum(0);
      bar.setMaximum(100);
      bar.setSelection(bar.getMinimum());
      bar.setSize(250, 40);

      percentage = new Label(container, SWT.NONE);
      percentage.setText(0 + "%     "); //$NON-NLS-1$
      percentage.setSize(80, 20);

      return container;
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Merging XML files"); //$NON-NLS-1$
    }

    @Override
    protected Point getInitialSize() {
      return new Point(450, 250);
    }

    @Override
    public int open() {
        super.setBlockOnOpen(false);
        super.open();
        super.setBlockOnOpen(true);

        final Shell shell = super.getShell();
        final Button okButton = super.getButton(IDialogConstants.OK_ID);
        final Button stopButton = super.getButton(IDialogConstants.CANCEL_ID);
        super.createButton(shell, 2, "Cancel", false); //$NON-NLS-1$
        okButton.setText("Finish"); //$NON-NLS-1$
        stopButton.setText("Pause"); //$NON-NLS-1$

        bar.setSelection(bar.getMinimum());
        percentage.setText(bar.getMinimum() + "%"); //$NON-NLS-1$
        final int maximum = bar.getMaximum();

        new Thread() {
            @Override
            public void run() {
                for(final int[] i = new int[1]; i[0] < maximum; i[0]++) {
                    try {
                        Thread.sleep(50);
                    }
                    catch (Throwable th) {}
                     Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            if(shell == null || bar.isDisposed() || percentage.isDisposed() || shell.isDisposed()) {
                                return;
                            }
                            bar.setSelection(i[0]);
                            percentage.setText(i[0] + "%"); //$NON-NLS-1$

                            XmlMerger.mergeXmlFiles(file1, file2);
                        }

                    });
                }
            }
        }.start();

        // Wait until the window is closed
        Display display = null;
        if(shell != null) {
            display = shell.getDisplay();
        } else {
            display = Display.getCurrent();
        }

        while (shell != null && !shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
                if(bar.getSelection() >= bar.getMaximum()) {
                    okButton.setEnabled(true);
                }
            } catch (Throwable e) {
            }
        }
        if (!display.isDisposed()) {
            display.update();
        }

        return super.getReturnCode();
    }

    /*private static void mergeFiles() {
        // Validate the files
        if(XmlUtils.xmlValidate(file1).isOK() && XmlUtils.xmlValidate(file2).isOK()) {
            // Parse the files
            DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
            Document doc1 = null, doc2 = null, mergeDoc = null;
            try {
                DocumentBuilder dBuilder = dbFact.newDocumentBuilder();
                doc1 = dBuilder.parse(file1);
                doc2 = dBuilder.parse(file2);
                doc1.normalize();
                doc2.normalize();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(doc1 != null && doc2 != null) {
                mergeDoc = doc1;
                Element root1 = doc1.getDocumentElement();
                Element root2 = doc2.getDocumentElement();

                NodeList root1Children = root1.getChildNodes();
                DataTreeNode[] dataRoot1Children = new DataTreeNode[root1Children.getLength()];
                for(int i = 0; i < root1Children.getLength(); i++) {
                    Node child = root1Children.item(i);
                    dataRoot1Children[i] = new DataTreeNode(child.getNodeName(), child);
                }
                DataTreeNode dataRoot1 = new DataTreeNode(root1.getNodeName(), root1, dataRoot1Children);
                DeltaDataTree deltaTree1 = new DeltaDataTree(dataRoot1);
                System.out.println(deltaTree1.getChildCount(new Path(dataRoot1.getName())));
            }


        }
    }*/
}
