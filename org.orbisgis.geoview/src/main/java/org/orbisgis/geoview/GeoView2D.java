/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at french IRSTV institute and is able
 * to manipulate and create vectorial and raster spatial information. OrbisGIS
 * is distributed under GPL 3 license. It is produced  by the geomatic team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/>, CNRS FR 2488:
 *    Erwan BOCHER, scientific researcher,
 *    Thomas LEDUC, scientific researcher,
 *    Fernando GONZALEZ CORTES, computer engineer.
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OrbisGIS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult:
 *    <http://orbisgis.cerma.archi.fr/>
 *    <http://sourcesup.cru.fr/projects/orbisgis/>
 *    <http://listes.cru.fr/sympa/info/orbisgis-developers/>
 *    <http://listes.cru.fr/sympa/info/orbisgis-users/>
 *
 * or contact directly:
 *    erwan.bocher _at_ ec-nantes.fr
 *    fergonco _at_ gmail.com
 *    thomas.leduc _at_ cerma.archi.fr
 */
package org.orbisgis.geoview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import net.infonode.docking.RootWindow;
import net.infonode.docking.View;
import net.infonode.docking.ViewSerializer;

import org.orbisgis.core.actions.ActionControlsRegistry;
import org.orbisgis.core.actions.EPActionHelper;
import org.orbisgis.core.actions.IAction;
import org.orbisgis.core.actions.IActionFactory;
import org.orbisgis.core.actions.ISelectableAction;
import org.orbisgis.core.actions.JActionMenuBar;
import org.orbisgis.core.actions.JActionToolBar;
import org.orbisgis.core.actions.MenuTree;
import org.orbisgis.core.actions.ToolBarArray;
import org.orbisgis.core.persistence.PersistenceException;
import org.orbisgis.core.windows.IWindow;
import org.orbisgis.core.windows.PersistenceContext;
import org.orbisgis.geocatalog.EPGeocatalogActionHelper;
import org.orbisgis.geocatalog.tools.about.HtmlViewer;
import org.orbisgis.geoview.images.IconLoader;
import org.orbisgis.pluginManager.PluginManager;
import org.orbisgis.pluginManager.SystemAdapter;
import org.orbisgis.tools.Automaton;
import org.orbisgis.tools.TransitionException;
import org.orbisgis.tools.ViewContext;

public class GeoView2D extends JFrame implements IWindow {

	private MapControl map;

	private Automaton defaultTool;

	private ViewContext viewContext;

	private JActionMenuBar menuBar;

	private JActionToolBar mainToolBar;

	private ArrayList<ViewDecorator> views = new ArrayList<ViewDecorator>();

	private RootWindow root;

	private ViewSerializer viewSerializer = new GeoviewSerializer();

	private Component welcomeComponent;

