package org.tanato.processing.preprocessing.triangulation;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
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
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.driver.generic.GenericObjectDriver;
import org.jdelaunay.delaunay.Delaunay;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.MyDrawing;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.List;

public class Modelisation2Delaunay {

	public static DataSourceFactory dsf = new DataSourceFactory();

	public static String pathTopo = "/home/bocher/Bureau/avupur/modelisation1/courbeschezine.shp";

//	public static String pathDitches = "/home/bocher/Bureau/avupur/modelisation1/fosses.shp";

	public static String pathDitches = "/tmp/line.shp";


	private static ConstrainedMesh aMesh;

	private static ArrayList<DPoint> points;

	private static LinkedList<DEdge> breaklines;
	private static int gid;

	/**
	 * @param args
	 * @throws DriverException
	 * @throws DataSourceCreationException
	 * @throws DriverLoadException
	 * @throws DelaunayError
	 */
	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException, DelaunayError {

		long startComputation = Calendar.getInstance().getTime().getTime();

		points = new ArrayList<DPoint>();

		breaklines = new LinkedList<DEdge>();

		DataSource mydata = dsf.getDataSource(new File(pathTopo));

		DataSource mydataditches = dsf.getDataSource(new File(pathDitches));

		SpatialDataSourceDecorator sdsTopo = new SpatialDataSourceDecorator(
				mydata);
		SpatialDataSourceDecorator sdsditches = new SpatialDataSourceDecorator(
				mydataditches);
		sdsTopo.open();

		System.out.println("Start prepare data ");
		gid = 0;

		int zField = sdsTopo.getFieldIndexByName("altitude");

		for (int i = 0; i < sdsTopo.getRowCount(); i++) {

			Geometry geom = sdsTopo.getGeometry(i);

			double zValue = sdsTopo.getFieldValue(i, zField).getAsDouble();
			for (int j = 0; j < geom.getNumGeometries(); j++) {

				Geometry subGeom = geom.getGeometryN(j);

				if (subGeom.getDimension() == 0) {

					for (int k = 0; k < subGeom.getCoordinates().length; k++) {

						Coordinate coord = subGeom.getCoordinates()[k];
						gid++;
						points.add(new DPoint(coord.x, coord.y, zValue, gid));
					}

				}

				else if (subGeom instanceof LineString) {

					LineString lineString = (LineString) subGeom;

					gid++;

					getBreaklines(lineString, zValue);

				}

				else if (subGeom instanceof Polygon) {

					Polygon p = (Polygon) subGeom;

					gid++;

					getBreaklines(p.getExteriorRing(), zValue);
				}
			}

		}

		sdsTopo.close();

		System.out
				.println("End prepare data "
						+ (Calendar.getInstance().getTime().getTime() - startComputation));

		System.out.println("Nombre de breaklines : " + breaklines.size());

		aMesh = new DMesh();
		aMesh.setPoints(points);
		aMesh.setEdges(breaklines);

		// aMesh.saveMesh();
		Delaunay delaunay = new Delaunay(aMesh);

		delaunay.setVerbose(true);
		aMesh.setStart();

		// process triangularization
		delaunay.processDelaunay();

		delaunay.removeFlatTriangles();

		sdsditches.open();

		aMesh.ditchStart();

		for (int i = 0; i < sdsditches.getRowCount(); i++) {

			Geometry geom = sdsditches.getGeometry(i);

			for (int j = 0; j < geom.getNumGeometries(); j++) {
				LinkedList<DPoint> pts = new LinkedList<DPoint>();
				Geometry subGeom = geom.getGeometryN(j);
				if (subGeom instanceof LineString) {

					if (subGeom.isValid()){

					LineString lineString = (LineString) subGeom;
					Coordinate[] coords = lineString.getCoordinates();
					for (int k = 0; k < coords.length; k++) {

						DPoint apoint = new DPoint(coords[k].x, coords[k].y,
								coords[k].z);

						pts.add(apoint);

					}
					}

				}

				aMesh.ditchSet(pts);
			}

		}
		sdsditches.close();
		aMesh.ditchValidate();

		delaunay.morphologicalQualification();

		// Refine Mesh
		// delaunay.setRefinment(Delaunay.refinement_minAngle);
		// delaunay.refineMesh();

		MyDrawing aff = new MyDrawing();
		aff.setTitle("Mesh");
		aff.add(aMesh);
		aMesh.setAffiche(aff);

		saveAll();

		// aMesh.saveMesh();

		aMesh.setEnd();
		System.out.println("Temps de triangulation " + aMesh.getDuration());

	}

	private static void getBreaklines(LineString lineString, double zValue) {
		Coordinate[] coords = lineString.getCoordinates();

		DPoint p1 = new DPoint(coords[0].x, coords[0].y, zValue, gid);
		DPoint p0;
		points.add(p1);
		for (int k = 1; k < coords.length; k++) {

			p0 = p1;
			gid++;
			p1 = new DPoint(coords[k].x, coords[k].y, zValue, gid);
			points.add(p1);
			DEdge edge = new DEdge(p0, p1, gid);
			breaklines.add(edge);
		}

	}

	private static void getBreaklines(LineString lineString) {
		Coordinate[] coords = lineString.getCoordinates();

		DPoint p1 = new DPoint(coords[0].x, coords[0].y, coords[0].z, gid);
		DPoint p0;
		points.add(p1);
		for (int k = 1; k < coords.length; k++) {

			p0 = p1;
			gid++;
			p1 = new DPoint(coords[k].x, coords[k].y, coords[k].z, gid);
			points.add(p1);
			DEdge edge = new DEdge(p0, p1, gid);
			breaklines.add(edge);
		}

	}

