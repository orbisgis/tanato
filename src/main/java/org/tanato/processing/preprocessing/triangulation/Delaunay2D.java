package org.tanato.processing.preprocessing.triangulation;

import java.util.ArrayList;
import java.util.LinkedList;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
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

public class Delaunay2D {

	private SpatialDataSourceDecorator sds;
	private Delaunay delaunay;
	private ArrayList<DPoint> points;

	public Delaunay2D(SpatialDataSourceDecorator sds) {
		this.sds = sds;
	}

	
	public void compute() {
		computeDelaunay();
	}

	private void computeDelaunay() {
		try {
			sds.open();

			ArrayList<DPoint> points = new ArrayList<DPoint>();
			int gid = 0;
			for (int i = 0; i < sds.getRowCount(); i++) {

				Geometry geom = sds.getGeometry(i);

				Coordinate[] coords = geom.getCoordinates();
				for (int k = 0; k < coords.length - 1; k++) {
					gid++;
					Coordinate coord = coords[k];
					if (Double.isNaN(coord.z)) {
						points.add(new DPoint(coord.x, coord.y, 0, gid));
					} else {
						points.add(new DPoint(coord.x, coord.y, coord.z, gid));
					}

				}
			}

			sds.close();

			ConstrainedMesh aMesh = new ConstrainedMesh();

			aMesh.setPoints(points);

			delaunay = new Delaunay(aMesh);
			aMesh.setStart();

			delaunay.processDelaunay();
			aMesh.setEnd();

		} catch (DriverException e) {
			e.printStackTrace();
		} catch (DelaunayError e) {
			e.printStackTrace();
		}

	}

	public GenericObjectDriver getNodes() throws DriverException {
		if (delaunay != null) {
			DefaultMetadata metadata = new DefaultMetadata(new Type[] {
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.STRING),
					TypeFactory.createType(Type.GEOMETRY) }, new String[] {
					"gid", "type", "the_geom" });

			GenericObjectDriver driverNodes = new GenericObjectDriver(metadata);

			points = delaunay.getMesh().getPoints();

			GeometryFactory gf = new GeometryFactory();

			for (DPoint aPoint : points) {
				int id = aPoint.getGID();

				Point point = gf.createPoint(new Coordinate(aPoint.getX(), aPoint.getY(),
						aPoint.getZ()));

				driverNodes.addValues(new Value[] {
						ValueFactory.createValue(id),
						ValueFactory.createValue(aPoint.type),
						ValueFactory.createValue(point) });
			}
			return driverNodes;

		}
		return null;
	}

	public GenericObjectDriver getEdges() throws DriverException {

		if (delaunay != null) {
			DefaultMetadata metadata = new DefaultMetadata(new Type[] {
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.STRING),
					TypeFactory.createType(Type.GEOMETRY) }, new String[] {
					"gid", "type", "the_geom" });

			GenericObjectDriver driverFaces = new GenericObjectDriver(metadata);
			ArrayList<MyEdge> edges = delaunay.getMesh().getEdges();

			GeometryFactory gf = new GeometryFactory();

			for (DEdge edge : edges) {

				DPoint pts_start = edge.getStartPoint();

				DPoint pts_end = edge.getEndPoint();

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

	public GenericObjectDriver getTriangles() throws DriverException {

		if (delaunay != null) {
			DefaultMetadata metadata = new DefaultMetadata(new Type[] {
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.STRING),
					TypeFactory.createType(Type.GEOMETRY),
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.INT),
					TypeFactory.createType(Type.INT) }, new String[] { "gid",
					"type", "the_geom", "edge1", "edge2", "edge3" });

			GenericObjectDriver driverFaces = new GenericObjectDriver(metadata);
			LinkedList<DTriangle> triangles = delaunay.getMesh()
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
