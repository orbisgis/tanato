package org.tanato.processing.postprocessing;

import java.io.File;
import java.util.ArrayList;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;
import org.gdms.driver.generic.GenericObjectDriver;
import org.tanato.SetUpData;
import org.tanato.model.ECell;
import org.tanato.model.NCell;
import org.tanato.model.TCell;

public class TestTriangleRidge {

	public static DataSourceFactory dsf = new DataSourceFactory();

	private static final String GDMSOUTPUTFILE = "/tmp/triangleridge.gdms";

	private static SpatialDataSourceDecorator sdsFaces;


	private static SpatialDataSourceDecorator sdsEdges;

	private static SpatialDataSourceDecorator sdsNodes;

	private static ArrayList<ECell> eCells;

	private static ArrayList<TCell> tCells;

	private static ArrayList<NCell> nCells;

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException {

		SetUpData.buildHydrologicalGraph(SetUpData.FULL_TEST);

		eCells = SetUpData.getEcells();
		nCells = SetUpData.getNcells();
		tCells = SetUpData.getTcells();

		buildAccumulationAndSaveResult();

	}


	private static void buildAccumulationAndSaveResult()
			throws DriverException, DriverLoadException,
			DataSourceCreationException {


		sdsFaces = SetUpData.getSdsFaces();
		sdsEdges = SetUpData.getSdsEdges();
		sdsNodes = SetUpData.getSdsNodes();

		sdsFaces.open();
		sdsEdges.open();
		sdsNodes.open();

		HydroNetworkExport hydroNetworkExport = new HydroNetworkExport(
				sdsFaces, sdsEdges, sdsNodes);

		GenericObjectDriver driver = hydroNetworkExport
				.exportRidge(tCells);

		sdsFaces.close();

		sdsEdges.close();

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
