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
import org.tanato.processing.preprocessing.sewer.NewSewer;
import org.tanato.processing.preprocessing.sewer.Sewer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class SewerTest extends TestCase {

	public static String building = "data/modelisation/chezine/bati_extrait.shp";
	public static String sewer = "data/modelisation/chezine/sewer.shp";
	private String ResultdataPath = "/temp/dataconnected_new5.gdms";

	static DataSourceFactory dsf = new DataSourceFactory();

	public void testsewer() throws Exception {
		Sewer s = new Sewer();
		ArrayList<Geometry> geoms = s.getSewer(building, sewer);
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
		//for (int j=0;j<geoms.size();j++)
		//{
		//System.out.println(geoms.get(j).toString())	;
		//}

		File gdmsFile = new File(ResultdataPath);
		gdmsFile.delete();
		dsf.getSourceManager().register("result", gdmsFile);

		DataSource ds = dsf.getDataSource(data);
		ds.open();
		dsf.saveContents("result", ds);
		ds.close();
	}
}
