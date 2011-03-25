/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Geometry;
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
public class ST_LINEARINTERPOLATION implements Function {

        GeometryFactory gf = new GeometryFactory();

        @Override
        public Value evaluate(DataSourceFactory dsf, Value... values) throws FunctionException {
                Geometry geom = values[0].getAsGeometry();
                int nbGeom = geom.getNumGeometries();

                for (int i = 0; i < nbGeom; i++) {
                        Geometry subGeom = geom.getGeometryN(i);
                        if (subGeom instanceof LineString) {
                                double startz = ((LineString) subGeom).getStartPoint().getCoordinates()[0].z;
                                double endz = ((LineString) subGeom).getEndPoint().getCoordinates()[0].z;
                                double length = subGeom.getLength();
                                subGeom.apply(new LinearZInterpolationFilter(startz, endz, length));

                        }

                }


                return ValueFactory.createValue(geom);

        }

        @Override
        public String getName() {
                return "ST_LINEARINTERPOLATION";
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
                return "Update the z coordinates of a geometry based on a linear interpolation between first and last coordinates.";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_LINEARINTERPOLATION(the_geom) FROM table";
        }

        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY)};

        }

        private class LinearZInterpolationFilter implements CoordinateSequenceFilter {

                private boolean done = false;
                private double startZ = 0;
                private double endZ = 0;
                private double dZ = 0;
                private final double length;
                private int seqSize = 0;
                private double sumLenght = 0;

                public LinearZInterpolationFilter(double startZ, double endZ, double length) {
                        this.startZ = startZ;
                        this.endZ = endZ;
                        this.length = length;

                }

                @Override
                public void filter(CoordinateSequence seq, int i) {
                        if (i == 0) {
                                seqSize = seq.size();
                                dZ = endZ - startZ;
                        } else if (i == seqSize){
                                done =true;
                        }
                        else {
                                Coordinate coord = seq.getCoordinate(i);
                                Coordinate previousCoord = seq.getCoordinate(i - 1);
                                sumLenght += coord.distance(previousCoord);
                                seq.setOrdinate(i, 2, startZ + dZ * sumLenght / length);
                        }
                       
                }

                public boolean isGeometryChanged() {
                        return true;
                }

                public boolean isDone() {
                        return done;
                }
        }
}
