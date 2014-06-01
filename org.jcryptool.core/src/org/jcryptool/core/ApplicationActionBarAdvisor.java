// -----BEGIN DISCLAIMER-----
/*******************************************************************************
 * Copyright (c) 2010, 2014 JCrypTool Team and Contributors
 * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
// -----END DISCLAIMER-----
package org.jcryptool.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;
import org.jcryptool.core.actions.ShowPluginViewHandler;
import org.jcryptool.core.operations.CommandInfo;
import org.jcryptool.core.operations.CommandOrAction;
import org.jcryptool.core.operations.OperationsPlugin;
import org.jcryptool.core.operations.algorithm.ShadowAlgorithmHandler;

/**
 * <p>
 * The ApplicationActionBarAdvisor class configures the action bars of the main JCrypTool window. All cryptographic
 * menus - <b>Algorithms</b>, <b>Analysis</b>, <b>Games</b> and <b>Visuals</b> are hidden in the <b>FlexiProvider</b>
 * perspective.
 * </p>
 * 
 * <p>
 * The <b>Preferences</b> menu entry is hidden on Mac OS X systems since Carbon automatically adds this action to the
 * menu.
 * </p>
 * 
 * <p>
 * Non standard actions that are used in the toolbar/coolbar must have a <code>setToolTipText()</code> property set as
 * well.
 * </p>
 * 
 * @author amro
 * @author Dominik Schadow
 * @author Holger Friedrich (support for Commands)
 * @version 0.9.7
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
    private IWorkbenchAction preferencesAction;
    private IWorkbenchAction saveAllAction;
    private IWorkbenchAction closeAction;
    private IWorkbenchAction exitAction;
    private IWorkbenchAction undoAction;
    private IWorkbenchAction redoAction;
    private IWorkbenchAction findAction;
    private IWorkbenchAction saveAction;
    private IWorkbenchAction saveAsAction;
    private IWorkbenchAction cutAction;
    private IWorkbenchAction copyAction;
    private IWorkbenchAction pasteAction;
    private IWorkbenchAction deleteAction;
    private IWorkbenchAction selectAllAction;
    private IWorkbenchAction printAction;
    private IWorkbenchAction closeAllAction;
    private IWorkbenchAction editActionSetsAction;
    private IWorkbenchAction resetPerspectiveAction;

    /** Perspectives sub menu. */
    private MenuManager perspectiveMenu;
    /** Views sub menu. */
    private MenuManager showViewMenu;
    private IWorkbenchWindow window;
    /** Buffer for the algorithm actions. */
    private static CommandInfo[] algorithmCommands;

    private static final String OS_MAC_OS_X = "macosx"; //$NON-NLS-1$
    private static final String OS = System.getProperty("osgi.os"); //$NON-NLS-1$
    private MenuManager hiddenMenu = new MenuManager("Hidden", "org.jcryptool.core.hidden"); //$NON-NLS-1$ //$NON-NLS-2$

    private static Comparator<String> menuStringsComparator = new Comparator<String>() {
        public int compare(String o1, String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    };

    /**
     * Creates a new action bar advisor to configure a workbench window's action bars via the given action bar
     * configurer.
     * 
     * @param configurer the action bar configurer
     */
    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
    }

    @Override
    protected void fillCoolBar(ICoolBarManager coolBar) {
        // ToolBar File & Additions
        IToolBarManager fileToolBar = new ToolBarManager(coolBar.getStyle());
        fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
        fileToolBar.add(saveAction);
        fileToolBar.add(saveAllAction);
        fileToolBar.add(printAction);
        fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
        fileToolBar.add(new Separator());
        fileToolBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        coolBar.add(new ToolBarContributionItem(fileToolBar, CorePlugin.PLUGIN_ID + ".toolbar")); //$NON-NLS-1$

        // ToolBar Help
        IToolBarManager helpToolBar = new ToolBarManager(coolBar.getStyle());

        coolBar.add(new ToolBarContributionItem(helpToolBar, CorePlugin.PLUGIN_ID + ".helpToolBar")); //$NON-NLS-1$

        // CoolBar Context Menu
        MenuManager coolBarContextMenuManager = new MenuManager(null, CorePlugin.PLUGIN_ID + ".contextMenu"); //$NON-NLS-1$
        coolBar.setContextMenuManager(coolBarContextMenuManager);
        coolBarContextMenuManager.add(getAction(ActionFactory.LOCK_TOOL_BAR.getId()));
        coolBarContextMenuManager.add(getAction(ActionFactory.EDIT_ACTION_SETS.getId()));
        coolBarContextMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    /**
     * Fills the menu bar with the main menus for the window.
     * 
     * @param menuBar the menu manager for the menu bar
     */
    protected void fillMenuBar(IMenuManager menuBar) {
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());

        // algorithm menu will only be created if algorithm actions are
        // available
        if (OperationsPlugin.getDefault().getAlgorithmsManager() != null) {
            menuBar.add(createAlgorithmMenu());
        }
        // analysis menu will only be created if visuals are available
        if (extensionsAvailable("analysis")) { //$NON-NLS-1$
            menuBar.add(createExtensionsMenu(Messages.ApplicationActionBarAdvisor_0, "analysis")); //$NON-NLS-1$
        }
        // visuals menu will only be created if visuals are available
        if (extensionsAvailable("visuals")) { //$NON-NLS-1$
            menuBar.add(createExtensionsMenu(Messages.ApplicationActionBarAdvisor_2, "visuals")); //$NON-NLS-1$
        }
        // games menu will only be created if games are available
        if (extensionsAvailable("games")) { //$NON-NLS-1$
            menuBar.add(createExtensionsMenu(Messages.ApplicationActionBarAdvisor_5, "games")); //$NON-NLS-1$
        }

        menuBar.add(createWindowMenu());

        if (OS_MAC_OS_X.equalsIgnoreCase(OS)) {
            hiddenMenu.setVisible(false);
            menuBar.add(hiddenMenu);
        }
    }

    private IMenuManager createExtensionsMenu(String name, String type) {
        ICommandService commandService = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
        MenuManager menu = new MenuManager(name, CorePlugin.PLUGIN_ID + "." + type); //$NON-NLS-1$

        IConfigurationElement[] elements = Platform.getExtensionRegistry()
                .getExtensionPoint("org.jcryptool.core.operations." + type).getConfigurationElements(); //$NON-NLS-1$

        SortedMap<String, IConfigurationElement> sortedElements = new TreeMap<String, IConfigurationElement>(
                menuStringsComparator);
        for (IConfigurationElement element : elements) {
            sortedElements.put(element.getAttribute("name"), element); //$NON-NLS-1$
        }

        IConfigurationElement element;
        while (!sortedElements.isEmpty()) {
            element = sortedElements.get(sortedElements.firstKey());
            sortedElements.remove(sortedElements.firstKey());
            String commandId = element.getAttribute("viewId");
            String viewName = element.getAttribute("name");
            Command command = commandService.getCommand(commandId);
            IHandler handler = new ShowPluginViewHandler(commandId, viewName); //$NON-NLS-1$ //$NON-NLS-2$
	        command.define(viewName, null, commandService.getCategory(CommandManager.AUTOGENERATED_CATEGORY_ID));
	        command.setHandler(handler);
	        
            String contextHelpId = element.getAttribute("contextHelpId"); //$NON-NLS-1$
            if (contextHelpId != null) {
                commandService.setHelpContextId(handler, contextHelpId);
            }
	        menu.add(createContributionItem(PlatformUI.getWorkbench(), null, commandId));
        }

        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        return menu;
    }

    private static IContributionItem createContributionItem(IServiceLocator locator, Object contribSource, String commandId) {
    	CommandContributionItemParameter contributionItemParameter
    		= new CommandContributionItemParameter(locator, null, commandId, SWT.PUSH);
    	return new CommandContributionItem(contributionItemParameter);
   	}
    
    private boolean extensionsAvailable(String type) {
        IExtensionPoint extensions = Platform.getExtensionRegistry().getExtensionPoint(
                "org.jcryptool.core.operations." + type); //$NON-NLS-1$

        if (extensions != null) {
            return extensions.getConfigurationElements().length == 0 ? false : true;
        } else {
            return false;
        }
    }

    protected void makeActions(IWorkbenchWindow window) {
        this.window = window;

        registerActionsForCommands();

        createFileActions(window);
        createEditActions(window);
        createWindowActions(window);
        createContextActions(window);

        // force initialization of algorithm actions
        OperationsPlugin.getDefault().initAlgorithmsManager();

        // algorithm actions will only be registered if they have been created
        if (OperationsPlugin.getDefault().getAlgorithmsManager() != null) {
            registerAlgorithmActions();
        }
    }

    /**
     * Register all Eclipse Actions that are later used as Commands. Without registration, all command menu entries will
     * be disabled in the menus.
     */
    private void registerActionsForCommands() {

        if (OS_MAC_OS_X.equalsIgnoreCase(OS)) {
            // hide the about action, Mac OS X adds this automatically
            hiddenMenu.add(ActionFactory.ABOUT.create(window));
        }

        register(ActionFactory.HELP_SEARCH.create(window));
        register(ActionFactory.DYNAMIC_HELP.create(window));
        register(ActionFactory.INTRO.create(window));
        register(ActionFactory.ABOUT.create(window));
    }

    private IMenuManager createFileMenu() {
        MenuManager menu = new MenuManager(Messages.applicationActionBarAdvisor_Menu_File,
                IWorkbenchActionConstants.M_FILE);

        menu.add(new MenuManager(Messages.applicationActionBarAdvisor_Menu_New_File, "newfile")); //$NON-NLS-1$
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
        menu.add(new Separator());
        menu.add(closeAction);
        menu.add(closeAllAction);
        menu.add(new Separator());
        menu.add(saveAction);
        menu.add(saveAsAction);
        menu.add(saveAllAction);
        menu.add(new Separator());
        menu.add(printAction);
        menu.add(ContributionItemFactory.REOPEN_EDITORS.create(window));
        menu.add(new GroupMarker(IWorkbenchActionConstants.MRU));
        menu.add(new Separator());
        menu.add(exitAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        return menu;
    }

    private IMenuManager createEditMenu() {
        MenuManager menu = new MenuManager(Messages.applicationActionBarAdvisor_Menu_Edit,
                IWorkbenchActionConstants.M_EDIT);
        menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));

        // undo, redo
        menu.add(undoAction);
        menu.add(redoAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
        menu.add(new Separator());

        // cut, copy, paste
        menu.add(cutAction);
        menu.add(copyAction);
        menu.add(pasteAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.CUT_EXT));
        menu.add(new Separator());

        // delete, select all
        menu.add(deleteAction);
        menu.add(selectAllAction);
        menu.add(new Separator());

        // find
        menu.add(findAction);
        menu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT));
        menu.add(new GroupMarker(IWorkbenchActionConstants.ADD_EXT));

        // provide a uniform location for the "show in" or "open with" actions
        MenuManager showin = new MenuManager(Messages.ApplicationActionBarAdvisor_1, "showin"); //$NON-NLS-1$
        showin.add(new GroupMarker("start")); //$NON-NLS-1$
        menu.add(showin);

        menu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        return menu;
    }

    /**
     * Creates the algorithm menu.
     * 
     * @return the menu manager
     */
    public static IMenuManager createAlgorithmMenu() {
        ICommandService commandService = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
        MenuManager menu = new MenuManager(Messages.applicationActionBarAdvisor_Menu_Algorithms, CorePlugin.PLUGIN_ID
                + ".algorithms"); //$NON-NLS-1$

        // id->compare-relevant-name map
        final Map<String, String> idNameMap = new HashMap<String, String>();

        // id-comparator (compares the names derived from the ids over the map
        // (above)
        Comparator<String> idComparator = new Comparator<String>() {
            public int compare(String id1, String id2) {
                return menuStringsComparator.compare(idNameMap.get(id1), idNameMap.get(id2));
            }
        };

        // put existing id->name pairs
        idNameMap.put("classic", Messages.applicationActionBarAdvisor_Menu_Algorithms_Classic); //$NON-NLS-1$
        idNameMap.put("symmetric", Messages.applicationActionBarAdvisor_Menu_Algorithms_Symmetric); //$NON-NLS-1$
        idNameMap.put("asymmetric", Messages.applicationActionBarAdvisor_Menu_Algorithms_Asymmetric); //$NON-NLS-1$
        idNameMap.put("hybrid", Messages.applicationActionBarAdvisor_Menu_Algorithms_Hybrid); //$NON-NLS-1$
        idNameMap.put("hash", Messages.applicationActionBarAdvisor_Menu_Algorithms_Hash); //$NON-NLS-1$
        idNameMap.put("misc", Messages.applicationActionBarAdvisor_Menu_Algorithms_Misc); //$NON-NLS-1$
        idNameMap.put("mac", Messages.applicationActionBarAdvisor_Menu_Algorithms_Mac); //$NON-NLS-1$
        idNameMap.put("prng", Messages.applicationActionBarAdvisor_Menu_Algorithms_PRNG); //$NON-NLS-1$
        idNameMap.put("signature", Messages.applicationActionBarAdvisor_Menu_Algorithms_Signature); //$NON-NLS-1$
        idNameMap.put("xml", Messages.applicationActionBarAdvisor_Menu_Algorithms_XML_Security); //$NON-NLS-1$

        // Sorted submenus map
        SortedMap<String, SortedMap<String, CommandInfo>> sortedSubMenus
        	= new TreeMap<String, SortedMap<String, CommandInfo>>(idComparator);

        // fill sub menu sorting map
        for (CommandInfo algorithmCommand : algorithmCommands) {
            String type = OperationsPlugin.getDefault().getAlgorithmsManager().getAlgorithmType(algorithmCommand);

            if (!sortedSubMenus.containsKey(type)) {
                // create a generic submenu (displayed name and menu id =
                // algorithm type) when id has no textual mapping
                if (!idNameMap.containsKey(type)) {
                    idNameMap.put(type, type);
                }

                // make a new submenu
                sortedSubMenus.put(type, new TreeMap<String, CommandInfo>(menuStringsComparator));
            }

// #ifdef _DEBUG
			if(algorithmCommand.getText() == null)
			{
				System.err.println("text is null for algorithmCommand " + algorithmCommand.getCommandId());
				algorithmCommand.setText("?");
			}
// #endif
           	sortedSubMenus.get(type).put(algorithmCommand.getText(), algorithmCommand);
        }

        for (String subMenuKey : sortedSubMenus.keySet()) {
            MenuManager subMenu = new MenuManager(idNameMap.get(subMenuKey), subMenuKey);
            menu.add(subMenu);

            Map<String, CommandInfo> subMenuActions = sortedSubMenus.get(subMenuKey);
            for (String subMenuActionKey : subMenuActions.keySet()) {
            	CommandInfo commandInfo = subMenuActions.get(subMenuActionKey);
       	        Command cmd = commandService.getCommand(commandInfo.getCommandId());
       	        cmd.define(commandInfo.getText(), null, commandService.getCategory(CommandManager.AUTOGENERATED_CATEGORY_ID));
       	        cmd.setHandler(commandInfo.getHandler());
        	        
       	        subMenu.add(createContributionItem(PlatformUI.getWorkbench(), null, commandInfo.getCommandId()));
            }
        }

        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        return menu;
    }

    private IMenuManager createWindowMenu() {
        MenuManager menu = new MenuManager(Messages.applicationActionBarAdvisor_Menu_Window,
                IWorkbenchActionConstants.M_WINDOW);

        // create actions for "open perspective" menu
        perspectiveMenu = new MenuManager(Messages.applicationActionBarAdvisor_Menu_Open_Perspective, "openPerspective"); //$NON-NLS-1$
        perspectiveMenu.add(ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window));

        // create actions for "open view" menu
        showViewMenu = new MenuManager(Messages.applicationActionBarAdvisor_Menu_Show_View, "showView"); //$NON-NLS-1$
        showViewMenu.add(ContributionItemFactory.VIEWS_SHORTLIST.create(window));

        menu.add(perspectiveMenu);
        menu.add(showViewMenu);
        menu.add(new Separator());
        menu.add(editActionSetsAction);
        menu.add(resetPerspectiveAction);

        if (OS_MAC_OS_X.equalsIgnoreCase(OS)) {
            // hide the preferences action, Mac OS X adds this automatically
            hiddenMenu.add(preferencesAction);
        } else {
            menu.add(new Separator());
            menu.add(preferencesAction);
        }

        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        return menu;
    }

    /* TODO:  Are those the kind of Action we no longer want to support? */
    private void createFileActions(IWorkbenchWindow window) {
        saveAction = ActionFactory.SAVE.create(window);
        register(saveAction);

        saveAsAction = ActionFactory.SAVE_AS.create(window);
        register(saveAsAction);

        saveAllAction = ActionFactory.SAVE_ALL.create(window);
        register(saveAllAction);

        closeAction = ActionFactory.CLOSE.create(window);
        register(closeAction);

        closeAllAction = ActionFactory.CLOSE_ALL.create(window);
        register(closeAllAction);

        printAction = ActionFactory.PRINT.create(window);
        register(printAction);

        exitAction = ActionFactory.QUIT.create(window);
        exitAction.setAccelerator(SWT.ALT | SWT.F4);
        register(exitAction);
    }

    private void createEditActions(IWorkbenchWindow window) {
        undoAction = ActionFactory.UNDO.create(window);
        register(undoAction);

        redoAction = ActionFactory.REDO.create(window);
        register(redoAction);

        cutAction = ActionFactory.CUT.create(window);
        register(cutAction);

        copyAction = ActionFactory.COPY.create(window);
        register(copyAction);

        pasteAction = ActionFactory.PASTE.create(window);
        register(pasteAction);

        deleteAction = ActionFactory.DELETE.create(window);
        register(deleteAction);

        selectAllAction = ActionFactory.SELECT_ALL.create(window);
        register(selectAllAction);

        findAction = ActionFactory.FIND.create(window);
        register(findAction);
    }

    private void createWindowActions(IWorkbenchWindow window) {
        editActionSetsAction = ActionFactory.EDIT_ACTION_SETS.create(window);
        register(editActionSetsAction);

        resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
        register(resetPerspectiveAction);

        preferencesAction = ActionFactory.PREFERENCES.create(window);
        register(preferencesAction);
    }

    private void createContextActions(IWorkbenchWindow window) {
        register(ActionFactory.LOCK_TOOL_BAR.create(window));
        register(ActionFactory.EDIT_ACTION_SETS.create(window));
    }

    /**
     * This used to creates/register the algorithm actions. The plug-in activator is used to retrieve the algorithms manager.
     * Further via the algorithms manager the AbstractAlgorithmsAction objects are retrieved.
     * With the removal of support for Actions, this should be obsolete.
     * 
     */
    private void registerAlgorithmActions() {
        algorithmCommands = OperationsPlugin.getDefault().getAlgorithmsManager().getShadowAlgorithmCommands();
    }

    /**
     * Translates the type into the local language used to provide unique translations for other plug-ins (e.g.
     * FileExplorer).
     * 
     * @param type the type provided from a plug-in
     * @return the translated type
     */
    public static String getTypeTranslation(String type) {
        if (type.equals("classic")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Classic;
        } else if (type.equals("symmetric")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Symmetric;
        } else if (type.equals("asymmetric")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Asymmetric;
        } else if (type.equals("hybrid")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Hybrid;
        } else if (type.equals("hash")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Hash;
        } else if (type.equals("misc")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Misc;
        } else if (type.equals("signature")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Signature;
        } else if (type.equals("mac")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_Mac;
        } else if (type.equals("prng")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_PRNG;
        } else if (type.equals("xml")) { //$NON-NLS-1$
            return Messages.applicationActionBarAdvisor_Menu_Algorithms_XML_Security;
        }
        return type;
    }
}
