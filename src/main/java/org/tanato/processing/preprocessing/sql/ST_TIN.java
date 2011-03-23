/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tanato.processing.preprocessing.sql;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.values.Value;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Arguments;
import org.orbisgis.progress.IProgressMonitor;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a constrained delaunay triangulation from the geometry given in input.
 *
 * 
 * @author alexis
 */
public class ST_TIN implements CustomQuery {

	@Override
	public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getName() {
		return "ST_TIN";
	}

	@Override
	public String getDescription() {
		return "Compute a TIN from the lines of the geometry given in argument.";
	}

	@Override
	public String getSqlOrder() {
		return "SELECT ST_TIN(false, true, points, edges, triangles) FROM source_table;";
	}

	@Override
	public Metadata getMetadata(Metadata[] tables) throws DriverException {
		return null;
	}

	@Override
	public TableDefinition[] getTablesDefinitions() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Arguments[] getFunctionArguments() {
		Arguments[] arg = new Arguments[5];
		
		return arg;
	}

}
