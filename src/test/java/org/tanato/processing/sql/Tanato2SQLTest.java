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
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.DataSource;
import org.gdms.data.indexes.DefaultAlphaQuery;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.schema.MetadataUtilities;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.gdms.driver.memory.MemoryDataSetDriver;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.spatial.geometry.edit.ST_AddZ;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.NullProgressMonitor;
import org.orbisgis.utils.FileUtils;
import org.tanato.model.TINSchema;

/**
 *
 * @author ebocher
 */
public class Tanato2SQLTest extends TestCase {

        public static final List<Point> SMALL_CHEZINE_POINTS;
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
        private static final GeometryFactory gf = new GeometryFactory();
        private SQLDataSourceFactory dsf;

        static {
                SMALL_CHEZINE_POINTS = new ArrayList<Point>();
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(108, 222, 20)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(136, 262, 20)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(140, 125, 20)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(175, 163, 10)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(209, 95, 10)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(213, 273, 20)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(217, 284, 20)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(310, 100, 10)));
                SMALL_CHEZINE_POINTS.add(gf.createPoint(new Coordinate(331, 142, 20)));
        }

        @Override
        protected void setUp() throws Exception {
                //Create a folder to save all results
                File backUpFile = new File("target/backup");
                FileUtils.deleteDir(backUpFile);
                backUpFile.mkdir();
                //Create the datasourcefactory that uses the folder
                dsf = new SQLDataSourceFactory("target/backup", "target/backup");

                //Create some geometries
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

                jTS3DCollection = gf.createGeometryCollection(new Geometry[]{jTSMultiPolygon2D, jTSLineString3D});
        }

        @Override
        protected void tearDown() throws Exception {
                //Delete the folder that contains result
                File backUpFile = new File("target/backup");
                FileUtils.deleteDir(backUpFile);

        }

        /**
         * A test to valid the ST_LINEARINTERPOLATION function
         * @throws Exception
         */
        public void testST_LinearInterpolation() throws Exception {
                ST_AddZ function = new ST_AddZ();

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

        public void testST_ParallelLine() throws Exception {
                ST_ParalleleLine fun = new ST_ParalleleLine();
                LineString edge = gf.createLineString(new Coordinate[]{
                                new Coordinate(1, 1, 0),
                                new Coordinate(5, 1, 0)});
                Value first = ValueFactory.createValue(edge);
                Value second = ValueFactory.createValue(1.0d);
                Value res = fun.evaluate(new SQLDataSourceFactory(), new Value[]{first, second});
                Geometry geom = res.getAsGeometry();
                assertTrue(geom.equals(gf.createLineString(new Coordinate[]{
                                new Coordinate(1, 0, 0),
                                new Coordinate(5, 0, 0)})));
        }

        public void testST_CreateHydroProperies() throws Exception {
                ST_CreateHydroProperties fun = new ST_CreateHydroProperties();
                assertFalse(fun.isAggregate());
                Value[] values = new Value[]{};
                try {
                        fun.evaluate(dsf, values);
                        assertTrue(false);
                } catch (FunctionException f) {
                        assertTrue(true);
                }
                String talweg = "TALWEG";
                Value val = ValueFactory.createValue(talweg);
                values = new Value[]{val};
                Value out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == HydroProperties.TALWEG);
                talweg = "$" + HydroProperties.toString(HydroProperties.TALWEG);
                val = ValueFactory.createValue(talweg);
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == HydroProperties.NONE);
                talweg = HydroProperties.toString(HydroProperties.TALWEG) + " + "
                        + HydroProperties.toString(HydroProperties.DITCH);
                val = ValueFactory.createValue(talweg);
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == (HydroProperties.DITCH | HydroProperties.TALWEG));
                talweg = HydroProperties.toString(HydroProperties.TALWEG) + " &&&+ "
                        + HydroProperties.toString(HydroProperties.DITCH);
                val = ValueFactory.createValue(talweg);
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == (HydroProperties.TALWEG));
                talweg = HydroProperties.toString(HydroProperties.TALWEG) + " + "
                        + HydroProperties.toString(HydroProperties.DITCH) + " + "
                        + HydroProperties.toString(HydroProperties.RIVER) + " - "
                        + HydroProperties.toString(HydroProperties.DITCH);
                val = ValueFactory.createValue(talweg);
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == (HydroProperties.RIVER | HydroProperties.TALWEG));
                val = ValueFactory.createValue("ALL");
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == HydroProperties.ANY);
                val = ValueFactory.createValue("ANY");
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == HydroProperties.ANY);
                val = ValueFactory.createValue("NONE");
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == HydroProperties.NONE);
                talweg = "LEFTCOLINEAR";
                val = ValueFactory.createValue(talweg);
                values = new Value[]{val};
                out = fun.evaluate(dsf, values);
                assertTrue(out.getAsInt() == HydroProperties.LEFTCOLINEAR);


        }

        /**
         * Test on ST_TIN. We remove flat triangles.
         * @throws Exception 
         */
        public void testST_TIN() throws Exception {
                ST_TIN query = new ST_TIN();
                //The original input contains exactly 9 points and 7 constrained edges.
                DataSource in = dsf.getDataSource(new File("src/test/resources/data/source/small_data/small_courbes_chezine.shp"));
                //Output tables will be stored in out_points, out_edges and out_triangles
                Value[] vals = new Value[]{
                        ValueFactory.createValue(true),
                        ValueFactory.createValue(true)
                };
                query.evaluate(dsf, new DataSource[]{in}, vals, new NullProgressMonitor());
                DataSource sds = dsf.getDataSource(in.getName() + "_points");
                assertNotNull(sds);
                sds.open();
                //There have been two insertions, because two triangles were flat.
                assertTrue(sds.getRowCount() == 11);
                for (Point pt : SMALL_CHEZINE_POINTS) {
                        DefaultAlphaQuery daq = new DefaultAlphaQuery("the_geom", ValueFactory.createValue(pt));
                        Iterator<Integer> it = sds.queryIndex(daq);
                        assertTrue(it.hasNext());
                }
                //We must test the two remaining points.
                DTriangle dt1 = new DTriangle(
                        new DEdge(209, 95, 10, 175, 163, 10),
                        new DEdge(175, 163, 10, 217, 184, 10),
                        new DEdge(217, 184, 10, 209, 95, 10));
                Point pt1 = gf.createPoint(dt1.getBarycenter().getCoordinate());
                Point pt2 = gf.createPoint(dt1.getCircumCenter());
                DefaultAlphaQuery daq = new DefaultAlphaQuery("the_geom", ValueFactory.createValue(pt1));
                Iterator<Integer> it = sds.queryIndex(daq);
                DefaultAlphaQuery daq2 = new DefaultAlphaQuery("the_geom", ValueFactory.createValue(pt2));
                Iterator<Integer> it2 = sds.queryIndex(daq2);
                assertTrue(it.hasNext() || it2.hasNext());
                dt1 = new DTriangle(
                        new DEdge(209, 95, 10, 217, 184, 10),
                        new DEdge(217, 184, 10, 310, 100, 10),
                        new DEdge(310, 100, 10, 209, 95, 10));
                pt1 = gf.createPoint(dt1.getBarycenter().getCoordinate());
                pt2 = gf.createPoint(dt1.getCircumCenter());
                daq = new DefaultAlphaQuery("the_geom", ValueFactory.createValue(pt1));
                it = sds.queryIndex(daq);
                daq2 = new DefaultAlphaQuery("the_geom", ValueFactory.createValue(pt2));
                it2 = sds.queryIndex(daq2);
                assertTrue(it.hasNext() || it2.hasNext());

                sds.close();
                sds = dsf.getDataSource(in.getName() + "_triangles");
                assertNotNull(sds);
                sds.open();
                assertTrue(sds.getRowCount() == 13);
                sds.close();

        }

        /**
         * We tet the small lines from chezine, but we don't remove flat triangles.
         * @throws Exception 
         */
        public void testST_TINWithFlats() throws Exception {
                ST_TIN query = new ST_TIN();
                //The original input contains exactly 9 points and 7 constrained edges.
                DataSource in = dsf.getDataSource(new File("src/test/resources/data/source/small_data/small_courbes_chezine.shp"));

                deleteFileIfExists(new File(in.getName() + "_points"));
                deleteFileIfExists(new File(in.getName() + "_edges"));
                deleteFileIfExists(new File(in.getName() + "_triangles"));
                //Output tables will be stored in out_points, out_edges and out_triangles
                Value[] vals = new Value[]{
                        ValueFactory.createValue(true),
                        ValueFactory.createValue(false)
                };
                query.evaluate(dsf, new DataSource[]{in}, vals, new NullProgressMonitor());
                DataSource sds = dsf.getDataSource(in.getName() + "_points");
                assertNotNull(sds);
                sds.open();
                assertTrue(sds.getRowCount() == 9);
                for (Point pt : SMALL_CHEZINE_POINTS) {
                        DefaultAlphaQuery daq = new DefaultAlphaQuery("the_geom", ValueFactory.createValue(pt));
                        Iterator<Integer> it = sds.queryIndex(daq);
                        assertTrue(it.hasNext());
                }
                sds.close();
                //We treat the triangles now
                List<DTriangle> expectedTriangles = new ArrayList<DTriangle>();
                expectedTriangles.add(new DTriangle(
                        new DEdge(140, 125, 20, 175, 163, 10),
                        new DEdge(175, 163, 10, 209, 95, 10),
                        new DEdge(209, 95, 10, 140, 125, 20)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(140, 125, 20, 175, 163, 10),
                        new DEdge(175, 163, 10, 108, 222, 20),
                        new DEdge(108, 222, 20, 140, 125, 20)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(136, 262, 20, 175, 163, 10),
                        new DEdge(175, 163, 10, 108, 222, 20),
                        new DEdge(108, 222, 20, 136, 262, 20)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(136, 262, 20, 175, 163, 10),
                        new DEdge(175, 163, 10, 217, 184, 10),
                        new DEdge(217, 184, 10, 136, 262, 20)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(209, 95, 10, 175, 163, 10),
                        new DEdge(175, 163, 10, 217, 184, 10),
                        new DEdge(217, 184, 10, 209, 95, 10)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(209, 95, 10, 310, 100, 10),
                        new DEdge(310, 100, 10, 217, 184, 10),
                        new DEdge(217, 184, 10, 209, 95, 10)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(331, 142, 20, 310, 100, 10),
                        new DEdge(310, 100, 10, 217, 184, 10),
                        new DEdge(217, 184, 10, 331, 142, 20)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(331, 142, 20, 213, 273, 20),
                        new DEdge(213, 273, 20, 217, 184, 10),
                        new DEdge(217, 184, 10, 331, 142, 20)));
                expectedTriangles.add(new DTriangle(
                        new DEdge(136, 262, 20, 213, 273, 20),
                        new DEdge(213, 273, 20, 217, 184, 10),
                        new DEdge(217, 184, 10, 136, 262, 20)));

                sds = dsf.getDataSource(in.getName() + "_triangles");
                assertNotNull(sds);
                sds.open();
                assertTrue(sds.getRowCount() == 9);
                for (DTriangle dTriangle : expectedTriangles) {
                        Polygon geom = gf.createPolygon(gf.createLinearRing(
                                new Coordinate[]{
                                        dTriangle.getPoint(0).getCoordinate(),
                                        dTriangle.getPoint(1).getCoordinate(),
                                        dTriangle.getPoint(2).getCoordinate(),
                                        dTriangle.getPoint(0).getCoordinate()
                                }), new LinearRing[]{});
                        DefaultAlphaQuery daq = new DefaultAlphaQuery("the_geom", ValueFactory.createValue(geom));
                        Iterator<Integer> it = sds.queryIndex(daq);
                        assertTrue(it.hasNext());
                }
                sds.close();

        }

        private void deleteFileIfExists(File file) {
                if (file.exists()) {
                        file.delete();
                }
        }

        public void testST_TriangleSlope() throws Exception {
                ST_TriangleSlope fun = new ST_TriangleSlope();
                assertTrue(Type.DOUBLE == fun.getType(new Type[]{}).getTypeCode());
                Geometry geom = gf.createLinearRing(new Coordinate[]{
                                new Coordinate(0, 0, 0),
                                new Coordinate(4, 0, 0),
                                new Coordinate(2, 2, 10),
                                new Coordinate(0, 0, 0)
                        });
                double slope = 100 * 10 / 2;
                Value out = fun.evaluate(dsf, new Value[]{ValueFactory.createValue(geom)});
                assertTrue(slope == out.getAsDouble());
                geom = gf.createPoint(new Coordinate(42, 42, 42));
                try {
                        fun.evaluate(dsf, new Value[]{ValueFactory.createValue(geom)});
                        assertTrue(false);
                } catch (IllegalArgumentException e) {
                        assertTrue(true);
                }
                geom = gf.createLineString(new Coordinate[]{
                                new Coordinate(0, 0, 0),
                                new Coordinate(2, 4, 6),
                                new Coordinate(8, 9, 3),
                                new Coordinate(7, 2, 6)
                        });
        }

        public void testST_TINSlopeDirection() throws Exception {
                ST_TINSlopeDirection fun = new ST_TINSlopeDirection();
                assertTrue(Type.GEOMETRY == fun.getType(new Type[]{}).getTypeCode());
                Geometry geom = gf.createLinearRing(new Coordinate[]{
                                new Coordinate(0, 0, 0),
                                new Coordinate(4, 0, 0),
                                new Coordinate(2, 3, 9),
                                new Coordinate(0, 0, 0)
                        });
                Value out = fun.evaluate(dsf, new Value[]{ValueFactory.createValue(geom)});
                LineString ls = gf.createLineString(new Coordinate[]{
                                new Coordinate(2, 1, 3),
                                new Coordinate(2, 0, 0)
                        });
                assertTrue(ls.equals(out.getAsGeometry()));


        }

        public void testST_GetHydroProperty() throws Exception {
                ST_GetHydroProperty fun = new ST_GetHydroProperty();
                assertTrue(Type.STRING == fun.getType(new Type[]{}).getTypeCode());
                Value out = fun.evaluate(null, new Value[]{ValueFactory.createValue(HydroProperties.BORDER)});
                assertTrue(out.getAsString().contentEquals(HydroProperties.toString(HydroProperties.BORDER)));
        }

        public void testST_DropletLine1() throws Exception {
                //First open a TIN
                DataSource dsPoints = dsf.getDataSource(new File("src/test/resources/data/tin/small_courbes_chezine/without_flat_points.shp"));
                DataSource dsEdges = dsf.getDataSource(new File("src/test/resources/data/tin/small_courbes_chezine/without_flat_edges.shp"));
                DataSource dsTriangles = dsf.getDataSource(new File("src/test/resources/data/tin/small_courbes_chezine/without_flat_triangles.shp"));

                //Now compute the runoff path with the droplet function
                ST_DropletLine sT_DropletLine = new ST_DropletLine();

                DefaultMetadata metadata = new DefaultMetadata();
                metadata.addField("the_geom", TypeFactory.createType(Type.GEOMETRY));

                MemoryDataSetDriver driver = new MemoryDataSetDriver(metadata);

                WKTReader wKTReader = new WKTReader();
                //This target point start on the triangle gid number 5
                // The result geometry path crosses the TIN features :
                // triangle 5  -> edge 34 -> triangle 6 -> edge 30 (a talweg) -> -> edge 38 (a talweg) -> points 9
                Geometry targetPoint = wKTReader.read("POINT (178.11619336849773 180.89460753843517)");
                driver.addValues(new Value[]{ValueFactory.createValue(targetPoint)});


                DataSet sds = sT_DropletLine.evaluate(dsf, new DataSet[]{dsPoints, dsEdges, dsTriangles, driver}, new Value[]{}, new NullProgressMonitor());

                assertTrue(sds != null);
                assertTrue(sds.getRowCount() > 0);
                Geometry geom = sds.getGeometry(0, MetadataUtilities.getGeometryFieldIndex(sds.getMetadata()));
                assertTrue(geom.getDimension() == 1);
                //TODO : Check if the result geometry intersects excepted TIN features
                Coordinate[] coords = geom.getCoordinates();

                //Check for each coordinate path if the coordinate intersects a geometry in the input datasource
                assertTrue(checkFeature(coords[0], 5, dsTriangles));
                assertTrue(checkFeature(coords[1], 34, dsEdges));
                assertTrue(checkFeature(coords[2], 6, dsTriangles));
                assertTrue(checkFeature(coords[2], 30, dsEdges));
//                assertTrue(checkFeature(coords[3], 30, dsTriangles));
                assertTrue(checkFeature(coords[3], 30, dsEdges));
                assertTrue(checkFeature(coords[3], 38, dsEdges));
                assertTrue(checkFeature(coords[4], 38, dsEdges));
        }

        public void testST_DropletLine2() throws Exception {
                DataSource dsPoints = dsf.getDataSource(new File("src/test/resources/data/tin/chezine_amont/chezine_amont_points.shp"));
                DataSource dsEdges = dsf.getDataSource(new File("src/test/resources/data/tin/chezine_amont/chezine_amont_edges.shp"));
                DataSource dsTriangles = dsf.getDataSource(new File("src/test/resources/data/tin/chezine_amont/chezine_amont_triangles.shp"));

                ST_DropletLine sT_DropletLine = new ST_DropletLine();

                DefaultMetadata metadata = new DefaultMetadata();
                metadata.addField("the_geom", TypeFactory.createType(Type.GEOMETRY));

                MemoryDataSetDriver driver = new MemoryDataSetDriver(metadata);
                WKTReader wKTReader = new WKTReader();

                //This target point starts on a flat and thin triangle
                Geometry targetPoint = wKTReader.read("POINT (343725.4125771761 6698264.727716073)");
                driver.addValues(new Value[]{ValueFactory.createValue(targetPoint)});

                DataSet sds = sT_DropletLine.evaluate(dsf, new DataSet[]{dsPoints, dsEdges, dsTriangles, driver}, new Value[]{}, new NullProgressMonitor());

                assertTrue(sds != null);
                assertTrue(sds.getRowCount() > 0);
                Geometry geom = sds.getGeometry(0, MetadataUtilities.getGeometryFieldIndex(sds.getMetadata()));
                assertTrue(geom.getDimension() == 1);
        }

        /**
         * A method to check if the coordinate intersect a requiered geometry defined by its GID
         * @param coordinate
         * @param gid
         * @param ds
         * @return
         */
        private boolean checkFeature(Coordinate coordinate, int gid, DataSource ds) throws DriverException {
                ds.open();
                int gidIndex = ds.getFieldIndexByName(TINSchema.GID);
                int geomDSIndex = ds.getFieldIndexByName(TINSchema.GEOM_FIELD);
                Geometry geomDS = null;
                for (int i = 0; i < ds.getRowCount(); i++) {
                        Value[] values = ds.getRow(i);
                        if (values[gidIndex].getAsInt() == gid) {
                                geomDS = values[geomDSIndex].getAsGeometry();
                                break;
                        }
                }
                ds.close();
                Point aPoint = gf.createPoint(coordinate);
                return aPoint.isWithinDistance(geomDS, 1.0e-10);
        }
}
