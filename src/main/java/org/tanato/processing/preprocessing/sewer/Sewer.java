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
import com.vividsolutions.jts.geom.Polygon;

public class Sewer {

	public static DataSourceFactory dsf = new DataSourceFactory();
	public static GeometryFactory gf = new GeometryFactory();
	public static int MAXVALUE = 10000000;
	public static float Zepsilon =0.1f;
	public static float MAXlength =150.0f;

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
			System.out.println("boucle "+i+" sur "+sds1.getRowCount());
			BatiConnection bati = new BatiConnection((Polygon) sds1.getGeometry(i).getGeometryN(0),gf);
			bati.setDirectionSlope();
			// calculation of the nearest sewer
			bati.SewerConnect(sds2);
			bati.SewerIntersection(sds1);
			//calculate the possible habitation
			bati.BatiConnect(sds1);
			bati.BatiIntersection(sds1, sds2);
			sds2.close();
			//choose between ewer and habitation
			result.add(gf.createLineString(bati.CompareDistances()));
		}
		sds1.close();		
		System.out.println("delete crossed");
		deleteCrossedLink(result);
		return result;
	}
	/**
	 * Deal the problem of the crossed links
	 */
	public static void deleteCrossedLink(ArrayList<Geometry> arcs)
	{	
		int i=0;
		while ((i<arcs.size()))
		{
			LineString s1=(LineString)arcs.get(i);
			int j=0;
			while (j<i)
			{
				LineString s2=(LineString)arcs.get(j);
				if (Intersect(s1,s2))
				{	System.out.println("croisement en i="+i+" et j="+j);
				Coordinate[] coord = new Coordinate[2]; 
				//coord[0]=(s1.getCoordinates())[0];
				//coord[1]=milieu((s1.getCoordinates())[0],(s1.getCoordinates())[1],(s2.getCoordinates())[0],(s2.getCoordinates())[1]);
				//arcs.set(i, gf.createLineString(coord));
				coord[0]=(s2.getCoordinates())[0];
				coord[1]=middle((s1.getCoordinates())[0],(s1.getCoordinates())[1],(s2.getCoordinates())[0],(s2.getCoordinates())[1]);
				arcs.set(j, gf.createLineString(coord));
				}
				j++;
			}
			i++;
		}

	}
	public static Coordinate middle(Coordinate A,Coordinate B,Coordinate C,Coordinate D)
	{
		Coordinate coord= new Coordinate();

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
		coord.x=Sx;
		coord.y=Sy;		
		return coord;
	}
	public static boolean Intersect(Geometry g1,Geometry g2)
	{
		boolean result=false;
		int i=1;
		while((!result)&(i<g1.getGeometryN(0).getCoordinates().length))
		{
			int j=1;
			while((!result)&(j<g2.getGeometryN(0).getCoordinates().length))
			{
				result=Intersect(g1.getGeometryN(0).getCoordinates()[0],
						g1.getGeometryN(0).getCoordinates()[1],
						g2.getGeometryN(0).getCoordinates()[0],
						g2.getGeometryN(0).getCoordinates()[1]);
				
				j++;
			}
			i++;
		}
		return result;
	}
	public static boolean Intersect(Coordinate A,Coordinate B,Coordinate C,Coordinate D)
	{
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
			if(Cx==Dx) return false;
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
		if((Sx<Ax && Sx<Bx)|(Sx>Ax && Sx>Bx) | (Sx<Cx && Sx<Dx)|(Sx>Cx && Sx>Dx)
				| (Sy<Ay && Sy<By)|(Sy>Ay && Sy>By) | (Sy<Cy && Sy<Dy)|(Sy>Cy && Sy>Dy))
		{return false;}
				else {return true;}
	}
}