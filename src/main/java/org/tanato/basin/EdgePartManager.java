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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to handle the EdgePart that are generated while processing
 * the basin graph of a hydrotin.<br/>
 * It uses a queue of edge GIDs, backed with a Map where keys are the GIDs, and 
 * Values are set of EdgeParts. Our goal, with this Map, is to compute the intersections
 * in the triangles as rarely as possible.
 * 
 * 
 * @author alexis
 */
final class EdgePartManager {
        //The map of GIDs and EdgeParts. EdgeParts willl be merged as often as possible.
        private Map<Integer, List<EdgePart>> mergingStructure;
        //The queue of GIDs, saved as a LinkedList
        private LinkedList<Integer> epQueue;
        
        /**
         * Create a new EdgePartManager.
         */
        public EdgePartManager(){
                mergingStructure = new HashMap<Integer, List<EdgePart>>();
                epQueue = new LinkedList<Integer>();
        }
        
        /**
         * Add an Edgepart to this manager. We'll first check that there is not 
         * already a reference to an EdgePart with the same GID as ep. If it is,
         * we'll do a merge in the Map. If it is not, we'll add ep at the end of 
         * queue of EdgeParts.
         * @param ep 
         */
        public final void addEdgePart(EdgePart ep){
                int gid = ep.getGid();
                List<EdgePart> list = mergingStructure.get(gid);
                if(list==null){
                        list = new ArrayList<EdgePart>();
                        list.add(ep);
                        epQueue.addLast(gid);
                        mergingStructure.put(gid, list);
                }
        }
        
        /**
         * Get the set of <code>EdgePart</code> associated  to the edge GID gid.
         * Using this method, you directly access the map. Elements are neither deleted
         * from it, nor from the queue.
         * @param gid
         * @return 
         */
        public final List<EdgePart> getEdgeParts(int gid){
                return mergingStructure.get(gid);
        }
        
        /**
         * Get the list of <code>EdgePart</code> associated to the first GID of the
         * queue. This GID, and its corresponding value, are logically removed from
         * the map.
         * @return 
         */
        public final List<EdgePart> getEdgeParts(){
                int key = epQueue.getFirst();
                epQueue.removeFirst();
                return mergingStructure.remove(key);
        }
        
        /**
         * Get the size of the queue, ie the number of edges GID that have EdgePart
         * associated to them, and are currently waiting to be processed.
         * @return 
         */
        public final int getQueueSize(){
                return epQueue.size();
        }
        
        private void mergeInList(EdgePart ep, List<EdgePart> list){
                if(list.isEmpty()){
                        list.add(ep);
                } else {
                        
                }
        }
}
