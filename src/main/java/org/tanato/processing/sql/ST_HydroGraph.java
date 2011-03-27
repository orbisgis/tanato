/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.values.Value;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.jhydrocell.hydronetwork.HydroTINBuilder;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.model.HydroGraphBuilder;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a constrained delaunay triangulation from the geometry given in input.
 *
 * 
 * @author alexis
 */
public class ST_HydroGraph implements CustomQuery {

        private static final Logger logger = Logger.getLogger(ST_HydroGraph.class.getName());

        @Override
        public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
                Value[] values, IProgressMonitor pm) throws ExecutionException {
                try {
                        DataSource ds = tables[0];
                        //We need to read our source.
                        SpatialDataSourceDecorator sdsPoints = new SpatialDataSourceDecorator(ds);
                        ds = tables[1];
                        //We need to read our source.
                        SpatialDataSourceDecorator sdsEdges = new SpatialDataSourceDecorator(ds);
                        ds = tables[2];
                        //We need to read our source.
                        SpatialDataSourceDecorator sdsTriangles = new SpatialDataSourceDecorator(ds);
                        HydroGraphBuilder hydroGraphBuilder = new HydroGraphBuilder(dsf, sdsTriangles, sdsEdges, sdsPoints);
                        ObjectDriver[] drivers = hydroGraphBuilder.createGraph(pm);
                        dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(values[0].getAsString()+"_hydronodes"), drivers[0]);
                        dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(values[0].getAsString()+"_hydroedges"), drivers[1]);
                        return null;
                } catch (NoSuchTableException ex) {
                        Logger.getLogger(ST_HydroGraph.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IndexException ex) {
                        Logger.getLogger(ST_HydroGraph.class.getName()).log(Level.SEVERE, null, ex);
                } catch (DriverException ex) {
                        Logger.getLogger(ST_HydroGraph.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                        Logger.getLogger(ST_HydroGraph.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
        }

        @Override
        public String getName() {
                return "ST_HydroGraph";
        }

        @Override
        public String getDescription() {
                return "Compute an HydroGraph based on 3 TIN tables.";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_HydroGraph( FROM tin_points, tin_edges, tin_triangles;";
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                return null;
        }

        /**
         * The tables we need after the clause FROM in the query.
         * @return
         */
        @Override
        public TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};
        }

        /**
         *  
         * STRING : Prefix name of the hydrotin tables<br/>
         * @return
         */
        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.STRING)};
        }
}
