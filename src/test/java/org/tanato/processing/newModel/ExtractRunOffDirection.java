package org.tanato.processing.newModel;

import java.io.File;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.driver.generic.GenericObjectDriver;
import org.tanato.utilities.MathUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class ExtractRunOffDirection {

	static DataSourceFactory dsf = new DataSourceFactory();

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException {

		String nodesPath = "/tmp/tinnodes.gdms";
		String edesPath = "/tmp/tinedges.gdms";
		String facesPath = "/tmp/tinfaces.gdms";

		SpatialDataSourceDecorator sdsNodes = new SpatialDataSourceDecorator(
				dsf.getDataSource(new File(nodesPath)));

		SpatialDataSourceDecorator sdsEdges = new SpatialDataSourceDecorator(
				dsf.getDataSource(new File(edesPath)));

		SpatialDataSourceDecorator sdsFaces = new SpatialDataSourceDecorator(
				dsf.getDataSource(new File(facesPath)));

		sdsFaces.open();

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.DOUBLE) }, new String[] { "gid",
				"the_geom", "slopeindegree" });

		GenericObjectDriver driver = new GenericObjectDriver(metadata);

		GeometryFactory gf = new GeometryFactory();

		int k = 0;
		for (int i = 0; i < sdsFaces.getRowCount(); i++) {

			Coordinate slope = sdsFaces.getFieldValue(i,
					sdsFaces.getFieldIndexByName("slope")).getAsGeometry()
					.getCoordinate();

			if (slope.z != 0) {
				k++;
				Coordinate p1 = sdsFaces.getGeometry(i).getCentroid()
						.getCoordinate();
				Coordinate p2 = MathUtil.SommeVectoriel(p1, MathUtil.Produit(
						slope, 2));

				double slopeDeg = sdsFaces.getFieldValue(i,
						sdsFaces.getFieldIndexByName("slopedeg")).getAsDouble();
				driver
						.addValues(new Value[] {
								ValueFactory.createValue(k),

								ValueFactory.createValue(gf
										.createLineString(new Coordinate[] {
												p1, p2 })),

								ValueFactory.createValue(slopeDeg) });
			}
		}

		sdsFaces.close();
		DataSource ds = dsf.getDataSource(driver);

		File gdmsFile = new File("/tmp/faceDIrection.gdms");
		gdmsFile.delete();
		dsf.getSourceManager().register("result", gdmsFile);

		ds.open();
		dsf.saveContents("result", ds);
		ds.close();
	}
}
