/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.indexes.DefaultSpatialIndexQuery;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.generic.GenericObjectDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Arguments;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.factory.TINFeatureFactory;

public class ST_SetZFromTriangles implements CustomQuery {

        @Override
        public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {


                DataSource ds = tables[0];
                //We need to read our source.
                SpatialDataSourceDecorator sdsTriangle = new SpatialDataSourceDecorator(ds);

                ds = tables[1];
                //We need to read our source.
                SpatialDataSourceDecorator sdsToBeInterpolate = new SpatialDataSourceDecorator(ds);

                try {
                        sdsTriangle.open();
                        sdsToBeInterpolate.open();

                        GenericObjectDriver driver = new GenericObjectDriver(sdsToBeInterpolate.getMetadata());
                        long countToBeInterpol = sdsToBeInterpolate.getRowCount();
                        String geomField = sdsTriangle.getSpatialFieldName();
                        int geomIndex = sdsTriangle.getSpatialFieldIndex();
                        Geometry geomToBeInterpol;
                        Value[] valuesToBeInterpolate;
                        for (int i = 0; i < countToBeInterpol; i++) {

                                valuesToBeInterpolate = sdsToBeInterpolate.getRow(i);
                                geomToBeInterpol = sdsToBeInterpolate.getGeometry(i);
                                DefaultSpatialIndexQuery query = new DefaultSpatialIndexQuery(geomToBeInterpol.getEnvelopeInternal(), geomField);
                                Iterator<Integer> it = sdsTriangle.queryIndex(query);
                                ArrayList<DTriangle> triangles = new ArrayList<DTriangle>();
                                while (it.hasNext()) {
                                        Integer index = it.next();
                                        Geometry g = sdsTriangle.getGeometry(index);
                                        DTriangle dTriangle = TINFeatureFactory.createDTriangle(g);
                                        triangles.add(dTriangle);
                                }
                                Geometry result = (Geometry) geomToBeInterpol.clone();
                                result.apply(new TINZFilter(triangles));

                                valuesToBeInterpolate[geomIndex] = ValueFactory.createValue(result);
                                driver.addValues(valuesToBeInterpolate);

                        }
                        sdsTriangle.close();
                        sdsToBeInterpolate.close();

                        return driver;

                } catch (DriverException ex) {
                        Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);

                } catch (DelaunayError ex) {
                        Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);
                }
                return  null;
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                return tables[1];
        }

        @Override
        public TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};
        }

        private class TINZFilter implements CoordinateSequenceFilter {

                boolean done = false;
                private final ArrayList<DTriangle> triangles;

                public TINZFilter(ArrayList<DTriangle> triangles) {
                        this.triangles = triangles;
                }

                @Override
                public void filter(CoordinateSequence seq, int i) {
                        Coordinate coord = seq.getCoordinate(i);
                        seq.setOrdinate(i, 2, getZFromTriangles(coord, triangles));
                        if (i == seq.size()) {
                                done = true;
                        }
                }

                public boolean isGeometryChanged() {
                        return true;
                }

                public boolean isDone() {
                        return done;
                }

                private double getZFromTriangles(Coordinate coord, ArrayList<DTriangle> triangles) {
                        try {
                                coord.z = 0;
                                for (DTriangle dTriangle : triangles) {
                                        DPoint dPoint = TINFeatureFactory.createDPoint(coord);
                                        if (dTriangle.isInside(dPoint)) {
                                                return dTriangle.interpolateZ(TINFeatureFactory.createDPoint(coord));

                                        }
                                }

                        } catch (DelaunayError ex) {
                                Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return Double.NaN;
                }
        }

        @Override
        public String getName() {
                return "ST_SetZFromTriangles";
        }

        @Override
        public String getDescription() {
                return "Set a z to the geometry based on triangle interpolation";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_SetZFromTriangles() FROM triangles, tableTobeInterpolate";
        }

        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments()};

        }
}
