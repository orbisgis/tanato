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
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.indexes.DefaultSpatialIndexQuery;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.generic.GenericObjectDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Arguments;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.factory.TINFeatureFactory;

/**
 * This custom query will try to affect a z value to each point of a geometry. To do so,
 * it will use a triangular irregulated network, and will interpolate the z values from it.
 * @author alexis
 */
public class ST_SetZFromTriangles implements CustomQuery {

        @Override
        public final ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {


                DataSource ds = tables[0];
                //We need to read our source.
                SpatialDataSourceDecorator sdsTriangle = new SpatialDataSourceDecorator(ds);

                ds = tables[1];
                //We need to read our source.
                SpatialDataSourceDecorator sdsToBeInterpolate = new SpatialDataSourceDecorator(ds);

                try {
                        sdsTriangle.open();
                        sdsToBeInterpolate.open();


                        GenericObjectDriver driver = new GenericObjectDriver(sdsToBeInterpolate.getMetadata());
                        long countToBeInterpol = sdsToBeInterpolate.getRowCount();
                        String geomField = sdsTriangle.getSpatialFieldName();
                        int geomIndex = sdsTriangle.getSpatialFieldIndex();

                        if (!dsf.getIndexManager().isIndexed(sdsTriangle.getName(), geomField)) {
                                dsf.getIndexManager().buildIndex(sdsTriangle.getName(), geomField, pm);
                        }


                        Geometry geomToBeInterpol;
                        Value[] valuesToBeInterpolate;
                        for (int i = 0; i < countToBeInterpol; i++) {

                                valuesToBeInterpolate = sdsToBeInterpolate.getRow(i);
                                geomToBeInterpol = sdsToBeInterpolate.getGeometry(i);
                                DefaultSpatialIndexQuery query = new DefaultSpatialIndexQuery(geomToBeInterpol.getEnvelopeInternal(), geomField);
                                Iterator<Integer> it = sdsTriangle.queryIndex(query);
                                ArrayList<DTriangle> triangles = new ArrayList<DTriangle>();
                                while (it.hasNext()) {
                                        Integer index = it.next();
                                        Geometry g = sdsTriangle.getGeometry(index);
                                        DTriangle dTriangle = TINFeatureFactory.createDTriangle(g);
                                        triangles.add(dTriangle);
                                }
                                Geometry result = (Geometry) geomToBeInterpol.clone();
                                result.apply(new TINZFilter(triangles));

                                valuesToBeInterpolate[geomIndex] = ValueFactory.createValue(result);
                                driver.addValues(valuesToBeInterpolate);

                        }
                        sdsTriangle.close();
                        sdsToBeInterpolate.close();

                        return driver;

                } catch (IndexException ex) {
                        Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);

                } catch (NoSuchTableException ex) {
                        Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);

                } catch (DriverException ex) {
                        Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);

                } catch (DelaunayError ex) {
                        Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
        }

        @Override
        public final Metadata getMetadata(Metadata[] tables) throws DriverException {
                return tables[1];
        }

        @Override
        public final TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};
        }

        private class TINZFilter implements CoordinateSequenceFilter {

                private boolean done = false;
                private final ArrayList<DTriangle> triangles;

                public TINZFilter(ArrayList<DTriangle> triangles) {
                        this.triangles = triangles;
                }

                @Override
                public void filter(CoordinateSequence seq, int i) {
                        Coordinate coord = seq.getCoordinate(i);
                        seq.setOrdinate(i, 2, getZFromTriangles(coord, triangles));
                        if (i == seq.size()) {
                                done = true;
                        }
                }

		@Override
                public boolean isGeometryChanged() {
                        return true;
                }

		@Override
                public boolean isDone() {
                        return done;
                }

                private double getZFromTriangles(Coordinate coord, ArrayList<DTriangle> triangles) {
                        try {
                                coord.z = 0;
                                for (DTriangle dTriangle : triangles) {
                                        DPoint dPoint = TINFeatureFactory.createDPoint(coord);
                                        if (dTriangle.isInside(dPoint)) {
                                                return dTriangle.interpolateZ(TINFeatureFactory.createDPoint(coord));

                                        }
                                }

                        } catch (DelaunayError ex) {
                                Logger.getLogger(ST_SetZFromTriangles.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return Double.NaN;
                }
        }

        @Override
        public final String getName() {
                return "ST_SetZFromTriangles";
        }

        @Override
        public final String getDescription() {
                return "Set a z to the geometry based on triangle interpolation";
        }

        @Override
        public final String getSqlOrder() {
                return "SELECT ST_SetZFromTriangles() FROM triangles, tableTobeInterpolate";
        }

        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments()};

        }
}
