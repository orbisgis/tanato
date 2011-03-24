//package org.tanato.processing.postprocessing;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.LinkedList;
//
//import org.gdms.data.DataSource;
//import org.gdms.data.DataSourceCreationException;
//import org.gdms.data.DataSourceFactory;
//import org.gdms.data.SpatialDataSourceDecorator;
//import org.gdms.data.metadata.DefaultMetadata;
//import org.gdms.data.metadata.Metadata;
//import org.gdms.data.types.Type;
//import org.gdms.data.types.TypeFactory;
//import org.gdms.data.values.Value;
//import org.gdms.data.values.ValueFactory;
//import org.gdms.driver.DriverException;
//import org.gdms.driver.driverManager.DriverLoadException;
//import org.gdms.driver.generic.GenericObjectDriver;
//import org.tanato.SetUpData;
//import org.tanato.model.ECell;
//import org.tanato.model.HydroCell;
//import org.tanato.model.NCell;
//import org.tanato.model.TCell;
//
//import com.vividsolutions.jts.geom.Geometry;
//import com.vividsolutions.jts.geom.GeometryFactory;
//
//public class TestWatershed {
//
//	public static DataSourceFactory dsf = new DataSourceFactory();
//
//	private static final String GDMSOUTPUTFILE = "/tmp/watershed.gdms";
//
//	private static SpatialDataSourceDecorator sdsFaces;
//
//	private static String pathFaces;
//
//	private static String pathEdges;
//
//	private static String pathNodes;
//
//	public static GeometryFactory gf = new GeometryFactory();
//
//	private static ArrayList<ECell> eCells;
//
//	private static ArrayList<NCell> nCells;
//
//	private static ArrayList<TCell> tCells;
//
//	private static SpatialDataSourceDecorator sdsEdges;
//
//	private static SpatialDataSourceDecorator sdsNodes;
//
//	/**
//	 * @param args
//	 * @throws DriverException
//	 * @throws DataSourceCreationException
//	 * @throws DriverLoadException
//	 */
//	public static void main(String[] args) throws DriverLoadException,
//			DataSourceCreationException, DriverException {
//		SetUpData.buildHydrologicalGraph(SetUpData.FULLSMALL_TEST);
//		eCells = SetUpData.getEcells();
//		nCells = SetUpData.getNcells();
//		tCells = SetUpData.getTcells();
//		sdsEdges = SetUpData.getSdsEdges();
//		sdsNodes = SetUpData.getSdsNodes();
//		sdsFaces = SetUpData.getSdsFaces();
//
//
//		buildWatershedAndSaveResult();
//	}
//
//
//	private static void buildWatershedAndSaveResult() throws DriverException,
//			DriverLoadException, DataSourceCreationException {
//
//
//
//		Metadata metadata = new DefaultMetadata(new Type[] {
//				TypeFactory.createType(Type.SHORT),
//				TypeFactory.createType(Type.GEOMETRY) }, new String[] { "gid",
//				"the_geom" });
//
//		GenericObjectDriver driver = new GenericObjectDriver(metadata);
//
//
//		sdsFaces.open();
//		sdsEdges.open();
//
//		int numero = 25;
//		ECell cellPointed = eCells.get(numero);
//
//		System.out.println(cellPointed.getGID());
//
//		HydroNetworkProcess hydroNetworkProcess = new HydroNetworkProcess(
//				sdsFaces, sdsEdges, sdsNodes);
//
//		LinkedList cellsBasin = hydroNetworkProcess.getWatershed(cellPointed);
//
//		int k = 0;
//		for (Iterator i = cellsBasin.iterator(); i.hasNext();) {
//
//			HydroCell cell = (HydroCell) i.next();
//
//			Geometry geom = null;
//			if (cell instanceof TCell) {
//				k++;
//				geom = sdsFaces.getGeometry(cell.getGID() - 1);
//
//			}
//
//			else if (cell instanceof ECell) {
//
//				k++;
//				geom = sdsEdges.getGeometry(cell.getGID() - 1);
//			}
//
//			driver.addValues(new Value[] { ValueFactory.createValue(k),
//
//			ValueFactory.createValue(geom) });
//
//		}
//
//		sdsFaces.close();
//
//		sdsEdges.close();
//
//		DataSource ds = dsf.getDataSource(driver);
//
//		File gdmsFile = new File(GDMSOUTPUTFILE);
//		gdmsFile.delete();
//		dsf.getSourceManager().register("result", gdmsFile);
//
//		ds.open();
//		dsf.saveContents("result", ds);
//		ds.close();
//	}
//
//}