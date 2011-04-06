/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.gdms.GdmsWriter;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.basin.BasinBuilder;
import org.tanato.model.TINSchema;

/**
 *
 * @author alexis
 */
public class ST_BasinGraph implements CustomQuery  {


	private static final Logger logger = Logger.getLogger(ST_BasinGraph.class.getName());
	// Table informations to navigate
	private SpatialDataSourceDecorator sdsPoints = null;
	private SpatialDataSourceDecorator sdsEdges = null;
	private SpatialDataSourceDecorator sdsTriangles = null;
	
	@Override
	public final ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {
		if (tables.length < 3) {
		    // There MUST be at least 3 tables
		    throw new ExecutionException("needs points, edges and triangles.");
		} else {
			try {
				// First is points
				DataSource dsPoints = tables[0];
				sdsPoints = new SpatialDataSourceDecorator(dsPoints);
				sdsPoints.open();
				if (!dsf.getIndexManager().isIndexed(dsPoints.getName(), TINSchema.GID)) {
					dsf.getIndexManager().buildIndex(dsPoints.getName(), TINSchema.GID, pm);
				}
				DataSource dsEdges = tables[1];
				sdsEdges = new SpatialDataSourceDecorator(dsEdges);
				sdsEdges.open();
				if (!dsf.getIndexManager().isIndexed(sdsEdges.getName(), TINSchema.GID)) {
					dsf.getIndexManager().buildIndex(sdsEdges.getName(), TINSchema.GID, pm);
				}
				DataSource dsTriangles = tables[2];
				sdsTriangles = new SpatialDataSourceDecorator(dsTriangles);
				sdsTriangles.open();
				if (!dsf.getIndexManager().isIndexed(sdsTriangles.getName(), TINSchema.GID)) {
					dsf.getIndexManager().buildIndex(sdsTriangles.getName(), TINSchema.GID, pm);
				}
				BasinBuilder bb = new BasinBuilder(dsf, sdsPoints, sdsEdges, sdsTriangles,
						values[0].getAsInt(), values[1].getAsInt());
				bb.computeBasin();
				registerFile(values[2].getAsString(), dsf, bb.getBasin());
				sdsEdges.close();
				sdsTriangles.close();
				sdsPoints.close();

			} catch (IOException ex) {
				Logger.getLogger(ST_BasinGraph.class.getName()).log(Level.SEVERE, null, ex);
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
		return "SELECT ST_BasinGraph(555,0,'out') FROM points, edges, triangles;";
	}

	@Override
	public final Metadata getMetadata(Metadata[] tables) throws DriverException {
                return null;
	}

	@Override
	public final TableDefinition[] getTablesDefinitions() {
        return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};
	}

	@Override
	public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.INT, Argument.INT, Argument.STRING)};
	}
	
	private void registerFile(String name, DataSourceFactory dsf, Geometry geom) throws IOException, DriverException {
                File out = new File(name + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY),},
                        new String[]{TINSchema.GEOM_FIELD});
                int triangleCount = 1;
                writer.writeMetadata(triangleCount, md);
		writer.addValues(new Value[]{ValueFactory.createValue(geom),});
                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(name), out);
        }
}
