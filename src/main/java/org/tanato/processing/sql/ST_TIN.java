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
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.gdms.sql.function.FunctionException;
import org.gdms.data.DataSource;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.schema.MetadataUtilities;
import org.gdms.data.types.Dimension3DConstraint;
import org.gdms.data.types.GeometryTypeConstraint;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.gdms.driver.gdms.GdmsWriter;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.executor.AbstractExecutorFunction;
import org.gdms.sql.function.executor.ExecutorFunctionSignature;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.orbisgis.progress.ProgressMonitor;
import org.tanato.model.TINSchema;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a constrained delaunay triangulation from the geometry given in input.
 *
 * 
 * @author alexis
 */
public class ST_TIN extends AbstractExecutorFunction {

        @Override
        public final void evaluate(SQLDataSourceFactory dsf, DataSet[] tables,
                Value[] values, ProgressMonitor pm) throws FunctionException {
                //We need to read our source.
                DataSet sds = tables[0];
                long count = 0;

                //To simplify the use of st_tin the user can execute only st_tin() without any arguments.
                //Consequently some parameters are filled by default.
                boolean inter = true;
                boolean flat = false;
                String name = sds instanceof DataSource ? ((DataSource) sds).getName() : dsf.getUID();
                int geomIndex = -1;
                try{
                        geomIndex = MetadataUtilities.getSpatialFieldIndex(sds.getMetadata());
                } catch (DriverException d){
                        throw new FunctionException("Can't find a geometry index", d);
                }
                if (values.length != 0) {
                        //We retrieve the values to know how we are supposed to proceed.
                        inter = values[0].getAsBoolean();
                        flat = values[1].getAsBoolean();
                }


                //We open the source
                try {
                        count = sds.getRowCount();
                } catch (DriverException ex) {
                        throw new FunctionException("There has been an error while opening the table, or counting its lines.\n", ex);
                }
                Geometry geom = null;
                //We prepare our input structures.
                List<DPoint> pointsToAdd = new ArrayList<DPoint>();
                ArrayList<DEdge> edges = new ArrayList<DEdge>();
                //We fill the input structures with our table.
                for (long i = 0; i < count; i++) {
                        try {
                                geom = sds.getGeometry(i,geomIndex);
                        } catch (DriverException ex) {
                                throw new FunctionException("Can't retrieve the  geometry.\n", ex);
                        }
                        if (geom instanceof Point) {
                                addPoint(pointsToAdd, (Point) geom);
                        } else if (geom instanceof MultiPoint) {
                                addMultiPoint(pointsToAdd, (MultiPoint) geom);
                        } else if (geom instanceof GeometryCollection) {
                                addGeometryCollection(edges, (GeometryCollection) geom);
                        } else {
                                addGeometry(edges, geom);
                        }
                }
                //We have filled the input of our mesh. We can close our source.
                Collections.sort(edges);

                ConstrainedMesh mesh = new ConstrainedMesh();
                mesh.setVerbose(true);
                try {
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
                } catch (DelaunayError ex) {
                        throw new FunctionException("Generation of the mesh failed.\n", ex);
                }
                //And we write and register our results.
                String edgesOut = name + "_edges";
                String pointsOut = name + "_points";
                String trianglesOut = name + "_triangles";
                try {
                        registerEdges(edgesOut, dsf, mesh);
                } catch (IOException ex) {
                        throw new FunctionException("Failed to write the file containing the edges.\n", ex);
                } catch (DriverException ex) {
                        throw new FunctionException("Driver failure while saving the edges.\n", ex);
                }
                try {
                        registerPoints(pointsOut, dsf, mesh);
                } catch (IOException ex) {
                        throw new FunctionException("Failed to write the file containing the points.\n", ex);
                } catch (DriverException ex) {
                        throw new FunctionException("Driver failure while saving the points.\n", ex);
                }
                try {
                        registerTriangles(trianglesOut, dsf, mesh);
                } catch (IOException ex) {
                        throw new FunctionException("Failed to write the file containing the triangles.\n", ex);
                } catch (DriverException ex) {
                        throw new FunctionException("Driver failure while saving the triangles.\n", ex);
                }
        }

        @Override
        public final String getName() {
                return "ST_TIN";
        }

        @Override
        public final String getDescription() {
                return "Compute a TIN from the lines of the geometry given in argument.";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_TIN(false, true, tinName) FROM source_table;";
        }


