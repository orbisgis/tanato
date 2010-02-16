package org.tanato.processing.preprocessing.sewer;

import java.io.File;
import java.util.Calendar;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;

import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;

import com.vividsolutions.jts.geom.Geometry;


public class Sewer {

	public static DataSourceFactory dsf = new DataSourceFactory();
	public static String path = "data/small_courbes.shp";

	public static void main(String[] args) throws DriverLoadException, DataSourceCreationException, DriverException {

		DataSource mydata = dsf.getDataSource(new File(path));
		SpatialDataSourceDecorator sds = new SpatialDataSourceDecorator(mydata);
		sds.open();
		for (int i = 0; i < sds.getRowCount(); i++) {
			Geometry geom = sds.getGeometry(i);
			}
		sds.close();
	}

}
