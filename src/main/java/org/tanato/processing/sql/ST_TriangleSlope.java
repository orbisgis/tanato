/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tanato.processing.sql;

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
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.tanato.factory.TINFeatureFactory;

/**
 *
 * @author ebocher
 */
public class ST_TriangleSlope implements Function{

        @Override
        public Value evaluate(DataSourceFactory dsf, Value... args) throws FunctionException {
                
                Value val = args[0];
                Geometry geom = val.getAsGeometry();
                try{
                        DTriangle triangle = TINFeatureFactory.createDTriangle(geom);
                        double slope = triangle.getSlopeInPercent();
                        Value ret = ValueFactory.createValue(slope);
                        return ret;
                        
                } catch (DelaunayError d){
                        Logger.getLogger(ST_TINSlopeDirection.class.getName()).log(Level.SEVERE, null, d);
                }
                return ValueFactory.createNullValue();

        }

        @Override
        public String getName() {
                return "ST_TriangleSlope";
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
        public Type getType(Type[] argsTypes) throws InvalidTypeException {
                return TypeFactory.createType(Type.DOUBLE);
        }

        @Override
        public String getDescription() {
                return "Compute the slope of triangles in an TIN expresed in percent.";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_TriangleSlope(the_geom) FROM tin;" ;
        }

        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY)};
        }

}
