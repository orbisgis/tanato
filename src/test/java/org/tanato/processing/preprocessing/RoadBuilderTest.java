package org.tanato.processing.preprocessing;

import java.io.File;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.generic.GenericObjectDriver;
import org.tanato.processing.preprocessing.road.RoadBuilder;
import org.tanato.processing.preprocessing.sewer.NewSewer;
import org.tanato.processing.preprocessing.sewer.Sewer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

public class RoadBuilderTest extends TestCase {

	public static String sewer = "data/modelisation/chezine/sewer.shp";
	private String ResultdataPath = "/temp/dataroad2.gdms";

	static DataSourceFactory dsf = new DataSourceFactory();

	public void testsewer() throws Exception {
		RoadBuilder s = new RoadBuilder();
		BufferParameters b =new BufferParameters();
		ArrayList<Geometry> geoms = s.getRoad(sewer,5.0,b);
		System.out.println(geoms.size());

		DefaultMetadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.GEOMETRY) }, new String[] { "gid",
		"the_geom" });

		GenericObjectDriver data = new GenericObjectDriver(metadata);

		int i = 0;
		for (Geometry geometry : geoms) {
			i++;
			data.addValues(ValueFactory.createValue(i), ValueFactory
					.createValue(geometry));

		}

		File gdmsFile = new File(ResultdataPath);
		gdmsFile.delete();
		dsf.getSourceManager().register("result", gdmsFile);

		DataSource ds = dsf.getDataSource(data);
		ds.open();
		dsf.saveContents("result", ds);
		ds.close();

	}
}
