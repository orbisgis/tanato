/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.GeometryFactory;
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
import org.jhydrocell.hydronetwork.HydroProperties;

/**
 *
 * @author ebocher
 */
public class ST_GetHydroProperty implements Function {

        GeometryFactory gf = new GeometryFactory();

        @Override
        public Value evaluate(DataSourceFactory dsf, Value... values) throws FunctionException {
                return ValueFactory.createValue(HydroProperties.toString(values[0].getAsInt()));
        }

        @Override
        public String getName() {
                return "ST_GetHydroProperty";
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
                return TypeFactory.createType(Type.STRING);
        }

        @Override
        public String getDescription() {
                return "Get the hydro property as string";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_GetHydroProperty(propertyField) FROM table";
        }

        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.INT)};

        }
}
