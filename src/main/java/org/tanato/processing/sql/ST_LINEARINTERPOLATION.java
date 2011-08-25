/* 
 * TANATO  is a library dedicated to the modelling of water pathways based on 
 * triangulate irregular network. TANATO takes into account anthropogenic and 
 * natural artifacts to evaluate their impacts on the watershed response. 
 * It ables to compute watershed, main slope directions and water flow pathways.
 * 
 * This library has been originally created  by Erwan Bocher during his thesis 
 * “Impacts des activités humaines sur le parcours des écoulements de surface dans 
 * un bassin versant bocager : essai de modélisation spatiale. Application au 
 * Bassin versant du Jaudy-Guindy-Bizien (France)”. It has been funded by the 
 * Bassin versant du Jaudy-Guindy-Bizien and Syndicat d’Eau du Trégor.
 * 
 * The new version is developed at French IRSTV institut as part of the 
 * AvuPur project, funded by the French Agence Nationale de la Recherche 
 * (ANR) under contract ANR-07-VULN-01.
 * 
 * TANATO is distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 * Copyright (C) 2010 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
 * Copyright (C) 2011 Erwan BOCHER, , Alexis GUEGANNO, Jean-Yves MARTIN
 * 
 * TANATO is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * TANATO is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * TANATO. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, please consult: <http://trac.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */

package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.AbstractScalarFunction;
import org.gdms.sql.function.BasicFunctionSignature;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;

/**
 *
 * @author ebocher
 */
public class ST_LINEARINTERPOLATION extends AbstractScalarFunction {

        private GeometryFactory gf = new GeometryFactory();

        @Override
        public final Value evaluate(SQLDataSourceFactory dsf, Value... values) throws FunctionException {
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
        public FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{
                        new BasicFunctionSignature(Type.GEOMETRY, ScalarArgument.GEOMETRY)};
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
