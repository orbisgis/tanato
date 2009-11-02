package org.tanato;

import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.gdms.data.DataSource;
import org.gdms.data.DataSourceCreationException;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;

import org.tanato.model.*;

public class SetUpData extends TestCase {

	
	public static String internalSource =  "src/test/resources/data/source/";
	public static String internalTIN =  "src/test/resources/data/tin/";
	
	public static final int FULL_TEST = 1;
	public static final int TALWEG_TEST = 2;
	public static final int SIMPLE_TEST = 4;
	public static final int FACEPARALLELE_TEST = 8;
	public static final int MERCIERTOPO_TEST = 16;
	public static final int FULLSMALL_TEST = 32;
	public static final int TALWEGFACE_TEST = 64;
	public static final int TMP_TEST = 128;

	private static String pathFaces;
	private static String pathEdges;
	private static String pathNodes;
	private static SpatialDataSourceDecorator sdsFaces;
	private static SpatialDataSourceDecorator sdsEdges;
	private static SpatialDataSourceDecorator sdsNodes;
	private static ArrayList<ECell> ecells;
	private static ArrayList<TCell> tcells;
	private static ArrayList<NCell> ncells;

	public static DataSourceFactory dsf = new DataSourceFactory();



	// Initialize the hydrological graph

	public static void buildHydrologicalGraph(int testType) {
		switch (testType) {
		case FULL_TEST:

			buildFullTest();

			break;

		case TALWEG_TEST:

			buildTalweg();

			break;

		case SIMPLE_TEST:

			buildSimpleCase();

			break;

		case FACEPARALLELE_TEST:

			buildParalleleFace();

			break;

		case MERCIERTOPO_TEST:

			buildMercierTopo();

			break;

		case FULLSMALL_TEST:

			buildSmallCompleteCase();

			break;

		case TALWEGFACE_TEST:

			buildCasTalwegFace();
			break;


		case TMP_TEST:

			buildTMP();
			break;


		default:
			break;
		}
	}

	//A temporal test data for debug mode
	private static void buildTMP() {

		pathFaces = "/tmp/tinfaces.gdms";

		pathEdges = "/tmp/tinedges.gdms";

		pathNodes = "/tmp/tinnodes.gdms";

		buildHydrologicalGraph();


	}

	// Cas talweg triangle
	private static void buildCasTalwegFace() {

		pathFaces = internalTIN+ "cas_talweg_triangle/tinfaces.shp";

		pathEdges = internalTIN+ "cas_talweg_triangle/tinedges.shp";

		pathNodes = internalTIN +"cas_talweg_triangle/tinnodes.shp";

		buildHydrologicalGraph();
	}

	// Cas complet taille limitée
	private static void buildSmallCompleteCase() {

		pathFaces = internalTIN + "cas_complet_petit/tinfaces.shp";

		pathEdges = internalTIN + "cas_complet_petit/tinedges.shp";

		pathNodes = internalTIN+ "cas_complet_petit/tinnodes.shp";

		buildHydrologicalGraph();
	}

	// Cas avec données Mercier
	private static void buildMercierTopo() {

		pathFaces = internalTIN + "cas_mercier_topo/tinfaces.shp";

		pathEdges = internalTIN + "cas_mercier_topo/tinedges.shp";

		pathNodes = internalTIN + "cas_mercier_topo/tinnodes.shp";

		buildHydrologicalGraph();
	}

	// Cas avec face parallele
	private static void buildParalleleFace() {

		pathFaces = internalTIN + "cas_face_parallele/tinfaces.shp";

		pathEdges = internalTIN +  "cas_face_parallele/tinedges.shp";

		pathNodes = internalTIN +  "cas_face_parallele/tinnodes.shp";

		buildHydrologicalGraph();
	}

	// Cas simple de modélisation

	private static void buildSimpleCase() {

		pathFaces = internalTIN +  "cas_simple/tinfaces.shp";

		pathEdges = internalTIN +  "cas_simple/tinedges.shp";

		pathNodes = internalTIN +  "cas_simple/tinnodes.shp";

		buildHydrologicalGraph();
	}

	// Cas talweg continu
	private static void buildTalweg() {

		pathFaces = internalTIN +  "cas_talweg/tinfaces.shp";

		pathEdges = internalTIN +  "cas_talweg/tinedges.shp";

		pathNodes = internalTIN +  "cas_talweg/tinnodes.shp";

		buildHydrologicalGraph();

	}

	private static void buildFullTest() {

		pathFaces = internalTIN +  "cas_complet/tinfaces.shp";
		pathEdges = internalTIN + "cas_complet/tinedges.shp";
		pathNodes = internalTIN +  "cas_complet/tinnodes.shp";

		buildHydrologicalGraph();

	}

	private static void buildHydrologicalGraph() {
		try {
			DataSource dsFaces = dsf.getDataSource(new File(pathFaces));

			sdsFaces = new SpatialDataSourceDecorator(dsFaces);

			DataSource dsEdges = dsf.getDataSource(new File(pathEdges));

			sdsEdges = new SpatialDataSourceDecorator(dsEdges);

			DataSource dsNodes = dsf.getDataSource(new File(pathNodes));

			sdsNodes = new SpatialDataSourceDecorator(dsNodes);

			HydroTINModel hydrotinModel = new HydroTINModel(sdsFaces, sdsEdges,
					sdsNodes);
			// Obtient le graph hydrologique avec l'ensemble des connexions
			ecells = hydrotinModel.getEcells();
			tcells = hydrotinModel.getTcells();
			ncells = hydrotinModel.getNcells();

		} catch (DriverLoadException e) {
			e.printStackTrace();
		} catch (DataSourceCreationException e) {
			e.printStackTrace();
		} catch (DriverException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<ECell> getEcells() {
		return ecells;
	}

	public static ArrayList<TCell> getTcells() {
		return tcells;
	}

	public static ArrayList<NCell> getNcells() {
		return ncells;
	}

	public static SpatialDataSourceDecorator getSdsFaces() {
		return sdsFaces;
	}

	public static SpatialDataSourceDecorator getSdsEdges() {
		return sdsEdges;
	}

	public static SpatialDataSourceDecorator getSdsNodes() {
		return sdsNodes;
	}
}
