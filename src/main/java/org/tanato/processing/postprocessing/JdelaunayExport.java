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
import org.gdms.driver.generic.GenericObjectDriver;
import org.jdelaunay.delaunay.MyEdge;
import org.jdelaunay.delaunay.MyMesh;
import org.jdelaunay.delaunay.MyPoint;
import org.jdelaunay.delaunay.MyTriangle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;


public class JdelaunayExport {

	
	public static void exportGDMS(MyMesh aMesh, String path) throws DriverException
	{
		// save points
		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT) }, new String[] {
				"the_geom",  "gid", "type" });

		GenericObjectDriver driver = new GenericObjectDriver(metadata);
		
		for(MyPoint aPoint:aMesh.getPoints())
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
				"the_geom",  "gid", "type", "N_start", "N_end", "Triangle_L", "Triangle_R", "topo_L", "topo_R" });//FIXME change name

		driver = new GenericObjectDriver(metadata);
		
		for(MyEdge anEdge:aMesh.getEdges())
		{
			Collection<LineString> lineStrings = new ArrayList<LineString>();
			lineStrings.add(	new GeometryFactory().createLineString(
														new Coordinate[]{anEdge.getStartPoint().getCoordinate(), anEdge.getEndPoint().getCoordinate()}
														)
			);
			
			new GeometryFactory();
			driver.addValues(new Value[] { ValueFactory.createValue(
																		new GeometryFactory().createMultiLineString(
																				GeometryFactory.toLineStringArray(lineStrings
																						)
																		)
																	),
			ValueFactory.createValue(anEdge.getGID()),
			ValueFactory.createValue(anEdge.getProperty()),
			ValueFactory.createValue(anEdge.getStartPoint().getGID()),
			ValueFactory.createValue(anEdge.getEndPoint().getGID()),
			ValueFactory.createValue( (anEdge.getLeft()==null?-1:anEdge.getLeft().getGID())),
			ValueFactory.createValue( (anEdge.getRight()==null?-1:anEdge.getRight().getGID())),
			ValueFactory.createValue(false), 
			ValueFactory.createValue(false)
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

		driver = new GenericObjectDriver(metadata);
		
		for(MyTriangle aTriangle:aMesh.getTriangles())
		{
			
			Collection<LineString> lineStrings = new ArrayList<LineString>();
			lineStrings.add(	new GeometryFactory().createLineString(
														new Coordinate[]{aTriangle.getEdge(0).getStartPoint().getCoordinate(), aTriangle.getEdge(0).getEndPoint().getCoordinate()}
														)
			);
			lineStrings.add(	new GeometryFactory().createLineString(
					new Coordinate[]{aTriangle.getEdge(1).getStartPoint().getCoordinate(), aTriangle.getEdge(1).getEndPoint().getCoordinate()}
					)
);
			lineStrings.add(	new GeometryFactory().createLineString(
					new Coordinate[]{aTriangle.getEdge(2).getStartPoint().getCoordinate(), aTriangle.getEdge(2).getEndPoint().getCoordinate()}
					)
);
			
			new GeometryFactory();
			driver.addValues(new Value[] { ValueFactory.createValue(
																		new GeometryFactory().createMultiLineString(
																				GeometryFactory.toLineStringArray(lineStrings
																						)
																		)
																	),
			ValueFactory.createValue(aTriangle.getGID()),
			ValueFactory.createValue(aTriangle.getProperty()),
			ValueFactory.createValue(0),
			ValueFactory.createValue(0)
			});
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
}
