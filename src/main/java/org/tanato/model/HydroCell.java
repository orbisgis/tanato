package org.tanato.model;

import java.util.LinkedList;
import java.util.ListIterator;

public abstract class HydroCell {

	private int gid = -1;

	private LinkedList<HydroCell> peresCells;

	private LinkedList<HydroCellValued> filsCells;

	private boolean talweg = false;

	private boolean ridge = false;

	private String type;

	private double cumularea = 0.d;

	boolean isVisited = false;

	int nbPeresVisited = 0;

	public HydroCell() {
		peresCells = new LinkedList<HydroCell>();
		filsCells = new LinkedList<HydroCellValued>();
	}

	void setParent(HydroCell hydroCell) {
		if (!peresCells.contains(hydroCell))
			peresCells.add(hydroCell);
	}

	public String getHydroCellType(){

		return this.getClass().getSimpleName();

	}

	void setChildren(HydroCell hydroCell) {

		HydroCellValued hydroCellValued = null;
		if (!childExist(hydroCell)) {
			hydroCellValued = new HydroCellValued();
			hydroCellValued.setHydroCell(hydroCell);
			filsCells.add(hydroCellValued);
		}
	}

	boolean childExist(HydroCell hydroCell){
		boolean exist = false;

		ListIterator<HydroCellValued> iter = filsCells.listIterator();

		while (iter.hasNext()&& !exist) {
			HydroCellValued hydroCellValued = (HydroCellValued) iter.next();

			if (hydroCellValued.getHydroCell()== hydroCell){
				exist = true;
			}



		}

		return exist;

	}


	void updateChildren(LinkedList<HydroCellValued> filsCells) {
		this.filsCells = filsCells;
	}

	public LinkedList<HydroCell> getParent() {
		return peresCells;
	}

	public LinkedList<HydroCellValued> getChildrenCells() {
		return filsCells;
	}

	public void setTalweg(boolean talweg) {
		this.talweg = talweg;
	}

	public void setRidge(boolean ridge) {
		this.ridge = ridge;

	}

	public boolean isTalweg() {
		return talweg;
	}

	public boolean isRidge() {
		return ridge;
	}

	public int getGID() {
		return gid;
	}

	void setGID(int gid) {
		this.gid = gid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void cleanChildren() {
		filsCells = new LinkedList<HydroCellValued>();
	}

	public void removeParent(HydroCell cell){

		peresCells.remove(cell);

	}

	public void setAccumulationArea(double cumularea) {
		this.cumularea += cumularea;
	}

	public double getAccumulation() {
		return cumularea;
	}

	public boolean isVisited() {
		return isVisited;
	}

	public void setVisited(boolean isVisited) {
		this.isVisited = isVisited;
	}

	public int getNbParentsToBeVisited() {
		return nbPeresVisited;
	}

	public void setNbParentsVisited(int nbPeresVisited) {
		this.nbPeresVisited = nbPeresVisited;
	}

	public void decrementParents() {
		this.nbPeresVisited--;

	}


}
