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
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.DimensionConstraint;
import org.gdms.data.types.GeometryConstraint;
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

        @Override
        public final ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
                Value[] values, IProgressMonitor pm) throws ExecutionException {

                try {
                        //Process contourlines
                        DataSource ds = tables[0];
                        //We need to read our source.
                        SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(ds);
                        sds.open();
                        HashMap<Integer, Integer> weights = new HashMap<Integer, Integer>();

                        int propertyIndex = sds.getFieldIndexByName(TINSchema.PROPERTY_FIELD);
                        int heightIndex = sds.getFieldIndexByName(TINSchema.HEIGHT_FIELD);
                        int weigthIndex = sds.getFieldIndexByName(TINSchema.WEIGTH_FIELD);
                        int gidIndex = sds.getFieldIndexByName(TINSchema.GID);


                        if ((propertyIndex == -1) || (heightIndex == -1)) {
                                throw new IllegalArgumentException("The table must contains a property and height fields");
                        }

                        boolean useTriangulationRules = false;
                        boolean inter = true;
                        boolean flat = true;
                        if (values.length != 0) {
                                //We retrieve the values to know how we are supposed to proceed.
                                inter = values[0].getAsBoolean();
                                flat = values[1].getAsBoolean();
                                useTriangulationRules = values[2].getAsBoolean();
                                if (useTriangulationRules) {
                                        if ((weigthIndex == -1)) {
                                                throw new IllegalArgumentException("The table must contains a weight field that defines rules weight.");
                                        }
                                }
                        }
                        String name = ds.getName();
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
                                double heightValue = sds.getFieldValue(i, heightIndex).getAsDouble();
                                propertyValue = sds.getFieldValue(i, propertyIndex).getAsInt();
                                if (gidIndex != -1) {
                                        int gidValue = sds.getFieldValue(i, gidIndex).getAsInt();
                                }
                                //If rules is used then get property and weight
                                if (useTriangulationRules) {
                                        int weight = sds.getFieldValue(i, weigthIndex).getAsInt();
                                        weights.put(propertyValue, weight);
                                        notSewer = HydroProperties.check(propertyValue, HydroProperties.SEWER);
                                }

                                //Dot no take into account sewer in the triangulation
                                if (!notSewer) {
                                        if (geom instanceof GeometryCollection) {
                                                final int nbOfGeometries = geom.getNumGeometries();

                                                for (int j = 0; j < nbOfGeometries; j++) {
                                                        addGeometry(geom.getGeometryN(j), pointsToAdd, edges, propertyValue, heightValue, gidIndex);
                                                }

                                        }
                                }
                        }

                        //We have filled the input of our mesh. We can close our source.
                        sds.close();
                        Collections.sort(edges);
                        HydroTINBuilder mesh = new HydroTINBuilder();
                        if (useTriangulationRules) {
                                mesh.setWeights(weights);
                        }

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


                } catch (IOException ex) {
                        throw new ExecutionException("Failed to write the file containing the edges.\n", ex);
                } catch (DriverException ex) {
                        throw new ExecutionException("Driver failure while saving the edges.\n", ex);
                } catch (DelaunayError ex) {
                        throw new ExecutionException("Generation of the mesh failed.\n", ex);
                }

                return null;


        }

        @Override
        public final String getName() {
                return "ST_HydroTIN";


        }

        @Override
        public final String getDescription() {
                return "Compute a TIN based on constraints. "
                        + "Several options can be activated : flat triangle removal, intersection detection";


        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_HydroTIN(true, true, false) FROM source_table;";


        }

        @Override
        public final Metadata getMetadata(Metadata[] tables) throws DriverException {
                return null;


        }

        /**
         * The tables we need after the clause FROM in the query.
         * @return
         */
        @Override
        public final TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY};


        }

        /**
         * Retrieve the arguments this function can take. We always need three arguments<br/><br/>
         *
         *
         * BOOLEAN : Flat triangles removal or not.<br/>
         * BOOLEAN : Intersection processing <br/>
         * BOOLEAN : Rules used or not  <br/>
         *
         * @return
         */
        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(), new Arguments(Argument.BOOLEAN, Argument.BOOLEAN, Argument.BOOLEAN)};


        }

        /**
         * We add a geometry to the given list
         * @param points
         * @param geom
         */
        private void addGeometry(Geometry geom, List<DPoint> pointsToAdd, List<DEdge> edges, int propertyValue,
                double height, int gidSource) throws ExecutionException {
                if (geom instanceof Point) {
                        addPoint(pointsToAdd, (Point) geom, propertyValue, height, gidSource);

                } else if (geom instanceof LineString) {
                        addGeometry(edges, geom, propertyValue, height, gidSource);

                } else if (geom instanceof Polygon) {
                        addGeometry(edges, geom, propertyValue, height, gidSource);


                }
        }

        /**
         * We add a point to the given list
         * @param points
         * @param geom
         */
        private void addPoint(List<DPoint> points, Point geom, int propertyValue, double height,
                int gidSource) throws ExecutionException {
                try {
                        DPoint dPoint = TINFeatureFactory.createDPoint(geom.getCoordinate());
                        dPoint.setProperty(propertyValue);
                        dPoint.setExternalGID(gidSource);
                        dPoint.setHeight(height);
                        points.add(dPoint);


                } catch (DelaunayError ex) {
                        throw new ExecutionException("You're trying to create a 3D point with a NaN value.\n", ex);
                }

        }

        private void addGeometry(List<DEdge> edges, Geometry geometry, int propertyValue,
                double height, int gidSource) throws ExecutionException {

                Coordinate c1 = geometry.getCoordinates()[0];
                Coordinate c2;
                Coordinate[] coords = geometry.getCoordinates();


                int count = coords.length;

                for (int k = 1; k < count; k++) {
                        c2 = coords[k];
                        try {
                                DEdge edge = new DEdge(new DPoint(c1), new DPoint(c2));
                                edge.setProperty(propertyValue);
                                edge.setExternalGID(gidSource);
                                edge.setHeight(height);
                                edges.add(edge);
                        } catch (DelaunayError d) {
                                throw new ExecutionException("You're trying to craete a 3D point with a NaN value.\n", d);
                        }
                        c1 = c2;


                }
        }

        /**
         * Saves the edges in a file, and register them with the dsf.
         * @param acName
         * @param dsf
         * @param mesh
         * @throws IOException
         * @throws DriverException
         */
        private void registerEdges(final String name, final DataSourceFactory dsf,
                final ConstrainedMesh mesh) throws IOException, DriverException {
                final String acName = dsf.getSourceManager().getUniqueName(name);
                File out = new File(acName + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY, new GeometryConstraint(
                                GeometryConstraint.LINESTRING), new DimensionConstraint(3)),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.FLOAT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD,
                                TINSchema.GID,
                                TINSchema.STARTPOINT_NODE_FIELD,
                                TINSchema.ENDPOINT_NODE_FIELD,
                                TINSchema.LEFT_TRIANGLE_FIELD,
                                TINSchema.RIGHT_TRIANGLE_FIELD,
                                TINSchema.HEIGHT_FIELD,
                                TINSchema.PROPERTY_FIELD,
                                TINSchema.GID_SOURCE_FIELD});


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
                dsf.getSourceManager().register(acName, out);
        }

        private void registerPoints(final String name, final DataSourceFactory dsf,
                final ConstrainedMesh mesh) throws IOException, DriverException {
                final String acName = dsf.getSourceManager().getUniqueName(name);
                File out = new File(acName + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY, new GeometryConstraint(
                                GeometryConstraint.POINT), new DimensionConstraint(3)),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.FLOAT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),},
                        new String[]{TINSchema.GEOM_FIELD,
                                TINSchema.GID,
                                TINSchema.HEIGHT_FIELD,
                                TINSchema.PROPERTY_FIELD,
                                TINSchema.GID_SOURCE_FIELD});


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
                dsf.getSourceManager().register(acName, out);


        }

        private void registerTriangles(final String name, final DataSourceFactory dsf,
                final ConstrainedMesh mesh) throws IOException, DriverException {
                final String acName = dsf.getSourceManager().getUniqueName(name);
                File out = new File(acName + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY, new GeometryConstraint(
                                GeometryConstraint.POLYGON), new DimensionConstraint(3)),
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
                dsf.getSourceManager().register(acName, out);

        }
}
