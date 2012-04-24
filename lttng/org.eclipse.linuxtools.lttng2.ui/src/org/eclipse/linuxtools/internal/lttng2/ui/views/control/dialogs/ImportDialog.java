/**********************************************************************
 * Copyright (c) 2012 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.internal.lttng2.ui.views.control.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.window.Window;
import org.eclipse.linuxtools.internal.lttng2.ui.Activator;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.Messages;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TraceSessionComponent;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.remote.IRemoteSystemProxy;
import org.eclipse.linuxtools.tmf.core.TmfProjectNature;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.subsystems.files.core.servicesubsystem.IFileServiceSubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * <b><u>ImportDialog</u></b>
 * <p>
 * Dialog box for collecting trace import information.
 * </p>
 */
public class ImportDialog extends Dialog implements IImportDialog {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    /**
     * The icon file for this dialog box.
     */
    public static final String IMPORT_ICON_FILE = "icons/elcl16/import_trace.gif"; //$NON-NLS-1$
    
    public static final String UST_PARENT_DIRECTORY = "ust"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * The dialog composite.
     */
    private Composite fDialogComposite = null;
    /**
     * The checkbox tree viewer for selecting available traces
     */
    private CheckboxTreeViewer fFolderViewer;
    /**
     * The combo box for selecting a project.
     */
    private CCombo fCombo;
    /**
     * The overwrite button 
     */
    private Button fOverwriteButton;
    /**
     * List of available LTTng 2.0 projects
     */
    private List<IProject> fProjects;
    /**
     * The parent where the new node should be added.
     */
    private TraceSessionComponent fSession = null;
    /**
     * List of traces to import  
     */
    private List<ImportFileInfo> fTraces = new ArrayList<ImportFileInfo>();
    /**
     * Selection index in project combo box. 
     */
    private int fProjectIndex;
    /**
     * Flag to indicate that something went wrong when creating the dialog box.
     */
    private boolean fIsError = false;
    
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Constructor
     * @param shell - a shell for the display of the dialog
     */
    public ImportDialog(Shell shell) {
        super(shell);
        setShellStyle(SWT.RESIZE);
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------
    /*
     * (non-Javadoc)
     * @see org.eclipse.linuxtools.internal.lttng2.ui.views.control.dialogs.IImportDialog#getTracePathes()
     */
    @Override
    public List<ImportFileInfo> getTracePathes() {
        List<ImportFileInfo> retList = new ArrayList<ImportFileInfo>();
        retList.addAll(fTraces);
        return retList;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.linuxtools.internal.lttng2.ui.views.control.dialogs.IImportDialog#getProject()
     */
    @Override
    public IProject getProject() {
        return fProjects.get(fProjectIndex);
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.linuxtools.internal.lttng2.ui.views.control.dialogs.IImportDialog#setSession(org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TraceSessionComponent)
     */
    @Override
    public void setSession(TraceSessionComponent session) {
        fSession = session;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.TraceControl_ImportDialogTitle);
        newShell.setImage(Activator.getDefault().loadIcon(IMPORT_ICON_FILE));
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        
        // Main dialog panel
        fDialogComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        fDialogComposite.setLayout(layout);
        fDialogComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group contextGroup = new Group(fDialogComposite, SWT.SHADOW_NONE);
        contextGroup.setText(Messages.TraceControl_ImportDialogTracesGroupName);
        layout = new GridLayout(1, true);
        contextGroup.setLayout(layout);
        contextGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        IRemoteSystemProxy proxy = fSession.getTargetNode().getRemoteSystemProxy();
        
        IFileServiceSubSystem fsss = proxy.getFileServiceSubSystem();

        try {
            IRemoteFile remoteFolder = fsss.getRemoteFileObject(fSession.getSessionPath(), new NullProgressMonitor());

            fFolderViewer = new CheckboxTreeViewer(contextGroup, SWT.BORDER);
            GridData data = new GridData(GridData.FILL_BOTH);
            Tree tree = fFolderViewer.getTree();
            tree.setLayoutData(data);
            tree.setFont(parent.getFont());
            tree.setToolTipText(Messages.TraceControl_ImportDialogTracesTooltip);
            
            fFolderViewer.setContentProvider(new FolderContentProvider());
            fFolderViewer.setLabelProvider(new WorkbenchLabelProvider());
            
            fFolderViewer.addCheckStateListener(new ICheckStateListener() {
                @Override
                public void checkStateChanged(CheckStateChangedEvent event) {
                    Object elem = event.getElement();
                    if (elem instanceof IRemoteFile) {
                        IRemoteFile element = (IRemoteFile) elem;
                        if (!element.isDirectory()) {
                            // A trick to keep selection of a file in sync with the directory
                            boolean p = fFolderViewer.getChecked((element.getParentRemoteFile()));
                            fFolderViewer.setChecked(element, p);
                            return;
                        }
                        fFolderViewer.setSubtreeChecked(event.getElement(), event.getChecked());
                        if (!event.getChecked()) { 
                            fFolderViewer.setChecked(element.getParentRemoteFile(), false);
                        }
                    }
                }
            });
            fFolderViewer.setInput(remoteFolder);

            Group projectGroup = new Group(fDialogComposite, SWT.SHADOW_NONE);
            projectGroup.setText(Messages.TraceControl_ImportDialogProjectsGroupName);
            layout = new GridLayout(1, true);
            projectGroup.setLayout(layout);
            projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            fProjects = new ArrayList<IProject>();
            List<String> projectNames = new ArrayList<String>();
            for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
                try {
                    if (project.isOpen() && project.hasNature(TmfProjectNature.ID)) {
                        fProjects.add(project);
                        projectNames.add(project.getName());
                    }
                } catch (CoreException e) {
                    createErrorComposite(parent, e.fillInStackTrace());
                    return fDialogComposite;
                }
            }

            fCombo = new CCombo(projectGroup, SWT.READ_ONLY);
            fCombo.setToolTipText(Messages.TraceControl_ImportDialogProjectsTooltip);
            fCombo.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
            fCombo.setItems(projectNames.toArray(new String[projectNames.size()]));
            
            Group overrideGroup = new Group(fDialogComposite, SWT.SHADOW_NONE);
            layout = new GridLayout(1, true);
            overrideGroup.setLayout(layout);
            overrideGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            fOverwriteButton = new Button(overrideGroup, SWT.CHECK);
            fOverwriteButton.setText(Messages.TraceControl_ImportDialogOverwriteButtonText);

            getShell().setMinimumSize(new Point(500, 400));

            
        } catch (SystemMessageException e) {
            createErrorComposite(parent, e.fillInStackTrace());
            return fDialogComposite;
        }
        
        return fDialogComposite;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, "&Cancel", true); //$NON-NLS-1$
        createButton(parent, IDialogConstants.OK_ID, "&Ok", true); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        if (!fIsError) {
            // Validate input data
            fTraces.clear();

            fProjectIndex = fCombo.getSelectionIndex();

            if (fProjectIndex < 0) {
                MessageDialog.openError(getShell(),
                        Messages.TraceControl_ImportDialogTitle,
                        Messages.TraceControl_ImportDialogNoProjectSelectedError);
                return;
            }

            IProject project = fProjects.get(fProjectIndex);
            IFolder traceFolder = project.getFolder(TmfTraceFolder.TRACE_FOLDER_NAME);

            if (!traceFolder.exists()) {
                // Invalid LTTng 2.0 project
                MessageDialog.openError(getShell(),
                        Messages.TraceControl_ImportDialogTitle,
                        Messages.TraceControl_ImportDialogInvalidTracingProject + " (" + TmfTraceFolder.TRACE_FOLDER_NAME + ")");  //$NON-NLS-1$//$NON-NLS-2$
                return;
            }

            boolean overwriteAll = fOverwriteButton.getSelection();

            Object[] checked = fFolderViewer.getCheckedElements();
            for (int i = 0; i < checked.length; i++) {
                IRemoteFile file = (IRemoteFile) checked[i];
                
                // Only add actual trace directories  
                if (file.isDirectory() && !UST_PARENT_DIRECTORY.equals(file.getName())) {
                    
                    ImportFileInfo info = new ImportFileInfo(file, file.getName(), overwriteAll);
                    String traceName = info.getLocalTraceName();
                    IFolder folder = traceFolder.getFolder(traceName);

                    // Verify if trace directory already exists (and not overwrite) 
                    if (folder.exists() && !overwriteAll) {

                        // Ask user for overwrite or new name
                        IImportConfirmationDialog conf = TraceControlDialogFactory.getInstance().getImportConfirmationDialog();
                        conf.setTraceName(traceName);

                        // Don't add trace to list if dialog was cancelled.
                        if (conf.open() == Window.OK) {
                            info.setOverwrite(conf.isOverwrite());
                            if (!conf.isOverwrite()) {
                                info.setLocalTraceName(conf.getNewTraceName());
                            }
                            fTraces.add(info);
                        }
                    } else { 
                        fTraces.add(info);
                    }
                }
            }

            if (fTraces.isEmpty()) {
                MessageDialog.openError(getShell(),
                        Messages.TraceControl_ImportDialogTitle,
                        Messages.TraceControl_ImportDialogNoTraceSelectedError);
                return;
            }
        }

        // validation successful -> call super.okPressed()
        super.okPressed();
    }

