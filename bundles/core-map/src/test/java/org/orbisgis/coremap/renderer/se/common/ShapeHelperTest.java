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
package org.orbisgis.coremap.renderer.se.common;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D.Double;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Maxence Laurent
 */
public class ShapeHelperTest {


	/**
	 * Test of splitLine method, of class ShapeHelper.
	 */ 
        @Test
        public void testSplitLine_Shape_double() {
		System.out.println("splitLine");

		Path2D.Double line = new Path2D.Double() {};
		line.moveTo(0,0);
		line.lineTo(10, 10);
		line.lineTo(0, 20);
		line.lineTo(10, 30);
		line.lineTo(0, 40);

		double coords[] = new double[6];
		List<Shape> result1 = ShapeHelper.splitLine(line, 28.28);


		for (Shape shp : result1){
			PathIterator it = shp.getPathIterator(null);
			System.out.println ("Shape: ");

			while (!it.isDone()){
				it.currentSegment(coords);
				System.out.println ("(" + coords[0] + ";" + " " + coords[1] + ")");
				it.next();
			}
		};

		result1 = ShapeHelper.splitLine(line, 35.0);

		for (Shape shp : result1){
			PathIterator it = shp.getPathIterator(null);
			System.out.println ("Shape: ");

			while (!it.isDone()){
				it.currentSegment(coords);
				System.out.println ("(" + coords[0] + ";" + " " + coords[1] + ")");
				it.next();
			}
		}


		result1 = ShapeHelper.splitLine(line, 70.0);

		for (Shape shp : result1){
			PathIterator it = shp.getPathIterator(null);
			System.out.println ("Shape: ");

			while (!it.isDone()){
				it.currentSegment(coords);
				System.out.println ("(" + coords[0] + ";" + " " + coords[1] + ")");
				it.next();
			}
		}
	}

         @Test
	 public void testGetPointAt() {
         Path2D.Double path = new Path2D.Double();
         path.moveTo(10, 10);

         path.lineTo(20, 10);
         path.lineTo(20, 20);


         Double pointAt = ShapeHelper.getPointAt(path, 30);

         System.out.println ("PT: " + pointAt.getX() + ";" + pointAt.getY());
         assertEquals(pointAt.getX(), 20.0, 0.00001);
         assertEquals(pointAt.getY(), 30.0, 0.00001);


         path = new Path2D.Double();




     }
}
