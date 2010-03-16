package org.tanato.processing.preprocessing.road;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import com.vividsolutions.jts.noding.SegmentString;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.operation.buffer.OffsetCurveBuilder;
import com.vividsolutions.jts.operation.buffer.OffsetCurveSetBuilder;

public class RoadBuilder {
	public static DataSourceFactory dsf = new DataSourceFactory();
	public static GeometryFactory gf = new GeometryFactory();

	public static ArrayList<Geometry> getRoad(String roadDataPath, double dist, BufferParameters bufParams) throws DriverLoadException,
	DataSourceCreationException, DriverException {
		ArrayList<Geometry> result = new ArrayList<Geometry>();
		DataSource mydata1 = dsf.getDataSource(new File(roadDataPath));
		SpatialDataSourceDecorator sds1 = new SpatialDataSourceDecorator(
				mydata1);
		sds1.open();
		for (int i = 0; i < sds1.getRowCount(); i++) {
			LineString g = (LineString) sds1.getGeometry(i).getGeometryN(0);
			for (int j=1;j<g.getNumPoints();j++)
			{
				Coordinate[] coords= new Coordinate[2];
				coords[0]=g.getCoordinateN(j-1);
				coords[1]=g.getCoordinateN(j);
				LineString gline = gf.createLineString(coords);
				result.add(buildCurveSet(gline,dist,bufParams));
			}

		}
		sds1.close();
		return result;
	}



	private static Geometry buildCurveSet(LineString g, double dist, BufferParameters bufParams)
	{
		Coordinate a=g.getCoordinates()[0];
		Coordinate b=g.getCoordinates()[1];
		// --- now construct curve
		OffsetCurveBuilder ocb = new OffsetCurveBuilder(
				g.getFactory().getPrecisionModel(),
				bufParams);
		OffsetCurveSetBuilder ocsb = new OffsetCurveSetBuilder(g, dist, ocb);
		List curves = ocsb.getCurves();
		List lines = new ArrayList();
		for (Iterator i = curves.iterator(); i.hasNext(); ) {
			SegmentString ss = (SegmentString) i.next();
			Coordinate[] pts = ss.getCoordinates();
			for (int j=0;j<pts.length;j++)
			{
				Coordinate c = pts[j];
				double coef =0.0;
				if (a.x!=b.x)
				{
					coef=(c.x-a.x)/(b.x-a.x);}
				else {
					if (a.y!=b.y)
					{
						coef=(c.y-a.y)/(b.y-a.y);}
				}
				c.z=a.z+coef*(b.z-a.z);
			}
			lines.add(g.getFactory().createLineString(pts));
		}
		Geometry curve = g.getFactory().buildGeometry(lines);
		return curve;
	}
}
