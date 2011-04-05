package org.tanato.processing.sql;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Constraint;
import org.gdms.data.types.DimensionConstraint;
import org.gdms.data.types.InvalidTypeException;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.geometryUtils.CoordinatesUtils;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.gdms.sql.function.Function;
import org.gdms.sql.function.FunctionException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class ST_SetZToExtremities implements Function {

	@Override
        public final Value evaluate(DataSourceFactory dsf, Value[] args) throws FunctionException {

                Geometry geom = args[0].getAsGeometry();
                double startZ = args[1].getAsDouble();
                double endZ = args[2].getAsDouble();
                if (geom instanceof MultiLineString) {
                        int nbGeom = geom.getNumGeometries();
                        LineString[] lines = new LineString[nbGeom];
                        for (int i = 0; i < nbGeom; i++) {
                                Geometry subGeom = geom.getGeometryN(i);
                                lines[i] = (LineString) CoordinatesUtils.force_3DStartEnd(subGeom, startZ, endZ);
                        }

                } else if (geom instanceof LineString) {
                        geom = CoordinatesUtils.force_3DStartEnd(geom, startZ, endZ);
                }


                return ValueFactory.createValue(geom);

        }

	@Override
        public final String getDescription() {
                return "This function modify (or set) the z component of each vertex extremities lines"
                        + " given by a two fields.";
        }

	@Override
        public final Arguments[] getFunctionArguments() {

                return new Arguments[]{new Arguments(Argument.GEOMETRY,
                                Argument.NUMERIC, Argument.NUMERIC)};
        }

	@Override
        public final String getName() {
                return "ST_SetZToExtremities";
        }

	@Override
        public final String getSqlOrder() {
                return "select ST_SetZToExtremities(the_geom, startz, endz) from lines;";
        }

	@Override
        public final Type getType(Type[] argsTypes) {

                Type type = argsTypes[0];
                Constraint[] constrs = type.getConstraints(Constraint.ALL
                        & ~Constraint.GEOMETRY_DIMENSION);
                Constraint[] result = new Constraint[constrs.length + 1];
                System.arraycopy(constrs, 0, result, 0, constrs.length);
                result[result.length - 1] = new DimensionConstraint(3);

                return TypeFactory.createType(type.getTypeCode(), result);

        }

	@Override
        public final boolean isAggregate() {
                return false;
        }

	@Override
        public final Value getAggregateResult() {
                return null;
        }
}
