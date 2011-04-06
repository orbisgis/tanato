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
	 */
	public final void setOwnerType(int ownerType) {
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
