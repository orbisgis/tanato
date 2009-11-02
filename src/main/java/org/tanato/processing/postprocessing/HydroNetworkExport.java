package org.tanato.processing.postprocessing;

import java.util.ArrayList;
import java.util.Iterator;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.memory.ObjectMemoryDriver;
import org.tanato.model.ECell;
import org.tanato.model.HydroCellValued;
import org.tanato.model.NCell;
import org.tanato.model.TCell;
import org.tanato.utilities.GeomUtil;
import org.tanato.utilities.HydroPolygonUtil;
import org.tanato.utilities.MathUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class HydroNetworkExport {

	private SpatialDataSourceDecorator sdsFaces;
	private SpatialDataSourceDecorator sdsEdges;
	private SpatialDataSourceDecorator sdsNodes;

	public static GeometryFactory gf = new GeometryFactory();

	public HydroNetworkExport(SpatialDataSourceDecorator sdsFaces,
			SpatialDataSourceDecorator sdsEdges,
			SpatialDataSourceDecorator sdsNodes) {

		this.sdsFaces = sdsFaces;
		this.sdsEdges = sdsEdges;
		this.sdsNodes = sdsNodes;

	}

	public ObjectMemoryDriver exportRunOffDirection(ArrayList<TCell> tcells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"the_geom", "slopeindegree" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		for (TCell tCell : tcells) {

			Geometry tcellGeom = sdsFaces.getGeometry(tCell.getGID() - 1);
			Coordinate interiorPoint = tcellGeom.getInteriorPoint()
					.getCoordinate();
			Iterator<HydroCellValued> iterator = tCell.getFilsCells()
					.iterator();

			Coordinate coord = null;
			while (iterator.hasNext()) {

				HydroCellValued fCell = iterator.next();

				if (fCell.getHydroCell() instanceof ECell) {

					ECell eFCell = (ECell) fCell.getHydroCell();

					Geometry geom = sdsEdges.getGeometry(eFCell.getGID() - 1)
							.getGeometryN(0);
					LineString lineString = (LineString) geom;
					coord = MathUtil.getIntersection(lineString.getStartPoint()
							.getCoordinate(), lineString.getEndPoint()
							.getCoordinate(), interiorPoint, tCell.getSlope());

					if (coord != null) {
						Geometry result = null;
						if (interiorPoint.z > coord.z) {
							result = gf.createLineString(new Coordinate[] {
									interiorPoint, coord });

						}

						else if (interiorPoint.z < coord.z) {
							result = gf.createLineString(new Coordinate[] {
									coord, interiorPoint });
						} else {
							result = gf.createLineString(new Coordinate[] {
									interiorPoint, coord });

						}
						driver.addValues(new Value[] {
								ValueFactory.createValue(tCell.getGID()),

								ValueFactory.createValue(result),
								ValueFactory.createValue(tCell
										.getSlopeInDegree()) });
					}
				}

			}
		}

		return driver;

	}

	public ObjectMemoryDriver exportRunOffDirection(ArrayList<ECell> ecells,
			ArrayList<TCell> tcells) throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"the_geom", "slopeindegree" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		for (TCell tCell : tcells) {

			Geometry tcellGeom = sdsFaces.getGeometry(tCell.getGID() - 1);
			Coordinate interiorPoint = tcellGeom.getInteriorPoint()
					.getCoordinate();
			Iterator<HydroCellValued> iterator = tCell.getFilsCells()
					.iterator();

			Coordinate coord = null;
			while (iterator.hasNext()) {

				HydroCellValued fCell = iterator.next();

				if (fCell.getHydroCell() instanceof ECell) {

					ECell eFCell = (ECell) fCell.getHydroCell();

					Geometry geom = sdsEdges.getGeometry(eFCell.getGID() - 1)
							.getGeometryN(0);
					LineString lineString = (LineString) geom;
					coord = MathUtil.getIntersection(lineString.getStartPoint()
							.getCoordinate(), lineString.getEndPoint()
							.getCoordinate(), interiorPoint, tCell.getSlope());

					if (coord != null) {
						Geometry result = null;
						if (interiorPoint.z > coord.z) {
							result = gf.createLineString(new Coordinate[] {
									interiorPoint, coord });

						}

						else if (interiorPoint.z < coord.z) {
							result = gf.createLineString(new Coordinate[] {
									coord, interiorPoint });
						} else {
							result = gf.createLineString(new Coordinate[] {
									interiorPoint, coord });

						}
						driver.addValues(new Value[] {
								ValueFactory.createValue(tCell.getGID()),

								ValueFactory.createValue(result),
								ValueFactory.createValue(tCell
										.getSlopeInDegree()) });
					}
				}

			}

		}

		for (ECell ecell : ecells) {

			if (ecell.isTalweg()) {

				Geometry geom = sdsEdges.getGeometry(ecell.getGID() - 1)
						.getGeometryN(0);
				LineString lineString = (LineString) geom;

				LineString line = GeomUtil.zReverse(lineString);

				driver.addValues(new Value[] {
						ValueFactory.createValue(ecell.getGID()),
						ValueFactory.createValue((Geometry) line),
						ValueFactory.createValue(ecell.getSlopeInDegree()) });

			}

		}

		return driver;

	}

	public ObjectMemoryDriver exportRidge(ArrayList<TCell> tcells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"the_geom", "length" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		int k = 0;
		for (TCell cell : tcells) {

			Polygon polygon = (Polygon) sdsFaces.getGeometry(cell.getGID() - 1)
					.getGeometryN(0);
			HydroPolygonUtil hydroPolygonUtil = new HydroPolygonUtil(polygon);

			LineString line = hydroPolygonUtil.getLigneSeparatrice();

			driver.addValues(new Value[] { ValueFactory.createValue(k),
					ValueFactory.createValue((Geometry) line),
					ValueFactory.createValue(line.getLength()) });

		}
		return driver;
	}

	public ObjectMemoryDriver exportRidge(ArrayList<ECell> ecells,
			ArrayList<TCell> tcells) throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"the_geom", "length" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		int k = 0;
		for (TCell cell : tcells) {

			k++;
			Polygon polygon = (Polygon) sdsFaces.getGeometry(cell.getGID() - 1)
					.getGeometryN(0);
			HydroPolygonUtil hydroPolygonUtil = new HydroPolygonUtil(polygon);

			LineString line = hydroPolygonUtil.getLigneSeparatrice();

			driver.addValues(new Value[] { ValueFactory.createValue(k),
					ValueFactory.createValue((Geometry) line),
					ValueFactory.createValue(line.getLength()) });

		}

		for (ECell cell : ecells) {

			if (cell.isRidge()) {
				k++;
				Geometry geom = sdsEdges.getGeometry(cell.getGID() - 1);

				driver.addValues(new Value[] { ValueFactory.createValue(k),
						ValueFactory.createValue(geom),
						ValueFactory.createValue(geom.getLength()) });

			}

		}
		return driver;
	}

	public ObjectMemoryDriver exportGraphConnexion(ArrayList<TCell> tcells,
			ArrayList<NCell> ncells, ArrayList<ECell> ecells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.FLOAT) }, new String[] { "gid",
				"the_geom", "proportion" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		int k = 0;
		for (TCell cell : tcells) {

			Coordinate coordCell = sdsFaces.getGeometry(cell.getGID() - 1)
					.getCentroid().getCoordinate();
			for (HydroCellValued fCell : cell.getFilsCells()) {

				k++;

				Geometry line = null;

				if (fCell.getHydroCell().isTalweg()) {

					Geometry geom = sdsEdges.getGeometry(fCell.getHydroCell()
							.getGID() - 1);

					Coordinate coordEdge = geom.getCentroid().getCoordinate();

					line = gf.createLineString(new Coordinate[] { coordCell,
							coordEdge });

					driver
							.addValues(new Value[] {
									ValueFactory.createValue(k),
									ValueFactory.createValue(line),
									ValueFactory.createValue(fCell
											.getContribution()) });

					for (HydroCellValued ffcell : fCell.getHydroCell()
							.getFilsCells()) {

						Geometry geomNode = sdsNodes.getGeometry(ffcell
								.getHydroCell().getGID() - 1);

						driver.addValues(new Value[] {
								ValueFactory.createValue(k),
								ValueFactory.createValue(geomNode),
								ValueFactory.createValue(ffcell
										.getContribution()) });

					}

				} else {

					for (HydroCellValued ffcell : fCell.getHydroCell()
							.getFilsCells()) {

						if (ffcell.isTcell()) {

							Coordinate coordffCell = sdsFaces.getGeometry(
									ffcell.getHydroCell().getGID() - 1)
									.getCentroid().getCoordinate();

							line = gf.createLineString(new Coordinate[] {
									coordCell, coordffCell });

							driver.addValues(new Value[] {
									ValueFactory.createValue(k),
									ValueFactory.createValue(line),
									ValueFactory.createValue(fCell
											.getContribution()) });
						}

					}

				}
			}
		}

		for (NCell cell : ncells) {

			if (cell.getGID()!=-1){
			Coordinate coordCell = sdsNodes.getGeometry(cell.getGID() - 1)
					.getCentroid().getCoordinate();
			for (HydroCellValued fCell : cell.getFilsCells()) {

				k++;

				Geometry geom = null;
				Coordinate coordEdge = null;
				if (fCell.isEcell()) {
					ECell eCell = (ECell) fCell.getHydroCell();
					geom = sdsNodes
							.getGeometry(eCell.getBasNcell().getGID() - 1);

					coordEdge = geom.getCoordinate();

				} else if (fCell.isTcell()) {
					geom = sdsFaces
							.getGeometry(fCell.getHydroCell().getGID() - 1);
					coordEdge = geom.getCentroid().getCoordinate();

				}

				Geometry line = gf.createLineString(new Coordinate[] {
						coordCell, coordEdge });
				driver.addValues(new Value[] { ValueFactory.createValue(k),
						ValueFactory.createValue(line),
						ValueFactory.createValue(fCell.getContribution()) });

			}
			}
		}

		return driver;

	}

	public ObjectMemoryDriver exportOutlet(ArrayList<NCell> ncells)
			throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.FLOAT) }, new String[] { "gid",
				"the_geom" });

		ObjectMemoryDriver driver = new ObjectMemoryDriver(metadata);

		int k = 0;

		for (NCell cell : ncells) {

			if (cell.isTalweg() && cell.getFilsCells().size() == 0) {
				Geometry geom = sdsNodes.getGeometry(cell.getGID() - 1);
				driver.addValues(new Value[] { ValueFactory.createValue(k),
						ValueFactory.createValue(geom) });

			}
		}

		return driver;

	}

}
