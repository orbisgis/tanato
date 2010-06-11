package org.tanato.triangulation;

import java.io.File;
import java.io.IOException;
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
import org.jdelaunay.delaunay.MyEdge;
import org.jdelaunay.delaunay.MyMesh;
import org.jdelaunay.delaunay.MyPoint;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;

public class ConstrainedDelaunayLevelEdge {

	public static DataSourceFactory dsf = new DataSourceFactory();

	public static String path = "src/test/resources/data/source/chezine/courbes_niveaux.shp";

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException, DelaunayError,
			ParseException, IOException {

		long start = System.currentTimeMillis();
		
		MyMesh aMesh = new MyMesh();
		aMesh.setVerbose(true);
		DataSource mydata = dsf.getDataSource(new File(path));

		SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(mydata);
		sds.open();

		Envelope env = sds.getFullExtent();

		Geometry geomEnv = EnvelopeUtil.toGeometry(env);

		Coordinate[] coords = geomEnv.getCoordinates();

		ArrayList<MyPoint> points = new ArrayList<MyPoint>();

		for (int i = 0; i < coords.length - 1; i++) {
			coords[i].z = 0;
			points.add(new MyPoint(coords[i]));
		}

		int z;
		for (long i = 0; i < sds.getRowCount(); i++) {
			Geometry geom = sds.getGeometry(i);

			z = sds.getFieldValue(i, 2).getAsInt();

			for (int j = 0; j < geom.getNumGeometries(); j++) {
				Geometry subGeom = geom.getGeometryN(j);

				if (subGeom instanceof LineString) {

					Coordinate c1 = subGeom.getCoordinates()[0];
					Coordinate c2;
					c1.z = z;
					for (int k = 1; k < subGeom.getCoordinates().length; k++) {
						c2 = subGeom.getCoordinates()[k];
						c2.z = z;
						aMesh.addLevelEdge(new MyEdge(new MyPoint(c1),
								new MyPoint(c2)));
						c1 = c2;
					}
				}
			}
		}

		sds.close();

		aMesh.processDelaunay();
		
		System.out.println("\npoint : "+aMesh.getNbPoints()+"\nedges : "+aMesh.getNbEdges()+"\ntriangles : "+aMesh.getNbTriangles());
		
		
//		aMesh.removeFlatTriangles();//FIXME too long!

		
		long end = System.currentTimeMillis();
		System.out.println("Duration " + (end-start)+"ms soit "+((end-start)/60000)+"min");
		
		MyDrawing aff2 = new MyDrawing();
		aff2.add(aMesh);
		aMesh.setAffiche(aff2);
//		aMesh.VRMLexport();

		


	}

}
