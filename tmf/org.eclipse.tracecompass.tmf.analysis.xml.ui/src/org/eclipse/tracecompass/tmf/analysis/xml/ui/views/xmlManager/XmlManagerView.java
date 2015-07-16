package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;


/**
 * @author Jonathan Sauvé
 * @since 1.0
 */
public class XmlManagerView extends TmfView {

    private final static String ID = "org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager"; //$NON-NLS-1$
    XmlManagerViewer fViewer;
    /**
     * Constructor
     */
    public XmlManagerView() {
        super(ID);
    }

    @Override
    public void createPartControl(Composite parent) {
        fViewer = new XmlManagerViewer(parent);
    }

    @Override
    public void setFocus() {
        // TODO Auto-generated method stub

    }
}
