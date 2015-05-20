// -----BEGIN DISCLAIMER-----
/*******************************************************************************
 * Copyright (c) 2008 JCrypTool Team and Contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
// -----END DISCLAIMER-----
package org.jcryptool.core;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * The ApplicationWorkbenchWindowAdvisor class configures the workbench window.
 *
 * @author Dominik Schadow
 * @version 0.9.5
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    /**
     * Creates a new workbench window advisor for configuring a workbench window via the given workbench window
     * configurer.
     *
     * @param configurer an object for configuring the workbench window
     */
    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    /**
     * Creates a new action bar advisor to configure the action bars of the window via the given action bar configurer.
     *
     * @param configurer the action bar configurer for the window
     * @return the action bar advisor for the window
     */
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }

    @Override
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(950, 700));
        configurer.setShowCoolBar(true);
        configurer.setShowFastViewBars(true);
        configurer.setShowMenuBar(true);
        configurer.setShowPerspectiveBar(true);
        configurer.setShowProgressIndicator(true);
        configurer.setShowStatusLine(true);

    	System.out.println("about to configure drag & drop in preWindowOpen()...");
    	configurer.addEditorAreaTransfer(FileTransfer.getInstance());
    	configurer.configureEditorAreaDropListener(new EditorAreaDropTargetListener());
        
        PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.PERSPECTIVE_BAR_EXTRAS, Perspective.PERSPECTIVE_ID
                + ", org.jcryptool.crypto.flexiprovider.ui.perspective.FlexiProviderPerspective"); //$NON-NLS-1$
    }

    @Override
    public void postWindowOpen() {
        TrayDialog.setDialogHelpAvailable(true);

        super.postWindowOpen();
    }
}
