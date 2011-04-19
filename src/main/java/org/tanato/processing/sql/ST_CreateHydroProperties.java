/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * @author erwan, kwyhr
 */
public class ST_CreateHydroProperties implements Function {
        // Logger access

        private static final Logger logger = Logger.getLogger(DropletFollower.class.getName());
        // informations for decoding
        private String theString = null;
        private int length = 0;
        private int position = 0;
        private int error = 0;

        @Override
        public final Value evaluate(DataSourceFactory dsf, Value... values) throws FunctionException {
                int returnedValue = 0;
                if (values.length < 1) {
                        // There MUST be at least 1 value
                        throw new FunctionException("need string to process.");
                } else {
                        // Get value
                        theString = values[1].getAsString();

                        // Initialize
                        length = theString.length();
                        position = 0;
                        error = 0;

                        // Do like if we were adding next element
                        String operator = "+";
                        String keyWord = getNextKeyword();
                        while ((keyWord.length() > 0) && (error == 0)) {
                                // Convert value and process operation
                                int value = convertToInt(keyWord);
                                if (operator.equals("+")) {
                                        // add property
                                        returnedValue = returnedValue | value;
                                } else if (operator.equals("-")) {
                                        // remove property (add it then remove value)
                                        returnedValue = (returnedValue | value) - value;
                                }
                                operator = getNextOperator();
                                keyWord = getNextKeyword();
                        }
                }
                return ValueFactory.createValue(returnedValue);
        }

        @Override
        public final String getName() {
                return "ST_CreateHydroProperties";
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
                return TypeFactory.createType(Type.INT);
        }

        @Override
        public final String getDescription() {
                return "Combine the hydro property as int";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_CreateHHydroProperties(propertyField)";
        }

        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.STRING)};

        }

        // -----------------------------------------------------------
        /**
         * Get next keywork from the string
         * @return next keyword
         */
        private String getNextKeyword() {
                // Skip unsignifiant characters
                boolean found = false;
		StringBuilder sb = new StringBuilder();
                while ((position < length) && (!found) && (error == 0)) {
                        char theChar = theString.charAt(position);
                        if (Character.isLetterOrDigit(theChar)) {
                                found = true;
                        } else if (theChar == '_') {
                                found = true;
                        } else if (theChar != ' ') {
                                error = 1;
                        } else {
                                position++;
                        }
                }

                // save characters
                found = false;
                while ((position < length) && (!found) && (error == 0)) {
                        char theChar = theString.charAt(position);
                        if (Character.isLetterOrDigit(theChar)) {
                                // Uppercase character when saving it
                                sb.append(Character.toUpperCase(theChar));
                                position++;
                        } else if (theChar == '_') {
                                sb.append('_');
                                position++;
                        } else {
                                found = true;
                        }
                }
                return sb.toString();
        }

        /**
         * Get next operator from the string
         * @return the operator
         */
        private String getNextOperator() {
                String theOperator = "";

                // Skip unsignifiant characters, keep + or - operator
                boolean found = false;
                while ((position < length) && (!found) && (error == 0)) {
                        char theChar = theString.charAt(position);
                        if (theChar == '+') {
                                // Add value
                                theOperator = "+";
                                found = true;
                        } else if (theChar == '-') {
                                // remove value
                                theOperator = "-";
                                found = true;
                        } else if (theChar != ' ') {
                                // we can only find spaces between a keyword and an operator
                                error = 1;
                        }
                        position++;
                }

                return theOperator;
        }

        /**
         * Convert string to its equivalent inside HydroProperties
         * Uses reflection to be sure it works without redefining the function
         * Also convert ALL to -1 and NONE to 0
         * @param theString
         * @return value in HydroProperties
         */
        private int convertToInt(String theString) {
                int returnedValue = 0;

                if (theString.equals("ALL")) {
                        returnedValue = -1;
                } else if (theString.equals("NONE")) {
                        returnedValue = 0;
                } else {
                        // Get all possible fields
                        Field[] theList = HydroProperties.class.getFields();

                        // Check all values until we find it
                        int nbProperties = theList.length;
                        boolean found = false;
                        int i = 0;
                        Field field = null;
                        while ((i < nbProperties) && (!found)) {
                                field = theList[i];
                                if (field.getName().equals(theString)) {
                                        found = true;
                                } else {
                                        i++;
                                }
                        }
                        if (found) {
                                // If found is true, field is defined
                                try {
                                        returnedValue = field.getInt(field);
                                } catch (IllegalArgumentException ex) {
                                        logger.log(Level.SEVERE, null, ex);
                                } catch (IllegalAccessException ex) {
                                        logger.log(Level.SEVERE, null, ex);
                                }
                        }
                }

                return returnedValue;
        }
}
