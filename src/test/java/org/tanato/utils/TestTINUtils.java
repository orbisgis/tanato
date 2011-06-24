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
package org.tanato.utils;

import junit.framework.TestCase;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;

/**
 *
 * @author ebocher
 */
public class TestTINUtils extends TestCase {

        public void testDTriangleProportion() throws Exception {

                //The steepest vector point to the edge and interects it
                DEdge edge = new DEdge(new DPoint(0, 0, 0), new DPoint(10, 0, 0));
                DTriangle dTriangle = new DTriangle(new DPoint(0, 0, 0), new DPoint(10, 0, 0), new DPoint(5, 5, 10));
                assertTrue(TINUtils.getProportion(dTriangle, edge) == 1);

                //The steepest vector point to a triangle vertex
                edge = new DEdge(new DPoint(10, 10, 10), new DPoint(5, 0, 0));
                dTriangle = new DTriangle(new DPoint(0, 10, 10), new DPoint(10, 10, 10), new DPoint(5, 0, 0));
                assertTrue(TINUtils.getProportion(dTriangle, edge) == 0.5);

                //The steepest vector point to two edges
                edge = new DEdge(new DPoint(10, 20, 20), new DPoint(5, 0, 0));
                dTriangle = new DTriangle(new DPoint(0, 5, 10), new DPoint(10, 20, 20), new DPoint(5, 0, 0));
                assertEquals(TINUtils.getProportion(dTriangle, edge), 0.91, 0.01);

        }

        public void testDTriangleRidgeLine() throws Exception {

                //The steepest vector point to the edge and interects it
                DEdge edge = new DEdge(new DPoint(0, 0, 0), new DPoint(10, 0, 0));
                DTriangle dTriangle = new DTriangle(new DPoint(0, 0, 0), new DPoint(10, 0, 0), new DPoint(5, 5, 10));
                assertTrue(TINUtils.getRidgeLine(dTriangle).equals(new DEdge(edge.getMiddle(),  new DPoint(5, 5, 10))));

                //The steepest vector point to a triangle vertex
                edge = new DEdge(new DPoint(10, 10, 10), new DPoint(5, 0, 0));
                dTriangle = new DTriangle(new DPoint(0, 10, 10), new DPoint(10, 10, 10), new DPoint(5, 0, 0));
                assertTrue(TINUtils.getRidgeLine(dTriangle).equals(new DEdge(new DPoint(5, 0, 0), new DPoint(5, 10, 10))));


        }
}