	public GeoView2D() {
		// Init mapcontrol and fixed ui components
		mainToolBar = new JActionToolBar("OrbisGIS");
		menuBar = new JActionMenuBar();
		this.setLayout(new BorderLayout());
		this.getContentPane().add(mainToolBar, BorderLayout.PAGE_START);
		viewContext = new GeoViewContext(this);

		// Initialize views
		root = new RootWindow(viewSerializer);
		root.getRootWindowProperties().getSplitWindowProperties()
				.setContinuousLayoutEnabled(false);

		welcomeComponent = new HtmlViewer(getClass()
				.getResource("welcome.html"));
		View welcome = new View("OrbisGIS", null, welcomeComponent);
		root.setWindow(welcome);
		this.getContentPane().add(root, BorderLayout.CENTER);

		this.setJMenuBar(menuBar);
		MenuTree menuTree = new MenuTree();
		ToolBarArray toolBarArray = new ToolBarArray();
		EPActionHelper.configureParentMenusAndToolBars(new String[] {
				"org.orbisgis.geoview.Action", "org.orbisgis.geoview.Tool" },
				menuTree, toolBarArray);
		IActionFactory actionFactory = new GeoviewActionFactory();
		IActionFactory toolFactory = new GeoviewToolFactory();
		EPActionHelper.configureMenuAndToolBar("org.orbisgis.geoview.Action",
				"action", actionFactory, menuTree, toolBarArray);
		EPActionHelper.configureMenuAndToolBar("org.orbisgis.geoview.Tool",
				"tool", toolFactory, menuTree, toolBarArray);
		views = EPViewHelper.getViewsInfo(this);
		initializeViews();

		// Initialize actions
		EPViewHelper
				.addViewMenu(menuTree, root, new ViewActionFactory(), views);
		JComponent[] menus = menuTree.getJMenus();
		for (int i = 0; i < menus.length; i++) {
			menuBar.add(menus[i]);
		}
		for (JToolBar toolbar : toolBarArray.getToolBars()) {
			mainToolBar.add(toolbar);
		}
		this.setTitle("OrbisGIS :: G e o V i e w 2D");
		this.setIconImage(IconLoader.getIcon("mini_orbisgis.png").getImage());
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		this.setSize((int) (screenSize.width / 1.5),
				(int) (screenSize.height / 1.5));
		this.setLocationRelativeTo(null);

		// TODO remove when the window management is implemented
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				EPGeocatalogActionHelper.executeAction(null,
						"org.orbisgis.geocatalog.Exit");
			}

		});

		PluginManager.addSystemListener(new SystemAdapter() {

			@Override
			public void statusChanged() {
				try {
					viewContext.getToolManager().checkToolStatus();
				} catch (TransitionException e) {
				}
			}

		});

	}

	private void initializeViews() {
		for (ViewDecorator view : views) {
			view.getView().initialize(this);
		}
	}

	public ViewContext getViewContext() {
		return viewContext;
	}

	public MapControl getMap() {
		if (map == null) {
			map = (MapControl) getView("org.orbisgis.geoview.MapControl");
		}
		return map;
	}

	public void showWindow() {
		this.setVisible(true);
	}

	public Component getView(String viewId) {
		ViewDecorator ret = getViewDecorator(viewId);
		if (ret != null) {
			if (!ret.isOpen()) {
				ret.open(root);
			}
			return ret.getViewComponent();
		} else {
			return null;
		}
	}

	public void showView(String id) {
		ViewDecorator view = getViewDecorator(id);
		if (view != null) {
			view.open(root);
		}
	}

	public void hideView(String id) {
		ViewDecorator view = getViewDecorator(id);
		if (view != null) {
			view.close();
		}
	}

	private ViewDecorator getViewDecorator(String id) {
		for (ViewDecorator view : views) {
			if (view.getId().equals(id)) {
				return view;
			}
		}

		return null;
	}

	public void enableControls() {
		ActionControlsRegistry.refresh();
	}

	private final class GeoviewToolFactory implements IActionFactory {

		public IAction getAction(Object action,
				HashMap<String, String> attributes) {
			return new IGeoviewToolDecorator(action, attributes
					.get("mouse-cursor"));
		}

		public ISelectableAction getSelectableAction(Object action,
				HashMap<String, String> attributes) {
			return new IGeoviewToolDecorator(action, attributes
					.get("mouse-cursor"));
		}
	}

	private final class IGeoviewToolDecorator implements IAction,
			ISelectableAction {

		private Automaton action;

		public IGeoviewToolDecorator(Object action, String mouseCursor) {
			this.action = (Automaton) action;
			this.action.setMouseCursor(mouseCursor);
			if (defaultTool == null) {
				defaultTool = this.action;
			}
		}

		public boolean isVisible() {
			return action.isVisible(viewContext, viewContext.getToolManager());
		}

		public boolean isEnabled() {
			return action.isEnabled(viewContext, viewContext.getToolManager());
		}

		public void actionPerformed() {
			try {
				getMap().setTool(action);
			} catch (TransitionException e) {
				PluginManager.error("Cannot use tool", e);
			}
		}

		public boolean isSelected() {
			return viewContext.getToolManager().getTool().getClass().equals(
					action.getClass());
		}
	}

	private final class ViewActionFactory implements IActionFactory {

		public IAction getAction(Object action,
				HashMap<String, String> attributes) {
			throw new RuntimeException("bug");
		}

		public ISelectableAction getSelectableAction(Object action,
				HashMap<String, String> attributes) {
			return new ViewSelectableAction((String) action);
		}
	}

	private final class ViewSelectableAction implements IAction,
			ISelectableAction {

		private String id;
		private ViewDecorator viewDecorator;

		public ViewSelectableAction(String id) {
			this.id = id;
		}

		public void actionPerformed() {
			if (getViewDecorator().isOpen()) {
				getViewDecorator().close();
			} else {
				getViewDecorator().open(root);
			}
		}

		public boolean isEnabled() {
			return true;
		}

		public boolean isVisible() {
			return true;
		}

		public boolean isSelected() {
			return getViewDecorator().isOpen();
		}

		private ViewDecorator getViewDecorator() {
			if (viewDecorator == null) {
				for (ViewDecorator view : views) {
					if (view.getId().equals(id)) {
						viewDecorator = view;
						break;
					}
				}
			}

			return viewDecorator;
		}
	}

	private final class GeoviewActionFactory implements IActionFactory {

		public IAction getAction(Object action,
				HashMap<String, String> attributes) {
			return new IGeoviewActionDecorator(action);
		}

		public ISelectableAction getSelectableAction(Object action,
				HashMap<String, String> attributes) {
			return new IGeoviewActionDecorator(action);
		}
	}

	private final class IGeoviewActionDecorator implements IAction,
			ISelectableAction {

		private IGeoviewAction action;

		public IGeoviewActionDecorator(Object action) {
			this.action = (IGeoviewAction) action;
		}

		public boolean isVisible() {
			return action.isVisible(GeoView2D.this);
		}

		public boolean isEnabled() {
			return action.isEnabled(GeoView2D.this);
		}

		public void actionPerformed() {
			action.actionPerformed(GeoView2D.this);
		}

		public boolean isSelected() {
			return ((IGeoviewSelectableAction) action)
					.isSelected(GeoView2D.this);
		}
	}

	public Rectangle getPosition() {
		return this.getBounds();
	}

	public boolean isOpened() {
		return this.isVisible();
	}

	public void load(PersistenceContext pc) throws PersistenceException {
		// we override the default layout
		this.getContentPane().remove(root);
		root = new RootWindow(viewSerializer);
		this.getContentPane().add(root, BorderLayout.CENTER);

		try {
			FileInputStream fis = new FileInputStream(pc.getFile("layout"));
			ObjectInputStream ois = new ObjectInputStream(fis);
			root.read(ois);
			ois.close();
		} catch (Exception e) {
			PluginManager.error("Cannot recover the layout of the window", e);
		}
		viewContext.loadStatus(pc.getFile("viewContext"));
	}

	public void save(PersistenceContext pc) throws PersistenceException {
		try {
			FileOutputStream fos = new FileOutputStream(pc.getFile("layout"));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			root.write(oos);
			oos.close();

			viewContext.saveStatus(pc.getFile("viewContext", "viewContext",
					".xml"));
		} catch (IOException e) {
			throw new PersistenceException(e);
		}
	}

	public void setPosition(Rectangle position) {
		this.setBounds(position);
	}

	/**
	 * Writes the id of the view and then writes the status. Reads the id,
	 * obtains the data from the extension xml and reads the status
	 *
	 * @author Fernando Gonzalez Cortes
	 */
	private class GeoviewSerializer implements ViewSerializer {

		public View readView(ObjectInputStream ois) throws IOException {
			String id = ois.readUTF();
			if (id.equals("welcome")) {
				return new View("OrbisGIS", null, welcomeComponent);
			} else {
				ViewDecorator vd = GeoView2D.this.getViewDecorator(id);
				if (vd != null) {
					try {
						vd.loadStatus(ois);
						return vd.getDockingView();
					} catch (Throwable t) {
						PluginManager.error("Cannot recover view " + id, t);
					}
				}
			}

			return null;
		}

		public void writeView(View view, ObjectOutputStream oos)
				throws IOException {
			ViewDecorator vd = getViewDecorator(view);
			if (vd != null) {
				oos.writeUTF(vd.getId());
				vd.getView().saveStatus(oos);
			} else if (view.getComponent() == welcomeComponent) {
				oos.writeUTF("welcome");
			}
		}

		private ViewDecorator getViewDecorator(View view) {
			for (ViewDecorator viewDecorator : views) {
				if (viewDecorator.getDockingView() == view) {
					return viewDecorator;
				}
			}

			return null;
		}

	}

	public void delete() {
		this.setVisible(false);
		this.dispose();
		for (ViewDecorator vd : views) {
			if (vd.getViewComponent() != null) {
				vd.getView().delete();
			}
		}
	}

	public Automaton getDefaultTool() {
		return defaultTool;
	}
}