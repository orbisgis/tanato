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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
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
import org.gdms.data.values.ValueFactory;
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
public class ST_DropletLine implements CustomQuery {

        public final String getName() {
                return "ST_DropletLine";
        }

        public final String getDescription() {
                return "get the line a droplet will follow on the TIN.";
        }

        public final String getSqlOrder() {
                return "SELECT ST_DropletLine([, autorizedProperties [, endingproperties]]) FROM out_point, out_edges, out_triangles";
        }

        public final TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};


        }

        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(),
                                new Arguments(Argument.INT),
                                new Arguments(Argument.INT, Argument.INT)
                        };
        }

        @Override
        public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {

                try {
                        DropletFollower dropletFollower = new DropletFollower(dsf, tables, values, pm);
                        // Create writer
                        DiskBufferDriver writer = new DiskBufferDriver(dsf, getMetadata(null));

                        SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(tables[3]);
                        sds.open();
                        pm.startTask("Processing runoff path");
                        long rowCount = sds.getRowCount();
                        for (int i = 0; i < rowCount; i++) {
                                if (i / 100 == i / 100.0) {
                                        if (pm.isCancelled()) {
                                                break;
                                        } else {
                                                pm.progressTo((int) (100 * i / rowCount));
                                        }
                                }

                                Geometry geom = sds.getGeometry(i);
                                if (GeometryTypeUtil.isPoint(geom)) {
                                        ArrayList<DPoint> result = dropletFollower.getPath(geom);
                                        if (result != null) {
                                                int resultSize = result.size();
                                                System.out.println("Passage " + i + " size " + resultSize);
                                                if (resultSize > 1) {
                                                        // Process points to build a line
                                                        GeometryFactory gf = new GeometryFactory();
                                                        Coordinate[] coords = new Coordinate[resultSize];
                                                        int k = 0;
                                                        for (DPoint aPoint : result) {
                                                                coords[k] = aPoint.getCoordinate();
                                                                k++;
                                                        }

                                                        // save line
                                                        CoordinateSequence cs = new CoordinateArraySequence(coords);

                                                        LineString mp = new LineString(cs, gf);
                                                        writer.addValues(new Value[]{ValueFactory.createValue(mp), ValueFactory.createValue(i)});
                                                }
                                        }
                                }
                        }
                        pm.endTask();
                        writer.writingFinished();
                        sds.close();
                        dropletFollower.closeData();
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
                        new Type[]{TypeFactory.createType(Type.GEOMETRY, new GeometryConstraint(
                                GeometryConstraint.LINESTRING)), TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID});
                return md;
        }
}
