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

package org.tanato.basin;

import java.util.ArrayList;
import java.util.Collections;
import junit.framework.TestCase;
import org.jdelaunay.delaunay.Tools;

/**
 *
 * @author alexis
 */
public class EdgePartTest extends TestCase{

	public void testConstructor() {
		EdgePart ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
		assertEquals(ep.getEnd(), 0.6);
		assertEquals(ep.getStart(), 0.5);
		assertEquals(ep.getGid(), 0);
		assertEquals(ep.getGidStart(), 8);
		assertEquals(ep.getGidEnd(), 9);
		assertEquals(ep.getGidLeft(), 40);
		assertEquals(ep.getGidRight(), 41);
	}
        
        /**
         * Test the merge operations between two EdgeParts
         */
        public void testMerging() {
            EdgePart ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            EdgePart other = new EdgePart(0, 0.55,0.65,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.5);
            assertTrue(ep.getEnd() == 0.65);
            assertTrue(ep.contains(other));
            ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            other = new EdgePart(0, 0.45,0.65,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.45);
            assertTrue(ep.getEnd() == 0.65);
            assertTrue(ep.contains(other));
            ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            other = new EdgePart(0, 0.55,0.56,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.5);
            assertTrue(ep.getEnd() == 0.6);
            assertTrue(ep.contains(other));
            ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            other = new EdgePart(0, 0.25,0.26,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.5);
            assertTrue(ep.getEnd() == 0.6);
            assertFalse(ep.contains(other));
            ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            other = new EdgePart(0, 0.65,0.66,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.5);
            assertTrue(ep.getEnd() == 0.6);
            assertFalse(ep.contains(other));
            ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            other = new EdgePart(0, 0.45,0.55,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.45);
            assertTrue(ep.getEnd() == 0.6);
            assertTrue(ep.contains(other));
            ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            other = new EdgePart(0, 0.45,0.50-Tools.EPSILON2,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.45);
            assertTrue(ep.getEnd() == 0.6);
            assertTrue(ep.contains(other));
            ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            other = new EdgePart(0, 0.60+Tools.EPSILON2,0.65,8,9,40,41);
            ep.expandToInclude(other);
            assertTrue(ep.getStart()==0.5);
            assertTrue(ep.getEnd() == 0.65);
            assertTrue(ep.contains(other));
        }
        
        /**
         * Test the EdgePart.contains method.
         */
        public void testContains() {
            EdgePart ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
            EdgePart other = new EdgePart(0, 0.55,0.56,8,9,40,41);
            assertTrue(ep.contains(other));
            other = new EdgePart(0, 0.25,0.56,8,9,40,41);
            assertFalse(ep.contains(other));
            other = new EdgePart(0, 0.25,0.3,8,9,40,41);
            assertFalse(ep.contains(other));
            other = new EdgePart(1, 0.55,0.56,8,9,40,41);
            assertFalse(ep.contains(other));
        }
        
        public void testComparison(){
                EdgePart ep = new EdgePart(8, 0.5, 0.6, 6, 4, 8, 2);
                EdgePart other = new EdgePart(7,0.4,0.7,7,9,5,2);
                assertTrue(ep.compareTo(other)==1);
                assertTrue(other.compareTo(ep)==-1);
                other = new EdgePart(8, 0.4, 0.7, 6, 4, 8, 2);
                assertTrue(ep.compareTo(other)==0);
                assertTrue(other.compareTo(ep)==0);
                other = new EdgePart(8, 0.4, 0.45, 6, 4, 8, 2);
                assertTrue(ep.compareTo(other)==1);
                assertTrue(other.compareTo(ep)==-1);
                other = new EdgePart(8, 0.64, 0.7, 6, 4, 8, 2);
                assertTrue(ep.compareTo(other)==-1);
                assertTrue(other.compareTo(ep)==1);
        }
        
        public void testSort() {
                EdgePart e1 = new EdgePart(8, 0.75, 0.76, 6, 4, 8, 2);
                EdgePart e2 = new EdgePart(8, 0.35, 0.36, 6, 4, 8, 2);
                EdgePart e3 = new EdgePart(8, 0.55, 0.56, 6, 4, 8, 2);
                EdgePart e4 = new EdgePart(8, 0.15, 0.16, 6, 4, 8, 2);
                EdgePart e5 = new EdgePart(8, 0.95, 0.96, 6, 4, 8, 2);
                EdgePart e6 = new EdgePart(8, 0.85, 0.86, 6, 4, 8, 2);
                ArrayList<EdgePart> set = new ArrayList<EdgePart>();
                set.add(e1);
                set.add(e2);
                set.add(e3);
                set.add(e4);
                set.add(e5);
                set.add(e6);
                Collections.sort(set);
                for (int i = 0; i < 5; i++) {
                        assertTrue(set.get(i).getEnd() < set.get(i+1).getStart());
                }
        }
        
        public void testEquality() {
                EdgePart e1 = new EdgePart(8, 0.75, 0.76, 6, 4, 8, 2);
                EdgePart e2 = new EdgePart(8, 0.35, 0.36, 6, 4, 8, 2);
                assertFalse(e1.equals(e2));
                e2 = new EdgePart(9, 0.75, 0.76, 6, 4, 8, 2);
                assertFalse(e1.equals(e2));
                e2 = new EdgePart(8, 0.75, 0.76, 6, 4, 8, 2);
                assertTrue(e1.equals(e2));
                int hash = e2.hashCode();
                assertTrue(hash == 1958558354);
        }
        
}
