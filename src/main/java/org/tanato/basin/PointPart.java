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

/**
 *
 * @author alexis
 */
class PointPart {

	private Coordinate pt;
	private int ownerGID;
	private int ownerType;
        public final static int POINT_TYPE = 0;
        public final static int EDGE_TYPE = 1;
	/**
	 *
	 * @param orig
	 * @param oGID
	 * @param oType
	 *	* 1 : The owner is an edge
	 *	* 0 : the owner is a point.
         * @throws IllegalArgumentException
         *      when the type of the owner is neither 0 nor 1.
	 */
	public PointPart(Coordinate orig, int oGID, int oType){
		pt = orig;
		ownerGID = oGID;
                if(oType != POINT_TYPE && oType != EDGE_TYPE){
                        throw new IllegalArgumentException("The type of the owner should be 0 or 1 !");
                }
		ownerType = oType;
	}

	/**
	 * Get the GId of the element that owns this point in the mesh. It can be a point
	 * or an edge, respectively if it is a point of the mesh or not.
	 * @return
	 */
	public final int getOwnerGID() {
		return ownerGID;
	}

	/**
	 * Set the GID of the element that owns this point
	 * @param ownerGID
	 */
	public final void setOwnerGID(int ownerGID) {
		this.ownerGID = ownerGID;
	}

	/**
	 * Get the type of the owner
	 * @return
	 *	* 1 : The owner is an edge
	 *	* 0 : the owner is a point.
	 */
	public final int getOwnerType() {
		return ownerType;
	}

	/**
	 * Set the type of the owner.
	 * @param ownerType
         * @throws IllegalArgumentException
         *      if ownerType is neither 0 nor 1.
	 */
	public final void setOwnerType(int ownerType) {
                if(ownerType != POINT_TYPE && ownerType != EDGE_TYPE){
                        throw new IllegalArgumentException("The type of the owner should be 0 or 1 !");
                }
		this.ownerType = ownerType;
	}

	/**
	 * get the coordinate of the point.
	 * @return
	 */
	public final Coordinate getPt() {
		return pt;
	}

	/**
	 * Set the coordinate of the point.
	 * @param pt
	 */
	public final void setPt(Coordinate pt) {
		this.pt = pt;
	}

	@Override
	public String toString(){
		return "GID : "+ownerGID+" - Type : "+(ownerType==0?"Point":"Edge");
	}
	
}
