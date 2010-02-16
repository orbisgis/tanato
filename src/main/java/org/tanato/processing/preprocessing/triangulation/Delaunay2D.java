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
import org.jdelaunay.delaunay.MyEdge;
import org.jdelaunay.delaunay.MyMesh;
import org.jdelaunay.delaunay.MyPoint;
import org.jdelaunay.delaunay.MyTriangle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Delaunay2D {

	private SpatialDataSourceDecorator sds;
	private Delaunay delaunay;
	private ArrayList<MyPoint> points;

	public Delaunay2D(SpatialDataSourceDecorator sds) {
		this.sds = sds;
	}

	
	public void compute() {
		computeDelaunay();
	}

	private void computeDelaunay() {
		try {
			sds.open();

			ArrayList<MyPoint> points = new ArrayList<MyPoint>();
			int gid = 0;
			for (int i = 0; i < sds.getRowCount(); i++) {

				Geometry geom = sds.getGeometry(i);

				Coordinate[] coords = geom.getCoordinates();
				for (int k = 0; k < coords.length - 1; k++) {
					gid++;
					Coordinate coord = coords[k];
					if (Double.isNaN(coord.z)) {
						points.add(new MyPoint(coord.x, coord.y, 0, gid));
					} else {
						points.add(new MyPoint(coord.x, coord.y, coord.z, gid));
					}

				}
			}

			sds.close();

			MyMesh aMesh = new MyMesh();

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

			for (MyPoint aPoint : points) {
				int id = aPoint.getGid();

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

			for (MyEdge edge : edges) {

				MyPoint pts_start = edge.getStart();

				MyPoint pts_end = edge.getEnd();

				Coordinate c1 = new Coordinate(pts_start.x, pts_start.y,
						pts_start.z);
				Coordinate c2 = new Coordinate(pts_end.x, pts_end.y, pts_end.z);

				LineString line = gf
						.createLineString(new Coordinate[] { c1, c2 });

				driverFaces.addValues(new Value[] {
						ValueFactory.createValue(edge.getGid()),
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
			LinkedList<MyTriangle> triangles = delaunay.getMesh()
					.getTriangles();

			GeometryFactory gf = new GeometryFactory();

			for (MyTriangle aTriangle : triangles) {

				MyPoint[] pts = aTriangle.points;

				Coordinate[] coords = new Coordinate[] {
						new Coordinate(pts[0].x, pts[0].y, pts[0].z),
						new Coordinate(pts[1].x, pts[1].y, pts[1].z),
						new Coordinate(pts[2].x, pts[2].y, pts[2].z),
						new Coordinate(pts[0].x, pts[0].y, pts[0].z) };

				Polygon polygon = gf.createPolygon(gf.createLinearRing(coords),
						null);

				driverFaces.addValues(new Value[] {
						ValueFactory.createValue(aTriangle.getGid()),
						ValueFactory.createValue(""),
						ValueFactory.createValue(polygon),
						ValueFactory.createValue(aTriangle.edge(0).getGid()),
						ValueFactory.createValue(aTriangle.edge(1).getGid()),
						ValueFactory.createValue(aTriangle.edge(2).getGid()) });

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
