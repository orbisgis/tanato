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
 * Copyright (C) 2011 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
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

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import java.io.File;
import junit.framework.TestCase;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.DataSource;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.jdelaunay.delaunay.tools.Tools;

/**
 * Tests on the ST_BasinGraph class.
 * @author alexis
 */
public class TestSTBasinGraph extends TestCase {
        
        private SQLDataSourceFactory dsf = new SQLDataSourceFactory("target", "target");
        private GeometryFactory gf = new GeometryFactory();
        private String trianglesWithoutFlat = "src/test/resources/data/tin/small_courbes_chezine/without_flat_triangles.shp";
        private String edgesWithoutFlat =  "src/test/resources/data/tin/small_courbes_chezine/without_flat_edges.shp";
        private String pointsWithoutFlat = "src/test/resources/data/tin/small_courbes_chezine/without_flat_points.shp";
        
        /**
         * A simple test on the small data from chezine, with flat triangles removal.
         * There are no walls, rivers, ridges, roads... and we select the lowest point 
         * of the mesh after the removal of triangles.
         * @throws Exception 
         */
        public void testBasinGraphSimple() throws Exception{
                ST_BasinGraph fun = new ST_BasinGraph();
                DataSource points = dsf.getDataSource(new File(pointsWithoutFlat));
                DataSource edges = dsf.getDataSource(new File(edgesWithoutFlat));
                DataSource triangles = dsf.getDataSource(new File(trianglesWithoutFlat));
                points.open();
                edges.open();
                triangles.open();
                DataSet od = fun.evaluate(dsf,
                        new DataSource[]{points, edges, triangles},
                        new Value[]{ValueFactory.createValue(9), ValueFactory.createValue(0)}, 
                        null);
                Geometry geom = gf.createPolygon(gf.createLinearRing(new Coordinate [] {
                        new Coordinate(213,273,20),
                        new Coordinate(331,142,20),
                        new Coordinate(310,100,10),
                        new Coordinate(209,95,10),
                        new Coordinate(140,125,20),
                        new Coordinate(108,222,20),
                        new Coordinate(136,262,20),
                        new Coordinate(213,273,20),
                }), new LinearRing[]{});
                Geometry buff = geom.buffer(Tools.EPSILON);
                Geometry bis = od.getFieldValue(0, 0).getAsGeometry();
                ConvexHull ch = new ConvexHull(bis);
                Geometry ter = ch.getConvexHull();
                assertTrue(buff.covers(ter));
                points.close();
                edges.close();
                triangles.close();
        }
        
}
