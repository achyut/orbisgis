/*
 * OrbisGIS is a GIS application dedicated to scientific spatial simulation.
 * This cross-platform GIS is developed at French IRSTV institute and is able
 * to manipulate and create vector and raster spatial information. OrbisGIS
 * is distributed under GPL 3 license. It is produced  by the geo-informatic team of
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
 *
 * or contact directly:
 *    erwan.bocher _at_ ec-nantes.fr
 *    fergonco _at_ gmail.com
 *    thomas.leduc _at_ cerma.archi.fr
 */
package org.gdms.sql.function.spatial.geometryProperties;

import org.gdms.data.values.Value;
import org.gdms.sql.FunctionTest;
import org.gdms.sql.function.spatial.geometry.properties.ST_Area;
import org.gdms.sql.function.spatial.geometry.properties.ST_Dimension;
import org.gdms.sql.function.spatial.geometry.properties.ST_GeometryN;
import org.gdms.sql.function.spatial.geometry.properties.ST_GeometryType;
import org.gdms.sql.function.spatial.geometry.properties.ST_IsEmpty;
import org.gdms.sql.function.spatial.geometry.properties.ST_IsSimple;
import org.gdms.sql.function.spatial.geometry.properties.ST_IsValid;
import org.gdms.sql.function.spatial.geometry.properties.ST_Length;
import org.gdms.sql.function.spatial.geometry.properties.ST_NumInteriorRing;
import org.gdms.sql.function.spatial.geometry.properties.ST_NumPoints;
import org.gdms.sql.function.spatial.geometry.properties.ST_X;
import org.gdms.sql.function.spatial.geometry.properties.ST_Y;
import org.gdms.sql.function.spatial.geometry.properties.ST_Z;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class PropertiesFunctionTest extends FunctionTest {

	public void testArea() throws Exception {
		double d = testSpatialFunction(new ST_Area(), g1, 1).getAsDouble();
		assertTrue(g1.getArea() == d);
	}

	public void testDimension() throws Exception {
		int d = testSpatialFunction(new ST_Dimension(), g1, 1).getAsInt();
		assertTrue(g1.getDimension() == d);
		d = testSpatialFunction(new ST_Dimension(), g2, 1).getAsInt();
		assertTrue(g2.getDimension() == d);
		d = testSpatialFunction(new ST_Dimension(), g3, 1).getAsInt();
		assertTrue(g3.getDimension() == d);
	}

	public void testGeometryN() throws Exception {
		int d = testSpatialFunction(new ST_GeometryN(), g1, 1).getAsInt();
		assertTrue(g1.getNumGeometries() == d);
	}

	public void testGeometryType() throws Exception {
		String v = testSpatialFunction(new ST_GeometryType(), g1, 1).getAsString();
		assertTrue(g1.getGeometryType().equals(v));
	}

	public void testGetZValue() throws Exception {
		Value v = testSpatialFunction(new ST_Z(), new GeometryFactory()
				.createPoint(new Coordinate(0, 50)), 1);
		assertTrue(v.isNull());
		double d = testSpatialFunction(new ST_Z(),
				new GeometryFactory().createPoint(new Coordinate(0, 50, 23)), 1)
				.getAsDouble();
		assertTrue(d == 23);
	}

	public void testGetX() throws Exception {
		Value v = testSpatialFunction(new ST_X(), new GeometryFactory()
				.createPoint(new Coordinate(0, 50)), 1);
		assertTrue(!v.isNull());
		double d = testSpatialFunction(new ST_X(),
				new GeometryFactory().createPoint(new Coordinate(0, 50, 23)), 1)
				.getAsDouble();
		assertTrue(d == 0);
	}

	public void testGetY() throws Exception {
		Value v = testSpatialFunction(new ST_Y(), new GeometryFactory()
				.createPoint(new Coordinate(0, 50)), 1);
		assertTrue(!v.isNull());
		double d = testSpatialFunction(new ST_Y(),
				new GeometryFactory().createPoint(new Coordinate(0, 50, 23)), 1)
				.getAsDouble();
		assertTrue(d == 50);
	}

	public void testIsEmpty() throws Exception {
		boolean v = testSpatialFunction(new ST_IsEmpty(),
				new GeometryFactory().createLinearRing(new Coordinate[0]), 1)
				.getAsBoolean();
		assertTrue(v);
	}

	public void testIsSimple() throws Exception {
		boolean v = testSpatialFunction(new ST_IsSimple(), g2, 1).getAsBoolean();
		assertTrue(v == g2.isSimple());
	}

	public void testIsValid() throws Exception {
		boolean v = testSpatialFunction(new ST_IsValid(), g2, 1).getAsBoolean();
		assertTrue(v == g2.isValid());
	}

	public void testLength() throws Exception {
		double v = testSpatialFunction(new ST_Length(), g2, 1).getAsDouble();
		assertTrue(v == g2.getLength());
	}

	public void testNumPoints() throws Exception {
		int v = testSpatialFunction(new ST_NumPoints(), g2, 1).getAsInt();
		assertTrue(v == g2.getNumPoints());
	}

	public void testNumInteriorRing() throws Exception {
		int v = testSpatialFunction(new ST_NumInteriorRing(), g4, 1).getAsInt();
		Polygon p = (Polygon) g4;
		assertTrue(p.getNumInteriorRing() == v);

		// Test with geometry collections
		v = testSpatialFunction(new ST_NumInteriorRing(), geomCollection, 1)
				.getAsInt();
		assertTrue(v == 4);
	}

}