
package org.tanato.processing.preprocessing.sewer;

import java.io.File;
import java.util.ArrayList;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Sewer {

	public static DataSourceFactory dsf = new DataSourceFactory();
	public static GeometryFactory gf = new GeometryFactory();
	public static String building = "data/modelisation/chezine/bati_extrait.shp";
	public static String sewer = "data/modelisation/chezine/sewer.shp";

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException {
		// ArrayList<Geometry> sewer =getSewer();
	}

	public static ArrayList<Geometry> getSewer() throws DriverLoadException,
			DataSourceCreationException, DriverException {
		ArrayList<Geometry> result = new ArrayList<Geometry>();
		ArrayList<Geometry> resultok = new ArrayList<Geometry>();
		ArrayList<Geometry> resultnotok = new ArrayList<Geometry>();
		ArrayList<Geometry> housenotok = new ArrayList<Geometry>();

		DataSource mydata1 = dsf.getDataSource(new File(building));
		DataSource mydata2 = dsf.getDataSource(new File(sewer));
		SpatialDataSourceDecorator sds1 = new SpatialDataSourceDecorator(
				mydata1);
		SpatialDataSourceDecorator sds2 = new SpatialDataSourceDecorator(
				mydata2);

		// creation of the tree
		sds1.open();
		ArrayList<Geometry> geoms = new ArrayList<Geometry>();
		for (int i = 0; i < sds1.getRowCount(); i++) {
			Geometry geom1 = sds1.getGeometry(i);
			geoms.add(geom1);
		}
		KDTree3D tree = new KDTree3D(geoms);
		sds1.close();

		// creation of the links
		sds2.open();
		for (int j = 0; j < sds2.getRowCount(); j++) {
				Geometry geom2 = sds2.getGeometry(j);
				LineString newls;
				if (geom2 instanceof Polygon)
				{	Coordinate[] coord = new Coordinate[2];
					coord[0]=((Point)geom2).getCoordinate();
					coord[1]=(tree.nearestNeighborWithInfZ((Polygon)geom2)).getCoordinate();
					newls=gf.createLineString(coord);
					result.add(newls);	
				}

		}

		//research of the cases with some houses in the way
		for (int i=0;i<result.size();i++)
		{	int j = 0;
			boolean b =true;
			while (b && j < sds2.getRowCount())
			{	Geometry geom2 = sds2.getGeometry(j);
				if (geom2 instanceof Polygon)
					{	b=b&&(!geom2.intersects(result.get(i)));
					}
			}
			if (b)	{resultok.add(result.get(i));}	
				else {resultnotok.add(result.get(i));
					housenotok.add((Polygon) sds2.getGeometry(j));
					}
		}

		// resolving the cases with some houses in the way
		//while (resultnotok.size() > 0) {
			
		//}
		sds2.close();
		return result;
	}

}