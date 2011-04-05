/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
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

/**
 *
 * @author ebocher
 */
public class ST_ParalleleLine implements Function {

        private GeometryFactory gf = new GeometryFactory();

        @Override
        public final Value evaluate(DataSourceFactory dsf, Value... values) throws FunctionException {

                LineString geom = (LineString) values[0].getAsGeometry();

                return ValueFactory.createValue(getParallel(geom, values[1].getAsDouble()));

        }

        @Override
        public final String getName() {
                return "ST_ParalleleLine";
        }

        @Override
        public final boolean isAggregate() {
                return false;
        }

        @Override
        public final Value getAggregateResult() {
                return null;
        }

        @Override
        public final Type getType(Type[] types) {
                return TypeFactory.createType(Type.GEOMETRY);
        }

        @Override
        public final String getDescription() {
                return "Create a parallele line.";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_ParalleleLine(the_geom, 2) FROM table";
        }

        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY, Argument.NUMERIC)};

        }

        public final LineString getParallel(LineString line, double d) {
                Coordinate[] coords = line.getCoordinates();
                if (coords.length != 2) {
                        throw new IllegalArgumentException("You must have only two coordinates");
                } else {
                        double x0 = coords[0].x;
                        double y0 = coords[0].y;
                        double dx = coords[1].x - x0;
                        double dy = coords[1].y - y0;
                        double dd = Math.sqrt(dx * dx + dy * dy);
                        return gf.createLineString(new Coordinate[]{
                                        new Coordinate(x0 + dy * d / dd, y0 - dx * d / dd), new Coordinate(
                                        x0 + dx + dy * d / dd,
                                        y0 + dy - dx * d / dd)
                                });

                }

        }
}
