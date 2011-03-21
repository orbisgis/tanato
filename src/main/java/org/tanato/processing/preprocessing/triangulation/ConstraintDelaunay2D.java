package org.tanato.processing.preprocessing.triangulation;

import java.util.ArrayList;
import java.util.LinkedList;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.DiskBufferDriver;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.MyDrawing;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.List;

public class ConstraintDelaunay2D {

	private SpatialDataSourceDecorator sds;
	private Delaunay delaunay;
	private ArrayList<DPoint> points;
	private LinkedList<DEdge> breaklines;
	private DataSourceFactory dsf;
	private boolean verbose = false;
	private boolean morphologicalQualification = false;

	public ConstraintDelaunay2D(DataSourceFactory dsf,
			SpatialDataSourceDecorator sds) {
		this.sds = sds;
		this.dsf = dsf;
	}

	public void compute() {
		computeDelaunay();
	}

	private void computeDelaunay() {
		try {
			sds.open();

			points = new ArrayList<DPoint>();
			breaklines = new LinkedList<DEdge>();
			for (int i = 0; i < sds.getRowCount(); i++) {

				Geometry geom = sds.getGeometry(i);

				addConstraint(geom, points, breaklines);
			}

			sds.close();

			ConstrainedMesh aMesh = new ConstrainedMesh();

			aMesh.setPoints(points);

			delaunay = new Delaunay(aMesh);
			delaunay.setVerbose(verbose);
			aMesh.setStart();
			delaunay.processDelaunay();
			if (morphologicalQualification) {
				delaunay.morphologicalQualification();
			}
			aMesh.setEnd();

		} catch (DriverException e) {
			e.printStackTrace();
		} catch (DelaunayError e) {
			e.printStackTrace();
		}

	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setMorphologicalQualification(boolean morphologicalQualification) {
		this.morphologicalQualification = morphologicalQualification;
	}

	private void addConstraint(Geometry geometry, ArrayList<DPoint> points,
			LinkedList<DEdge> breakLineList) {
		if (geometry instanceof Point) {
			points.add(toMyPoint(geometry.getCoordinate()));
		} else if (geometry instanceof LineString) {
			addConstraint((LineString) geometry, points, breakLineList);
		} else if (geometry instanceof Polygon) {
			Polygon polygon = (Polygon) geometry;
			addConstraint(polygon.getExteriorRing(), points, breakLineList);
			for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
				addConstraint(polygon.getInteriorRingN(i), points,
						breakLineList);
			}
		} else if (geometry instanceof GeometryCollection) {
			GeometryCollection gc = (GeometryCollection) geometry;
			for (int i = 0; i < gc.getNumGeometries(); i++) {
				addConstraint(gc.getGeometryN(i), points, breakLineList);
			}
		}
	}

	private void addConstraint(LineString lineString,
			ArrayList<DPoint> points, LinkedList<DEdge> breakLineList) {
		Coordinate[] coords = lineString.getCoordinates();

		DPoint p1 = toMyPoint(coords[0]);
		DPoint p0;
		points.add(p1);
		for (int k = 1; k < coords.length; k++) {

			p0 = p1;
			p1 = toMyPoint(coords[k]);
			points.add(p1);
			DEdge edge = new DEdge(p0, p1);
			breaklines.add(edge);
		}
	}

	public DPoint toMyPoint(Coordinate coord) throws DelaunayError {

		if (Double.isNaN(coord.z)) {
			return new DPoint(coord.x, coord.y, 0);
		} else {
			return new DPoint(coord.x, coord.y, coord.z);
		}

	}

