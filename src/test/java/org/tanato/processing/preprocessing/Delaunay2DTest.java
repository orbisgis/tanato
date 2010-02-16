package org.tanato.processing.preprocessing;

import java.io.File;

import junit.framework.TestCase;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.tanato.processing.preprocessing.triangulation.Delaunay2D;

public class Delaunay2DTest extends TestCase {

//	private static final String FILE_PATH = "/home/ebocher/Documents/data/BD_Topo_Nantes_shp/BATI_surface.shp";
	
//private static final String FILE_PATH ="data/modelisation/chezine/chezinecourbe.shp";
	
	private static final String FILE_PATH ="data/modelisation/chezine/courbesZ.shp";
	
	DataSourceFactory dsf = new DataSourceFactory();

	public void testLargeDataDelaunay2D() throws Exception {

		DataSource ds = dsf.getDataSource(new File(FILE_PATH));

		Delaunay2D delaunay2d = new Delaunay2D(new SpatialDataSourceDecorator(
				ds));
		
		delaunay2d.compute();

		delaunay2d.show();

		File gdmsFile = new File("/tmp/tinfaces.gdms");
		gdmsFile.delete();
		dsf.getSourceManager().register("faces", gdmsFile);

		ds = dsf.getDataSource(delaunay2d.getEdges());
		ds.open();
		dsf.saveContents("faces", ds);
		ds.close();
	}

}
