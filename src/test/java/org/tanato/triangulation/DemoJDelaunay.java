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
import org.jdelaunay.delaunay.MyBox;
import org.jdelaunay.delaunay.MyDrawing;
import org.jdelaunay.delaunay.MyEdge;
import org.jdelaunay.delaunay.MyMesh;
import org.jdelaunay.delaunay.MyPoint;
import org.jdelaunay.delaunay.MyPolygon;
import org.tanato.processing.postprocessing.JdelaunayExport;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

/**
 * Demo of JDelaunay library.
 * @author Adelin PIAU
 * @date 2010-07-27
 * @version 1.3
 */
public class DemoJDelaunay {
	public static DataSourceFactory dsf = new DataSourceFactory();



	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException, DelaunayError,
			ParseException, IOException {

		
	String help="Demo 1.3\n" +
			"Parameter : [-v] [-c <path level edges>] [-p <path buildings>]\n" +
			"-v : verbose\n" +
			"-e : level edges\n" +
			"-p : polygons\n" +
			"example :\n" +
			"-v -e courbes_niveaux.shp -p bati_extrait.shp";
		
		
		if(args.length==0)
		{
			System.out.println(help);
		}
		else
		{
	
			String path="", pathBuilding="";
			MyMesh aMesh = new MyMesh();
			boolean callhelp =false;
			
			if(args.length==2)
			{
				if(args[0].equals("-e"))
				{
					path = args[1];
				}
				else
				if(args[0].equals("-p"))
				{
					pathBuilding=args[1];
				}
				else
					callhelp=true;
			}
			else
			if(args.length>=3)
			{
				
				if(args[0].equals("-v"))
				{
					aMesh.setVerbose(true);
				}
				else
					callhelp=true;
				
				if(args[1].equals("-e"))
				{
					path = args[2];
					
					if(args.length==5)
					{
						if(args[3].equals("-p"))
						{
							pathBuilding=args[4];
						}
						else
							callhelp=true;
					}
				}
				else
				if(args[1].equals("-p"))
				{
					pathBuilding=args[2];
				}
				else
					callhelp=true;
			}
			
			
			
			
			if(callhelp==true)
			{
				System.out.println(help);
			}
			else
			{
				
				long start = System.currentTimeMillis();
				
	
				DataSource mydata;
				SpatialDataSourceDecorator sds;
				
				if(!path.equals(""))// level edges
				{
					
					MyBox abox= new MyBox();
					if(!pathBuilding.equals(""))
					{
						// adding polygons
						mydata = dsf.getDataSource(new File(pathBuilding));
						sds = new SpatialDataSourceDecorator(mydata);
						sds.open();
						Envelope env = sds.getFullExtent();
						Geometry geomEnv = EnvelopeUtil.toGeometry(env);
						Coordinate[] coords = geomEnv.getCoordinates();
						for (int i = 0; i < coords.length - 1; i++) {
							abox.alterBox(coords[i].x, coords[i].y, 0);
						}
						sds.close();
					}
					
					
					
					mydata = dsf.getDataSource(new File(path));	
					sds = new SpatialDataSourceDecorator(mydata);
					sds.open();
			
					
					Envelope env = sds.getFullExtent();
					Geometry geomEnv = EnvelopeUtil.toGeometry(env);
					Coordinate[] coords = geomEnv.getCoordinates();
					for (int i = 0; i < coords.length - 1; i++) {
						abox.alterBox(coords[i].x, coords[i].y, 0);
					}
					aMesh.init(abox);
					
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
					
					// Uncomment it for adding polygons after triangularization.
					// (Don't forget to comment the same function after adding polygons!)
					aMesh.processDelaunay();
				}
				
				
				if(!pathBuilding.equals(""))// polygones
				{
					// adding polygons
					mydata = dsf.getDataSource(new File(pathBuilding));
					sds = new SpatialDataSourceDecorator(mydata);
					sds.open();
					
					
					if(path.equals(""))
					{
						MyBox abox = new MyBox();
						Envelope env = sds.getFullExtent();
						Geometry geomEnv = EnvelopeUtil.toGeometry(env);
						Coordinate[] coords = geomEnv.getCoordinates();
						ArrayList<MyPoint> points = new ArrayList<MyPoint>();
						for (int i = 0; i < coords.length - 1; i++) {
							abox.alterBox(coords[i].x, coords[i].y, 0);
						}
						aMesh.init(abox);

					}
					System.out.println("\nadding polygon :\n");
					MyPolygon aPolygon;
					long max;
					max=sds.getRowCount();// can take more (or lot of more) than 2 minutes
					for (long i = 0; i < max; i++) {// can take more (or lot of more) than 2 minutes
						Geometry geom = sds.getGeometry(i);
						for (int j = 0; j < geom.getNumGeometries(); j++) {
							Geometry subGeom = geom.getGeometryN(j);
							if (subGeom instanceof Polygon) {
								aPolygon=new MyPolygon((Polygon) subGeom);
								aPolygon.setEmpty(true);
								aPolygon.setUsePolygonZ(false);
			//					aPolygon.setMustBeTriangulated(true);
								System.out.print("\r"+i+" / "+(max-1));
								aMesh.addPolygon(aPolygon);
							}
						}
					}
					sds.close();
					
					if(path.equals(""))
					{
						aMesh.processDelaunay();
					}
				}
				
				
				// Uncomment it for triangulate polygons and level edge in the same time.
				// (Don't forget to comment the same function befor adding polygons!)
		//		aMesh.processDelaunay();
		
				System.out.println("\npoint : "+aMesh.getNbPoints()+"\nedges : "+aMesh.getNbEdges()+"\ntriangles : "+aMesh.getNbTriangles());
				
				long end = System.currentTimeMillis();
				System.out.println("Duration " + (end-start)+"ms ==> ~ "+((end-start)/60000)+"min");
				
				
				MyDrawing aff2 = new MyDrawing();
				aff2.add(aMesh);
				aMesh.setAffiche(aff2);
				
				System.out.println("Save in Mesh.wrl ...");
				aMesh.VRMLexport();// save mesh in Mesh.wrl
				
				System.out.println("Save in mesh*.gdms");
				JdelaunayExport.exportGDMS(aMesh, "mesh");
				
				
				System.out.println("Check triangularization...");
				aMesh.checkTriangularization();
		
				
				end = System.currentTimeMillis();
				System.out.println("Duration " + (end-start)+"ms ==> ~ "+((end-start)/60000)+"min");
				System.out.println("Finish");
				
				
				
				
			}
		}
	}

}
