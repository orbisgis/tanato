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

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.generic.GenericObjectDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Arguments;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.IProgressMonitor;

public class ST_TINPropertyHelp implements CustomQuery {

        static final String TOPOGRAPHIC = "topographic";
        static final String MORPHOLOGIC = "morphologic";
        static final String TOPOLOGIC = "topologic";
        static final String NONE = "none";
	static final String ANY = "any";

        @Override
        public final ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
                Value[] values, IProgressMonitor pm) throws ExecutionException {

                try {
                        DefaultMetadata defaultMetadata = new DefaultMetadata();
                        defaultMetadata.addField("property", TypeFactory.createType(Type.STRING));
                        defaultMetadata.addField("text", TypeFactory.createType(Type.STRING));
                        defaultMetadata.addField("type", TypeFactory.createType(Type.STRING));

                        GenericObjectDriver genericObjectDriver = new GenericObjectDriver(
                                defaultMetadata);

                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.ANY),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.ANY)),
                                ValueFactory.createValue(ANY));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.BORDER),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.BORDER)),
                                ValueFactory.createValue(TOPOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.DITCH),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.DITCH)),
                                ValueFactory.createValue(TOPOGRAPHIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.DOUBLECOLINEAR),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.DOUBLECOLINEAR)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.FLAT),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.FLAT)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.LEFTCOLINEAR),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.LEFTCOLINEAR)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.LEFTSIDE),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.LEFTSIDE)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.LEFTTSLOPE),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.LEFTTSLOPE)));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.LEFTWELL),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.LEFTWELL)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.LEVEL),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.LEVEL)),
                                ValueFactory.createValue(TOPOGRAPHIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.NONE),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.NONE)),
                                ValueFactory.createValue(NONE));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.RIDGE),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.RIDGE)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.RIGHTCOLINEAR),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.RIGHTCOLINEAR)));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.RIGHTSIDE),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.RIGHTSIDE)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.RIGHTSLOPE),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.RIGHTSLOPE)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.RIGHTWELL),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.RIGHTWELL)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.RIVER),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.RIVER)));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.ROAD),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.ROAD)),
                                ValueFactory.createValue(TOPOGRAPHIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.RURAL_PARCEL),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.RURAL_PARCEL)));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.SEWER),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.SEWER)),
                                ValueFactory.createValue(TOPOGRAPHIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.SEWER_INPUT),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.SEWER_INPUT)));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.SEWER_OUTPUT),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.SEWER_OUTPUT)),
                                ValueFactory.createValue(TOPOGRAPHIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.TALWEG),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.TALWEG)),
                                ValueFactory.createValue(MORPHOLOGIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.URBAN_PARCEL),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.URBAN_PARCEL)),
                                ValueFactory.createValue(TOPOGRAPHIC));
                        genericObjectDriver.addValues(
                                ValueFactory.createValue(HydroProperties.WALL),
                                ValueFactory.createValue(HydroProperties.toString(HydroProperties.WALL)),
                                ValueFactory.createValue(TOPOGRAPHIC));


                        return genericObjectDriver;
                } catch (DriverException e) {
                        throw new ExecutionException(e);
                }

        }

        @Override
        public final TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[0];
        }

        @Override
        public final String getDescription() {
                return "Create a table with all property value and text representation used by the mesh.";
        }

        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments()};
        }

        @Override
        public final Metadata getMetadata(Metadata[] tables) throws DriverException {

                return null;
        }

        @Override
        public final String getName() {
                return "ST_TINPropertyHelp";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_TINPropertyHelp()";
        }
}
