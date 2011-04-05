/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.jhydrocell.hydronetwork.HydroProperties;

/**
 *
 * @author ebocher
 */
public class ST_GetHydroProperty implements Function {

        @Override
        public final Value evaluate(DataSourceFactory dsf, Value... values) throws FunctionException {
                return ValueFactory.createValue(HydroProperties.toString(values[0].getAsInt()));
        }

        @Override
        public final String getName() {
                return "ST_GetHydroProperty";
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
                return TypeFactory.createType(Type.STRING);
        }

        @Override
        public final String getDescription() {
                return "Get the hydro property as string";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_GetHydroProperty(propertyField) FROM table";
        }

        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.INT)};

        }
}
