package org.tanato.model;

public class HydroCellValued {

	private HydroCell hydroCell;

	float contribution = 1.f;


	public HydroCellValued(){

	}


	public String getHydroCellType(){
		return hydroCell.getClass().getSimpleName();

	}
	void setHydroCell(HydroCell hydroCell) {
		this.hydroCell = hydroCell;
	}

	public HydroCell getHydroCell() {
		return hydroCell;
	}

	public float getContribution() {
		return contribution;
	}

	public void setContribution(float contribution) {
		this.contribution = contribution;
	}

	public boolean isTcell() {

		return (hydroCell instanceof TCell);

	}

	public boolean isEcell() {

		return (hydroCell instanceof ECell);

	}

	public boolean isNcell() {

		return (hydroCell instanceof NCell);

	}

}
