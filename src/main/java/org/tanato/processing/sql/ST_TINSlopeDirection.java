/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
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

/**
 *
 * @author ebocher
 */
public class ST_TINSlopeDirection implements Function {

        GeometryFactory gf = new GeometryFactory();

        @Override
        public Value evaluate(DataSourceFactory dsf, Value... values) throws FunctionException {
                try {
                        Geometry geom = values[0].getAsGeometry();
                        DTriangle dTriangle = TINFeatureFactory.createDTriangle(geom);
                        DPoint pointIntersection = dTriangle.getSteepestIntersectionPoint(dTriangle.getBarycenter());
                        if (pointIntersection != null) {
                                return ValueFactory.createValue(gf.createLineString(new Coordinate[]{dTriangle.getBarycenter().getCoordinate(), dTriangle.getSteepestIntersectionPoint(dTriangle.getBarycenter()).getCoordinate()}));

                        }


                } catch (DelaunayError ex) {
                        Logger.getLogger(ST_TINSlopeDirection.class.getName()).log(Level.SEVERE, null, ex);
                }
                return ValueFactory.createNullValue();

        }

        @Override
        public String getName() {
                return "ST_TINSlopeDirection";
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
                return "Compute the steepest vector director for a triangle";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_TINSlopeDirection(the_geom) FROM table";
        }

        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY)};

        }
}
