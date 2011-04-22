/* 
 * TANATO  is a library dedicated to the modelling of water pathways based on 
 * triangulate irregular network. TANATO takes into account anthropogenic and 
 * natural artifacts to evaluate their impacts on the watershed response. 
 * It ables to compute watershed, main slope directions and water flow pathways.
 * 
 * This library has been originally created  by Erwan Bocher during his thesis 
 * “Impacts des activités humaines sur le parcours des écoulements de surface dans 
 * un bassin versant bocager : essai de modélisation spatiale. Application au 
 * Bassin versant du Jaudy-Guindy-Bizien (France)”. It has been funded by the 
 * Bassin versant du Jaudy-Guindy-Bizien and Syndicat d’Eau du Trégor.
 * 
 * The new version is developed at French IRSTV institut as part of the 
 * AvuPur project, funded by the French Agence Nationale de la Recherche 
 * (ANR) under contract ANR-07-VULN-01.
 * 
 * TANATO is distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 * Copyright (C) 2010 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
 * Copyright (C) 2011 Erwan BOCHER, , Alexis GUEGANNO, Jean-Yves MARTIN
 * 
 * TANATO is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * TANATO is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * TANATO. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, please consult: <http://trac.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */

package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKTReader;
import junit.framework.TestCase;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.spatial.geometry.edit.ST_AddZToGeometry;

/**
 *
 * @author ebocher
 */
public class Tanato2SQLTest extends TestCase {

        protected Geometry jTSMultiPolygon2D;
        protected Geometry jTSMultiLineString2D;
        protected Geometry jTSMultiPoint2D;
        protected Geometry jTSPolygon2D; // With two holes
        protected Geometry jTSGeometryCollection;
        protected Geometry jTSPoint3D;
        protected Geometry jTSLineString2D;
        protected Geometry jTSPoint2D;
        protected Geometry jTSLineString3D;
        protected GeometryCollection jTS3DCollection;
	private static GeometryFactory gf = new GeometryFactory();

        @Override
        protected void setUp() throws Exception {
                WKTReader wktr = new WKTReader();
                jTSMultiPolygon2D = wktr.read("MULTIPOLYGON (((0 0, 1 1, 0 1, 0 0)))");
                jTSMultiLineString2D = wktr.read("MULTILINESTRING ((0 0, 1 1, 0 1, 0 0))");
                jTSMultiPoint2D = wktr.read("MULTIPOINT (0 0, 1 1, 0 1, 0 0)");
                jTSPolygon2D = wktr.read("POLYGON ((181 124, 87 162, 76 256, 166 315, 286 325, 373 255, 387 213, 377 159, 351 121, 298 101, 234 56, 181 124), (165 244, 227 219, 234 300, 168 288, 165 244), (244 130, 305 135, 324 186, 306 210, 272 206, 206 174, 244 130))");

                jTSLineString2D = wktr.read("LINESTRING (1 1, 2 1, 2 2, 1 2, 1 1)");
                jTSPoint3D = wktr.read("POINT(0 10 20)");
                jTSPoint2D = wktr.read("POINT(0 10)");
                jTSLineString3D = wktr.read("LINESTRING (0 0 0, 5 0, 10 0 12)");

                jTSGeometryCollection = gf.createGeometryCollection(new Geometry[]{
                                jTSMultiPolygon2D, jTSMultiLineString2D, jTSPolygon2D});

                jTS3DCollection = gf.createGeometryCollection(new Geometry[]{jTSMultiPolygon2D,jTSLineString3D});
        }

        /**
         * A test to valid the ST_LINEARINTERPOLATION function
         * @throws Exception
         */
        public void testST_LinearInterpolation() throws Exception {
                ST_AddZToGeometry function = new ST_AddZToGeometry();

                Value[] values = new Value[]{ValueFactory.createValue(jTSLineString2D), ValueFactory.createValue(12)};
                Geometry geom = function.evaluate(null, values).getAsGeometry();
                Coordinate[] coords = geom.getCoordinates();

                for (Coordinate coordinate : coords) {
                        assertTrue(coordinate.z == 12);
                }
                ST_LINEARINTERPOLATION function2 = new ST_LINEARINTERPOLATION();

                values = new Value[]{ValueFactory.createValue(geom)};

                geom = function2.evaluate(null, values).getAsGeometry();
                coords = geom.getCoordinates();
                for (Coordinate coordinate : coords) {
                        assertTrue(coordinate.z == 12);
                }

                values = new Value[]{ValueFactory.createValue(jTSLineString3D)};

                geom = function2.evaluate(null, values).getAsGeometry();
                coords = geom.getCoordinates();

                assertTrue(coords[1].z == 6);

                values = new Value[]{ValueFactory.createValue(jTS3DCollection)};

                geom = function2.evaluate(null, values).getAsGeometry().getGeometryN(1);
                coords = geom.getCoordinates();
                assertTrue(coords[1].z == 6);
        }

	public void testST_ParallelLine() throws Exception{
		ST_ParalleleLine fun = new ST_ParalleleLine();
		LineString edge = gf.createLineString(new Coordinate[]{
				new Coordinate(1,1,0),
				new Coordinate(5,1,0)});
		Value first = ValueFactory.createValue(edge);
		Value second = ValueFactory.createValue(1.0d);
		Value res = fun.evaluate(new DataSourceFactory(), new Value[] {first, second});
		Geometry geom = res.getAsGeometry();
		assertTrue(geom.equals(gf.createLineString(new Coordinate[]{
				new Coordinate(1,0,0),
				new Coordinate(5,0,0)})));
	}
}