	public ObjectDriver getNodes() throws DriverException {
		if (delaunay != null) {
			DefaultMetadata metadata = new DefaultMetadata(new Type[] {
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.STRING),
					TypeFactory.createType(Type.GEOMETRY) }, new String[] {
					"gid", "type", "the_geom" });

			DiskBufferDriver driverNodes = new DiskBufferDriver(dsf, metadata);

			points = delaunay.getMesh().getPoints();

			GeometryFactory gf = new GeometryFactory();

			for (DPoint aPoint : points) {
				int id = aPoint.getGID();

				Point point = gf.createPoint(new Coordinate(aPoint.x, aPoint.y,
						aPoint.z));

				driverNodes.addValues(new Value[] {
						ValueFactory.createValue(id),
						ValueFactory.createValue(aPoint.type),
						ValueFactory.createValue(point) });
			}
			return driverNodes;

		}
		return null;
	}

	public ObjectDriver getEdges() throws DriverException {

		if (delaunay != null) {
			DefaultMetadata metadata = new DefaultMetadata(new Type[] {
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.STRING),
					TypeFactory.createType(Type.GEOMETRY) }, new String[] {
					"gid", "type", "the_geom" });

			DiskBufferDriver driverFaces = new DiskBufferDriver(dsf, metadata);
			ArrayList<MyEdge> edges = delaunay.getMesh().getEdges();

			GeometryFactory gf = new GeometryFactory();

			for (DEdge edge : edges) {

				DPoint pts_start = edge.getStart();

				DPoint pts_end = edge.getEnd();

				Coordinate c1 = new Coordinate(pts_start.getX(), pts_start.getY(),
						pts_start.getZ());
				Coordinate c2 = new Coordinate(pts_end.getX(), pts_end.getY(), pts_end.getZ());

				LineString line = gf
						.createLineString(new Coordinate[] { c1, c2 });

				driverFaces.addValues(new Value[] {
						ValueFactory.createValue(edge.getGID()),
						ValueFactory.createValue(""),
						ValueFactory.createValue(line) });

			}

			return driverFaces;
		}
		return null;
	}

	public ObjectDriver getTriangles() throws DriverException {

		if (delaunay != null) {
			DefaultMetadata metadata = new DefaultMetadata(new Type[] {
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.STRING),
					TypeFactory.createType(Type.GEOMETRY),
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.INT) }, new String[] { "gid",
					"type", "the_geom", "edge1", "edge2", "edge3" });

			DiskBufferDriver driverFaces = new DiskBufferDriver(dsf, metadata);
			List<DTriangle> triangles = delaunay.getMesh()
					.getTriangles();

			GeometryFactory gf = new GeometryFactory();

			for (DTriangle aTriangle : triangles) {

				List<DPoint> pts = aTriangle.getPoints();

				Coordinate[] coords = new Coordinate[] {
					new Coordinate(pts.get(0).getX(), pts.get(0).getY(), pts.get(0).getZ()),
					new Coordinate(pts.get(1).getX(), pts.get(1).getY(), pts.get(1).getZ()),
					new Coordinate(pts.get(2).getX(), pts.get(2).getY(), pts.get(2).getZ()),
					new Coordinate(pts.get(0).getX(), pts.get(0).getY(), pts.get(0).getZ()) };

				Polygon polygon = gf.createPolygon(gf.createLinearRing(coords),
						null);

				driverFaces.addValues(new Value[] {
						ValueFactory.createValue(aTriangle.getGID()),
						ValueFactory.createValue(""),
						ValueFactory.createValue(polygon),
						ValueFactory.createValue(aTriangle.getEdge(0).getGID()),
						ValueFactory.createValue(aTriangle.getEdge(1).getGID()),
						ValueFactory.createValue(aTriangle.getEdge(2).getGID()) });

			}
			return driverFaces;
		}
		return null;
	}

	public void show() {

		if (delaunay != null) {
			MyDrawing aff2 = new MyDrawing();
			aff2.add(delaunay.getMesh());
			delaunay.getMesh().setAffiche(aff2);

		}

	}

	public MyMesh getMesh() {
		if (delaunay != null)
			return delaunay.getMesh();
		return null;
	}
}
