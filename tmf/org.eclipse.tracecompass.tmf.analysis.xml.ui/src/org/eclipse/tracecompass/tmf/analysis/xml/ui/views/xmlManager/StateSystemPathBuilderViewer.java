package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tracecompass.internal.statesystem.core.AttributeTree;
import org.eclipse.tracecompass.internal.statesystem.core.StateSystem;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.views.statesystem.TmfStateSystemViewer;
import org.eclipse.tracecompass.tmf.ui.views.statesystem.TmfStateSystemViewer.StateEntry;
import org.eclipse.tracecompass.tmf.ui.views.statesystem.TmfStateSystemViewer.StateSystemEntry;

/**
 * This view allows the users to build a string that
 * represents the entry path for the XML analysis. The
 * method {@link StateSystemPathBuilderViewer#getBuildPath()}
 * must be called (after opening) to get the build path.
 *
 * @author Jonathan Sauvé
 *
 */

@SuppressWarnings("restriction")
public class StateSystemPathBuilderViewer extends Dialog {

    /** The shell */
    private Shell parentShell;

    /** Two main buttons */
    private Button quickPathButton;
    private Button customPathButton;

    /** Variables related to the State System Explorer */
    private TmfStateSystemViewer fViewer = null;
    private ITmfTrace trace = null;
    private StateSystem ss = null;
    private List<StateSystemEntry> ssEntries = new ArrayList<>();
    private List<StateSystem> stateSystems = new ArrayList<>();
    private AttributeTree attTree = null;

    /** Variable to keep the current path (in-building) */
    private static String currentBuildPath = ""; //$NON-NLS-1$

    /** Global Labels that change at runtime */
    private Label pathValue;
    private Label pathIsValid;
    private Label partialPathValue;
    private Label pointerValue;

    /** Global composites */
    private SashForm sash;
    private Group group1;
    private Composite pathBuilderComposite;
    private Group group2;
    private Tree tree;

    /** Variable to keep the current drag object */
    private static final Object[] currentDragSource = new Object[1];

    /** Constant strings */
    private static final String INVALID_PATH_MESSAGE = "*This is not a valid path for the currents state systems"; //$NON-NLS-1$

    /**
     * Public constructor
     * @param parent The parent's shell
     */
    public StateSystemPathBuilderViewer(Shell parent) {
        super(parent);
        super.setShellStyle(super.getShellStyle() | SWT.SHELL_TRIM);
        parentShell = parent;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        initializeQuickPath(container);
        initializeCustomPath(container);
        initializeCurrentPath(container);

        return container;
    }

