/**
 *
 */
package org.tanato.model;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author bocher
 *
 */
public class ECell extends HydroCell {

	private boolean flat = false;
	private boolean transfluent = false;
	private NCell hautNcell;
	private NCell basNcell;
	private Coordinate slope;
	private double slopeInDegree;

	private int leftGID = -1;
	private int rightGID = -1;

	private int startNodeGID = -1;
	private int endNodeGID = -1;

	// TODO ajouter getdroite getgidgauche

	public void setFlat(boolean flat) {
		this.flat = flat;

	}

	public void setTransfluent(boolean transfluent) {
		this.transfluent = transfluent;

	}

	public boolean isTransfluent() {
		return transfluent;
	}

	public boolean isFlat() {
		return flat;
	}

	public void setNodeHaut(NCell hautNCell) {
		this.hautNcell = hautNCell;
	}

	public NCell getHautNcell() {
		return hautNcell;
	}

	public void setHautNcell(NCell hautNcell) {
		this.hautNcell = hautNcell;
	}

	public NCell getBasNcell() {
		return basNcell;
	}

	public void setBasNcell(NCell basNcell) {
		this.basNcell = basNcell;
	}

	public void setSlope(Coordinate slope) {
		this.slope = slope;

	}

	public Coordinate getSlope() {
		return slope;
	}

	public void setSlopeInDegree(double slopeInPercent) {
		this.slopeInDegree = slopeInPercent;

	}

	public double getSlopeInDegree() {
		return slopeInDegree;
	}

	public int getLeftGID() {
		return leftGID;
	}

	public void setLeftGID(int leftGID) {
		this.leftGID = leftGID;
	}

	public int getRightGID() {
		return rightGID;
	}

	public void setRightGID(int rightGID) {
		this.rightGID = rightGID;
	}

	public int getStartNodeGID() {
		return startNodeGID;
	}

	public void setStartNodeGID(int startNODEGID) {
		this.startNodeGID = startNODEGID;
	}

	public int getEndNodeGID() {
		return endNodeGID;
	}

	public void setEndNodeGID(int endNODEGID) {
		this.endNodeGID = endNODEGID;
	}
}
