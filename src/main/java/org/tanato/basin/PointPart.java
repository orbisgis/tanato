/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

	public PointPart(Coordinate orig, int oGID, int oType){
		pt = orig;
		ownerGID = oGID;
		ownerType = oType;
	}

	public int getOwnerGID() {
		return ownerGID;
	}

	public void setOwnerGID(int ownerGID) {
		this.ownerGID = ownerGID;
	}

	public int getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(int ownerType) {
		this.ownerType = ownerType;
	}

	public Coordinate getPt() {
		return pt;
	}

	public void setPt(Coordinate pt) {
		this.pt = pt;
	}

	
}