    @Override
    public int open() {
        // Disable the event loop to write our own one
        super.setBlockOnOpen(false);
        super.open();
        //super.setBlockOnOpen(true);

        final Button okButton = super.getButton(IDialogConstants.OK_ID);
        okButton.setText("Finish"); //$NON-NLS-1$

        // Get the shell's display, if possible
        Display display;
        Shell shell = super.getShell();
        if (shell == null) {
            display = Display.getCurrent();
        } else {
            display = shell.getDisplay();
        }

        // loop till the shell is closed
        while (shell != null && !shell.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
                // Validate the current path ; if not valid, show an indicator
                if(!pathIsValid.isDisposed() && !pathValidator(currentBuildPath)) {
                    if(!((pathIsValid.getText()).equals(INVALID_PATH_MESSAGE))) {
                        pathIsValid.setText(INVALID_PATH_MESSAGE);
                    }
                }
                else {
                    if(!pathIsValid.isDisposed() && !pathIsValid.getText().equals("")) { //$NON-NLS-1$
                        pathIsValid.setText(""); //$NON-NLS-1$
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

        // return the returnCode : CANCEL or OK (finish)
        return super.getReturnCode();
    }

    @Override
    public boolean close() {
        if(fViewer != null) {
            fViewer.dispose();
        }
        if(pathValue != null && !pathValue.isDisposed()) {
            pathValue.dispose();
        }
        if(pathIsValid != null && !pathIsValid.isDisposed()) {
            pathIsValid.dispose();
        }

        return super.close();
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("State system path builder"); //$NON-NLS-1$
      newShell.setMinimumSize(1050, 600);
    }

    @Override
    protected Point getInitialSize() {
      return new Point(1050, 800);
    }

    /**
     * @return Return the path build by the user.
     * If the user build nothing, an empty string is return.
     */
    public String getBuildPath() {
        return currentBuildPath;
    }


    @SuppressWarnings("javadoc")
    protected void initializeQuickPath(Composite parent) {
        quickPathButton = new Button(parent, SWT.RADIO);
        quickPathButton.setText("Quick path builder"); //$NON-NLS-1$

        final Composite quickPathBuilderComposite = new Composite(parent, SWT.NONE);
        quickPathBuilderComposite.setLayout(new GridLayout(2, false));

        final Label comboText = new Label(quickPathBuilderComposite, SWT.NONE);
        comboText.setText("1. Choose one of the following paths: "); //$NON-NLS-1$
        GridData marginLeftData = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
        marginLeftData.horizontalIndent = 10;
        comboText.setLayoutData(marginLeftData);

        final Combo quickPathCombo = new Combo(quickPathBuilderComposite, SWT.READ_ONLY);
        String[] items = {"*", "CPUs/*", "Threads/*"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        quickPathCombo.setItems(items);
        quickPathCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                currentBuildPath = ((Combo)e.widget).getText();
                pathValue.setText(currentBuildPath);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        final Label textText = new Label(quickPathBuilderComposite, SWT.NONE);
        textText.setText("2. Write your own path: "); //$NON-NLS-1$
        textText.setLayoutData(marginLeftData);

        final Text customText = new Text(quickPathBuilderComposite, SWT.MULTI | SWT.WRAP | SWT.BORDER);
        customText.setLayoutData(new GridData(350, 19));
        customText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                currentBuildPath = ((Text)e.widget).getText();
                pathValue.setText(((Text)e.widget).getText());
            }
        });

        setEnabledRecursive(quickPathBuilderComposite, false);

        quickPathButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if(quickPathButton.getSelection()) {
                    setEnabledRecursive(quickPathBuilderComposite, true);
                    customPathButton.setSelection(false);
                    customPathButton.notifyListeners(SWT.Selection, new Event());
                }
                else {
                    setEnabledRecursive(quickPathBuilderComposite, false);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
    }

    @SuppressWarnings({ "javadoc" })
    protected void initializeCustomPath(Composite parent) {
        Composite customPathButtonsComposite = new Composite(parent, SWT.NONE);
        GridLayout grid2 = new GridLayout(2, false);
        grid2.marginHeight = 0; grid2.marginWidth = 0;
        grid2.horizontalSpacing = 0; grid2.verticalSpacing = 0;
        customPathButtonsComposite.setLayout(grid2);

        customPathButton = new Button(customPathButtonsComposite, SWT.RADIO);
        customPathButton.setText("Custom path builder"); //$NON-NLS-1$

        Button helpButton = new Button(customPathButtonsComposite, SWT.PUSH);
        helpButton.setText("Help"); //$NON-NLS-1$

        final Composite customPathComposite = new Composite(parent, SWT.NONE);
        customPathComposite.setLayout(new GridLayout());
        customPathComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        fViewer = new TmfStateSystemViewer(customPathComposite);
        fViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            fViewer.traceSelected(new TmfTraceSelectedSignal(this, trace));
            for(IAnalysisModule module : trace.getAnalysisModules()) {
                final ITmfTrace trace2 = trace;
                if (trace2 != null) {
                    ITmfStateSystem stateSystem = TmfStateSystemAnalysisModule.getStateSystem(trace2, module.getId());
                    if(stateSystem != null && stateSystem instanceof StateSystem) {
                        stateSystems.add((StateSystem) stateSystem);
                    }
                }
            }
        }

        fViewer.addSelectionChangeListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection() instanceof IStructuredSelection) {
                    Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
                    if(selection instanceof ITmfTreeViewerEntry) {
                        if(ssEntries.isEmpty()) {
                            ITmfTreeViewerEntry root = findRootEntryRecursive((ITmfTreeViewerEntry) selection);
                            initializeSSEntriesRecursive(root);
                        }

                        if (selection instanceof StateEntry) {
                            StateEntry entry = (StateEntry) selection;
                            setStateSystemAndAtributeTree(entry);

                            if(ss != null && attTree != null) {
                                try {
                                    // Query the state interval, get the full path to the attribute
                                    ITmfStateInterval si = ss.querySingleState(trace.getStartTime().getValue(), entry.getQuark());
                                    String path = attTree.getFullAttributeName(si.getAttribute());
                                    partialPathValue.setText(path);
                                    pointerValue.setText("${" + path + "}"); //$NON-NLS-1$ //$NON-NLS-2$
                                } catch (AttributeNotFoundException e) {
                                    e.printStackTrace();
                                } catch (TimeRangeException e) {
                                    e.printStackTrace();
                                } catch (StateSystemDisposedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

            private void setStateSystemAndAtributeTree(StateEntry entry) {
                ss = null;
                attTree = null;
                StateSystemEntry ssEntry = null;
                ITmfTreeViewerEntry current = entry;
                while(current != null) {
                    current = current.getParent();
                    if(current instanceof StateSystemEntry) {
                        ssEntry = (StateSystemEntry) current;
                        break;
                    }
                }

                if(ssEntry != null) {
                    final ITmfTrace trace2 = trace;
                    final String name = ssEntry.getName();
                    if (trace2 != null && name != null) {
                        ss = (StateSystem)TmfStateSystemAnalysisModule.getStateSystem(trace2, name);
                    }

                    if(ss != null && ss.getAttributeTree() != null) {
                        attTree = ss.getAttributeTree();
                    }
                }
            }

            private ITmfTreeViewerEntry findRootEntryRecursive(ITmfTreeViewerEntry entry) {
                if(entry.getParent() != null) {
                    return findRootEntryRecursive(entry.getParent());
                }
                return entry;
            }

            private void initializeSSEntriesRecursive(ITmfTreeViewerEntry root) {
                for(ITmfTreeViewerEntry child : root.getChildren()) {
                    if(child instanceof StateSystemEntry) {
                        ssEntries.add((StateSystemEntry) child);
                    }
                    initializeSSEntriesRecursive(child);
                }
            }
        });

        Composite partialPathsComposite = new Composite(customPathComposite, SWT.NONE);
        partialPathsComposite.setLayout(new GridLayout(6, false));
        partialPathsComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));

        Label partialPathText = new Label(partialPathsComposite, SWT.NONE);
        partialPathText.setText("Partial path: "); //$NON-NLS-1$

        partialPathValue = new Label(partialPathsComposite, SWT.BORDER | SWT.WRAP);
        partialPathValue.setLayoutData(new GridData(200, 20));

        Button addPartialPath = new Button(partialPathsComposite, SWT.PUSH);
        addPartialPath.setText("+"); //$NON-NLS-1$
        addPartialPath.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                List<String> subPaths = getPathElements(partialPathValue.getText());
                for(int i = 0; i < subPaths.size(); i++) {
                    addTreeItem(subPaths.get(i));
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        addPartialPath.pack();


        Label pointerText = new Label(partialPathsComposite, SWT.NONE);
        pointerText.setText("Pointer: "); //$NON-NLS-1$

        pointerValue = new Label(partialPathsComposite, SWT.BORDER | SWT.WRAP);
        pointerValue.setLayoutData(new GridData(200, 20));
        pointerValue.setSize(pointerValue.computeSize(200, 20, true));

        Button addPointerPath = new Button(partialPathsComposite, SWT.PUSH);
        addPointerPath.setText("+"); //$NON-NLS-1$
        addPointerPath.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                addTreeItem(pointerValue.getText());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        addPointerPath.pack();

        sash = new SashForm(customPathComposite, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        group1 = new Group(sash, SWT.NONE);
        group1.setText("Path builder"); //$NON-NLS-1$ // TODO To change
        group1.setLayout(new GridLayout(1, false));
        group1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        pathBuilderComposite = new Composite(group1, SWT.NONE);
        pathBuilderComposite.setLayout(new GridLayout(6, false));
        pathBuilderComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        pathBuilderComposite.setBackground(new Color(Display.getDefault(), 255, 255, 255));

        // Create a menu
        Menu menu = new Menu(pathBuilderComposite);
        MenuItem addBoxItem = new MenuItem(menu, SWT.NONE);
        addBoxItem.setText("New box"); //$NON-NLS-1$
        addBoxItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewBox(""); //$NON-NLS-1$
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        MenuItem setNewPath = new MenuItem(menu, SWT.NONE);
        setNewPath.setText("Set as current path"); //$NON-NLS-1$
        setNewPath.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                buildCurrentPathFromBoxes();
                if(!pathValue.isDisposed() && !pathContainsPointer(currentBuildPath)) {
                    pathValue.setText(currentBuildPath);
                }
                else if(pathContainsPointer(currentBuildPath)) {
                   if(ssEntries.isEmpty())
                   {
                       ErrorDialog.openError(parentShell, "Validation error", "An error occured during the validation of a pointer",  //$NON-NLS-1$//$NON-NLS-2$
                            new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The state systems aren't initialized")); //$NON-NLS-1$
                   }
                   else {
                       pathValue.setText(currentBuildPath);
                   }
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        pathBuilderComposite.setMenu(menu);

        pathBuilderComposite.pack();

        group2 = new Group(sash, SWT.SHADOW_ETCHED_IN);
        group2.setText("Stack"); //$NON-NLS-1$ // TODO To change
        group2.setLayout(new GridLayout(1, false));
        group2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        tree = new Tree(group2, SWT.NONE);
        tree.setLayout(new GridLayout());
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tree.setLinesVisible(true);
        setTreeDragDrop(tree);

        sash.setWeights(new int[]{3,1});


        /* Disable all widgets until the option is selected */
        setEnabledRecursive(customPathComposite, false);

        customPathButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if(customPathButton.getSelection()) {
                    setEnabledRecursive(customPathComposite, true);
                    quickPathButton.setSelection(false);
                    quickPathButton.notifyListeners(SWT.Selection, new Event());
                }
                else {
                    setEnabledRecursive(customPathComposite, false);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });


    }

    @SuppressWarnings("javadoc")
    protected void initializeCurrentPath(Composite parent) {
        Composite currentPathComposite = new Composite(parent, SWT.NONE);
        currentPathComposite.setLayout(new GridLayout());

        Label currentPathTitle = new Label(currentPathComposite, SWT.NONE);
        currentPathTitle.setText("Current path:"); //$NON-NLS-1$

        FontData fontData = currentPathTitle.getFont().getFontData()[0];
        Font font = new Font(parent.getDisplay(), new FontData(fontData.getName(), fontData
            .getHeight(), SWT.BOLD));

        currentPathTitle.setFont(font);

        Composite currentPathValueComp = new Composite(currentPathComposite, SWT.NONE);
        GridLayout currentPathValueLayout = new GridLayout(1, false);
        currentPathValueLayout.marginWidth = 15;
        currentPathValueComp.setLayout(currentPathValueLayout);

        pathValue = new Label(currentPathValueComp, SWT.WRAP | SWT.BORDER);
        pathValue.setText(""); //$NON-NLS-1$
        pathValue.setLayoutData(new GridData(400, SWT.DEFAULT));

        pathIsValid = new Label(currentPathValueComp, SWT.NONE);
        pathIsValid.setText(""); //$NON-NLS-1$
        pathIsValid.setLayoutData(new GridData(300, 20));

        FontData fontData2 = pathIsValid.getFont().getFontData()[0];
        Font font2 = new Font(parent.getDisplay(), new FontData(fontData2.getName(), 8, SWT.ITALIC));

        pathIsValid.setFont(font2);


    }

    private static void setEnabledRecursive(Composite parent, boolean enabled) {
        Control[] children = parent.getChildren();
        if(children.length != 0) {
            for(int i = 0; i < children.length; i++) {
                if(children[i] instanceof Composite) {
                    setEnabledRecursive((Composite)children[i], enabled);
                }
                else {
                    children[i].setEnabled(enabled);
                }
            }
        }
        parent.setEnabled(enabled);
    }

    private boolean pathValidator(String path) {
         List<String> pathElements = getPathElements(path);
         List<String> pathWithoutPointer = new ArrayList<>();
        // Get value(s) from pointer(s)
        for(int i = 0; i < pathElements.size(); i++) {
            String element = pathElements.get(i);
            if(!element.equals("")) { //$NON-NLS-1$
                // check if element is a pointer
                if(element.charAt(0) == '$') {
                    String value = extractValueFromPointer(element);
                    if(value == null) {
                        return false;
                    }
                    //replace the pointer by is value
                    pathWithoutPointer.add(value);
                }
                else {
                    pathWithoutPointer.add(element);
                }
            }
        }

        String fullPath = getFullPathFromElements(pathWithoutPointer);
        String value = validatePath(fullPath);
        if(value == null) {
            return false;
        }
        return true;
    }

    private String extractValueFromPointer(String substring) {
        return extractValueFromPath(substring.substring(2, substring.length()-1));
    }

    private String extractValueFromPath(String substring) {

        List<String> pathElements = getPathElements(substring);
        ITmfTreeViewerEntry currentEntry;
        // check for all the stateSystem
        for(StateSystemEntry ssEntry : ssEntries) {
            currentEntry = ssEntry;
            boolean isPresent = false;
            // check for all the elements in the path
            for(int i = 0; i < pathElements.size(); i++) {
                isPresent = false;
                // check is a children of the current entry match with the current element of the path
                if(!currentEntry.getChildren().isEmpty()) {
                    // Case 1: last element is * and currentEntry has children
                    if(pathElements.get(i).equals("*") && i == pathElements.size()-1) { //$NON-NLS-1$
                        if(currentEntry instanceof StateEntry) {
                            return ((StateEntry)currentEntry).getValue();
                        }
                        if(currentEntry instanceof StateSystemEntry) {
                            return ((StateSystemEntry)currentEntry).getName();
                        }
                    }
                    for(int j = 0; j < currentEntry.getChildren().size(); j++) {
                        ITmfTreeViewerEntry entry = currentEntry.getChildren().get(j);
                        if(entry.getName().equals(pathElements.get(i))) {
                            currentEntry = entry;
                            isPresent = true;
                            if(i == pathElements.size()-1) {
                                return ((StateEntry)currentEntry).getValue();
                            }
                            break;
                        }

                    }
                    if(!isPresent) {
                        break;
                    }
                }
                else {
                    // Case 2: reaching last element and currenEntry has no children
                    if(i == pathElements.size()-1 && isPresent) {
                        if(currentEntry instanceof StateEntry) {
                            return ((StateEntry)currentEntry).getValue();
                        }
                        if(currentEntry instanceof StateSystemEntry) {
                            return ((StateSystemEntry)currentEntry).getName();
                        }
                    }
                    break;
                }
            }
        }
        // Case 3: No match in all ssEntry
        return null;
    }

    private String validatePath(String substring) {

        List<String> pathElements = getPathElements(substring);
        int currentQuark = 0;
        // check for all the stateSystem
        for(StateSystem stateSystem : stateSystems) {
            currentQuark = -1; // the root
            boolean isPresent = false;
            // check for all the elements in the path
            for(int i = 0; i < pathElements.size(); i++) {
                isPresent = false;
                // check is a children of the current entry match with the current element of the path
                try {
                    if(!stateSystem.getSubAttributes(currentQuark, false).isEmpty()) {
                        // Case 1: last element is * and currentEntry has children
                        if(pathElements.get(i).equals("*") && i == pathElements.size()-1) { //$NON-NLS-1$
                            try {
                                String ret = stateSystem.getAttributeName(currentQuark);
                                return ret;
                            } catch (ArrayIndexOutOfBoundsException e1) {
                                return null;
                            }
                        }
                        try {
                            for(int quark : stateSystem.getSubAttributes(currentQuark, false)) {
                                String quarkName = stateSystem.getAttributeName(quark);
                                if(quarkName.equals(pathElements.get(i))) {
                                    currentQuark = quark;
                                    isPresent = true;
                                    if(i == pathElements.size()-1) {
                                        return quarkName;
                                    }
                                    break;
                                }
                            }
                        } catch (AttributeNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (ArrayIndexOutOfBoundsException e1) {
                            return null;
                        }
                        if(!isPresent) {
                            break;
                        }
                    }
                    else {
                        // Case 2: reaching last element and currenEntry has no children
                        if(i == pathElements.size()-1 && isPresent) {
                            try {
                                String ret = stateSystem.getAttributeName(currentQuark);
                                return ret;
                            } catch (ArrayIndexOutOfBoundsException e1) {
                                return null;
                            }
                        }
                        break;
                    }
                } catch (AttributeNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        // Case 3: No match in all stateSystem
        return null;

    }

    private void buildCurrentPathFromBoxes() {
        Control[] children = pathBuilderComposite.getChildren();
        currentBuildPath = ""; //$NON-NLS-1$
        List<String> pathElements = new ArrayList<>();
        for(int i = 0; i < children.length; i = i + 2) {
            String text = ((Label)children[i]).getText();
            if(!text.equals("")) { //$NON-NLS-1$
                pathElements.add(text);
            }
        }
        int size = pathElements.size();
        for(int j = 0; j < size; j++) {
            currentBuildPath += pathElements.get(j);
            if(j != size - 1) {
                currentBuildPath += File.separator;
            }
        }
    }

    private static List<String> getPathElements(String s) {
        List<String> ret = new ArrayList<>();
        int currentIndex = 0;
        for(int i = 0; i < s.length(); i++) {
            // Case 1: A pointer
            if(s.charAt(i) == '$') {
                while(s.charAt(i) != '}') {
                    if(i < s.length()) {
                        i++;
                    } else {
                        return null;
                    }
                }
                i++;
                ret.add(s.substring(currentIndex, i));
                currentIndex = i+1;
            }
            // Case 2: A simple slash (not IN a pointer)
            else if(s.charAt(i) == '/') {
                ret.add(s.substring(currentIndex, i));
                currentIndex = i+1;
            }
            // Case 3: At the end
            if(i == s.length()-1) {
                ret.add(s.substring(currentIndex, s.length()));
            }
        }
        return ret;
    }

    private static String getFullPathFromElements(List<String> sList) {
        String ret = ""; //$NON-NLS-1$
        for(int i = 0; i < sList.size(); i++) {
            ret += sList.get(i);
            if(i != sList.size()-1) {
                ret += "/"; //$NON-NLS-1$
            }
        }
        return ret;
    }

    private static boolean pathContainsPointer(String path) {
        return path.contains("${"); //$NON-NLS-1$
    }

    private void createNewBox(String value) {
        final Label text = new Label(pathBuilderComposite, SWT.BORDER | SWT.WRAP);
        text.setLayoutData(new GridData(200, 19));
        text.setText(value);
        setLabelDragDrop(text);

        Label separator = new Label(pathBuilderComposite, SWT.NONE);
        separator.setText(File.separator);
        separator.pack();

        pathBuilderComposite.layout(true, true);
    }

    private void addTreeItem(String value) {
        TreeItem newItem = new TreeItem(tree, SWT.NONE);
        newItem.setText(value);
    }

    private static void setLabelDragDrop(final Label obj) {

        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(obj, operations);
        source.setTransfer(types);
        source.addDragListener(new DragSourceListener() {

        @Override
        public void dragStart(DragSourceEvent event) {
            event.doit = (obj.getText().length() != 0);
            if(event.doit) {
                currentDragSource[0] = obj;
            }
        }

        @Override
        public void dragSetData(DragSourceEvent event) {
            event.data = obj.getText();
        }

        @Override
        public void dragFinished(DragSourceEvent event) {
            currentDragSource[0] = null;
        }
        });

        DropTarget target = new DropTarget(obj, operations);
        target.setTransfer(types);
        target.addDropListener(new DropTargetListener() {

            @Override
            public void dropAccept(DropTargetEvent event) {
            }

            @Override
            public void drop(DropTargetEvent event) {
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                  }

                String newText = (String)event.data;
                // Case 1: Label to Label : switch text
                if(currentDragSource[0] instanceof Label) {
                    String tmp = obj.getText();
                    obj.setText(newText);
                    ((Label)currentDragSource[0]).setText(tmp);
                }
                // Case 2: TreeItem to Label(empty) : move text, dispose TreeItem
                if(currentDragSource[0] instanceof TreeItem) {
                    if(obj.getText().equals("")) { //$NON-NLS-1$
                        obj.setText(newText);
                        ((TreeItem)currentDragSource[0]).dispose();
                    }
                    // Case 3: TreeItem to Label(not empty) : switch
                    else {
                        String tmp = obj.getText();
                        obj.setText(newText);
                        ((TreeItem)currentDragSource[0]).setText(tmp);
                    }

                }

            }

            @Override
            public void dragOver(DropTargetEvent event) {}

            @Override
            public void dragOperationChanged(DropTargetEvent event) {}

            @Override
            public void dragLeave(DropTargetEvent event) {}

            @Override
            public void dragEnter(DropTargetEvent event) {}
        });
    }

    private static void setTreeDragDrop(final Tree tree) {
        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(tree, operations);
        source.setTransfer(types);
        source.addDragListener(new DragSourceListener() {

        @Override
        public void dragStart(DragSourceEvent event) {
            TreeItem[] selection = tree.getSelection();
            if (selection.length > 0 && selection[0].getItemCount() == 0) {
                event.doit = true;
                currentDragSource[0] = selection[0];
            } else {
                event.doit = false;
            }
        }

        @Override
        public void dragSetData(DragSourceEvent event) {
            event.data = ((TreeItem)currentDragSource[0]).getText();
        }

        @Override
        public void dragFinished(DragSourceEvent event) {
            currentDragSource[0] = null;
        }
        });

        final DropTarget target = new DropTarget(tree, operations);
        target.setTransfer(types);
        target.addDropListener(new DropTargetListener() {

            @Override
            public void dropAccept(DropTargetEvent event) {
            }

            @Override
            public void drop(DropTargetEvent event) {
                if (event.data == null) {
                    event.detail = DND.DROP_NONE;
                    return;
                  }

                TreeItem obj = ((TreeItem)event.item);
                String newText = (String)event.data;
                // Case 4: Label to TreeItem : move text
                if(currentDragSource[0] instanceof Label) {
                    TreeItem newItem = new TreeItem(tree, SWT.NONE);
                    newItem.setText(newText);
                    ((Label)currentDragSource[0]).setText(""); //$NON-NLS-1$
                }
                // Case 5: TreeItem to TreeItem : switch text
                if(currentDragSource[0] instanceof TreeItem && event.item != null) {
                    String tmp = obj.getText();
                    obj.setText(newText);
                    ((TreeItem)currentDragSource[0]).setText(tmp);
                }
            }

            @Override
            public void dragOver(DropTargetEvent event) {}

            @Override
            public void dragOperationChanged(DropTargetEvent event) {}

            @Override
            public void dragLeave(DropTargetEvent event) {}

            @Override
            public void dragEnter(DropTargetEvent event) {}
        });
    }
}

