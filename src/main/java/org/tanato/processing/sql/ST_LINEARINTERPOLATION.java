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
import com.vividsolutions.jts.geom.MultiLineString;
import org.gdms.data.DataSourceFactory;
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

        private GeometryFactory gf = new GeometryFactory();

        @Override
        public final Value evaluate(DataSourceFactory dsf, Value... values) throws FunctionException {
                Geometry geom = values[0].getAsGeometry();
                if (geom instanceof MultiLineString) {
                        int nbGeom = geom.getNumGeometries();
                        LineString[] lines = new LineString[nbGeom];
                        for (int i = 0; i < nbGeom; i++) {
                                LineString subGeom = (LineString) geom.getGeometryN(i);
                                double startz = ((LineString) subGeom).getStartPoint().getCoordinates()[0].z;
                                double endz = ((LineString) subGeom).getEndPoint().getCoordinates()[0].z;
                                double length = subGeom.getLength();
                                subGeom.apply(new LinearZInterpolationFilter(startz, endz, length));
                                lines[i]=subGeom;

                        }
                        geom = gf.createMultiLineString(lines);

                } else if (geom instanceof LineString) {
                        double startz = ((LineString) geom).getStartPoint().getCoordinates()[0].z;
                        double endz = ((LineString) geom).getEndPoint().getCoordinates()[0].z;
                        double length = geom.getLength();
                        geom.apply(new LinearZInterpolationFilter(startz, endz, length));
                }
                return ValueFactory.createValue(geom);

        }

        @Override
        public final String getName() {
                return "ST_LINEARINTERPOLATION";
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
                return "Update the z coordinates of a geometry based on a linear interpolation between first and last coordinates.";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_LINEARINTERPOLATION(the_geom) FROM table";
        }

        @Override
        public final Arguments[] getFunctionArguments() {
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
                        } else if (i == seqSize) {
                                done = true;
                        } else {
                                Coordinate coord = seq.getCoordinate(i);
                                Coordinate previousCoord = seq.getCoordinate(i - 1);
                                sumLenght += coord.distance(previousCoord);
                                seq.setOrdinate(i, 2, startZ + dZ * sumLenght / length);
                        }

                }

		@Override
                public boolean isGeometryChanged() {
                        return true;
                }

		@Override
                public boolean isDone() {
                        return done;
                }
        }
}
