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
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import junit.framework.TestCase;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.values.Value;
import org.gdms.driver.ObjectDriver;

/**
 * This class is intended the Z interpolation we perform on tins to get a Z values
 * for each point of a geometry.
 * 
 * The sample table with the geometries that don't have Z values contain three points and a linestring :
 * 
 * POINT (224 142)
 * POINT (173 201)
 * POINT (0 0)
 * LINESTRING (225 120, 254 120)
 * 
 * @author alexis
 */
public class TestSetZFromTriangles extends TestCase {
        
        private DataSourceFactory dsf = new DataSourceFactory("target","target");
        
        public void testWithoutFlatRemoval() throws Exception {
                DataSource inp = dsf.getDataSource(new File("src/test/resources/data/source/small_data"
                        + "/points_to_interpolate_chezine.gdms"));
                DataSource triang = dsf.getDataSource(new File("src/test/resources/data/tin/small_courbes_chezine/"
                        + "with_flat_triangles.shp"));
                SpatialDataSourceDecorator input = new SpatialDataSourceDecorator(inp);
                SpatialDataSourceDecorator triangles = new SpatialDataSourceDecorator(triang);
                ST_SetZFromTriangles fun = new ST_SetZFromTriangles();
                ObjectDriver od = fun.evaluate(dsf, new DataSource[] {triangles, input}, new Value[]{}, null);
                long size = od.getRowCount();
                for(long i = 0; i < size; i++){
                        Geometry geom = od.getFieldValue(i,0).getAsGeometry();
                        Coordinate first = geom.getCoordinate();
                        if(first.x == 224){
                                assertTrue(first.z == 10);
                        }
                        if(first.x == 173){
                                assertTrue(first.z == 13.291139240506329);
                        }
                        if(first.x==0){
                                assertTrue(Double.isNaN(first.z));
                        }
                        if(first.x==225 || first.x==254){
                                assertTrue(geom.getCoordinates()[0].z==10);
                                assertTrue(geom.getCoordinates()[1].z==10);
                        }
                }
        }
}
