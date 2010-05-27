package org.tanato.triangulation;

import java.io.File;
import java.util.ArrayList;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.geometryUtils.EnvelopeUtil;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.MyDrawing;
import org.jdelaunay.delaunay.MyMesh;
import org.jdelaunay.delaunay.MyPoint;
import org.jdelaunay.delaunay.MyPolygon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

public class ConstrainedDelaunayPolygon {

	public static DataSourceFactory dsf = new DataSourceFactory();

	public static String path = "src/test/resources/data/source/polygon/polygon2d.shp";

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException, DelaunayError,
			ParseException {

		MyMesh aMesh = new MyMesh();
		DataSource mydata = dsf.getDataSource(new File(path));

		SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(mydata);
		sds.open();

		Envelope env = sds.getFullExtent();

		Geometry geomEnv = EnvelopeUtil.toGeometry(env);

		Coordinate[] coords = geomEnv.getCoordinates();

		ArrayList<MyPoint> points = new ArrayList<MyPoint>();

		for (int i = 0; i < coords.length - 1; i++) {
			points.add(new MyPoint(coords[i]));

		}

		aMesh.setPoints(points);

		for (int i = 0; i < 1; i++) {

			Geometry geom = sds.getGeometry(i);

			for (int j = 0; j < geom.getNumGeometries(); j++) {
				Geometry subGeom = geom.getGeometryN(j);

				if (subGeom instanceof Polygon) {
					aMesh.addPolygon(new MyPolygon((Polygon) subGeom));
				}
			}
		}

		sds.close();

		aMesh.processDelaunay();

		MyDrawing aff2 = new MyDrawing();
		aff2.add(aMesh);
		aMesh.setAffiche(aff2);
		System.out.println("read");
	}

}
