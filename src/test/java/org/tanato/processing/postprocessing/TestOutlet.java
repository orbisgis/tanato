package org.tanato.processing.postprocessing;

import java.io.File;
import java.util.ArrayList;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.driver.memory.ObjectMemoryDriver;
import org.tanato.SetUpData;
import org.tanato.model.ECell;
import org.tanato.model.NCell;
import org.tanato.model.TCell;
import org.tanato.processing.postprocessing.HydroNetworkExport;
import com.vividsolutions.jts.geom.GeometryFactory;

public class TestOutlet {

	public static DataSourceFactory dsf = new DataSourceFactory();

	private static final String GDMSOUTPUTFILE = "/tmp/outlet.gdms";

	private static SpatialDataSourceDecorator sdsFaces;

	public static GeometryFactory gf = new GeometryFactory();

	private static SpatialDataSourceDecorator sdsEdges;

	private static SpatialDataSourceDecorator sdsNodes;

	private static ArrayList<NCell> nCells;

	private static ArrayList<ECell> eCells;

	private static ArrayList<TCell> tCells;

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException {

		SetUpData.buildHydrologicalGraph(SetUpData.TALWEG_TEST);

		eCells = SetUpData.getEcells();
		nCells = SetUpData.getNcells();
		tCells = SetUpData.getTcells();

		buildAccumulationAndSaveResult();
	}

	private static void buildAccumulationAndSaveResult()
			throws DriverException, DriverLoadException,
			DataSourceCreationException {

		sdsNodes = SetUpData.getSdsNodes();

		sdsNodes.open();

		HydroNetworkExport hydroNetworkExport = new HydroNetworkExport(
				sdsFaces, sdsEdges, sdsNodes);

		ObjectMemoryDriver driver = hydroNetworkExport.exportOutlet(nCells);

		sdsNodes.close();

		DataSource ds = dsf.getDataSource(driver);


		File gdmsFile = new File(GDMSOUTPUTFILE);
		gdmsFile.delete();
		dsf.getSourceManager().register("result", gdmsFile);

		ds.open();
		dsf.saveContents("result", ds);
		ds.close();

	}

}
