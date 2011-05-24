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
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.Tools;

/**
 *
 * @author alexis
 */
public class EdgePartManagerTest extends TestCase {
        
        /**
         * test that we can add an EdgePart, and then retrieve it later.
         */
        public void testAddEdgePart() throws DelaunayError {
                EdgePartManager epm = new EdgePartManager();
                assertTrue(epm.getQueueSize()==0);
                assertTrue(epm.isEmpty());
                DEdge ed = new DEdge(2,2,2,4,3,2);
                ed.setGID(8);
                EdgePart ep = new EdgePart(ed, 0.38, 0.40,1,2);
                epm.addEdgePart(ep);
                assertTrue(epm.getQueueSize()==1);
                assertFalse(epm.isEmpty());
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                assertTrue(out.get(0)==ep);
                assertTrue(epm.getQueueSize()==0);
                assertTrue(epm.isEmpty());
        }
        
        public void testTwoBehindEdges() throws DelaunayError {
                EdgePartManager epm = new EdgePartManager();
                assertTrue(epm.getQueueSize()==0);
                DEdge ed = new DEdge(2,2,2,4,3,2);
                ed.setGID(8);
                EdgePart ep = new EdgePart(ed, 0.38, 0.40,1,2);
                DEdge ed2 = new DEdge(2,2,2,4,3,2);
                ed2.setGID(9);
                EdgePart e2 = new EdgePart(ed2, 0.38, 0.40,1,2);
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
        public void testAddAndMerge() throws DelaunayError{
                EdgePartManager epm = new EdgePartManager();
                DEdge ed = new DEdge(2,2,2,4,3,2);
                ed.setGID(8);
                EdgePart ep = new EdgePart(ed, 0.38, 0.40,1,2);
                EdgePart e2 = new EdgePart(ed, 0.39, 0.41,1,2);
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
        public void testAddAndMergeManyElements() throws DelaunayError{
                EdgePartManager epm = new EdgePartManager();
                DEdge ed = new DEdge(2,2,2,4,3,2);
                ed.setGID(8);
                EdgePart ep = new EdgePart(ed, 0.38, 0.40,1,2);
                EdgePart e2 = new EdgePart(ed, 0.45, 0.50,1,2);
                EdgePart e3 = new EdgePart(ed, 0.55, 0.60,1,2);
                EdgePart e4 = new EdgePart(ed, 0.95, 0.97,1,2);
                EdgePart e5 = new EdgePart(ed, 0.15, 0.17,1,2);
                //There will be a merging here.
                EdgePart e6 = new EdgePart(ed, 0.12, 0.16,1,2);
                EdgePart e7 = new EdgePart(ed, 0.25, 0.30,1,2);
                EdgePart e8 = new EdgePart(ed, 0.32, 0.33,1,2);
                epm.addEdgePart(ep);
                epm.addEdgePart(e2);
                epm.addEdgePart(e3);
                epm.addEdgePart(e4);
                epm.addEdgePart(e5);
                epm.addEdgePart(e6);
                epm.addEdgePart(e7);
                epm.addEdgePart(e8);
                EdgePart e9 = new EdgePart(ed, 0.31, 0.85,1,2);
                epm.addEdgePart(e9);
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==4);
                assertTrue(out.get(0).equals(new EdgePart(ed,0.12,0.17,1,2)));
                assertTrue(out.get(1).equals(new EdgePart(ed,0.25,0.30,1,2)));
                assertTrue(out.get(2).equals(new EdgePart(ed,0.31,0.85,1,2)));
                assertTrue(out.get(3).equals(new EdgePart(ed,0.95,0.97,1,2)));
                
        }
        
        /**
         * A merge operation is performed while there are two keys in the map.
         */
        public void testTwoKeysInMap() throws DelaunayError  {
                EdgePartManager epm = new EdgePartManager();
                DEdge ed = new DEdge(2,2,2,4,3,2);
                ed.setGID(8);
                DEdge edZ = new DEdge(9,6,4,5,7,2);
                ed.setGID(9);
                EdgePart ep = new EdgePart(edZ, 0.38, 0.40,1,2);
                EdgePart e2 = new EdgePart(ed, 0.45, 0.50,1,2);
                EdgePart e3 = new EdgePart(ed, 0.35, 0.47,1,2);
                epm.addEdgePart(ep);
                epm.addEdgePart(e2);
                epm.addEdgePart(e3);
                assertTrue(epm.getQueueSize()==2);
                List<EdgePart> out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                assertTrue(out.get(0).equals(new EdgePart(edZ, 0.38, 0.40,1,2)));
                out = epm.getEdgeParts();
                assertTrue(out.size()==1);
                assertTrue(out.get(0).equals(new EdgePart(ed, 0.35, 0.50,1,2)));
        }
        
        public void testGetTooSmallEdgePart() throws DelaunayError {
                EdgePartManager epm = new EdgePartManager();
                DEdge ed = new DEdge(1,0,0,0,0,0);
                ed.setGID(8);
                EdgePart e2 = new EdgePart(ed, 0.45, 0.45+Tools.EPSILON2,1,2);
                epm.addEdgePart(e2);
                assertFalse(epm.isEmpty());
                assertTrue(epm.getEdgeParts().isEmpty());
                EdgePart.setMaxIterNumber(3);
                epm.getEdgeParts();
                epm.getEdgeParts();
                epm.getEdgeParts();
                assertTrue(epm.isEmpty());
                assertTrue(epm.getEdgeParts().isEmpty());
        }
}
