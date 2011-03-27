/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.gdms.GdmsWriter;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.jhydrocell.hydronetwork.HydroTINBuilder;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.factory.TINFeatureFactory;
import org.tanato.model.TINSchema;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a constrained delaunay triangulation from the geometry given in input.
 *
 * 
 * @author alexis
 */
public class ST_HydroTIN implements CustomQuery {

        private static final Logger logger = Logger.getLogger(ST_HydroTIN.class.getName());

        @Override
        public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
                Value[] values, IProgressMonitor pm) throws ExecutionException {

                try {
                        //Process contourlines
                        DataSource ds = tables[0];
                        //We need to read our source.
                        SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(ds);
                        sds.open();
                        boolean useTriangulationRules = false;
                        Map<Integer, Integer> weights = new HashMap<Integer, Integer>();
                        int propertyIndex = -1;
                        int weigthIndex = -1;
                        if (values[3].getAsBoolean()) {
                                propertyIndex = ds.getFieldIndexByName(TINSchema.PROPERTY_FIELD);
                                weigthIndex = ds.getFieldIndexByName(TINSchema.WEIGTH_FIELD);
                                if ((propertyIndex != -1) && (weigthIndex != -1)) {
                                        useTriangulationRules = true;
                                }

                        }

                        //We retrieve the values to know how we are supposed to proceed.
                        boolean inter = values[0].getAsBoolean();
                        boolean flat = values[1].getAsBoolean();
                        String name = values[4].getAsString();
                        long count = sds.getRowCount();
                        Geometry geom = null;
                        //We prepare our input structures.
                        List<DPoint> pointsToAdd = new ArrayList<DPoint>();
                        ArrayList<DEdge> edges = new ArrayList<DEdge>();
                        int propertyValue = 0;
                        boolean notSewer = false;
                        //We fill the input structures with our table.
                        for (long i = 0; i < count; i++) {
                                geom = sds.getGeometry(i);
                                //If rules is used then get property and weight
                                if (useTriangulationRules) {
                                        int property = sds.getFieldValue(i, propertyIndex).getAsInt();
                                        int weight = sds.getFieldValue(i, weigthIndex).getAsInt();
                                        weights.put(property, weight);
                                        

                                        switch (property) {
                                                case HydroProperties.DITCH:
                                                        propertyValue = HydroProperties.DITCH;
                                                        break;
                                                case HydroProperties.LEVEL:
                                                        propertyValue = HydroProperties.LEVEL;
                                                        break;
                                                case HydroProperties.RIVER:
                                                        propertyValue = HydroProperties.RIVER;
                                                        break;
                                                case HydroProperties.SEWER_INPUT:
                                                        notSewer = true;
                                                        break;
                                                case HydroProperties.SEWER:
                                                        notSewer = true;
                                                        break;
                                                case HydroProperties.SEWER_OUTPUT:
                                                        notSewer = true;
                                                        break;
                                                case HydroProperties.WALL:
                                                        propertyValue = HydroProperties.WALL;
                                                        break;
                                                case HydroProperties.URBAN_PARCEL:
                                                        propertyValue = HydroProperties.URBAN_PARCEL;
                                                        break;
                                                case HydroProperties.RURAL_PARCEL:
                                                        propertyValue = HydroProperties.RURAL_PARCEL;
                                                        break;
                                                case HydroProperties.ROAD:
                                                        propertyValue = HydroProperties.ROAD;
                                                        break;
                                                default:
                                                        propertyValue = HydroProperties.NONE;
                                                        break;
                                        }
                                }
                                //Dot no take into account sewer in the triangulation
                                if (!notSewer) {
                                        if (geom instanceof GeometryCollection) {
                                                final int nbOfGeometries = geom.getNumGeometries();

                                                for (int j = 0; j < nbOfGeometries; j++) {
                                                        addGeometry(geom.getGeometryN(j), pointsToAdd, edges, propertyValue);
                                                }

                                        }
                                }

                                //We have filled the input of our mesh. We can close our source.
                                sds.close();
                                Collections.sort(edges);
                                HydroTINBuilder mesh = new HydroTINBuilder();

                                //We actually fill the mesh
                                mesh.setPoints(pointsToAdd);
                                mesh.setConstraintEdges(edges);
                                if (inter) {
                                        //If needed, we use the intersection algorithm
                                        mesh.forceConstraintIntegrity();
                                }
                                //we process delaunay
                                mesh.processDelaunay();
                                if (flat) {
                                        //If needed, we remove flat triangles.
                                        mesh.removeFlatTriangles();
                                }

                                mesh.morphologicalQualification();

                                //And we write and register our results.
                                String edgesOut = name + "_edges";
                                String pointsOut = name + "_points";
                                String trianglesOut = name + "_triangles";

                                registerEdges(edgesOut, dsf, mesh);

                                registerPoints(pointsOut, dsf, mesh);

                                registerTriangles(trianglesOut, dsf, mesh);

                        }
                } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Failed to write the file containing the edges.\n", ex);


                } catch (DriverException ex) {
                        logger.log(Level.SEVERE, "Driver failure while saving the edges.\n", ex);


                } catch (DelaunayError ex) {
                        logger.log(Level.SEVERE, "Generation of the mesh failed.\n", ex);


                }

                return null;


        }

        @Override
        public String getName() {
                return "ST_HydroTIN";


        }

        @Override
        public String getDescription() {
                return "Compute a TIN based on constraints. "
                        + "Several options can be activated : flat triangle removal, intersection detection";


        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_HydroTIN(true, true, tinName) FROM source_table;";


        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                return null;


        }

        /**
         * The tables we need after the clause FROM in the query.
         * @return
         */
        @Override
        public TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY};


        }

        /**
         * Retrieve the arguments this function can take. We always need three arguments<br/><br/>
         *
         *
         * BOOLEAN : Flat triangles removal or not.<br/>
         * BOOLEAN : Intersection processing <br/>
         * BOOLEAN : Rules used or not  <br/>
         * STRING : Name of the TIN table<br/>
         *
         * @return
         */
        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.BOOLEAN, Argument.BOOLEAN, Argument.BOOLEAN, Argument.STRING)};


        }

        /**
         * We add a geometry to the given list
         * @param points
         * @param geom
         */
        private void addGeometry(Geometry geom, List<DPoint> pointsToAdd, List<DEdge> edges, int propertyValue) {
                if (geom instanceof Point) {
                        addPoint(pointsToAdd, (Point) geom, propertyValue);


                } else if (geom instanceof LineString) {
                        addGeometry(edges, geom, propertyValue);


                } else if (geom instanceof Polygon) {
                        addGeometry(edges, geom, propertyValue);


                }
        }

        /**
         * We add a point to the given list
         * @param points
         * @param geom
         */
        private void addPoint(List<DPoint> points, Point geom, int propertyValue) {
                try {
                        DPoint dPoint = TINFeatureFactory.createDPoint(geom.getCoordinate());
                        dPoint.setProperty(propertyValue);
                        points.add(dPoint);


                } catch (DelaunayError ex) {
                        logger.log(Level.SEVERE, "You're trying to create a 3D point with a NaN value.\n", ex);


                }

        }

        private void addGeometry(List<DEdge> edges, Geometry geometry, int propertyValue) {

                Coordinate c1 = geometry.getCoordinates()[0];
                Coordinate c2;
                Coordinate[] coords = geometry.getCoordinates();


                int count = coords.length;

                for (int k = 1; k < count; k++) {
                        c2 = coords[k];
                        try {
                                DEdge edge = new DEdge(new DPoint(c1), new DPoint(c2));
                                edge.setProperty(propertyValue);
                                edges.add(new DEdge(new DPoint(c1), new DPoint(c2)));
                        } catch (DelaunayError d) {
                                logger.log(Level.SEVERE, "You're trying to craete a 3D point with a NaN value.\n", d);


                        }
                        c1 = c2;


                }
        }

        /**
         * Saves the edges in a file, and register them with the dsf.
         * @param name
         * @param dsf
         * @param mesh
         * @throws IOException
         * @throws DriverException
         */
        private void registerEdges(String name, DataSourceFactory dsf, ConstrainedMesh mesh) throws IOException, DriverException {
                File out = new File(name + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.FLOAT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID, TINSchema.STARTPOINT_NODE_FIELD, TINSchema.ENDPOINT_NODE_FIELD, TINSchema.LEFT_TRIANGLE_FIELD, TINSchema.RIGHT_TRIANGLE_FIELD,
                                TINSchema.HEIGHT_FIELD, TINSchema.PROPERTY_FIELD, TINSchema.GID_SOURCE_FIELD});


                int triangleCount = mesh.getEdges().size();
                writer.writeMetadata(triangleCount, md);
                GeometryFactory gf = new GeometryFactory();


                for (DEdge dt : mesh.getEdges()) {
                        Coordinate[] coords = new Coordinate[2];
                        coords[0] = dt.getPointLeft().getCoordinate();
                        coords[1] = dt.getPointRight().getCoordinate();
                        CoordinateSequence cs = new CoordinateArraySequence(coords);

                        LineString mp = new LineString(cs, gf);
                        writer.addValues(new Value[]{ValueFactory.createValue(mp),
                                        ValueFactory.createValue(dt.getGID()),
                                        ValueFactory.createValue(dt.getStartPoint().getGID()),
                                        ValueFactory.createValue(dt.getEndPoint().getGID()),
                                        ValueFactory.createValue(dt.getLeft() == null ? -1 : dt.getLeft().getGID()),
                                        ValueFactory.createValue(dt.getRight() == null ? -1 : dt.getRight().getGID()),
                                        ValueFactory.createValue(dt.getHeight()),
                                        ValueFactory.createValue(dt.getProperty()),
                                        ValueFactory.createValue(dt.getExternalGID()),});
                }
                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(name), out);
        }

        private void registerPoints(String name, DataSourceFactory dsf, ConstrainedMesh mesh) throws IOException, DriverException {
                File out = new File(name + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.FLOAT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID,
                                TINSchema.HEIGHT_FIELD, TINSchema.PROPERTY_FIELD, TINSchema.GID_SOURCE_FIELD});


                int triangleCount = mesh.getPoints().size();
                writer.writeMetadata(triangleCount, md);
                GeometryFactory gf = new GeometryFactory();


                for (DPoint dt : mesh.getPoints()) {
                        Coordinate[] coords = new Coordinate[1];
                        coords[0] = dt.getCoordinate();
                        CoordinateSequence cs = new CoordinateArraySequence(coords);
                        Point mp = new Point(cs, gf);

                        writer.addValues(new Value[]{ValueFactory.createValue(mp),
                                        ValueFactory.createValue(dt.getGID()),
                                        ValueFactory.createValue(dt.getHeight()),
                                        ValueFactory.createValue(dt.getProperty()),
                                        ValueFactory.createValue(dt.getExternalGID())});


                }

                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(name), out);


        }

        private void registerTriangles(String name, DataSourceFactory dsf, ConstrainedMesh mesh) throws IOException, DriverException {
                File out = new File(name + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.FLOAT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT)},
                        new String[]{"the_geom", TINSchema.GID,
                                TINSchema.HEIGHT_FIELD, TINSchema.PROPERTY_FIELD, TINSchema.GID_SOURCE_FIELD,
                                TINSchema.EDGE_0_GID_FIELD, TINSchema.EDGE_1_GID_FIELD, TINSchema.EDGE_2_GID_FIELD});


                int triangleCount = mesh.getTriangleList().size();
                writer.writeMetadata(triangleCount, md);
                GeometryFactory gf = new GeometryFactory();


                for (DTriangle dt : mesh.getTriangleList()) {
                        Coordinate[] coords = new Coordinate[4];
                        coords[0] = dt.getPoint(0).getCoordinate();
                        coords[1] = dt.getPoint(1).getCoordinate();
                        coords[2] = dt.getPoint(2).getCoordinate();
                        coords[3] = dt.getPoint(0).getCoordinate();
                        CoordinateSequence cs = new CoordinateArraySequence(coords);
                        LinearRing lr = new LinearRing(cs, gf);
                        Polygon poly = new Polygon(lr, null, gf);
                        MultiPolygon mp = new MultiPolygon(new Polygon[]{poly}, gf);

                        writer.addValues(new Value[]{ValueFactory.createValue(mp),
                                        ValueFactory.createValue(dt.getGID()),
                                        ValueFactory.createValue(dt.getHeight()),
                                        ValueFactory.createValue(dt.getProperty()),
                                        ValueFactory.createValue(dt.getExternalGID()),
                                        ValueFactory.createValue(dt.getEdge(0).getGID()),
                                        ValueFactory.createValue(dt.getEdge(1).getGID()),
                                        ValueFactory.createValue(dt.getEdge(2).getGID())});
                }
                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(name), out);

        }
}
