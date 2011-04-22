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
package org.tanato.demo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.gdms.GdmsWriter;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.jhydrocell.hydronetwork.HydroTINBuilder;
import org.tanato.model.TINSchema;

/**
 *
 * @author ebocher
 */
public class TINBuilder {

        static DataSourceFactory dsf = new DataSourceFactory();
        static String tinPoints = "/tmp/tinPoints.gdms";
        static String tinEdges = "/tmp/tinEdges.gdms";
        static String tinTriangles = "/tmp/tinTriangles.gdms";

        public TINBuilder() {
        }

        public static void createTIN(String path) throws Exception {
                long start = System.currentTimeMillis();
                HydroTINBuilder hydroNetwork = new HydroTINBuilder();
                hydroNetwork.setVerbose(true);
                DataSource mydata = dsf.getDataSource(new File(path));
                SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(mydata);
                sds.open();
                //We retrieve the data in the input file.
                double z;
                ArrayList<DEdge> toBeAdded = new ArrayList<DEdge>();
                for (long i = 0; i < sds.getRowCount(); i++) {
                        Geometry geom = sds.getGeometry(i);
                        if (geom.isValid()) {
                                for (int j = 0; j < geom.getNumGeometries(); j++) {
                                        Geometry subGeom = geom.getGeometryN(j);
                                        if (geom.isValid()) {
                                                Coordinate c1 = subGeom.getCoordinates()[0];
                                                Coordinate c2;
                                                for (int k = 1; k < subGeom.getCoordinates().length; k++) {
                                                        c2 = subGeom.getCoordinates()[k];
                                                        DEdge edge = new DEdge(new DPoint(c1), new DPoint(c2));
                                                        edge.setProperty(1);
                                                        toBeAdded.add(edge);
                                                        c1 = c2;
                                                }
                                        }
                                }
                        }
                }
                //We perform a first sort to gain time during the insertion.
                Collections.sort(toBeAdded);
                hydroNetwork.setConstraintEdges(toBeAdded);
                sds.close();

                System.out.println("Number of edges before intersection processing " + hydroNetwork.getConstraintEdges().size());
                //We force the integrity of the constraints given as an input.
                hydroNetwork.forceConstraintIntegrity();

                System.out.println("Number of edges after intersection processing " + hydroNetwork.getConstraintEdges().size());
                //We perform the triangulation
                hydroNetwork.processDelaunay();
                long triangulationTime = System.currentTimeMillis() - start;
                System.out.println("Temps de triangulation " + triangulationTime);

                hydroNetwork.removeFlatTriangles();
                triangulationTime = System.currentTimeMillis() - start;

                System.out.println("Suppresion des triangles plats " + triangulationTime);


                hydroNetwork.morphologicalQualification();

                long qualificationTime = System.currentTimeMillis() - start;
                System.out.println("Temps de qualification " + qualificationTime);


                //MeshDrawer meshDrawer = new MeshDrawer();
                //meshDrawer.add(hydroNetwork);
                //meshDrawer.setVisible(true);
                //We write the triangles in a GDMS output file
                File out = new File(tinTriangles);
                if (out.exists()) {
                        out.delete();
                }
                out = new File(tinTriangles);
                GdmsWriter writer = new GdmsWriter(out);

                Metadata md = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT)}, new String[]{TINSchema.GEOM_FIELD, TINSchema.GID});

                int triangleCount = hydroNetwork.getTriangleList().size();
                writer.writeMetadata(triangleCount, md);
                GeometryFactory gf = new GeometryFactory();
                for (DTriangle dt : hydroNetwork.getTriangleList()) {
                        Coordinate[] coords = new Coordinate[4];
                        coords[0] = dt.getPoint(0).getCoordinate();
                        coords[1] = dt.getPoint(1).getCoordinate();
                        coords[2] = dt.getPoint(2).getCoordinate();
                        coords[3] = dt.getPoint(0).getCoordinate();
                        Polygon poly = gf.createPolygon(gf.createLinearRing(coords), null);
                        Value v1 = ValueFactory.createValue(poly);
                        Value v2 = ValueFactory.createValue(dt.getGID());
                        writer.addValues(new Value[]{v1, v2});
                }
                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                //We write the output points in a GDMS file.
                File outPoints = new File(tinPoints);
                if (outPoints.exists()) {
                        outPoints.delete();
                }
                outPoints = new File(tinPoints);
                GdmsWriter writerb = new GdmsWriter(outPoints);
                md = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY), TypeFactory.createType(Type.INT)}, new String[]{TINSchema.GEOM_FIELD, TINSchema.GID});
                int pointCount = hydroNetwork.getPoints().size();
                writerb.writeMetadata(pointCount, md);
                gf = new GeometryFactory();
                for (DPoint dt : hydroNetwork.getPoints()) {
                        writerb.addValues(new Value[]{ValueFactory.createValue(gf.createPoint(dt.getCoordinate())), ValueFactory.createValue(dt.getGID())});
                }
                // write the row indexes
                writerb.writeRowIndexes();
                // write envelope
                writerb.writeExtent();
                writerb.close();
                // We write the constraint edges in a GDMS file
                File constraints = new File(tinEdges);
                if (constraints.exists()) {
                        constraints.delete();
                }
                constraints = new File(tinEdges);
                writerb = new GdmsWriter(constraints);
                md = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT), TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT), TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.DOUBLE)}, new String[]{TINSchema.GEOM_FIELD, TINSchema.GID, TINSchema.LEFT_TRIANGLE_FIELD,
                                TINSchema.RIGHT_TRIANGLE_FIELD, TINSchema.STARTPOINT_NODE_FIELD, TINSchema.ENDPOINT_NODE_FIELD, TINSchema.PROPERTY_FIELD, TINSchema.HEIGHT_FIELD});
                int cstrCount = hydroNetwork.getEdges().size();
                writerb.writeMetadata(cstrCount, md);
                gf = new GeometryFactory();
                for (DEdge dt : hydroNetwork.getEdges()) {
                        dt.forceTopographicOrientation();
                        Coordinate[] coords = new Coordinate[2];
                        coords[0] = dt.getPointLeft().getCoordinate();
                        coords[1] = dt.getPointRight().getCoordinate();
                        CoordinateSequence cs = new CoordinateArraySequence(coords);
                        LineString mp = new LineString(cs, gf);

                        int gidLeft = -1;
                        if (dt.getLeft() != null) {
                                gidLeft = dt.getLeft().getGID();
                        }

                        int gidRight = -1;
                        if (dt.getRight() != null) {
                                gidRight = dt.getRight().getGID();
                        }
                        writerb.addValues(new Value[]{ValueFactory.createValue(mp),
                                        ValueFactory.createValue(dt.getGID()),
                                        ValueFactory.createValue(gidLeft),
                                        ValueFactory.createValue(gidRight),
                                        ValueFactory.createValue(dt.getStart().getGID()),
                                        ValueFactory.createValue(dt.getEnd().getGID()),
                                        ValueFactory.createValue(dt.getProperty()),
                                        ValueFactory.createValue(dt.getHeight())});
                }
                // write the row indexes
                writerb.writeRowIndexes();
                // write envelope
                writerb.writeExtent();
                writerb.close();

                computeVectorDirection(hydroNetwork.getTriangleList());
        }

        private static void computeVectorDirection(List<DTriangle> triangleList) throws IOException, DriverException, DelaunayError {

                GeometryFactory gf = new GeometryFactory();
                //We write the output points in a GDMS file.
                File outPoints = new File("/tmp/vectorDirector.gdms");
                if (outPoints.exists()) {
                        outPoints.delete();
                }
                outPoints = new File("/tmp/vectorDirector.gdms");
                GdmsWriter writerb = new GdmsWriter(outPoints);
                DefaultMetadata md = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY)}, new String[]{"the_geom"});
                int pointCount = triangleList.size();
                writerb.writeMetadata(pointCount, md);

                for (DTriangle dTriangle : triangleList) {
                        DPoint pointIntersection = dTriangle.getSteepestIntersectionPoint(dTriangle.getBarycenter());
                        if (pointIntersection != null) {
                                writerb.addValues(new Value[]{ValueFactory.createValue(gf.createLineString(new Coordinate[]{dTriangle.getBarycenter().getCoordinate(), pointIntersection.getCoordinate()}))});
                        }
                }
                // write the row indexes
                writerb.writeRowIndexes();
                // write envelope
                writerb.writeExtent();
                writerb.close();

        }

        public static void main(String[] args) throws Exception {
                createTIN("/home/ebocher/Documents/projets/ANR/anr_avupur/modelisation_finale/chezine/data/courbesniveau.shp");
                //  createTIN("/home/ebocher/Bureau/demo_tanato/small_courbes.shp");



        }
}
