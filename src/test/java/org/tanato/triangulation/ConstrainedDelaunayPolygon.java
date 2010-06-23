package org.tanato.triangulation;

import java.io.File;
import java.util.ArrayList;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.grap.utilities.EnvelopeUtil;
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
		aMesh.setVerbose(true);
		DataSource mydata = dsf.getDataSource(new File(path));

		SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(mydata);
		sds.open();

		Envelope env = sds.getFullExtent();

		Geometry geomEnv = EnvelopeUtil.toGeometry(env);

		Coordinate[] coords = geomEnv.getCoordinates();

		ArrayList<MyPoint> points = new ArrayList<MyPoint>();

		MyPoint aPoint;
		for (int i = 0; i < coords.length - 1; i++) {
			aPoint=new MyPoint(coords[i]);
			aPoint.setZ(-30);
			points.add(aPoint);

		}

		
		aMesh.setPoints(points);
//		aMesh.addLevelEdge(new MyEdge(new MyPoint(100, -10, 60),new MyPoint(700, 900, 60)));
//		aMesh.createEdge(new MyPoint(100, -10, 60), new MyPoint(700, 900, 60));

		aMesh.processDelaunay();
		
		
		MyPolygon aPolygon;
		for (int i = 0; i < 3; i++) {

			Geometry geom = sds.getGeometry(i);

			for (int j = 0; j < geom.getNumGeometries(); j++) {
				Geometry subGeom = geom.getGeometryN(j);

				if (subGeom instanceof Polygon) {
					aPolygon=new MyPolygon((Polygon) subGeom, 2500);
					aPolygon.setUsePolygonZ(true);
					aPolygon.setEmpty(true);
//					aPolygon.setMustBeTriangulated(true);
					aMesh.addPolygon(aPolygon);
					
				}
			}
		}

		sds.close();


//		aMesh.processDelaunay();			
		
		
		// Set Z coordinate because polygon2d.shp don't have Z coordinate and VRMLexport don't like it.
		points=aMesh.getPoints();
		for(MyPoint aPoint2:points)
		{
			if((aPoint2.getZ()+"").equals("NaN"))
				aPoint2.setZ(0);
		}
		aMesh.setPoints(points);
			
		aMesh.checkTriangularization();
		
		aMesh.VRMLexport("poly.wrl");
		MyDrawing aff2 = new MyDrawing();
		aff2.add(aMesh);
		aMesh.setAffiche(aff2);
		

	}

}
