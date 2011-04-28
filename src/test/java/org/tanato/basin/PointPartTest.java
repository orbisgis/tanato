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

import com.vividsolutions.jts.geom.Coordinate;
import java.security.InvalidParameterException;
import junit.framework.TestCase;

/**
 * Tests on the PointPart class.
 * @author alexis
 */
public class PointPartTest extends TestCase{
        
        public void testPointPart() {
                PointPart pp = new PointPart(new Coordinate(5,5,0), 8,1);
                assertTrue(pp.getPt().equals(new Coordinate(5,5,0)));
                assertTrue(pp.getOwnerGID()==8);
                assertTrue(pp.getOwnerType()==1);
                pp.setPt(new Coordinate(25, 25, 1));
                assertTrue(pp.getPt().equals(new Coordinate(25,25,1)));
                pp.setOwnerGID(7);
                assertTrue(pp.getOwnerGID()==7);
        }
        
        public void testInvalidParameterException() {
                try{
                        PointPart pp = new PointPart(new Coordinate(0,0,4),8,5);
                        assertTrue(false);
                } catch (InvalidParameterException ipe){
                        assertTrue(true);
                }
                PointPart pp = new PointPart(new Coordinate(0,0,4),8,1);
                try {
                        pp.setOwnerType(8);
                        assertTrue(false);
                } catch (InvalidParameterException e) {
                        assertTrue(true);
                }
                PointPart pp2 = new PointPart(new Coordinate(0,0,4),8,1);
                try {
                        pp2.setOwnerType(0);
                        assertTrue(true);
                } catch (InvalidParameterException e) {
                        assertTrue(false);
                }
        }
        
}
