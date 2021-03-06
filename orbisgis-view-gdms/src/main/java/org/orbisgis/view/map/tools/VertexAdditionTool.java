/**
 * OrbisGIS is a java GIS application dedicated to research in GIScience.
 * OrbisGIS is developed by the GIS group of the DECIDE team of the 
 * Lab-STICC CNRS laboratory, see <http://www.lab-sticc.fr/>.
 *
 * The GIS group of the DECIDE team is located at :
 *
 * Laboratoire Lab-STICC – CNRS UMR 6285
 * Equipe DECIDE
 * UNIVERSITÉ DE BRETAGNE-SUD
 * Institut Universitaire de Technologie de Vannes
 * 8, Rue Montaigne - BP 561 56017 Vannes Cedex
 * 
 * OrbisGIS is distributed under GPL 3 license.
 *
 * Copyright (C) 2007-2014 CNRS (IRSTV FR CNRS 2488)
 * Copyright (C) 2015-2017 CNRS (Lab-STICC UMR CNRS 6285)
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
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.view.map.tools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.awt.Graphics;
import java.util.Observable;
import org.gdms.data.DataSource;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.driver.DriverException;
import org.gdms.geometryUtils.GeometryEdit;
import org.gdms.geometryUtils.GeometryException;
import org.orbisgis.coremap.layerModel.ILayer;
import org.orbisgis.coremap.layerModel.MapContext;
import org.orbisgis.view.icons.OrbisGISIcon;
import org.orbisgis.view.map.tool.DrawingException;
import org.orbisgis.view.map.tool.FinishedAutomatonException;
import org.orbisgis.view.map.tool.ToolManager;
import org.orbisgis.view.map.tool.TransitionException;
import org.orbisgis.view.map.tools.generated.VertexAddition;

import javax.swing.*;

/**
 * Insert a vertex into a linestring
 */
public class VertexAdditionTool extends VertexAddition {

        @Override
        public void update(Observable o, Object arg) {
                //PlugInContext.checkTool(this);
        }

        @Override
        public void transitionTo_Standby(MapContext vc, ToolManager tm)
                throws FinishedAutomatonException, TransitionException {
        }

        @Override
        public void transitionTo_Done(MapContext mc, ToolManager tm)
                throws FinishedAutomatonException, TransitionException {
                Point p = tm.getToolsFactory().createPoint(new Coordinate(tm.getValues()[0], tm.getValues()[1]));
                try {
                        ILayer activeLayer = mc.getActiveLayer();
                        DataSource sds = activeLayer.getDataSource();
                        for(Integer geomIndex : activeLayer.getSelection()) {
                                Geometry g = GeometryEdit.insertVertex(sds.getGeometry(geomIndex), p, tm.getTolerance());
                                if (g != null) {
                                        sds.setGeometry(geomIndex, g);
                                        break;
                                }
                        }
                } catch (GeometryException e) {
                        throw new TransitionException(e);
                } catch (DriverException e) {
                        throw new TransitionException(e);
                }

                transition(Code.INIT);
        }

        @Override
        public void transitionTo_Cancel(MapContext vc, ToolManager tm)
                throws FinishedAutomatonException, TransitionException {
        }

        @Override
        public void drawIn_Standby(Graphics g, MapContext mc, ToolManager tm)
                throws DrawingException {
                Point p = tm.getToolsFactory().createPoint(new Coordinate(tm.getLastRealMousePosition().getX(), tm.getLastRealMousePosition().getY()));
                try {
                        ILayer activeLayer = mc.getActiveLayer();
                        DataSource sds = activeLayer.getDataSource();
                        for(Integer geomIndex : activeLayer.getSelection()) {
                                Geometry geom = GeometryEdit.insertVertex(sds.getGeometry(geomIndex), p, tm.getTolerance());
                                if (geom != null) {
                                        tm.addGeomToDraw(geom);
                                }
                        }
                } catch (GeometryException e) {
                        throw new DrawingException(e);
                } catch (DriverException e) {
                        throw new DrawingException(e);
                }
        }

        @Override
        public void drawIn_Done(Graphics g, MapContext vc, ToolManager tm)
                throws DrawingException {
        }

        @Override
        public void drawIn_Cancel(Graphics g, MapContext vc, ToolManager tm)
                throws DrawingException {
        }

        @Override
        public boolean isEnabled(MapContext vc, ToolManager tm) {
                return ToolUtilities.activeSelectionGreaterThan(vc, 0)
                        && ToolUtilities.isActiveLayerEditable(vc) && ToolUtilities.isSelectionGreaterOrEqualsThan(vc, 1)
                        && !ToolUtilities.geometryTypeIs(vc, TypeFactory.createType(Type.POINT));
        }

        @Override
        public boolean isVisible(MapContext vc, ToolManager tm) {
                return isEnabled(vc, tm);
        }

        @Override
        public String getName() {
                return i18n.tr("Add a new vertex");
        }
        @Override
        public ImageIcon getImageIcon() {
            return OrbisGISIcon.getIcon("edition/vertexaddition");
        }
}
