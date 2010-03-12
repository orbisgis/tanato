package org.tanato.processing.preprocessing.sewer;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class BatiConnection {
	private Polygon bati;
	private Coordinate[] coordsewer;
	private Coordinate[] coordbati;
	private float sewervalue;
	private float bativalue;
	private boolean sewerConnected;
	private boolean batiConnected;
	private LineString toSewer;
	private LineString toBati;
	private GeometryFactory gf;
	private Point direction;
	private static float MAXVALUE=9999999.9f;
	private static float MAXlength=100f;
	private static float Zepsilon=0.5f;

	public BatiConnection(Polygon bati,GeometryFactory gf)
	{
		this.bati=bati;
		this.gf=gf;
		this.coordsewer=new Coordinate[2];
		this.coordbati=new Coordinate[2];
		coordsewer[0]=bati.getInteriorPoint().getCoordinate();
		coordbati[0]=bati.getInteriorPoint().getCoordinate();
		this.sewervalue=0.0f;
		this.bativalue=0.0f;
		this.sewerConnected=false;
		this.batiConnected=false;
		setDirectionSlope();

	}
	/**
	 * Return the vector which represent the direction of the slope
	 */
	public void setDirectionSlope()

	{
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
		this.direction= P;
	}
	/**
	 * Calculate the best choice to do between a sewer and an habitation
	 */
	public Coordinate[] CompareDistances()
	{
		if (sewerConnected&batiConnected)
		{if (sewervalue>bativalue)
		{return coordsewer;}
		else
		{return coordbati;}
		}
		else
		{
			if (sewerConnected)	
			{return coordsewer;}
			else
			{
				if (batiConnected) 
				{return coordbati;}
				else {return null;}
			}
		}
	}
	/**
	 * Return the cos of the angle between the vector of slope of bati1 and the vector between the centroids of bati1 and bati2
	 */
	public float BatiCosAngle(Polygon bati) {
		Point c1 = this.getBati().getCentroid();
		Point c2 = bati.getCentroid();
		Point cvector = gf.createPoint(new Coordinate(c2.getX()-c1.getX(),c2.getY()-c1.getY()));
		float snorm= (float) Math.sqrt(direction.getX()*direction.getX()+direction.getY()*direction.getY());
		float cvectornorm= (float) Math.sqrt(cvector.getX()*cvector.getX()+cvector.getY()*cvector.getY());
		float cos = (float) (cvector.getX()*direction.getX()+cvector.getY()*direction.getY())/(snorm*cvectornorm);
		return cos;
	}
	/**
	 * Return the distance between the centroids of 2 habitations
	 */
	public ProjectedPoint BatiDistance(Polygon bati2)
	{
		Point c1 = this.getBati().getCentroid();
		Point c2 = bati2.getCentroid();
		float dist = (float) Math.sqrt((c1.getX() - c2.getX())* (c1.getX() - c2.getX())
										+ (c1.getY() - c2.getY()) * (c1.getY() - c2.getY())
										);
		return new ProjectedPoint(c2,dist);
	}
	/**
	 * Return the nearest projected point of the habitation on the sewer
	 */
	public ProjectedPoint SewerDistance(LineString sewer)
	{

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
				if (direction.getX() == 0) {
					intersection = false;
				} else {
					double pCD = (direction.getY()) / (direction.getX());
					Sx = p1.getX();
					Sy = pCD * (p1.getX() - c.getX()) + c.getY();
				}
			} else {
				if (direction.getX() == 0) {
					double pAB = (p1.getY() - p2.getY())
					/ (p1.getX() - p2.getX());
					Sx = c.getX();
					Sy = pAB * (c.getX() - p1.getX()) + p1.getY();
				} else {
					double pCD = (direction.getY()) / (direction.getX());
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
							| (Sx < c.getX() && direction.getX() > 0)
							| (Sx > c.getX() && direction.getX() < 0)
							| (Sy < p1.getY() && Sy < p2.getY())
							| (Sy > p1.getY() && Sy > p2.getY())
							| (Sy < c.getY() && direction.getY() > 0) | (Sy > c.getY() && direction
							.getY() < 0))) {
				intersection = false;
			}

			double coeff = (Sx - p1.getX()) / (p2.getX() - p1.getX());
			double Sz = p1.getCoordinate().z + coeff
			* (p2.getCoordinate().z - p1.getCoordinate().z);

			if (intersection && ((c.getCoordinate().z) +Zepsilon < Sz)) {
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
		if (minDist>MAXlength) {minDist=MAXVALUE;}
		ProjectedPoint p = new ProjectedPoint(pfinal,minDist);
		return p;
	}
	/**
	 * Return the nearest Projected point of the habitation on the sewers
	 */
	public void SewerConnect(SpatialDataSourceDecorator sds2) throws DriverException
	{
		float distmin=MAXVALUE;
		Point pmin = null;
		for (int i = 0; i < sds2.getRowCount(); i++) {
			LineString sewer = (LineString) sds2.getGeometry(i).getGeometryN(0);
			ProjectedPoint p =SewerDistance(sewer);
			float dist = p.getDist();
			if (dist < distmin) {
				distmin = dist;
				pmin = p.getPoint();
			}
		}
		if (distmin < MAXlength)
		{
			coordsewer[1]=pmin.getCoordinate();
			sewervalue=1/distmin;
			sewerConnected=true;
			toSewer=gf.createLineString(coordsewer);
		}

	}
	/** 
	 * deal the case of an habitation on the way between the bati and the sewer
	 */
	public void SewerIntersection(SpatialDataSourceDecorator sds1) throws DriverException
	{
		for (int i = 0; ((i < sds1.getRowCount())&(sewerConnected)); i++) {
			Polygon bati2 = (Polygon) sds1.getGeometry(i).getGeometryN(0);
			if (toSewer.intersects(bati2)&&(!bati.equals(bati2)))
			{	
				sewerConnected=false;
				if (batiConnected)
				{
					if (bativalue<1/this.BatiDistance(bati2).getDist())
					{
						coordbati[1]=bati2.getCentroid().getCoordinate();
						bativalue=1/this.BatiDistance(bati2).getDist();
						toBati=gf.createLineString(coordbati);
					}
				}
				else
				{
					batiConnected=true;
					coordbati[1]=bati2.getCentroid().getCoordinate();
					bativalue=1/this.BatiDistance(bati2).getDist();
					toBati=gf.createLineString(coordbati);
				}
			}	
		}	
	}
	/**
	 * Return the nearest centroid point an habitation of an habitation
	 */
	public void BatiConnect(SpatialDataSourceDecorator sds1) throws DriverException
	{
		float valuemax=0f;
		float dist=MAXVALUE;
		Point pmax = null;
		for (int i = 0; i < sds1.getRowCount(); i++) {
			Polygon bati2 = (Polygon) sds1.getGeometry(i).getGeometryN(0);
			ProjectedPoint p =BatiDistance(bati2);
			float value = this.BatiCosAngle(bati2)/p.getDist();
			if ((value > valuemax)&&(BatiHigher(bati2)))
					{
				valuemax = value;
				dist=p.getDist();
				pmax = p.getPoint();
			}
		}
		if (dist < MAXlength)
		{
			coordbati[1]=pmax.getCoordinate();
			bativalue=valuemax;
			batiConnected=true;
			toBati=gf.createLineString(coordbati);
		}
	}
	/** 
	* deal the case of an habitation on the way between the object and an other habitation
	**/
	public void BatiIntersection(SpatialDataSourceDecorator sds1,SpatialDataSourceDecorator sds2) throws DriverException
	{
		for (int i = 0; ((i < sds2.getRowCount())&(batiConnected)); i++) {
			LineString sewer = (LineString) sds2.getGeometry(i).getGeometryN(0);
			for (int j=1;((j<sewer.getNumPoints())&(batiConnected));j++)
			{	Coordinate[] coords= new Coordinate[2];
			coords[0]=sewer.getCoordinateN(j-1);
			coords[1]=sewer.getCoordinateN(j);
			LineString sewerline = gf.createLineString(coords);
			if (Sewer.Intersect(toBati, sewerline))
			{
				batiConnected=false;
				Coordinate intersec =milieu(coords[0],coords[1]);
				float dist = (float) Math.sqrt((bati.getCentroid().getX() - intersec.x)
						* (bati.getCentroid().getX() - intersec.x) 
						+ (bati.getCentroid().getY() - intersec.x)*(bati.getCentroid().getY() - intersec.x));
				if (sewerConnected)
				{	
					if (sewervalue<1/dist)
					{
						coordsewer[1]=intersec;
						sewervalue=1/dist;
						toSewer=gf.createLineString(coordbati);
						SewerIntersection(sds1);
					}
				}
				else
				{
					sewerConnected=true;
					coordsewer[1]=intersec;
					bativalue=1/dist;
					toSewer=gf.createLineString(coordbati);
					SewerIntersection(sds1);
				}
			}
			}
		}
		
		boolean batiIsIntercept=false;
		
		for (int i = 0; ((i < sds1.getRowCount())&(batiConnected)); i++) {
			Polygon bati2 = (Polygon) sds1.getGeometry(i).getGeometryN(0);
			if ((Sewer.Intersect(bati2,toBati))&
					((coordbati[1].x!=bati2.getCentroid().getCoordinate().x)
							||(coordbati[1].y!=bati2.getCentroid().getCoordinate().y)))
			{
				batiIsIntercept=true;
				coordbati[1]=bati2.getCentroid().getCoordinate();
				bativalue=1/this.BatiDistance(bati2).getDist();
				toBati=gf.createLineString(coordbati);
			}
		}
		if (batiIsIntercept) {BatiIntersection(sds1,sds2);
		}

	}
	/**
	 * Calculate if the centroid of the bati1 is higher than the bati2 one
	 */
	public boolean BatiHigher(Polygon bati) {
		Coordinate[] c1 = this.getBati().getCoordinates();
		Coordinate[] c2 = bati.getCoordinates();
		float z1=0;
		float z2=0;
		for (int i=0;i<c1.length;i++)
		{ z1=(float) (z1+c1[i].z);}
		z1=z1/c1.length;
		for (int i=0;i<c2.length;i++)
		{ z2=(float) (z2+c2[i].z);}
		z2=z2/c2.length;
		return (z1+Zepsilon>=z2);
	}
	/**
	 * Construct the Sewer  Connection LineString of the object
	 */
	public void ConstrucSewer()
	{
		toSewer=gf.createLineString(coordsewer);
	}
	/**
	 * Construct the habitation Connection LineString of the object
	 */
	public void ConstrucBati()
	{
		toBati=gf.createLineString(coordbati);
	}
	public float getSewervalue() {
		return sewervalue;
	}
	public void setSewervalue(float sewervalue) {
		this.sewervalue = sewervalue;
	}
	public float getBativalue() {
		return bativalue;
	}
	public void setBativalue(float bativalue) {
		this.bativalue = bativalue;
	}
	public Polygon getBati() {
		return bati;
	}
	public void setBati(Polygon bati) {
		this.bati = bati;
	}
	public Coordinate[] getCoordsewer() {
		return coordsewer;
	}
	public void setCoordsewer(Coordinate[] coordsewer) {
		this.coordsewer = coordsewer;
	}
	public Coordinate[] getCoordbati() {
		return coordbati;
	}
	public void setCoordbati(Coordinate[] coordbati) {
		this.coordbati = coordbati;
	}
	public boolean isSewerConnected() {
		return sewerConnected;
	}
	public void setSewerConnected(boolean sewerConnected) {
		this.sewerConnected = sewerConnected;
	}
	public boolean isBatiConnected() {
		return batiConnected;
	}
	public void setBatiConnected(boolean batiConnected) {
		this.batiConnected = batiConnected;
	}
	public Coordinate milieu(Coordinate C, Coordinate D)
	{
		Coordinate A = this.getCoordbati()[0];
		Coordinate B = this.getCoordbati()[1];
		Coordinate result= new Coordinate();

		double Ax = A.x;
		double Ay = A.y;
		double Bx = B.x;
		double By = B.y;
		double Cx = C.x;
		double Cy = C.y;
		double Dx = D.x;
		double Dy = D.y;
		double Sx;
		double Sy;

		if(Ax==Bx)
		{
			if(Cx==Dx) 
			{
				Sx=Ax;Sy=Ay;
			}
			else
			{
				double pCD = (Cy-Dy)/(Cx-Dx);
				Sx = Ax;
				Sy = pCD*(Ax-Cx)+Cy;
			}
		}
		else
		{
			if(Cx==Dx)
			{
				double pAB = (Ay-By)/(Ax-Bx);
				Sx = Cx;
				Sy = pAB*(Cx-Ax)+Ay;
			}
			else
			{
				double pCD = (Cy-Dy)/(Cx-Dx);
				double pAB = (Ay-By)/(Ax-Bx);
				double oCD = Cy-pCD*Cx;
				double oAB = Ay-pAB*Ax;
				Sx = (oAB-oCD)/(pCD-pAB);
				Sy = pCD*Sx+oCD;
			}
		}		
		result.x=Sx;
		result.y=Sy;		
		return result;
	}
}
