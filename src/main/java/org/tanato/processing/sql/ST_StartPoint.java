package org.tanato.processing.sql;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class ST_StartPoint implements Function {

        public String getName() {
                return "ST_StartPoint";
        }

        public Type getType(Type[] types) {
                return TypeFactory.createType(Type.GEOMETRY);
        }

        public boolean isAggregate() {
                return false;
        }

        public String getDescription() {
                return "Returns the first point of a LINESTRING geometry as a POINT or NULL if the input parameter is not a LINESTRING.";
        }

        public String getSqlOrder() {
                return "select ST_StartPoint(geometry geomA) from myTable;";
        }

        @Override
        public Value evaluate(DataSourceFactory dsf, Value... args) throws FunctionException {
                final Geometry g = args[0].getAsGeometry();
                if (g instanceof LineString) {
                        LineString line = (LineString) g;
                        ValueFactory.createValue(line.getStartPoint());
                }
                return ValueFactory.createNullValue();
        }

        public Value getAggregateResult() {
                return null;
        }

        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY)};
        }
}