    // ------------------------------------------------------------------------
    // Helper methods and classes
    // ------------------------------------------------------------------------
    public static class FolderContentProvider extends WorkbenchContentProvider {
        @Override
        public Object[] getChildren(Object o) {
            if (o instanceof IRemoteFile) {
                IRemoteFile element = (IRemoteFile) o;
                // For our purpose, we need folders + files
                if (!element.isDirectory()) {
                    return new Object[0];
                }
            }
            return super.getChildren(o);
        }
    }
    
    /**
     * Creates a dialog composite with an error message which can be used
     * when an exception occurred during creation time of the dialog box.
     * @param parent - a parent composite
     * @param e - a error causing exception 
     */
    private void createErrorComposite(Composite parent, Throwable e) {
        fIsError = true;
        fDialogComposite.dispose();
        
        fDialogComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        fDialogComposite.setLayout(layout);
        fDialogComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Text errorText = new Text(fDialogComposite, SWT.MULTI);
        StringBuffer error = new StringBuffer();
        error.append(Messages.TraceControl_ImportDialogCreationError);
        error.append(System.getProperty("line.separator")); //$NON-NLS-1$
        error.append(System.getProperty("line.separator")); //$NON-NLS-1$
        error.append(e.toString());
        errorText.setText(error.toString());
        errorText.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    
 }