package org.tanato.model;

import java.util.LinkedList;
import java.util.ListIterator;

public abstract class HydroCell {

	private int gid = -1;

	private LinkedList<HydroCell> parentsCells;

	private LinkedList<HydroCellValued> childrenCells;

	private boolean talweg = false;

	private boolean ridge = false;

	private String type;

	private double cumularea = 0.d;

	boolean isVisited = false;

        private int property;

        private double height;

        private int gidSource;

	int nbParentsVisited = 0;

	public HydroCell() {
                property = -1;
                height = 0;
                gidSource = -1;
		parentsCells = new LinkedList<HydroCell>();
		childrenCells = new LinkedList<HydroCellValued>();
	}

	void setParent(HydroCell hydroCell) {
		if (!parentsCells.contains(hydroCell))
			parentsCells.add(hydroCell);
	}

	public String getHydroCellType(){

		return this.getClass().getSimpleName();

	}

	void setChildren(HydroCell hydroCell) {

		HydroCellValued hydroCellValued = null;
		if (!childExist(hydroCell)) {
			hydroCellValued = new HydroCellValued();
			hydroCellValued.setHydroCell(hydroCell);
			childrenCells.add(hydroCellValued);
		}
	}

	boolean childExist(HydroCell hydroCell){
		boolean exist = false;

		ListIterator<HydroCellValued> iter = childrenCells.listIterator();

		while (iter.hasNext()&& !exist) {
			HydroCellValued hydroCellValued = (HydroCellValued) iter.next();

			if (hydroCellValued.getHydroCell()== hydroCell){
				exist = true;
			}



		}

		return exist;

	}

        /**
         * Property associated to the cell.
         * @return
         */
        public int getProperty() {
                return property;
        }

        /**
         * Set the property associated to the cell.
         * @return
         */
        public void setProperty(int property) {
                this.property = property;
        }

        /**
         * Get the gid of the source
         * @return
         */
        public int getGidSource() {
                return gidSource;
        }

        /**
         * Set the gid of the source
         * @return
         */
        public void setGidSource(int gidSource) {
                this.gidSource = gidSource;
        }

        /**
         * Get the height of the object.
         * @return
         */
        public double getHeight() {
                return height;
        }

        /**
         * set the height of the object.
         * @return
         */
        public void setHeight(double height) {
                this.height = height;
        }



        void updateChildren(LinkedList<HydroCellValued> filsCells) {
		this.childrenCells = filsCells;
	}

	public LinkedList<HydroCell> getParent() {
		return parentsCells;
	}

	public LinkedList<HydroCellValued> getChildrenCells() {
		return childrenCells;
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
		childrenCells = new LinkedList<HydroCellValued>();
	}

	public void removeParent(HydroCell cell){

		parentsCells.remove(cell);

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
		return nbParentsVisited;
	}

	public void setNbParentsVisited(int nbPeresVisited) {
		this.nbParentsVisited = nbPeresVisited;
	}

	public void decrementParents() {
		this.nbParentsVisited--;

	}


}
