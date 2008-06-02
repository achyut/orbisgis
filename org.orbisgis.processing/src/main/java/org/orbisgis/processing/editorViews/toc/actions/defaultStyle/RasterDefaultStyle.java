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
package org.orbisgis.processing.editorViews.toc.actions.defaultStyle;

import ij.ImagePlus;

import java.awt.image.ColorModel;
import java.io.IOException;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.orbisgis.Services;
import org.orbisgis.layerModel.ILayer;
import org.orbisgis.layerModel.MapContext;
import org.orbisgis.renderer.legend.RasterLegend;
import org.sif.UIFactory;

public class RasterDefaultStyle implements
		org.orbisgis.editorViews.toc.action.ILayerAction {

	public boolean accepts(ILayer layer) {
		try {
			if (layer.isRaster()) {
				SpatialDataSourceDecorator ds = layer.getDataSource();
				if (ds.getRaster(0).getType() != ImagePlus.COLOR_RGB) {
					return true;
				}
			}
		} catch (IOException e) {
		} catch (DriverException e) {
		}
		return false;
	}

	public boolean acceptsSelectionCount(int selectionCount) {
		return 1 == selectionCount;
	}

	public void execute(final MapContext viewContext, final ILayer layer) {

		try {
			RasterLegend legend = (RasterLegend) layer.getLegend()[0];
			final RasterDefaultStyleUIPanel rasterDefaultStyleUIClass = new RasterDefaultStyleUIPanel(
					legend, layer.getRaster().getDefaultColorModel());

			if (UIFactory.showDialog(rasterDefaultStyleUIClass)) {
				ColorModel colorModel = rasterDefaultStyleUIClass
						.getColorModel();
				float opacity = rasterDefaultStyleUIClass.getOpacity();
				if (colorModel == null) {
					colorModel = legend.getColorModel();
				}
				RasterLegend newLegend = new RasterLegend(colorModel, opacity);
				layer.setLegend(newLegend);
			}

		} catch (DriverException e) {
			Services.getErrorManager().error("Cannot get the legend", e);
		} catch (IOException e) {
			Services.getErrorManager().error("Cannot get the default style", e);

			Services.getErrorManager().error("Cannot get the default style", e);
		}
	}
}