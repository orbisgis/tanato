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

import org.gdms.data.DataSourceFactory;
import org.gdms.data.types.Constraint;
import org.gdms.data.types.DimensionConstraint;
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
