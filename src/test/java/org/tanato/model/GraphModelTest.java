package org.tanato.model;

import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.TestCase;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.tanato.SetUpData;

public class GraphModelTest extends TestCase {

	/**
	 *
	 * Same row input and output
	 * @throws DriverException
	 *
	 */
	public void testRowCellNumber() throws DriverException {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<NCell> ncells = SetUpData.getNcells();
		SpatialDataSourceDecorator sdsNodes = SetUpData.getSdsNodes();
		sdsNodes.open();
		long rowCount = SetUpData.getSdsNodes().getRowCount();

		sdsNodes.close();
		assertTrue(ncells.size()== rowCount);

	}

	/**
	 * gid test
	 *
	 * All gid must be greater than 0
	 */
	public void testHydroCellGID() {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<NCell> ncells = SetUpData.getNcells();

		System.out.println(ncells.size());

		for (NCell cell : ncells) {

			System.out.println(cell.getGID());
			assertTrue(cell.getGID() > 0);

		}

		ArrayList<ECell> ecells = SetUpData.getEcells();

		for (ECell cell : ecells) {
			assertTrue(cell.getGID() > 0);
			assertTrue(cell.getStartNodeGID() > 0);
			assertTrue(cell.getEndNodeGID() > 0);
			assertTrue((cell.getLeftGID() == -1 )||(cell.getLeftGID()>0));
			assertTrue((cell.getRightGID() == -1 )||(cell.getRightGID()>0));
		}

		ArrayList<TCell> tcells = SetUpData.getTcells();

		for (TCell cell : tcells) {
			assertTrue(cell.getGID() > 0);
		}

	}

	/**
	 * Ncell father test. A father cannot be a TCELL.
	 */
	public void testNcellFathers() {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<NCell> ncells = SetUpData.getNcells();

		for (NCell cell : ncells) {

			for (HydroCell fcell : cell.getPeresCells()) {

				assertFalse(fcell instanceof TCell);

			}

		}

	}

	/**
	 * Tcell childs test. A child must be an Edge.
	 */
	public void testTcellChilds() {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<TCell> tcells = SetUpData.getTcells();

		for (TCell cell : tcells) {

			for (HydroCellValued fcell : cell.getFilsCells()) {

				assertTrue(fcell.isEcell());

			}

		}

	}

	/**
	 * Sum contribution for a face must be equal to 1
	 */
	public void testTcellSumContribution() {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<TCell> tcells = SetUpData.getTcells();

		for (TCell cell : tcells) {

			float sumContribution = 0;
			for (HydroCellValued fcell : cell.getFilsCells()) {

				float contribution = fcell.getContribution();
				sumContribution = sumContribution + contribution;
			}

			assertTrue(sumContribution == 0 || sumContribution == 1);

		}

	}

	/**
	 * Contribution for all HydroCell must be between 0 to 1
	 */
	public void testHydroCellContribution() {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<TCell> tcells = SetUpData.getTcells();
		ArrayList<ECell> ecells = SetUpData.getEcells();
		ArrayList<NCell> ncells = SetUpData.getNcells();

		for (TCell cell : tcells) {

			for (HydroCellValued fcell : cell.getFilsCells()) {

				float contribution = fcell.getContribution();

				System.out.println("Triangle cell :  " + cell.getGID()
						+ "  Fils : " + fcell.getHydroCell().getGID()
						+ " Cell type  :  "
						+ fcell.getHydroCell().getHydroCellType());

				assertTrue(contribution >= 0 && contribution <= 1);
			}

		}

		for (ECell cell : ecells) {

			for (HydroCellValued fcell : cell.getFilsCells()) {

				float contribution = fcell.getContribution();

				System.out.println("Edge cell :  " + cell.getGID()
						+ "  Fils : " + fcell.getHydroCell().getGID()
						+ " Cell type  :  "
						+ fcell.getHydroCell().getHydroCellType());

				assertTrue(contribution >= 0 && contribution <= 1);
			}

		}

		for (NCell cell : ncells) {

			for (HydroCellValued fcell : cell.getFilsCells()) {

				float contribution = fcell.getContribution();

				System.out.println("Noeud cell :  " + cell.getGID()
						+ "  Fils : " + fcell.getHydroCell().getGID()
						+ " Cell type  :  "
						+ fcell.getHydroCell().getHydroCellType());

				assertTrue(contribution >= 0 && contribution <= 1);
			}

		}

	}

	/**
	 * A ecell talweg must have a contribution equal to 1.
	 *
	 */

	public void testEcellTalwegContribution(){

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<NCell> ncells = SetUpData.getNcells();

		for (NCell cell : ncells) {

			for (HydroCellValued fcell : cell.getFilsCells()) {


				if (fcell.isEcell()){

					if (fcell.getHydroCell().isTalweg()){
						float contribution = fcell.getContribution();

						System.out.println("Noeud cell :  " + cell.getGID()
						+ "  Fils : " + fcell.getHydroCell().getGID()
						+ " Cell type  :  "
						+ fcell.getHydroCell().getHydroCellType() + " Contribution  :  "
						+ contribution);

						assertTrue(contribution == 1);
				}
				}
			}

		}



	}

	/**
	 * A NCell talweg must have only one soon.
	 */
	public void testNcellTalwegChilds() {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<NCell> ncells = SetUpData.getNcells();

		for (NCell cell : ncells) {

			if (cell.isTalweg()) {

				int k = 0;
				for (HydroCellValued fCell : cell.getFilsCells()) {
					k++;
					if (fCell.isNcell()) {
						assertTrue(false);
					} else if (fCell.isTcell()) {

						System.out.println("Nombre de TCell talweg " + k);

					} else if (fCell.isEcell()) {

						System.out.println("Nombre de ECell talweg " + k);

					}

				}
			}

		}

	}

	public void checkDuplicateNCeLL() {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		ArrayList<NCell> ncells = SetUpData.getNcells();

		HashSet<Integer> uniqueGIDList = new HashSet<Integer>();
		for (NCell cell : ncells) {

			System.out.println("Identifiant avant : " + cell.getGID());

			uniqueGIDList.add(cell.getGID());

		}

		for (Integer gid : uniqueGIDList) {

			System.out.println("Identifiant après : " + gid);
		}

	}
}
