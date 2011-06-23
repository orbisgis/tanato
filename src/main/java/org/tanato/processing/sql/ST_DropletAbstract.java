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

import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.GeometryConstraint;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.geometryUtils.GeometryTypeUtil;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DelaunayError;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.model.TINSchema;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a droplet path on an existing triangularization.
 *
 *
 * @author kwyhr
 */
public abstract class ST_DropletAbstract implements CustomQuery {
		DiskBufferDriver writer = null;
		
        @Override
        public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {
                try {
                        pm.startTask("Processing runoff path");

                        // Generate Droplet element
                        DropletFollower dropletFollower = new DropletFollower(dsf, tables, values, pm);

                         // Create writer
                        writer = new DiskBufferDriver(dsf, getMetadata(null));

                       // Get points to process
                        SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(tables[3]);
                        sds.open();

                        long rowCount = sds.getRowCount();
                        for (int i = 0; i < rowCount; i++) {
                                if (pm.isCancelled()) {
                                        break;
                                }
                                // Alter progression bar
                                pm.progressTo((int) (100 * i / rowCount));

                                Geometry geom = sds.getGeometry(i);
                                if (GeometryTypeUtil.isPoint(geom)) {
                                        // We have the right geometry - Generate path and save it
                                        ArrayList<DPoint> result = dropletFollower.getPath(geom);
                                        saveDropletData(i, geom, result);
                                }
                        }

                        // Close writer and tables
                        sds.close();
                        dropletFollower.closeData();
                        writer.writingFinished();

                        pm.endTask();

                        return writer;
                } catch (DelaunayError ex) {
                        Logger.getLogger(ST_DropletLine.class.getName()).log(Level.SEVERE, null, ex);
                } catch (DriverException ex) {
                        Logger.getLogger(ST_DropletLine.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY, new GeometryConstraint(GeometryConstraint.POINT)),
                                   TypeFactory.createType(Type.INT),
                                   TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID, "path"});
                return md;
        }

        @Override
        public final TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};


        }

        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(),
                                new Arguments(Argument.INT),
                                new Arguments(Argument.INT, Argument.INT)
                        };
        }

        /**
         * Save data in writer
         * @param writer
         * @param index
         * @param geom
         * @param result 
         */
        protected abstract void saveDropletData(int index, Geometry geom, ArrayList<DPoint> result) throws DriverException;
}