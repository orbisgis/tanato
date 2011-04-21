/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import junit.framework.TestCase;
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
}
