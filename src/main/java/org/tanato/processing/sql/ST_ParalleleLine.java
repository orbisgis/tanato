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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
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
 * This function creates an edge parallel to the one given in its first argument,
 * at a distance equal to the value given in the second argument.
 *
 * The resulting edge is the same as the input one. It has been orthogonally transfered,
 * and lies at the specified distance from the original edge.
 * @author ebocher
 */
public class ST_ParalleleLine extends  AbstractScalarFunction {

        private GeometryFactory gf = new GeometryFactory();

        @Override
        public final Value evaluate(SQLDataSourceFactory dsf, Value... values) throws FunctionException {

                LineString geom = (LineString) values[0].getAsGeometry();

                return ValueFactory.createValue(getParallel(geom, values[1].getAsDouble()));

        }

        @Override
        public final String getName() {
                return "ST_ParalleleLine";
        }

        @Override
        public final Type getType(Type[] types) {
                return TypeFactory.createType(Type.GEOMETRY);
        }

        @Override
        public final String getDescription() {
                return "Create a parallele line.";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_ParalleleLine(the_geom, 2) FROM table";
        }

        @Override
        public FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{new BasicFunctionSignature(Type.GEOMETRY, 
                        ScalarArgument.GEOMETRY, ScalarArgument.DOUBLE)};
        }

	/**
	 * Actually compute the segement parallel to line, at distance d from line.
	 * @param line
	 * @param d
	 * @return
	 */
        public final LineString getParallel(LineString line, double d) {
                Coordinate[] coords = line.getCoordinates();
                if (coords.length != 2) {
                        throw new IllegalArgumentException("You must have only two coordinates");
                } else {
                        double x0 = coords[0].x;
                        double y0 = coords[0].y;
                        double dx = coords[1].x - x0;
                        double dy = coords[1].y - y0;
                        double dd = Math.sqrt(dx * dx + dy * dy);
                        return gf.createLineString(new Coordinate[]{
                                        new Coordinate(x0 + dy * d / dd, y0 - dx * d / dd), new Coordinate(
                                        x0 + dx + dy * d / dd,
                                        y0 + dy - dx * d / dd)
                                });

                }

        }
}
