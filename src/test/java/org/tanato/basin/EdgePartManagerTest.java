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

import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author alexis
 */
public class EdgePartManagerTest extends TestCase {
        
        /**
         * test that we can add an EdgePart, and then retrieve it later.
         */
        public void testAddEdgePart() {
                EdgePartManager epm = new EdgePartManager();
                assertTrue(epm.getQueueSize()==0);
                assertTrue(epm.isEmpty());
                EdgePart ep = new EdgePart(8, 0.38, 0.40, 5, 9, 64, 52);
                epm.addEdgePart(ep);
                assertTrue(epm.getQueueSize()==1);
                assertFalse(epm.isEmpty());
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                assertTrue(out.get(0)==ep);
                assertTrue(epm.getQueueSize()==0);
                assertTrue(epm.isEmpty());
        }
        
        public void testTwoBehindEdges() {
                EdgePartManager epm = new EdgePartManager();
                assertTrue(epm.getQueueSize()==0);
                EdgePart ep = new EdgePart(8, 0.38, 0.40, 5, 9, 64, 52);
                EdgePart e2 = new EdgePart(9, 0.38, 0.40, 5, 9, 64, 52);
                epm.addEdgePart(ep);
                epm.addEdgePart(e2);
                assertTrue(epm.getQueueSize()==2);
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                assertTrue(out.get(0)==ep);
                assertTrue(epm.getQueueSize()==1);
                
        }
        
        /**
         * A simple merging test.
         */
        public void testAddAndMerge(){
                EdgePartManager epm = new EdgePartManager();
                EdgePart ep = new EdgePart(8, 0.38, 0.40, 5, 9, 64, 52);
                EdgePart e2 = new EdgePart(8, 0.39, 0.41, 5, 9, 64, 52);
                epm.addEdgePart(ep);
                epm.addEdgePart(e2);
                assertTrue(epm.getQueueSize()==1);
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                EdgePart merge = out.get(0);
                assertTrue(merge.getStart() == 0.38);
                assertTrue(merge.getEnd()==0.41);
        }
    
        /**
         * A more complicated merging test.
         */
        public void testAddAndMergeManyElements(){
                EdgePartManager epm = new EdgePartManager();
                EdgePart ep = new EdgePart(8, 0.38, 0.40, 5, 9, 64, 52);
                EdgePart e2 = new EdgePart(8, 0.45, 0.50, 5, 9, 64, 52);
                EdgePart e3 = new EdgePart(8, 0.55, 0.60, 5, 9, 64, 52);
                EdgePart e4 = new EdgePart(8, 0.95, 0.97, 5, 9, 64, 52);
                EdgePart e5 = new EdgePart(8, 0.15, 0.17, 5, 9, 64, 52);
                //There will be a merging here.
                EdgePart e6 = new EdgePart(8, 0.12, 0.16, 5, 9, 64, 52);
                EdgePart e7 = new EdgePart(8, 0.25, 0.30, 5, 9, 64, 52);
                EdgePart e8 = new EdgePart(8, 0.32, 0.33, 5, 9, 64, 52);
                epm.addEdgePart(ep);
                epm.addEdgePart(e2);
                epm.addEdgePart(e3);
                epm.addEdgePart(e4);
                epm.addEdgePart(e5);
                epm.addEdgePart(e6);
                epm.addEdgePart(e7);
                epm.addEdgePart(e8);
                EdgePart e9 = new EdgePart(8, 0.31, 0.85, 5, 9, 64, 52);
                epm.addEdgePart(e9);
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==4);
                assertTrue(out.get(0).equals(new EdgePart(8,0.12,0.17,5,9,64,62)));
                assertTrue(out.get(1).equals(new EdgePart(8,0.25,0.30,5,9,64,62)));
                assertTrue(out.get(2).equals(new EdgePart(8,0.31,0.85,5,9,64,62)));
                assertTrue(out.get(3).equals(new EdgePart(8,0.95,0.97,5,9,64,62)));
                
        }
        
        /**
         * A merge operation is performed while there are two keys in the map.
         */
        public void testTwoKeysInMap()  {
                EdgePartManager epm = new EdgePartManager();
                EdgePart ep = new EdgePart(9, 0.38, 0.40, 5, 9, 64, 52);
                EdgePart e2 = new EdgePart(8, 0.45, 0.50, 5, 9, 64, 52);
                EdgePart e3 = new EdgePart(8, 0.35, 0.47, 5, 9, 64, 52);
                epm.addEdgePart(ep);
                epm.addEdgePart(e2);
                epm.addEdgePart(e3);
                assertTrue(epm.getQueueSize()==2);
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                assertTrue(out.get(0).equals(new EdgePart(9, 0.38, 0.40, 5, 9, 64, 52)));
                out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                assertTrue(out.get(0).equals(new EdgePart(8, 0.35, 0.50, 5, 9, 64, 52)));
        }
}
