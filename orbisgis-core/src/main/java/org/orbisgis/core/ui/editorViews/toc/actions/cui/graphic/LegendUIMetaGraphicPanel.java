/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able to
 * manipulate and create vector and raster spatial information. OrbisGIS is
 * distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 *
 *
 *  Team leader Erwan BOCHER, scientific researcher,
 *
 *  User support leader : Gwendall Petit, geomatic engineer.
 *
 *
 * Copyright (C) 2007 Erwan BOCHER, Fernando GONZALEZ CORTES, Thomas LEDUC
 *
 * Copyright (C) 2010 Erwan BOCHER, Pierre-Yves FADET, Alexis GUEGANNO, Maxence LAURENT
 *
 * This file is part of OrbisGIS.
 *
 * OrbisGIS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OrbisGIS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OrbisGIS. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 *
 * or contact directly:
 * erwan.bocher _at_ ec-nantes.fr
 * gwendall.petit _at_ ec-nantes.fr
 */



package org.orbisgis.core.ui.editorViews.toc.actions.cui.graphic;

import org.orbisgis.core.ui.editorViews.toc.actions.cui.type.LegendUIMarkGraphicType;
import javax.swing.Icon;
import org.orbisgis.core.images.OrbisGISIcon;
import org.orbisgis.core.renderer.se.graphic.Graphic;
import org.orbisgis.core.renderer.se.graphic.MarkGraphic;
import org.orbisgis.core.ui.editorViews.toc.actions.cui.LegendUIAbstractMetaPanel;
import org.orbisgis.core.ui.editorViews.toc.actions.cui.LegendUIComponent;
import org.orbisgis.core.ui.editorViews.toc.actions.cui.LegendUIController;
import org.orbisgis.core.ui.editorViews.toc.actions.cui.LegendUIEmptyPanel;
import org.orbisgis.core.ui.editorViews.toc.actions.cui.type.LegendUIEmptyPanelType;
import org.orbisgis.core.ui.editorViews.toc.actions.cui.type.LegendUIType;

/**
 *
 * @author maxence
 */
public abstract class LegendUIMetaGraphicPanel extends LegendUIAbstractMetaPanel {

	private Graphic graphic;

	private LegendUIType initialType;
	private LegendUIComponent initialPanel;
	private LegendUIType[] types;

	public LegendUIMetaGraphicPanel(String name, LegendUIController controller, LegendUIComponent parent, Graphic g) {
		super(name, controller, parent, 0);

		this.graphic = g;

		types = new LegendUIType[2];

		types[0] = new LegendUIEmptyPanelType("no " + name, controller);
		types[1] = new LegendUIMarkGraphicType("Mark " + name, controller);

		if (g instanceof MarkGraphic){
			System.out.println ("Graphic is :" + g);
			initialType = types[1];
			initialPanel = new LegendUIMarkGraphicPanel(controller, this, (MarkGraphic) g);
		}else{
			initialType = types[0];
			initialPanel = new LegendUIEmptyPanel("not yet implemented (" + name  + ")", controller, this);
		}
	}

	@Override
	public void init(){
		init(types, initialType, initialPanel);
	}

	@Override
	protected void switchTo(LegendUIType type, LegendUIComponent comp) {
		this.graphic = ((LegendUIGraphicComponent)comp).getGraphic();
		this.graphicChanged(graphic);
	}

	@Override
	public Icon getIcon() {
		return OrbisGISIcon.PALETTE;
	}

	public abstract void graphicChanged(Graphic newGraphic);




}