package org.tanato.processing.postprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.DiskBufferDriver;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import org.jdelaunay.delaunay.DelaunayError;

/**
 * postprocessing Package.
 * 
 * @author Adelin PIAU
 * @date 2010-07-27
 * @revision 2010-10-04
 * @version 1.0
 */
public class JdelaunayExport {

	static DataSourceFactory dsf = new DataSourceFactory();
	
	public static void exportGDMS(ConstrainedMesh aMesh, String path) throws DriverException, DelaunayError
	{
		// save points
		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT) }, new String[] {
				"the_geom",  "gid", "type" });

		DiskBufferDriver driver = new DiskBufferDriver(dsf, metadata);
		
		for(DPoint aPoint:aMesh.getPoints())
		{
			driver.addValues(new Value[] { ValueFactory.createValue(new GeometryFactory().createPoint(aPoint.getCoordinate())),
			ValueFactory.createValue(aPoint.getGID()),
			ValueFactory.createValue(aPoint.getProperty()) });
		}
		
		saveDriver(path+"Points.gdms", driver);
		
		
		// save edges
		metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.BOOLEAN),
				TypeFactory.createType(Type.BOOLEAN)}, new String[] {
				"the_geom",  "gid", "type", "N_start", "N_end", "Triangle_L", "Triangle_R", "topo_L", "topo_R" });//TODO check name

		driver = new DiskBufferDriver(dsf, metadata);
		
		for(DEdge anEdge:aMesh.getEdges())
		{
			Collection<LineString> lineStrings = new ArrayList<LineString>();
			lineStrings.add(	new GeometryFactory().createLineString(
				new Coordinate[]{anEdge.getStartPoint().getCoordinate(), anEdge.getEndPoint().getCoordinate()}
														)
			);
			
			new GeometryFactory();
			driver.addValues(new Value[] { ValueFactory.createValue(
					new GeometryFactory().createMultiLineString(
					GeometryFactory.toLineStringArray(lineStrings))
				),
			ValueFactory.createValue(anEdge.getGID()),
			ValueFactory.createValue(anEdge.getProperty()),
			ValueFactory.createValue(anEdge.getStartPoint().getGID()),
			ValueFactory.createValue(anEdge.getEndPoint().getGID()),
			ValueFactory.createValue( (anEdge.getLeft()==null?-1:anEdge.getLeft().getGID())),
			ValueFactory.createValue( (anEdge.getRight()==null?-1:anEdge.getRight().getGID())),
			ValueFactory.createValue( anEdge.getLeft()==null?false:anEdge.getLeft().isTopoOrientedToEdge(anEdge)),
			ValueFactory.createValue((anEdge.getRight()==null?false:anEdge.getRight().isTopoOrientedToEdge(anEdge)))
			});
		}
		

		saveDriver(path+"Edges.gdms", driver);
		
		
		// save triangles
		metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.DOUBLE),
				TypeFactory.createType(Type.DOUBLE)}, new String[] {
				"the_geom",  "gid", "type", "slope", "direction" });

		driver = new DiskBufferDriver(dsf, metadata);
		
		for(DTriangle aTriangle:aMesh.getTriangleList())
		{
			Collection<Polygon> polygons = new ArrayList<Polygon>();
			polygons.add(new GeometryFactory().createPolygon(
					new GeometryFactory().createLinearRing(new Coordinate[] {
							aTriangle.getPoint(0).getCoordinate(),
							aTriangle.getPoint(1).getCoordinate(),
							aTriangle.getPoint(2).getCoordinate(),
							aTriangle.getPoint(0).getCoordinate() }), null));

			new GeometryFactory();
			driver.addValues(new Value[] {
					ValueFactory.createValue(new GeometryFactory()
							.createMultiPolygon(GeometryFactory
									.toPolygonArray(polygons))),
					ValueFactory.createValue(aTriangle.getGID()),
					ValueFactory.createValue(aTriangle.getProperty()),
					ValueFactory.createValue(aTriangle.getSlopeInPercent()),
					ValueFactory.createValue(aTriangle.getSlopeAspect()) });
		}
		
		saveDriver(path+"Triangles.gdms", driver);
		
	}
	
	
	private static void saveDriver(String name, ObjectDriver driver) throws DriverException
	{
			DataSourceFactory dsf = new DataSourceFactory();
			File gdmsFile = new File(name);
			gdmsFile.delete();
			dsf.getSourceManager().register(name, gdmsFile);
			DataSource ds = dsf.getDataSource(driver);
			ds.open();
				dsf.saveContents(name, ds);
			ds.close();
	}
	
	
	
//	public static void recomputeEdgeProperties(String pathEdge, String pathTriangle)
//	{
//		 DataSourceFactory dsf = new DataSourceFactory();
//		DataSource mydata;
//		SpatialDataSourceDecorator sds;
//		
//		mydata = dsf.getDataSource(new File(pathBuilding));
//		sds = new SpatialDataSourceDecorator(mydata);
//		sds.open();
//		Envelope env = sds.getFullExtent();
//		Geometry geomEnv = EnvelopeUtil.toGeometry(env);
//		Coordinate[] coords = geomEnv.getCoordinates();
//	
//		
//		
//		
//	}

}
