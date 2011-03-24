/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.preprocessing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Geometry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.InvalidTypeException;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.tanato.factory.TINFeatureFactory;

public class ST_SetZFromTriangle implements Function {

        @Override
        public Value evaluate(DataSourceFactory dsf, Value... args) throws FunctionException {
                try {
                        DTriangle dTriangle = TINFeatureFactory.createDTriangle(args[0].getAsGeometry());
                        Geometry geomToBeInterpolate = args[1].getAsGeometry();
                        geomToBeInterpolate.apply(new TINZFilter(dTriangle));

                        return ValueFactory.createValue(geomToBeInterpolate);


                } catch (DelaunayError ex) {
                        Logger.getLogger(ST_SetZFromTriangle.class.getName()).log(Level.SEVERE, null, ex);
                }

                return ValueFactory.createNullValue();
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
        public boolean isAggregate() {
                return false;
        }

        @Override
        public Value getAggregateResult() {
                return null;
        }

        @Override
        public Type getType(Type[] types) throws InvalidTypeException {
                return TypeFactory.createType(Type.GEOMETRY);
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