        /**
         * Retrieve the the signature of this function. We need 0 or 2 arguments<br/>
         *<ul><li>
         * BOOLEAN : Flat triangles removal or not.</li>
         * <li>BOOLEAN : Intersection processing </li></ul>
         * @return
         */
        @Override
        public FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{
                        new ExecutorFunctionSignature(),
                        new ExecutorFunctionSignature(ScalarArgument.BOOLEAN, ScalarArgument.BOOLEAN)};
        }
        /**
         * We add a point to the given list
         * @param points
         * @param geom
         * @throws FunctionException
         */
        private void addPoint(List<DPoint> points, Point geom) throws FunctionException {
                Coordinate pt = geom.getCoordinate();
                double z = Double.isNaN(pt.z) ? 0 : pt.z;
                try {
                        points.add(new DPoint(pt.x, pt.y, z));
                } catch (DelaunayError ex) {
                        throw new FunctionException("You're trying to create a 3D point with a NaN value.\n", ex);
                }

        }

        /**
         * Add a MultiPoint geometry.
         * @param points
         * @param pts
         * @throws FunctionException
         */
        private void addMultiPoint(List<DPoint> points, MultiPoint pts) throws FunctionException {
                Coordinate[] coords = pts.getCoordinates();
                for (int i = 0; i < coords.length; i++) {
                        try {
                                points.add(new DPoint(
                                                coords[i].x, 
                                                coords[i].y, 
                                                Double.isNaN(coords[i].z) ? 0 : coords[i].z));
                        } catch (DelaunayError ex) {
                                throw new FunctionException("You're trying to create a 3D point with a NaN value.\n", ex);
                        }
                }
        }

        /**
         * add a geometry to the input.
         * @param edges
         * @param geom
         * @throws FunctionException
         */
        private void addGeometry(List<DEdge> edges, Geometry geom) throws FunctionException {
                if (geom.isValid()) {
                        Coordinate c1 = geom.getCoordinates()[0];
                        c1.z = Double.isNaN(c1.z) ? 0 : c1.z;
                        Coordinate c2;
                        for (int k = 1; k < geom.getCoordinates().length; k++) {
                                c2 = geom.getCoordinates()[k];
                                c2.z = Double.isNaN(c2.z) ? 0 : c2.z;
                                try {
                                        edges.add(new DEdge(new DPoint(c1), new DPoint(c2)));
                                } catch (DelaunayError d) {
                                        throw new FunctionException("You're trying to create a 3D point with a NaN value.\n", d);
                                }
                                c1 = c2;
                        }
                }
        }

        /**
         * Add a GeometryCollection
         * @param edges
         * @param geomcol
         * @throws FunctionException
         */
        private void addGeometryCollection(List<DEdge> edges, GeometryCollection geomcol) throws FunctionException {
                int num = geomcol.getNumGeometries();
                for (int i = 0; i < num; i++) {
                        addGeometry(edges, geomcol.getGeometryN(i));
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
        private void registerEdges(final String name, final SQLDataSourceFactory dsf,
                final ConstrainedMesh mesh) throws IOException, DriverException {
                final String acName = dsf.getSourceManager().getUniqueName(name);
                File out = new File(dsf.getResultDir()+acName + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{
                                TypeFactory.createType(
                                        Type.GEOMETRY, 
                                        new GeometryTypeConstraint(GeometryTypeConstraint.LINESTRING), 
                                        new Dimension3DConstraint(3)
                                ),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID, TINSchema.STARTPOINT_NODE_FIELD, TINSchema.ENDPOINT_NODE_FIELD, TINSchema.LEFT_TRIANGLE_FIELD, TINSchema.RIGHT_TRIANGLE_FIELD
                        });
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
                                        ValueFactory.createValue(dt.getRight() == null ? -1 : dt.getRight().getGID())});
                }

                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                dsf.getSourceManager().register(acName, out);
        }

        private void registerPoints(final String name, final SQLDataSourceFactory dsf,
                final ConstrainedMesh mesh) throws IOException, DriverException {
                final String acName = dsf.getSourceManager().getUniqueName(name);
                File out = new File(dsf.getResultDir()+acName + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{
                                TypeFactory.createType(
                                        Type.GEOMETRY, 
                                        new GeometryTypeConstraint(GeometryTypeConstraint.POINT),
                                        new Dimension3DConstraint(Dimension3DConstraint.DIMENSION_3D)
                                ),
                                TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID});
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
                                        ValueFactory.createValue(dt.getExternalGID())});
                }

                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                dsf.getSourceManager().register(acName, out);
        }

        private void registerTriangles(final String name, final SQLDataSourceFactory dsf,
                final ConstrainedMesh mesh) throws IOException, DriverException {
                final String acName = dsf.getSourceManager().getUniqueName(name);
                File out = new File(dsf.getResultDir()+acName + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{
                                TypeFactory.createType(
                                        Type.GEOMETRY, 
                                        new GeometryTypeConstraint(GeometryTypeConstraint.POLYGON), 
                                        new Dimension3DConstraint(Dimension3DConstraint.DIMENSION_3D)),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID,
                                TINSchema.EDGE_0_GID_FIELD, TINSchema.EDGE_1_GID_FIELD, TINSchema.EDGE_2_GID_FIELD});

                int triangleCount = mesh.getTriangleList().size();
                writer.writeMetadata(triangleCount, md);
                GeometryFactory gf = new GeometryFactory();
                for (DTriangle dt : mesh.getTriangleList()) {
                        Coordinate[] coords = new Coordinate[DTriangle.PT_NB + 1];
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
