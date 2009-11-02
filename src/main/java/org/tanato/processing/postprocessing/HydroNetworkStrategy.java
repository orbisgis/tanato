package org.tanato.processing.postprocessing;

import java.util.ArrayList;
import java.util.LinkedList;

import org.tanato.model.ECell;
import org.tanato.model.HydroCell;
import org.tanato.model.HydroCellValued;
import org.tanato.model.NCell;
import org.tanato.model.TCell;

public class HydroNetworkStrategy {

	private ArrayList<NCell> nCells;
	private ArrayList<ECell> eCells;
	private ArrayList<TCell> tCells;
	private double maxAccumulation;

	public HydroNetworkStrategy(ArrayList<NCell> nCells,
			ArrayList<ECell> eCells, ArrayList<TCell> tCells) {

		this.nCells = nCells;
		this.eCells = eCells;
		this.tCells = tCells;
	}

	/**
	 * Les données sont ordonnées afin que les accumulations soient calculées à
	 * partir des cellules sommets.
	 */
	public void buildAccumulation() {

		LinkedList<HydroCell> hydrocellList = new LinkedList<HydroCell>();

		for (NCell cell : nCells) {

			if (cell.getPeresCells().size() == 0) {

				hydrocellList.add(cell);
			}

			cell.setNbPeresVisited(cell.getPeresCells().size());

		}

		for (ECell cell : eCells) {
			if (cell.getPeresCells().size() == 0) {

				hydrocellList.add(cell);
			}

			cell.setNbPeresVisited(cell.getPeresCells().size());
		}

		for (TCell cell : tCells) {
			if (cell.getPeresCells().size() == 0) {

				hydrocellList.add(cell);
			}

			cell.setAccumulationArea(cell.getArea());

			cell.setNbPeresVisited(cell.getPeresCells().size());
		}

		maxAccumulation = Double.NaN;
		while (!hydrocellList.isEmpty()) {

			while (!hydrocellList.isEmpty()) {
				HydroCell cell = hydrocellList.getFirst();

				hydrocellList.removeFirst();

				double area = cell.getAccumulation();

				for (HydroCellValued fcell : cell.getFilsCells()) {

					double contrib = fcell.getContribution();

					double accumulation = contrib * area;
					fcell.getHydroCell().setAccumulationArea(accumulation);

					if (Double.isNaN(maxAccumulation)) {
						maxAccumulation = accumulation;
					} else {
						if (accumulation > maxAccumulation) {
							maxAccumulation = accumulation;
						}
					}

					fcell.getHydroCell().decrementPeres();

					if (fcell.getHydroCell().getNbPeresToBeVisited() == 0) {
						hydrocellList.add(fcell.getHydroCell());
					}

				}

			}

			int valMin = -1;
			TCell tCellRestart = null;

			for (TCell cell : tCells) {
				int nbPeres = cell.getNbPeresToBeVisited();
				if (nbPeres > 0) {

					if (tCellRestart == null) {

						valMin = nbPeres;
						tCellRestart = cell;
					}

					else {
						if (valMin > nbPeres) {

							valMin = nbPeres;
							tCellRestart = cell;
						}

					}
				}

			}

			if (tCellRestart != null) {
				hydrocellList.add(tCellRestart);
				tCellRestart.setNbPeresVisited(0);
			}

		}

	}

	public double getMaxAccumulation() {
		return maxAccumulation;
	}

	/**
	 * Construit un nouveau réseau l'extraction des axes d'écoulements.
	 */
	public void buildRiverNetwork() {

		LinkedList<HydroCell> networkList = new LinkedList<HydroCell>();
		for (NCell cell : nCells) {

			if (cell.isTalweg()) {
				for (HydroCellValued fCell : cell.getFilsCells()) {

					if (fCell.isEcell()) {
						networkList.add(fCell.getHydroCell());

					}

				}
			}
		}

		for (ECell cell : eCells) {

			if (cell.isTalweg()) {
				for (HydroCellValued fCell : cell.getFilsCells()) {

					if (fCell.isNcell()) {
						if (networkList.contains(fCell.getHydroCell())){
						networkList.add(fCell.getHydroCell());
						}

					}

				}
			}

		}

	}

}
