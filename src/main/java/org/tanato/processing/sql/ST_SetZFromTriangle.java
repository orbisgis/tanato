/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Geometry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.InvalidTypeException;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.factory.TINFeatureFactory;

public class ST_SetZFromTriangle implements CustomQuery {

        
        @Override
        public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {

              try {
                        DTriangle dTriangle = TINFeatureFactory.createDTriangle(values[0].getAsGeometry());
                        Geometry geomToBeInterpolate = values[1].getAsGeometry();
                        geomToBeInterpolate.apply(new TINZFilter(dTriangle));

                       


                } catch (DelaunayError ex) {
                        Logger.getLogger(ST_SetZFromTriangle.class.getName()).log(Level.SEVERE, null, ex);
                }
              return null;
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public TableDefinition[] getTablesDefinitions() {
                throw new UnsupportedOperationException("Not supported yet.");
        }

        private class TINZFilter implements CoordinateSequenceFilter {

                boolean done = false;
                private final DTriangle dTriangle;

                public TINZFilter(DTriangle dTriangle) {
                        this.dTriangle = dTriangle;
                }

                @Override
                public void filter(CoordinateSequence seq, int i) {
                        Coordinate coord = seq.getCoordinate(i);
                        double x = coord.x;
                        double y = coord.y;
                        double z = coord.z;
                        try {
                                DPoint dPoint = TINFeatureFactory.createDPoint(coord);
                                if (dTriangle.isInside(dPoint)) {
                                        z = dTriangle.interpolateZ(TINFeatureFactory.createDPoint(coord));

                                }
                        } catch (DelaunayError ex) {
                                Logger.getLogger(ST_SetZFromTriangle.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        seq.setOrdinate(i, 0, x);
                        seq.setOrdinate(i, 1, y);
                        seq.setOrdinate(i, 2, z);
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
        }

        @Override
        public String getName() {
                return "ST_SetZFromTriangle";
        }

        @Override
        public String getDescription() {
                return "Set a z to the geometry based on triangle interpolation";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_SetZFromTriangle(triangleGeom, the_geom) FROM table";
        }

        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY, Argument.GEOMETRY)};

        }
}
