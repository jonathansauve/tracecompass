/*******************************************************************************
 * Copyright (c) 2014, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jonathan Sauvé - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * This viewer allows the user to choose one or more of the three
 * templates of possible root elements in XML file (timeGraphView, xyView
 * and stateProvider).
 *
 * @author Jonathan Sauvé
 * @since 1.0
 */
public class XmlTemplateChooserViewer extends Dialog {

    /** An array to keep the selected templates */
    private static List<File> selectedTemplates = new ArrayList<>();

    /**
     * Public constructor
     * @param parentShell
     *              The parent's shell
     */
    public XmlTemplateChooserViewer(Shell parentShell) {
        super(parentShell);

    }

    // TODO Create XML templates files and save them to a folder in the runtime folder.
    @Override
    protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);

      Label choose = new Label(container, SWT.NONE);
      choose.setText("Choose one or more templates to open:"); //$NON-NLS-1$
      choose.setLocation(10, 10);
      choose.pack();

      SelectionListener sl = new SelectionListener() {

          @Override
          public void widgetSelected(SelectionEvent e) {
              Button selectedTemplate = (Button) e.widget;
              if(selectedTemplate.getSelection())
              {
                  if(((File)selectedTemplate.getData()).exists())
                  {
                      selectedTemplates.add((File)selectedTemplate.getData());
                  }
              }
              else
              {
                  for(int i = 0; i < selectedTemplates.size(); i++)
                  {
                      if(((File)selectedTemplate.getData()).getName().equals(selectedTemplates.get(i).getName()))
                      {
                          selectedTemplates.remove(i);
                      }
                  }
              }
          }

          @Override
          public void widgetDefaultSelected(SelectionEvent e) {
              widgetSelected(e);
          }
    };

      Button tgvButton = new Button(container, SWT.CHECK);
      tgvButton.setText("Time graph view"); //$NON-NLS-1$
      tgvButton.setLocation(20, choose.getLocation().y + choose.getSize().y);
      tgvButton.setData(new File("/home/jonathansauve/Documents/xmlTemplates/timeGraphViewTemplate.xml")); //$NON-NLS-1$
      tgvButton.addSelectionListener(sl);
      tgvButton.pack();

      Button xyButton = new Button(container, SWT.CHECK);
      xyButton.setText("XY view"); //$NON-NLS-1$
      xyButton.setLocation(20,  tgvButton.getLocation().y + tgvButton.getSize().y);
      xyButton.setData(new File("/home/jonathansauve/Documents/xmlTemplates/xyViewTemplate.xml")); //$NON-NLS-1$
      xyButton.addSelectionListener(sl);
      xyButton.pack();

      Button filterButton = new Button(container, SWT.CHECK);
      filterButton.setText("Filter"); //$NON-NLS-1$
      filterButton.setLocation(20,  xyButton.getLocation().y + xyButton.getSize().y);
      filterButton.setData(new File("/home/jonathansauve/Documents/xmlTemplates/filterTemplate.xml")); //$NON-NLS-1$
      filterButton.addSelectionListener(sl);
      filterButton.pack();

      return container;
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("XML template chooser"); //$NON-NLS-1$
    }

    @Override
    protected Point getInitialSize() {
      return new Point(450, 250);
    }

    /**
     * This method must be called after {@link XmlTemplateChooserViewer#open()} function
     * @return
     *              List of selected templates. Can be empty.
     */
    public List<String> getChoosedTemplates()
    {
        List<String> temp = new ArrayList<>();
        for(int i = 0; i < selectedTemplates.size(); i++)
        {
            temp.add(selectedTemplates.get(i).getAbsolutePath());
        }
        return temp;
    }

}
