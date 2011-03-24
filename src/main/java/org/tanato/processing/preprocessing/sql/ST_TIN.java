/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tanato.processing.preprocessing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.AlreadyClosedException;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.gdms.GdmsWriter;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.orbisgis.progress.IProgressMonitor;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a constrained delaunay triangulation from the geometry given in input.
 *
 * 
 * @author alexis
 */
public class ST_TIN implements CustomQuery {
	private static final Logger logger = Logger.getLogger(ST_TIN.class.getName());

	@Override
	public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables,
			Value[] values, IProgressMonitor pm) throws ExecutionException {
		DataSource ds = tables[0];
		//We need to read our source.
		SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(ds);
		long count = 0;
		//We retrieve the values to know how we are supposed to proceed.
		boolean inter = values[0].getAsBoolean();
		boolean flat = values[1].getAsBoolean();
		String name = values[2].getAsString();
		//We open the source
                try {
                        sds.open();
                        count = sds.getRowCount();
                } catch (DriverException ex) {
                        logger.log(Level.SEVERE, "There has been an error while opening the table, or counting its lines.\n",ex);
                }
		Geometry geom = null;
		//We prepare our input structures.
		List<DPoint> pointsToAdd = new ArrayList<DPoint>();
		ArrayList<DEdge> edges = new ArrayList<DEdge>();
		//We fill the input structures with our table.
		for(long i=0; i<count;i++){
			try {
				geom = sds.getGeometry(i);
			} catch (DriverException ex) {
				logger.log(Level.SEVERE, "Can't retrieve the  geometry.\n",ex);
				break;
			}
			if(geom instanceof Point){
				addPoint(pointsToAdd, (Point) geom);
			} else if(geom instanceof MultiPoint){
				addMultiPoint(pointsToAdd, (MultiPoint) geom);
			} else if (geom instanceof GeometryCollection) {
				addGeometryCollection(edges, (GeometryCollection) geom);
			} else if(geom instanceof Geometry){
				addGeometry(edges, (Geometry) geom);
			}
		}
		//We have filled the input of our mesh. We can close our source.
		try {
			sds.close();
		} catch (DriverException ex) {
				logger.log(Level.SEVERE, "The driver failed during the closure.\n",ex);
		} catch (AlreadyClosedException ex) {
				logger.log(Level.SEVERE, "The source seems to have been closed externally\n",ex);
		}
		Collections.sort(edges);

		ConstrainedMesh mesh = new ConstrainedMesh();
		try {
			//We actually fill the mesh
			mesh.setPoints(pointsToAdd);
			mesh.setConstraintEdges(edges);
                        System.out.println(mesh.getConstraintEdges().size());
			if(inter){
				//If needed, we use the intersection algorithm
				mesh.forceConstraintIntegrity();
			}
			//we process delaunay
			mesh.processDelaunay();
			if(flat){
				//If needed, we remove flat triangles.
				mesh.removeFlatTriangles();
			}
		} catch (DelaunayError ex) {
				logger.log(Level.SEVERE, "Generation of the mesh failed.\n",ex);
		}
		//And we write and register our results.
		String edgesOut = name+"_edges";
		String pointsOut = name+"_points";
		String trianglesOut = name+"_triangles";
		try {
			registerEdges(edgesOut, dsf, mesh);
		} catch (IOException ex) {
				logger.log(Level.SEVERE, "Failed to write the file containing the edges.\n",ex);
		} catch (DriverException ex) {
				logger.log(Level.SEVERE, "Driver failure while saving the edges.\n",ex);
		}
		try {
			registerPoints(pointsOut, dsf, mesh);
		} catch (IOException ex) {
				logger.log(Level.SEVERE, "Failed to write the file containing the points.\n",ex);
		} catch (DriverException ex) {
				logger.log(Level.SEVERE, "Driver failure while saving the points.\n",ex);
		}
		try {
			registerTriangles(trianglesOut, dsf, mesh);
		} catch (IOException ex) {
				logger.log(Level.SEVERE, "Failed to write the file containing the triangles.\n",ex);
		} catch (DriverException ex) {
				logger.log(Level.SEVERE, "Driver failure while saving the triangles.\n",ex);
		}
                return null;
	}

	@Override
	public String getName() {
		return "ST_TIN";
	}

	@Override
	public String getDescription() {
		return "Compute a TIN from the lines of the geometry given in argument.";
	}

	@Override
	public String getSqlOrder() {
		return "SELECT ST_TIN(false, true, tinName) FROM source_table;";
	}

	@Override
	public Metadata getMetadata(Metadata[] tables) throws DriverException {
		return null;
	}

	/**
	 * The tables we need after the clause FROM in the query.
	 * @return
	 */
	@Override
	public TableDefinition[] getTablesDefinitions() {
		return new TableDefinition[] {TableDefinition.GEOMETRY};
	}

	/**
	 * Retrieve the arguments this function can take. We always need three arguments<br/><br/>
	 *
	 * BOOLEAN : Flat triangles removal or not.<br/>
	 * BOOLEAN : Intersection processing <br/>
	 * STRING : Prefixe name of the TIN table<br/>
	 * @return
	 */
	@Override
	public Arguments[] getFunctionArguments() {
		return new Arguments[] {new Arguments(Argument.BOOLEAN, Argument.BOOLEAN, Argument.STRING)};
	}

	/**
	 * We add a point to the given list
	 * @param points
	 * @param geom
	 */
	private void addPoint(List<DPoint> points, Point geom){
		Coordinate pt = geom.getCoordinate();
		double z = Double.isNaN(pt.z) ? 0 : pt.z;
		try {
			points.add(new DPoint(pt.x, pt.y, z));
		} catch (DelaunayError ex) {
			logger.log(Level.SEVERE, "You're trying to craete a 3D point with a NaN value.\n",ex);
		}

	}

	/**
	 * Add a MultiPoint geometry.
	 * @param points
	 * @param pts
	 */
	private void addMultiPoint(List<DPoint> points, MultiPoint pts ){
		Coordinate[] coords = pts.getCoordinates();
		for(int i=0; i<coords.length; i++){
			try {
				points.add(new DPoint(coords[i].x, coords[i].y, coords[i].z));
			} catch (DelaunayError ex) {
				logger.log(Level.SEVERE, "You're trying to craete a 3D point with a NaN value.\n",ex);
			}
		}
	}

	/**
	 * add a geometry to the input.
	 * @param edges
	 * @param geom
	 */
	private void addGeometry(List<DEdge> edges, Geometry geom){
                if (geom.isValid()){
                        Coordinate c1 = geom.getCoordinates()[0];
                        c1.z = Double.isNaN(c1.z)  ? 0 : c1.z;
                        Coordinate c2;
                        for (int k = 1; k < geom.getCoordinates().length; k++) {
                                c2 = geom.getCoordinates()[k];
                                c2.z = Double.isNaN(c2.z) ? 0 : c2.z;
                                try{
                                        edges.add(new DEdge(new DPoint(c1), new DPoint(c2)));
                                } catch(DelaunayError d){
                                        logger.log(Level.SEVERE, "You're trying to craete a 3D point with a NaN value.\n",d);
                                }
                                c1 = c2;
                        }
                }
	}

	/**
	 * Add a GeometryCollection
	 * @param edges
	 * @param geomcol
	 */
	private void addGeometryCollection(List<DEdge> edges, GeometryCollection geomcol){
		int num = geomcol.getNumGeometries();
		for(int i=0; i<num; i++){
			addGeometry(edges, geomcol.getGeometryN(i));
		}
	}

	/**
	 * Saves the edges in a file, and register them with the dsf.
	 * @param name
	 * @param dsf
	 * @param mesh
	 * @throws IOException
	 * @throws DriverException
	 */
	private void registerEdges(String name, DataSourceFactory dsf, ConstrainedMesh mesh) throws IOException, DriverException{
		File out =new File(name+".gdms");
		GdmsWriter writer = new GdmsWriter(out);
		Metadata md = new DefaultMetadata(
			new Type[] {TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.FLOAT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT)},
			new String[] {"the_geom","GID","StartP_GID","EndP_GID","LeftT_GID","RightT_GID",
					"Height","Property","Source_GID"});
		int triangleCount = mesh.getEdges().size();
		writer.writeMetadata(triangleCount, md);
		GeometryFactory gf = new GeometryFactory();
		for(DEdge dt : mesh.getEdges()){
			Coordinate[] coords = new Coordinate[2];
			coords[0] = dt.getPointLeft().getCoordinate();
			coords[1] = dt.getPointRight().getCoordinate();
			CoordinateSequence cs = new CoordinateArraySequence(coords);

			LineString mp = new LineString(cs, gf);
			writer.addValues(new Value[] {ValueFactory.createValue(mp),
						ValueFactory.createValue(dt.getGID()),
						ValueFactory.createValue(dt.getStartPoint().getGID()),
						ValueFactory.createValue(dt.getEndPoint().getGID()),
						ValueFactory.createValue(dt.getLeft() == null ? -1 :dt.getLeft().getGID()),
						ValueFactory.createValue(dt.getRight() == null ? -1 :dt.getRight().getGID()),
						ValueFactory.createValue(dt.getHeight()),
						ValueFactory.createValue(dt.getProperty()),
						ValueFactory.createValue(dt.getExternalGID()),});
		}

		// write the row indexes
		writer.writeRowIndexes();
		// write envelope
		writer.writeExtent();
		writer.close();
		dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(name), out);
	}

	private void registerPoints(String name, DataSourceFactory dsf, ConstrainedMesh mesh) throws IOException, DriverException{
		File out =new File(name+".gdms");
		GdmsWriter writer = new GdmsWriter(out);
		Metadata md = new DefaultMetadata(
			new Type[] {TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.FLOAT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT)},
			new String[] {"the_geom","GID",
					"Height","Property","Source_GID"});
		int triangleCount = mesh.getPoints().size();
		writer.writeMetadata(triangleCount, md);
		GeometryFactory gf = new GeometryFactory();
		for(DPoint dt : mesh.getPoints()){
			Coordinate[] coords = new Coordinate[1];
			coords[0] = dt.getCoordinate();
			CoordinateSequence cs = new CoordinateArraySequence(coords);

			Point mp = new Point(cs, gf);

			writer.addValues(new Value[] {ValueFactory.createValue(mp),
						ValueFactory.createValue(dt.getGID()),
						ValueFactory.createValue(dt.getHeight()),
						ValueFactory.createValue(dt.getProperty()),
						ValueFactory.createValue(dt.getExternalGID()),});
		}

		// write the row indexes
		writer.writeRowIndexes();
		// write envelope
		writer.writeExtent();
		writer.close();
		dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(name), out);
	}

	private void registerTriangles(String name, DataSourceFactory dsf, ConstrainedMesh mesh) throws IOException, DriverException{
		File out =new File(name+".gdms");
		GdmsWriter writer = new GdmsWriter(out);
		Metadata md = new DefaultMetadata(
			new Type[] {TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),				
				TypeFactory.createType(Type.FLOAT),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT)},
			new String[] {"the_geom","GID",
					"height","property","Source_GID"});
		int triangleCount = mesh.getTriangleList().size();
		writer.writeMetadata(triangleCount, md);
		GeometryFactory gf = new GeometryFactory();
		for(DTriangle dt : mesh.getTriangleList()){
			Coordinate[] coords = new Coordinate[4];
			coords[0] = dt.getPoint(0).getCoordinate();
			coords[1] = dt.getPoint(1).getCoordinate();
			coords[2] = dt.getPoint(2).getCoordinate();
			coords[3] = dt.getPoint(0).getCoordinate();
			CoordinateSequence cs = new CoordinateArraySequence(coords);
			LinearRing lr = new LinearRing(cs, gf);
			Polygon poly = new Polygon(lr, null, gf);
			MultiPolygon mp = new MultiPolygon(new Polygon[] {poly}, gf);

			writer.addValues(new Value[] {ValueFactory.createValue(mp),
						ValueFactory.createValue(dt.getGID()),
						ValueFactory.createValue(dt.getHeight()),
						ValueFactory.createValue(dt.getProperty()),
						ValueFactory.createValue(dt.getExternalGID()),});
		}

		// write the row indexes
		writer.writeRowIndexes();
		// write envelope
		writer.writeExtent();
		writer.close();
		dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(name), out);
	}
}
