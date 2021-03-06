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

import java.lang.reflect.Field;
import java.util.TreeMap;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.sql.function.FunctionException;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.gdms.driver.memory.MemoryDataSetDriver;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.ProgressMonitor;

/**
 * This customQuery will create a GenericObjectDriver, that encapsulates a table explaining
 * the link between the int values of the hydroproperties, and their meaning as 
 * defined in jDelaunay, in HydroProperties.
 * @see org.jhydrocell.hydronetwork.HydroProperties
 * @author erwan, kwyhr, alexis
 */
public class ST_TINPropertyHelp extends AbstractTableFunction {

        @Override
        public final DataSet evaluate(SQLDataSourceFactory dsf, DataSet[] tables,
                Value[] values, ProgressMonitor pm) throws FunctionException {

                try {
                        // Build header
                        DefaultMetadata defaultMetadata = new DefaultMetadata();
                        defaultMetadata.addField("property", TypeFactory.createType(Type.STRING));
                        defaultMetadata.addField("text", TypeFactory.createType(Type.STRING));
                        defaultMetadata.addField("type", TypeFactory.createType(Type.STRING));

                        MemoryDataSetDriver genericObjectDriver = new MemoryDataSetDriver(
                                defaultMetadata);

                        // Build list
                        TreeMap<String, Integer> theFieldList = new TreeMap<String, Integer>();
                        Field[] theList = HydroProperties.class.getFields();
                        for (Field theField : theList) {
                                if(theField.getType().isAssignableFrom(Integer.TYPE) && theField.getType().isPrimitive()){
                                        try {
                                                int intValue = theField.getInt(theField);
                                                theFieldList.put(theField.getName(), intValue);
                                        } catch (IllegalArgumentException ex) {
                                                throw new FunctionException("We're dealing with an object that is"
                                                        + "not an int. Surprising.",ex);
                                        } catch (IllegalAccessException ex) {
                                                throw new FunctionException("We're supposed to work only on public fields.",ex);
                                        }
                                        
                                }
                        }

                        // Then usees treeMap tu build result
                        for (String fieldName : theFieldList.keySet()) {
                                // Get informations
                                int fieldValue = theFieldList.get(fieldName);
                                String qualification = HydroProperties.getPropertyQualification(fieldValue);

                                // Build new line
                                genericObjectDriver.addValues(
                                        ValueFactory.createValue(fieldValue),
                                        ValueFactory.createValue(fieldName),
                                        ValueFactory.createValue(qualification));
                        }

                        return genericObjectDriver;
                } catch (DriverException e) {
                        throw new FunctionException("there has been a problem while building the driver",e);
                }

        }

        @Override
        public final String getDescription() {
                return "Create a table with all property value and text representation used by the mesh.";
        }
        
        @Override
        public final FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{
                        new TableFunctionSignature(TableDefinition.ANY)};
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
                return "CALL ST_TINPropertyHelp()";
        }
}
