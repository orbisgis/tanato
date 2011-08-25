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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.driver.DataSet;
import org.gdms.sql.function.FunctionException;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.memory.MemoryDataSetDriver;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.progress.ProgressMonitor;
import org.tanato.basin.BasinBuilder;
import org.tanato.model.TINSchema;

/**
 *
 * @author alexis
 */
public class ST_BasinGraph extends AbstractTableFunction  {


	private static final Logger logger = Logger.getLogger(ST_BasinGraph.class.getName());
	// Table informations to navigate
	private DataSet sdsPoints = null;
	private DataSet sdsEdges = null;
	private DataSet sdsTriangles = null;
	
	@Override
	public final DataSet evaluate(SQLDataSourceFactory dsf, DataSet[] tables, Value[] values, ProgressMonitor pm) throws FunctionException {
		if (tables.length < 3) {
		    // There MUST be at least 3 tables
		    throw new FunctionException("needs points, edges and triangles.");
		} else {
			try {
				// First is points
				sdsPoints =  tables[0];
				if (!dsf.getIndexManager().isIndexed(sdsPoints, TINSchema.GID)) {
					dsf.getIndexManager().buildIndex(sdsPoints, TINSchema.GID, pm);
				}
				sdsEdges = tables[1];
				if (!dsf.getIndexManager().isIndexed(sdsEdges, TINSchema.GID)) {
					dsf.getIndexManager().buildIndex(sdsEdges, TINSchema.GID, pm);
				}
				sdsTriangles =tables[2];
				if (!dsf.getIndexManager().isIndexed(sdsTriangles, TINSchema.GID)) {
					dsf.getIndexManager().buildIndex(sdsTriangles, TINSchema.GID, pm);
				}
				BasinBuilder bb = new BasinBuilder(dsf, sdsPoints, sdsEdges, sdsTriangles,
						values[0].getAsInt(), values[1].getAsInt());
				bb.computeBasin();
                                Metadata md = new DefaultMetadata(
                                        new Type[]{TypeFactory.createType(Type.GEOMETRY),},
                                        new String[]{TINSchema.GEOM_FIELD});
                                MemoryDataSetDriver od = new MemoryDataSetDriver(md);
                                od.addValues(ValueFactory.createValue(bb.getBasin()));
                                return od;
			} catch (NoSuchTableException ex) {
				Logger.getLogger(ST_DropletPath.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IndexException ex) {
				Logger.getLogger(ST_DropletPath.class.getName()).log(Level.SEVERE, null, ex);
			} catch (DriverException ex) {
				logger.log(Level.SEVERE, "There has been an error while opening a table, or counting its lines.\n", ex);
			}
		}
		return null;
	}

	@Override
	public final String getName() {
		return "ST_BasinGraph";
	}

	@Override
	public final String getDescription() {
		return "Compute the basin graph of an element of a TIN DEM";
	}

	@Override
	public final String getSqlOrder() {
		return "CALL ST_BasinGraph(555,0, points, edges, triangles);";
	}

	public final Metadata getMetadata(Metadata[] tables) throws DriverException {
                return null;
	}

	public final TableDefinition[] getTablesDefinitions() {
        return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};
	}


        @Override
        public FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{new TableFunctionSignature(
                        TableDefinition.GEOMETRY,
                        ScalarArgument.INT, 
                        ScalarArgument.INT,
                        new TableArgument(TableDefinition.GEOMETRY),
                        new TableArgument(TableDefinition.GEOMETRY),
                        new TableArgument(TableDefinition.GEOMETRY))};
        }
        
}
