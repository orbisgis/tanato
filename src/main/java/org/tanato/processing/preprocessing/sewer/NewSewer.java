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

public class NewSewer {

	public static DataSourceFactory dsf = new DataSourceFactory();
	public static GeometryFactory gf = new GeometryFactory();
	public static int MAXVALUE = 10000000;

	public static ArrayList<Geometry> getSewer(String buildingDataPath, String sewerDataPath) throws DriverLoadException,
			DataSourceCreationException, DriverException {
		ArrayList<Geometry> result = new ArrayList<Geometry>();
		DataSource mydata1 = dsf.getDataSource(new File(buildingDataPath));
		DataSource mydata2 = dsf.getDataSource(new File(sewerDataPath));
		SpatialDataSourceDecorator sds1 = new SpatialDataSourceDecorator(
				mydata1);
		SpatialDataSourceDecorator sds2 = new SpatialDataSourceDecorator(
				mydata2);


		sds1.open();
		for (int i = 0; i < sds1.getRowCount(); i++) {
			sds2.open();
			Polygon bati = (Polygon) sds1.getGeometry(i).getGeometryN(0);
			// calculation of the nearest sewer
			ProjectedPoint sewerdist = sewerIndex(bati, sds2);
			Coordinate[] coordc = new Coordinate[2];
			Coordinate[] coordb = new Coordinate[2];
			if (sewerdist.getPoint() != null) {
				coordc[0] = (bati.getCentroid()).getCoordinate();
				coordc[1] = (sewerdist.getPoint()).getCoordinate();
			}
			sds2.close();
			//calculate the posible habitation
			ProjectedPoint batidist =batiIndex(bati,sds1);
			if (batidist.getPoint() != null) {
				coordb[0] = (bati.getCentroid()).getCoordinate();
				coordb[1] = (batidist.getPoint()).getCoordinate();
			}
			//choose between ewer and habitation
			if ((sewerdist.getPoint() != null)	&& (batidist.getPoint() != null) )
				{if ((1/sewerdist.getDist())>batidist.getDist())
				{result.add(gf.createLineString(coordc));}
				else
				{result.add(gf.createLineString(coordb));}
			}
			else
			{
				if (sewerdist.getPoint() != null)	
				{result.add(gf.createLineString(coordc));}
				if (batidist.getPoint() != null) 
				{result.add(gf.createLineString(coordb));}
			}
		}
		sds1.close();
		return result;
	}

	/**
	 * Return the vector which represent the direction of the slope
	 */
	public static Point getVectorSlope(Polygon bati) {
		Coordinate c = new Coordinate(0.0, 0.0);
		Coordinate[] coord = bati.getCoordinates();

		for (int i = 0; (i < bati.getNumPoints() - 2); i++) {
			Coordinate p1 = coord[i];
			Coordinate p2 = coord[i + 1];
			Coordinate p3 = coord[i + 2];
			c.x = c.x + (p2.y - p1.y) * (p3.z - p1.z) - (p2.z - p1.z)
					* (p3.y - p1.y);
			c.y = c.y + (p2.z - p1.z) * (p3.x - p1.x) - (p2.x - p1.x)
					* (p3.z - p1.z);
		}
		c.x = c.x / (bati.getNumPoints() - 2);
		c.y = c.y / (bati.getNumPoints() - 2);
		Point P = gf.createPoint(c);
		return P;
	}

