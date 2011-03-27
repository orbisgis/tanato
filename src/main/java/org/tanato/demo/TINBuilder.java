/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.HashMap;
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
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.driver.gdms.GdmsWriter;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.display.MeshDrawer;
import org.jhydrocell.hydronetwork.HydroTINBuilder;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.NullProgressMonitor;
import org.tanato.model.HydroGraphBuilder;
import org.tanato.model.TINSchema;

/**
 *
 * @author ebocher
 */
public class TINBuilder {

        static DataSourceFactory dsf = new DataSourceFactory();
        static String tinPoints = "/tmp/tinPoint.gdms";
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

        private static void createHydroGraph() throws Exception {
                SpatialDataSourceDecorator sdsPoints = new SpatialDataSourceDecorator(dsf.getDataSource(new File(tinPoints)));
                SpatialDataSourceDecorator sdsEdges = new SpatialDataSourceDecorator(dsf.getDataSource(new File(tinEdges)));
                SpatialDataSourceDecorator sdsTriangles = new SpatialDataSourceDecorator(dsf.getDataSource(new File(tinTriangles)));

                HydroGraphBuilder hydroGraphBuilder = new HydroGraphBuilder(dsf, sdsTriangles, sdsEdges, sdsPoints);
                ObjectDriver[] drivers = hydroGraphBuilder.createGraph(new NullProgressMonitor());


        }

        public static void main(String[] args) throws Exception {
                //createTIN("/home/ebocher/Documents/projets/ANR/anr_avupur/modelisation_finale/chezine/data/courbesniveau.shp");
                 createTIN("/home/ebocher/Bureau/demo_tanato/small_courbes.shp");

                createHydroGraph();


//                SpatialDataSourceDecorator sdsTriangles = new SpatialDataSourceDecorator(dsf.getDataSource(new File(tinTriangles)));
//                sdsTriangles.open();
//
//                SpatialDataSourceDecorator sdsEdges = new SpatialDataSourceDecorator(dsf.getDataSource(new File(tinEdges)));
//
//                sdsEdges.open();
//
//                int leftTriangleFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.LEFT_TRIANGLE_FIELD);
//                int rightTriangleFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.RIGHT_TRIANGLE_FIELD);
//
//
//                for (int i = 0; i < sdsEdges.getRowCount(); i++) {
//                        int leftGid = sdsEdges.getFieldValue(i, leftTriangleFieldIndex).getAsInt();
//                        int rightGid = sdsEdges.getFieldValue(i, rightTriangleFieldIndex).getAsInt();
//
//                        if (leftGid!=-1){
//                        System.out.println(sdsTriangles.getGeometry(leftGid-1));
//                        }
//                        if(rightGid!=-1){
//                        System.out.println(sdsTriangles.getGeometry(rightGid-1));
//                        }
//
//                }
//
//                sdsEdges.close();
//                sdsTriangles.close();


        }
}
