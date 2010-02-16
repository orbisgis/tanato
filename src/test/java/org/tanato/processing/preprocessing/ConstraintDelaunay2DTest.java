package org.tanato.processing.preprocessing;

import java.io.File;

import junit.framework.TestCase;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.tanato.processing.preprocessing.triangulation.ConstraintDelaunay2D;

public class ConstraintDelaunay2DTest extends TestCase {

	 //private static final String FILE_PATH =
	 //"/home/ebocher/Documents/data/BD_Topo_Nantes_shp/BATI_surface.shp";

//	 private static final String FILE_PATH =
	// "/home/ebocher/Documents/data/BD_Topo_Nantes_shp/VOIES_COMM_ROUTE_ligne.shp";

	// private static final String FILE_PATH =
	// "/home/ebocher/Documents/avupur/modelisation3/routesurfacechezine.shp";
	private static final String FILE_PATH = "data/modelisation/chezine/chezinecourbe.shp";

	// private static final String FILE_PATH
	// ="data/modelisation/chezine/courbesZ.shp";

	DataSourceFactory dsf = new DataSourceFactory();

	public void testLargeDataDelaunay2D() throws Exception {

		long start = System.currentTimeMillis();
		DataSource ds = dsf.getDataSource(new File(FILE_PATH));

		ConstraintDelaunay2D constrainedDelaunay2d = new ConstraintDelaunay2D(
				dsf, new SpatialDataSourceDecorator(ds));

		constrainedDelaunay2d.setVerbose(true);
		//constrainedDelaunay2d.setMorphologicalQualification(true);

		constrainedDelaunay2d.compute();

		// constrainedDelaunay2d.show();

		File gdmsFile = new File("/tmp/tinfaces.gdms");
		gdmsFile.delete();
		dsf.getSourceManager().register("faces", gdmsFile);

		ds = dsf.getDataSource(constrainedDelaunay2d.getEdges());
		ds.open();
		dsf.saveContents("faces", ds);
		ds.close();

		long end = System.currentTimeMillis();

		System.out.println("Total time : " + (end - start));
	}

}
