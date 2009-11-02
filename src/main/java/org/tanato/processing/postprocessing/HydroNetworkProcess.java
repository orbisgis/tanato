package org.tanato.processing.postprocessing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Constraint;
import org.gdms.data.types.DimensionConstraint;
import org.gdms.data.types.GeometryConstraint;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.memory.ObjectMemoryDriver;
import org.tanato.model.ECell;
import org.tanato.model.HydroCell;
import org.tanato.model.HydroCellValued;
import org.tanato.model.NCell;
import org.tanato.model.TCell;
import org.tanato.utilities.GeomUtil;
import org.tanato.utilities.MathUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class HydroNetworkProcess {

	// Un compteur pour limiter les boucles
	private static final int MAXCOMPTEUR = 50;
	private SpatialDataSourceDecorator sdsFaces;
	private SpatialDataSourceDecorator sdsEdges;
	private SpatialDataSourceDecorator sdsNodes;
	private LinkedList<LineString> pathList;

	public LinkedList<HydroCell> result = new LinkedList<HydroCell>();

	public static GeometryFactory gf = new GeometryFactory();

	public HydroNetworkProcess(SpatialDataSourceDecorator sdsFaces,
			SpatialDataSourceDecorator sdsEdges,
			SpatialDataSourceDecorator sdsNodes) {

		this.sdsFaces = sdsFaces;
		this.sdsEdges = sdsEdges;
		this.sdsNodes = sdsNodes;

	}

	public ObjectMemoryDriver buildRunOffPath(ArrayList<TCell> tcells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY) }, new String[] { "gid",
				"the_geom" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		pathList = new LinkedList<LineString>();

		int compteur = 0;

		for (TCell cell : tcells) {

			Coordinate p = sdsFaces.getGeometry(cell.getGID() - 1)
					.getCentroid().getCoordinate();

			findRunOffPath(p, cell, compteur);

			int k = 0;
			for (Geometry result : pathList) {
				k++;
				driver.addValues(new Value[] { ValueFactory.createValue(k),
						ValueFactory.createValue(result) });

			}

		}

		return driver;

	}

	public ObjectMemoryDriver buildRunOffPath(Coordinate p, HydroCell cell)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY) }, new String[] { "gid",
				"the_geom" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		pathList = new LinkedList<LineString>();

		int compteur = 0;
		findRunOffPath(p, cell, compteur);

		int k = 0;
		for (Geometry result : pathList) {
			k++;
			driver.addValues(new Value[] { ValueFactory.createValue(k),
					ValueFactory.createValue(result) });

		}
		return driver;

	}

	public ObjectMemoryDriver buildRiverNetworkTalweg(ArrayList<ECell> ecells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY, new Constraint[] {
						new GeometryConstraint(GeometryConstraint.LINESTRING),
						new DimensionConstraint(3) }) }, new String[] { "gid",
				"the_geom" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		pathList = new LinkedList<LineString>();


		LinkedList<NCell> talwegOutletList = new LinkedList<NCell>();

		for (ECell ecell : ecells) {

			if (ecell.isTalweg()) {

				NCell ncell = ecell.getBasNcell();
				if (ncell.isTalweg()) {

					if (!talwegOutletList.contains(ncell)) {

						talwegOutletList.add(ncell);
					}

				}

			}

		}

		int compteur = 0;

		for (NCell cell : talwegOutletList) {
			Coordinate p = sdsNodes.getGeometry(cell.getGID() - 1)
					.getCoordinates()[0];

			findRunOffPath(p, cell, compteur);
		}

		int k = 0;

		for (LineString geom : pathList) {

			k++;
			driver.addValues(new Value[] { ValueFactory.createValue(k),
					ValueFactory.createValue(GeomUtil.zReverse(geom)) });

		}

		return driver;

	}

	public ObjectMemoryDriver getAccumulation(ArrayList<ECell> ecells,
			ArrayList<TCell> tcells, ArrayList<NCell> ncells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"the_geom", "sumarea", "area" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		HydroNetworkStrategy hydroNetworkStrategy = new HydroNetworkStrategy(
				ncells, ecells, tcells);

		hydroNetworkStrategy.buildAccumulation();

		System.out.println("Max accumulation :  "
				+ hydroNetworkStrategy.getMaxAccumulation());

		for (ECell ecell : ecells) {

			if (ecell.isTalweg()) {

				Geometry geom = sdsEdges.getGeometry(ecell.getGID() - 1);
				driver.addValues(new Value[] {
						ValueFactory.createValue(ecell.getGID()),
						ValueFactory.createValue(geom),
						ValueFactory.createValue(ecell.getAccumulation()),
						ValueFactory.createValue(0.d) });

			}

		}

		for (TCell ecell : tcells) {

			Geometry geom = sdsFaces.getGeometry(ecell.getGID() - 1);
			driver.addValues(new Value[] {
					ValueFactory.createValue(ecell.getGID()),
					ValueFactory.createValue(geom),
					ValueFactory.createValue(ecell.getAccumulation()),
					ValueFactory.createValue(ecell.getArea()) });

		}

		/*
		 * for (NCell ecell : ncells) {
		 *
		 * Geometry geom = sdsNodes.getGeometry(ecell.getGID() - 1);
		 * driver.addValues(new Value[] {
		 * ValueFactory.createValue(ecell.getGID()),
		 * ValueFactory.createValue(geom),
		 * ValueFactory.createValue(ecell.getAccumulation()),
		 * ValueFactory.createValue(0.d) }); }
		 */

		return driver;

	}

	public ObjectMemoryDriver getAccumulationOnTalweg(ArrayList<ECell> ecells,
			ArrayList<TCell> tcells, ArrayList<NCell> ncells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"the_geom", "sumarea" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		HydroNetworkStrategy hydroNetworkStrategy = new HydroNetworkStrategy(
				ncells, ecells, tcells);

		hydroNetworkStrategy.buildAccumulation();

		for (ECell ecell : ecells) {

			if (ecell.isTalweg()) {

				Geometry geom = sdsEdges.getGeometry(ecell.getGID() - 1);
				driver.addValues(new Value[] {
						ValueFactory.createValue(ecell.getGID()),
						ValueFactory.createValue(geom),
						ValueFactory.createValue(ecell.getAccumulation()) });

			}

		}

		for (NCell ecell : ncells) {

			if (ecell.isTalweg()) {

				Geometry geom = sdsNodes.getGeometry(ecell.getGID() - 1);
				driver.addValues(new Value[] {
						ValueFactory.createValue(ecell.getGID()),
						ValueFactory.createValue(geom),
						ValueFactory.createValue(ecell.getAccumulation()) });

			}

		}

		for (TCell cell : tcells) {

			if (cell.isTalweg()) {

				Geometry geom = sdsFaces.getGeometry(cell.getGID() - 1);
				driver.addValues(new Value[] {
						ValueFactory.createValue(cell.getGID()),
						ValueFactory.createValue(geom),
						ValueFactory.createValue(cell.getAccumulation()) });

			}

		}

		return driver;

	}

	/**
	 * Calcul le parcours d'une goutte d'eau à partir d'un point et de la
	 * cellule référente de ce point.
	 *
	 * @param p
	 * @param cell
	 * @param compteur
	 * @return
	 * @throws DriverException
	 */
	public LinkedList<LineString> findRunOffPath(Coordinate p, HydroCell cell,
			int compteur) throws DriverException {

		if (compteur < MAXCOMPTEUR) {
			// On regarde si on passe sur un triangle
			if (cell instanceof TCell) {

				// On ne fait le calcul sur le triangle
				TCell tCell = (TCell) cell;

				for (HydroCellValued fCell : cell.getFilsCells()) {

					if (fCell.getHydroCell() instanceof ECell) {

						Geometry geom = sdsEdges.getGeometry(
								fCell.getHydroCell().getGID() - 1)
								.getGeometryN(0);
						LineString lineString = (LineString) geom;
						Coordinate coord = MathUtil.getIntersection(lineString
								.getStartPoint().getCoordinate(), lineString
								.getEndPoint().getCoordinate(), p, tCell
								.getSlope());

						if (coord != null) {

							LineString result = gf
									.createLineString(new Coordinate[] { p,
											coord });

							if (!pathList.contains(result)) {

								pathList.add(result);
								findRunOffPath(coord, fCell.getHydroCell(),
										compteur + 1);
							}

						}

					} else if (fCell.getHydroCell() instanceof NCell) {

						Coordinate coordNode = sdsNodes.getGeometry(
								fCell.getHydroCell().getGID() - 1)
								.getCoordinates()[0];
						if (MathUtil.IsColinear(tCell.getSlope(), MathUtil
								.getVector(p, coordNode))) {

							LineString result = gf
									.createLineString(new Coordinate[] { p,
											coordNode });

							if (!pathList.contains(result)) {

								pathList.add(result);

								findRunOffPath(coordNode, fCell.getHydroCell(),
										compteur + 1);
							}

						}

					}

				}

			}

			else if (cell instanceof ECell) {

				if (!cell.isTalweg()){
				for (HydroCellValued fCell : cell.getFilsCells()) {

					if (fCell.getHydroCell() instanceof TCell) {
						findRunOffPath(p, fCell.getHydroCell(), compteur + 1);
					} else if (fCell.getHydroCell() instanceof NCell) {

						Coordinate coordNode = sdsNodes.getGeometry(
								fCell.getHydroCell().getGID() - 1)
								.getCoordinates()[0];
						LineString result = gf.createLineString(new Coordinate[] {
								p, coordNode });

						if (!pathList.contains(result)) {
							pathList.add(result);

							findRunOffPath(coordNode, fCell.getHydroCell(),
									compteur + 1);
						}

					}

				}
				}

			}

			else if (cell instanceof NCell) {

				for (HydroCellValued fCell : cell.getFilsCells()) {

					if (fCell.getHydroCell() instanceof ECell) {

						ECell efCell = (ECell) fCell.getHydroCell();

						NCell baseFCell = efCell.getBasNcell();

						Coordinate coordStartNode = sdsNodes.getGeometry(
								cell.getGID() - 1).getCoordinates()[0];

						Coordinate coordEndNode = sdsNodes.getGeometry(
								baseFCell.getGID() - 1).getCoordinates()[0];

						LineString result = gf.createLineString(new Coordinate[] {
								coordStartNode, coordEndNode });

						if (!pathList.contains(result)) {
							pathList.add(result);

							findRunOffPath(coordEndNode, baseFCell,
									compteur + 1);
						}

					} else {
						findRunOffPath(p, fCell.getHydroCell(), compteur + 1);
					}

				}

			}

		}

		return pathList;

	}

	/**
	 * Calcul le parcours d'une goutte d'eau à partir d'un point et de la
	 * cellule référente de ce point.
	 *
	 * @param p
	 * @param cell
	 * @param compteur
	 * @return
	 * @throws DriverException
	 */
	public LinkedList<LineString> findRunOffPath(Coordinate p, HydroCell cell)
			throws DriverException {

		HydroCell fCell;
		// On regarde si on passe sur un triangle
		if (cell instanceof TCell) {

			// On ne fait le calcul sur le triangle
			TCell tCell = (TCell) cell;

			for (HydroCellValued hydroCellValue : cell.getFilsCells()) {

				fCell = hydroCellValue.getHydroCell();

				if (fCell instanceof ECell) {

					if (!fCell.isTalweg()) {

						Geometry geom = sdsEdges.getGeometry(
								hydroCellValue.getHydroCell().getGID() - 1)
								.getGeometryN(0);
						LineString lineString = (LineString) geom;
						Coordinate coord = MathUtil.getIntersection(lineString
								.getStartPoint().getCoordinate(), lineString
								.getEndPoint().getCoordinate(), p, tCell
								.getSlope());

						if (coord != null) {

							LineString result = gf
									.createLineString(new Coordinate[] { p,
											coord });

							pathList.add(GeomUtil.zReverse(result));

							findRunOffPath(coord, hydroCellValue.getHydroCell());
						}

					} else if (hydroCellValue.getHydroCell() instanceof NCell) {

						Coordinate coordNode = sdsNodes.getGeometry(
								hydroCellValue.getHydroCell().getGID() - 1)
								.getCoordinates()[0];
						if (MathUtil.IsColinear(tCell.getSlope(), MathUtil
								.getVector(p, coordNode))) {

							LineString result = gf
									.createLineString(new Coordinate[] { p,
											coordNode });

							pathList.add(GeomUtil.zReverse(result));

							findRunOffPath(coordNode, hydroCellValue
									.getHydroCell());

						}

					}

				}
			}
		}

		else if (cell instanceof ECell) {

			for (HydroCellValued hydroCellValued : cell.getFilsCells()) {

				fCell = hydroCellValued.getHydroCell();
				if (fCell instanceof TCell) {
					findRunOffPath(p, fCell);
				} else if (fCell instanceof NCell) {

					Coordinate coordNode = sdsNodes.getGeometry(
							fCell.getGID() - 1).getCoordinates()[0];
					LineString result = gf.createLineString(new Coordinate[] {
							p, coordNode });

					pathList.add(GeomUtil.zReverse(result));

					findRunOffPath(coordNode, fCell);

				}

			}

		}

		else if (cell instanceof NCell) {

			for (HydroCellValued hydroCellValued : cell.getFilsCells()) {

				fCell = hydroCellValued.getHydroCell();
				if (fCell instanceof ECell) {

				} else {
					findRunOffPath(p, fCell);
				}

			}

		}

		return pathList;

	}

	/**
	 * Recherche tous les pères d'une hydrocell.
	 *
	 * @param cell
	 * @return
	 */
	public LinkedList getWatershed(HydroCell cell) {

		ListIterator<HydroCell> iter = cell.getPeresCells().listIterator();

		while (iter.hasNext()) {

			HydroCell fCell = iter.next();
			if (!result.contains(fCell)) {
				result.add(fCell);
			}

			getWatershed(fCell);

		}

		return result;

	}

	public LinkedList<LineString> getWatershed(ECell ecell) {

		for (HydroCell peCell : ecell.getPeresCells()) {

			if (peCell instanceof NCell) {

			} else if (peCell instanceof TCell) {

			}

		}

		return pathList;

	}

}
