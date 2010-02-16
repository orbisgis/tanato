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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class TestExtractRunoffPath {

	public static DataSourceFactory dsf = new DataSourceFactory();

	private static final String GDMSOUTPUTFILE = "/tmp/runoffpath.gdms";

	private static SpatialDataSourceDecorator sdsFaces;

	public static GeometryFactory gf = new GeometryFactory();

	private static SpatialDataSourceDecorator sdsEdges;

	private static SpatialDataSourceDecorator sdsNodes;

	private static ArrayList<ECell> eCells;

	private static ArrayList<NCell> nCells;

	private static ArrayList<TCell> tCells;

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);
		eCells = SetUpData.getEcells();
		nCells = SetUpData.getNcells();
		tCells = SetUpData.getTcells();
		sdsEdges = SetUpData.getSdsEdges();
		sdsNodes = SetUpData.getSdsNodes();
		sdsFaces = SetUpData.getSdsFaces();
		buildRiverAndSaveResult();
	}

	private static void buildRiverAndSaveResult() throws DriverException,
			DriverLoadException, DataSourceCreationException {

		sdsFaces.open();
		sdsEdges.open();
		sdsNodes.open();

		HydroNetworkProcess hydroNetworkProcess = new HydroNetworkProcess(
				sdsFaces, sdsEdges, sdsNodes);

		// On va chercher une hydrocell dans le tableau

		int numero = 20;


		NCell  cell = nCells.get(numero);

		Coordinate p = sdsNodes.getGeometry(cell.getGID() - 1).getCoordinates()[0];

		GenericObjectDriver driver = hydroNetworkProcess.buildRunOffPath(p, cell);

		System.out.println("Nombre de lignes  "+ driver.getRowCount());

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
