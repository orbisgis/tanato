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
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.display.MeshDrawer;
import org.jhydrocell.hydronetwork.HydroTINBuilder;
import org.jhydrocell.hydronetwork.HydroProperties;

/**
 *
 * @author ebocher
 */
public class TINBuilder {

        static DataSourceFactory dsf = new DataSourceFactory();
        static String targetPoints = "/tmp/tinPoint.gdms";
        static String targetConstraints = "/tmp/tinEdges.gdms";
        static String target = "/tmp/tinTriangles.gdms";

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
                        z = sds.getFieldValue(i, 2).getAsDouble();
                        if (geom.isValid()) {
                                for (int j = 0; j < geom.getNumGeometries(); j++) {
                                        Geometry subGeom = geom.getGeometryN(j);
                                        if (geom.isValid()) {
                                                Coordinate c1 = subGeom.getCoordinates()[0];
                                                c1.z = z;
                                                Coordinate c2;
                                                for (int k = 1; k < subGeom.getCoordinates().length; k++) {
                                                        c2 = subGeom.getCoordinates()[k];
                                                        c2.z = z;
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

                //hydroNetwork.removeFlatTriangles();
                //triangulationTime = System.currentTimeMillis() - start;

               // System.out.println("Suppresion des triangles plats " + triangulationTime);


                hydroNetwork.morphologicalQualification();

                long qualificationTime = System.currentTimeMillis() - start;
                System.out.println("Temps de qualification " + qualificationTime);


                MeshDrawer meshDrawer = new MeshDrawer();
                meshDrawer.add(hydroNetwork);
                meshDrawer.setVisible(true);
                //We write the triangles in a GDMS output file
                File out = new File(target);
                if (out.exists()) {
                        out.delete();
                }
                out = new File(target);
                GdmsWriter writer = new GdmsWriter(out);

                Metadata md = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.FLOAT)}, new String[]{"the_geom", "Z"});

                int triangleCount = hydroNetwork.getTriangleList().size();
                writer.writeMetadata(triangleCount, md);
                GeometryFactory gf = new GeometryFactory();
                double midZ;
                for (DTriangle dt : hydroNetwork.getTriangleList()) {
                        midZ = dt.interpolateZ(dt.getBarycenter());
                        Coordinate[] coords = new Coordinate[4];
                        coords[0] = dt.getPoint(0).getCoordinate();
                        coords[1] = dt.getPoint(1).getCoordinate();
                        coords[2] = dt.getPoint(2).getCoordinate();
                        coords[3] = dt.getPoint(0).getCoordinate();
                        Polygon poly = gf.createPolygon(gf.createLinearRing(coords), null);
                        Value v1 = ValueFactory.createValue(poly);
                        Value v2 = ValueFactory.createValue(midZ);
                        writer.addValues(new Value[]{v1, v2});
                }
                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                //We write the output points in a GDMS file.
                File outPoints = new File(targetPoints);
                if (outPoints.exists()) {
                        outPoints.delete();
                }
                outPoints = new File(targetPoints);
                GdmsWriter writerb = new GdmsWriter(outPoints);
                md = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY)}, new String[]{"the_geom"});
                int pointCount = hydroNetwork.getPoints().size();
                writerb.writeMetadata(pointCount, md);
                gf = new GeometryFactory();
                for (DPoint dt : hydroNetwork.getPoints()) {
                        writerb.addValues(new Value[]{ValueFactory.createValue(gf.createPoint(dt.getCoordinate()))});
                }
                // write the row indexes
                writerb.writeRowIndexes();
                // write envelope
                writerb.writeExtent();
                writerb.close();
                // We write the constraint edges in a GDMS file
                File constraints = new File(targetConstraints);
                if (constraints.exists()) {
                        constraints.delete();
                }
                constraints = new File(targetConstraints);
                writerb = new GdmsWriter(constraints);
                md = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT), TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT), TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.STRING),
                                TypeFactory.createType(Type.DOUBLE)}, new String[]{"the_geom", "t_left", "t_right", "n_start", "n_end", "property", "type", "slope"});
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
                                        ValueFactory.createValue(gidLeft),
                                        ValueFactory.createValue(gidRight),
                                        ValueFactory.createValue(dt.getStart().getGID()),
                                        ValueFactory.createValue(dt.getEnd().getGID()),
                                        ValueFactory.createValue(dt.getProperty()),
                                        ValueFactory.createValue(HydroProperties.toString(dt.getProperty())),
                                        ValueFactory.createValue(dt.getSlope())});
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

                        writerb.addValues(new Value[]{ValueFactory.createValue(gf.createLineString(new Coordinate[]{dTriangle.getBarycenter().getCoordinate(), dTriangle.getSteepestIntersectionPoint(dTriangle.getBarycenter()).getCoordinate()}))});

                }
                // write the row indexes
                writerb.writeRowIndexes();
                // write envelope
                writerb.writeExtent();
                writerb.close();

        }

        public static void main(String[] args) throws Exception {
               // createTIN("/home/ebocher/Bureau/demo_tanato/small_courbes.shp");
                //createTIN("/home/ebocher/Documents/data/Data_Girona/ICC_Topo/bt25m_al.shp");
                //createTIN("/home/ebocher/Documents/projets/ANR/anr_avupur/modelisation_avupur/data/courbeschezine.shp");
                //createTIN("/home/ebocher/Bureau/data3D_chezine/route_buff_3D.shp");
                //createTIN("/home/ebocher/Documents/projets/ANR/anr_avupur/data_nantes/data_cadastre/parc_dgi/Parc_dgi.shp");
                 createTIN("/home/ebocher/Documents/projets/ANR/anr_avupur/data_nantes/bdtopo_x191_shp_l2e/e_bati/bati_indifferencie.shp");
        }
}