	public static void saveAll() throws DriverException {

		getResults();

		System.out.println("Nombre d'edges : " + driverEdges.getRowCount());
		File gdmsFile = new File("/tmp/tinedges.gdms");
		gdmsFile.delete();
		dsf.getSourceManager().register("edges", gdmsFile);

		DataSource ds = dsf.getDataSource(driverEdges);
		ds.open();
		dsf.saveContents("edges", ds);
		ds.close();

		gdmsFile = new File("/tmp/tinfaces.gdms");
		gdmsFile.delete();
		dsf.getSourceManager().register("faces", gdmsFile);

		ds = dsf.getDataSource(driverFaces);
		ds.open();
		dsf.saveContents("faces", ds);
		ds.close();

		gdmsFile = new File("/tmp/tinnodes.gdms");
		gdmsFile.delete();
		dsf.getSourceManager().register("nodes", gdmsFile);

		ds = dsf.getDataSource(driverNodes);
		ds.open();
		dsf.saveContents("nodes", ds);
		ds.close();

	}

	private static GenericObjectDriver driverNodes;

	private static GenericObjectDriver driverEdges;

	private static GenericObjectDriver driverFaces;

	public static void getResults() throws DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.STRING),
				TypeFactory.createType(Type.GEOMETRY, new Constraint[] {
						new GeometryConstraint(GeometryConstraint.LINESTRING),
						new DimensionConstraint(3) }),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE),
				TypeFactory.createType(Type.STRING) }, new String[] { "gid",
				"start_n", "end_n", "left_t", "right_t", "type", "the_geom",
				"slope", "slopedeg", "topo" });

		driverEdges = new GenericObjectDriver(metadata);

		GeometryFactory gf = new GeometryFactory();

		List<DPoint> points = aMesh.getPoints();

		List<DEdge> edges = aMesh.getEdges();

		List<DTriangle> triangles = aMesh.getTriangleList();

		for (DEdge edge : edges) {

			int gidEdge = edges.indexOf(edge) + 1;

			DPoint p1 = edge.getStartPoint();
			DPoint p2 = edge.getEndPoint();

			int gidstart = points.indexOf(p1) + 1;

			int gidend = points.indexOf(p2) + 1;

			Coordinate[] coords = new Coordinate[] {
					new Coordinate(p1.getX(), p1.getY(), p1.getZ()),
					new Coordinate(p2.getX(), p2.getY(), p2.getZ()) };

			Geometry line = gf.createLineString(coords);

			int leftId = -1;
			if (edge.getLeft() == null) {

			} else {
				leftId = triangles.indexOf(edge.getLeft()) + 1;
			}

			int rightId = -1;
			if (edge.getRight() == null) {

			} else {
				rightId = triangles.indexOf(edge.getRight()) + 1;
			}

			String edgeType = edge.getEdgeType();

			Geometry slope = gf.createPoint(edge.getSlope());

			driverEdges.addValues(new Value[] {
					ValueFactory.createValue(gidEdge),
					ValueFactory.createValue(gidstart),
					ValueFactory.createValue(gidend),
					ValueFactory.createValue(leftId),
					ValueFactory.createValue(rightId),
					ValueFactory.createValue(edgeType),
					ValueFactory.createValue(line),
					ValueFactory.createValue(slope),
					ValueFactory.createValue(edge.getSlopeInDegree()),
					ValueFactory.createValue(edge.getTopoType()) });

		}
		metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.STRING),
				TypeFactory.createType(Type.GEOMETRY, new Constraint[] {
						new GeometryConstraint(GeometryConstraint.POINT),
						new DimensionConstraint(3) }) }, new String[] { "gid",
				"type", "the_geom" });

		driverNodes = new GenericObjectDriver(metadata);

		for (DPoint aPoint : points) {
			int id = points.indexOf(aPoint) + 1;

			Point point = gf.createPoint(new Coordinate(aPoint.getX(), aPoint.getY(),
					aPoint.getZ()));

			driverNodes.addValues(new Value[] { ValueFactory.createValue(id),
					ValueFactory.createValue(aPoint.type),
					ValueFactory.createValue(point) });

		}

		metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.STRING),
				TypeFactory.createType(Type.GEOMETRY, new Constraint[] {
						new GeometryConstraint(GeometryConstraint.POLYGON),
						new DimensionConstraint(3) }),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"type", "the_geom", "edge1", "edge2", "edge3", "slope",
				"slopedeg" });

		driverFaces = new GenericObjectDriver(metadata);

		for (DTriangle aTriangle : triangles) {

			List<DPoint> pts = aTriangle.getPoints();

			Coordinate[] coords = new Coordinate[] {
					new Coordinate(pts.get(0).getX(), pts.get(0).getY(), pts.get(0).getZ()),
					new Coordinate(pts.get(1).getX(), pts.get(1).getY(), pts.get(1).getZ()),
					new Coordinate(pts.get(2).getX(), pts.get(2).getY(), pts.get(2).getZ()),
					new Coordinate(pts.get(0).getX(), pts.get(0).getY(), pts.get(0).getZ()) };

			Polygon polygon = gf.createPolygon(gf.createLinearRing(coords),
					null);

			Geometry slope = gf.createPoint(aTriangle.getSlope());

			int id = triangles.indexOf(aTriangle) + 1;

			driverFaces.addValues(new Value[] { ValueFactory.createValue(id),
					ValueFactory.createValue(""),
					ValueFactory.createValue(polygon),
					ValueFactory.createValue(aTriangle.getEdge(0).getGID()),
					ValueFactory.createValue(aTriangle.getEdge(1).getGID()),
					ValueFactory.createValue(aTriangle.getEdge(2).getGID()),
					ValueFactory.createValue(slope),
					ValueFactory.createValue(aTriangle.getSlopeInDegree()) });

		}

	}

}