	/**
	 * Return the nearest projected point of the habitation on the sewer
	 */
	public static ProjectedPoint SewerBatiDistance(LineString sewer,
			Polygon bati) {
		Point s = getVectorSlope(bati);
		Point c = bati.getCentroid();
		Point pfinal = null;
		float minDist = MAXVALUE;

		// for each point part of the sewer, we calculate the projected of the
		// centroid of the habitation
		for (int i = 0; i < (sewer.getNumPoints() - 1); i++) {
			Point p1 = sewer.getPointN(i);
			Point p2 = sewer.getPointN(i + 1);
			double Sx = 0;
			double Sy = 0;
			boolean intersection = true;
			if (p1.getX() == p2.getX()) {
				if (s.getX() == 0) {
					intersection = false;
				} else {
					double pCD = (s.getY()) / (s.getX());
					Sx = p1.getX();
					Sy = pCD * (p1.getX() - c.getX()) + c.getY();
				}
			} else {
				if (s.getX() == 0) {
					double pAB = (p1.getY() - p2.getY())
							/ (p1.getX() - p2.getX());
					Sx = c.getX();
					Sy = pAB * (c.getX() - p1.getX()) + p1.getY();
				} else {
					double pCD = (s.getY()) / (s.getX());
					double pAB = (p1.getY() - p2.getY())
							/ (p1.getX() - p2.getX());
					double oCD = c.getY() - pCD * c.getX();
					double oAB = p1.getY() - pAB * p1.getX();
					Sx = (oAB - oCD) / (pCD - pAB);
					Sy = pCD * Sx + oCD;
				}
			}

			// we check if the projected point is on the segment in the good
			// direction of the habitation
			if (intersection
					&& ((Sx < p1.getX() && Sx < p2.getX())
							| (Sx > p1.getX() && Sx > p2.getX())
							| (Sx < c.getX() && s.getX() > 0)
							| (Sx > c.getX() && s.getX() < 0)
							| (Sy < p1.getY() && Sy < p2.getY())
							| (Sy > p1.getY() && Sy > p2.getY())
							| (Sy < c.getY() && s.getY() > 0) | (Sy > c.getY() && s
							.getY() < 0))) {
				intersection = false;
			}

			double coeff = (Sx - p1.getX()) / (p2.getX() - p1.getX());
			double Sz = p1.getCoordinate().z + coeff
					* (p2.getCoordinate().z - p1.getCoordinate().z);

			if (intersection && ((c.getCoordinate().z) < Sz)) {
				intersection = false;
			}

			if (intersection) {
				float dist = (float) Math.sqrt((c.getX() - Sx)
						* (c.getX() - Sx) + (c.getY() - Sy) * (c.getY() - Sy));
				if (dist < minDist) {
					minDist = dist;
					pfinal = gf.createPoint(new Coordinate(Sx, Sy, Sz));
				}
			}
		}
		ProjectedPoint result = new ProjectedPoint(pfinal, minDist);
		return result;
	}

	/**
	 * Return the distance between the centroids of 2 habitations
	 */
	public static float BatiBatiDistance(Polygon bati1,
			Polygon bati2) {
		Point c1 = bati1.getCentroid();
		Point c2 = bati2.getCentroid();
		float dist = (float) Math.sqrt((c1.getX() - c2.getX())
				* (c1.getX() - c2.getX()) + (c1.getY() - c2.getY()) * (c1.getY() - c2.getY()));;
		return dist;
	}
	
	/**
	 * Return the cos of te angle between te vector of slope of bati1 nd the vector between the centroids of bati1 and bati2
	 */
	public static float BatiBatiCosAngle(Polygon bati1,
			Polygon bati2) {
		Point s = getVectorSlope(bati1);
		Point c1 = bati1.getCentroid();
		Point c2 = bati2.getCentroid();
		Point cvector = gf.createPoint(new Coordinate(c2.getX()-c1.getX(),c2.getY()-c1.getY()));
		float snorm= (float) Math.sqrt(s.getX()*s.getX()+s.getY()*s.getY());
		float cvectornorm= (float) Math.sqrt(cvector.getX()*cvector.getX()+cvector.getY()*cvector.getY());
		float cos = (float) (cvector.getX()*s.getX()+cvector.getY()*s.getY())/(snorm*cvectornorm);
		return cos;
	}

	/**
	 * Return the nearest Projected point of the habitation on the sewers
	 */
	public static ProjectedPoint sewerIndex(Polygon bati,
			SpatialDataSourceDecorator sds2) throws DriverException {
		int indexmin = 0;
		ProjectedPoint pointdist = SewerBatiDistance((LineString) sds2
				.getGeometry(0).getGeometryN(0), bati);
		float distmin = pointdist.getDist();
		Point projectionmin = pointdist.getPoint();
		for (int i = 1; i < sds2.getRowCount(); i++) {
			LineString sewer = (LineString) sds2.getGeometry(i).getGeometryN(0);
			pointdist = SewerBatiDistance(sewer, bati);
			float dist = pointdist.getDist();
			if (dist < distmin) {
				distmin = dist;
				indexmin = i;
				projectionmin = pointdist.getPoint();
			}
		}
		ProjectedPoint result = new ProjectedPoint(projectionmin, distmin,
				indexmin);
		return result;
	}

	/**
	 * Return the nearest centroid point an habitation of an habitation
	 */
	public static ProjectedPoint batiIndex(Polygon bati,
			SpatialDataSourceDecorator sds1) throws DriverException {
		int indexmin = 0;
		float valuemax = 0;
		Point c=null;
		for (int i = 0; i < sds1.getRowCount(); i++) {
			Polygon poly = (Polygon) sds1.getGeometry(i).getGeometryN(0);
			float value = BatiBatiCosAngle(bati,poly)/BatiBatiDistance(bati,poly);
			if ((value > valuemax)&&(bati.getCentroid().getCoordinate().z>poly.getCentroid().getCoordinate().z)) {
				valuemax = value;
				indexmin = i;
				c = sds1.getGeometry(i).getGeometryN(0).getCentroid();
			}
		}
		ProjectedPoint result = new ProjectedPoint(c, valuemax,	indexmin);
		return result;
	}
	
}