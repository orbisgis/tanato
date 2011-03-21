package org.tanato.model;

import java.io.File;
import java.util.ArrayList;

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
import org.gdms.sql.parser.ParseException;
import org.gdms.sql.strategies.SemanticException;
import org.tanato.SetUpData;

public class TestBuildGraph {

	public static DataSourceFactory dsf = new DataSourceFactory();

	private static final String GDMSOUTPUTFILE = "/tmp/result.gdms";

	private static SpatialDataSourceDecorator sdsFaces;

	private static String pathFaces;

	private static String pathEdges;

	private static String pathNodes;

	private static ArrayList<ECell> eCells;

	private static SpatialDataSourceDecorator sdsEdges;

	private static SpatialDataSourceDecorator sdsNodes;

	private static ArrayList<NCell> nCells;

	private static ArrayList<TCell> tCells;

	/**
	 * @param args
	 * @throws DriverException
	 * @throws DataSourceCreationException
	 * @throws DriverLoadException
	 * @throws SemanticException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException, ParseException,
			SemanticException {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);
		eCells = SetUpData.getEcells();
		nCells = SetUpData.getNcells();
		tCells = SetUpData.getTcells();
		sdsEdges = SetUpData.getSdsEdges();
		sdsNodes = SetUpData.getSdsNodes();
		sdsFaces = SetUpData.getSdsFaces();

		buildGraphAndSaveResult();
	}

	private static void buildGraphAndSaveResult() throws DriverLoadException,
			DataSourceCreationException, DriverException {

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.SHORT),
				TypeFactory.createType(Type.GEOMETRY),
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT) }, new String[] { "gid",
				"the_geom", "peres", "fils" });

		GenericObjectDriver driver = new GenericObjectDriver(metadata);

		sdsEdges.open();

		for (ECell hydroCell : eCells) {

				driver.addValues(new Value[] {
						ValueFactory.createValue(hydroCell.getGID()),
						ValueFactory.createValue(sdsEdges.getGeometry(hydroCell
								.getGID() - 1)),
						ValueFactory.createValue(hydroCell.getParent()
								.size()),
						ValueFactory.createValue(hydroCell.getChildrenCells()
								.size()) });


		}

		sdsEdges.close();

		sdsNodes.open();

		for (NCell ncell : nCells) {

			System.out.println("Gid  "+ ncell.getGID());

				driver
						.addValues(new Value[] {
								ValueFactory.createValue(ncell.getGID()),
								ValueFactory.createValue(sdsNodes
										.getGeometry(ncell.getGID() - 1)),
								ValueFactory.createValue(ncell.getParent()
										.size()),
								ValueFactory.createValue(ncell.getChildrenCells()
										.size()) });

		}

		sdsNodes.close();

		sdsFaces.open();
		for (TCell tcell : tCells) {

			if (tcell.isTalweg()) {

				driver
						.addValues(new Value[] {
								ValueFactory.createValue(tcell.getGID()),
								ValueFactory.createValue(sdsFaces
										.getGeometry(tcell.getGID() - 1)),
								ValueFactory.createValue(tcell.getParent()
										.size()),
								ValueFactory.createValue(tcell.getChildrenCells()
										.size()) });

			}
		}

		sdsFaces.close();

		File gdmsFile = new File(GDMSOUTPUTFILE);
		gdmsFile.delete();
		dsf.getSourceManager().register("result", gdmsFile);

		DataSource ds = dsf.getDataSource(driver);

		ds.open();
		dsf.saveContents("result", ds);
		ds.close();

	}
}
