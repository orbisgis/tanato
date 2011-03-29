package org.tanato;

import java.io.File;
import junit.framework.TestCase;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;

public class SetUpData extends TestCase {

        public static String internalSource = "src/test/resources/data/source/";
        public static String internalTIN = "src/test/resources/data/tin/";
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
        public static DataSourceFactory dsf = new DataSourceFactory();

        // Initialize the hydrological graph
        public static void createDataSourcesForTest(int testType) throws Exception {
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
        private static void buildTMP() throws Exception {

                pathFaces = "/tmp/tinTriangles.gdms";

                pathEdges = "/tmp/tinEdges.gdms";

                pathNodes = "/tmp/tinPoints.gdms";

                createDatasources(pathNodes, pathEdges, pathFaces);


        }

        // Cas talweg triangle
        private static void buildCasTalwegFace() throws Exception {

                pathFaces = internalTIN + "cas_talweg_triangle/tinfaces.shp";

                pathEdges = internalTIN + "cas_talweg_triangle/tinedges.shp";

                pathNodes = internalTIN + "cas_talweg_triangle/tinnodes.shp";

                createDatasources(pathNodes, pathEdges, pathFaces);
        }

        // Cas complet taille limitée
        private static void buildSmallCompleteCase() throws Exception {

                pathFaces = internalTIN + "cas_complet_petit/tinfaces.shp";

                pathEdges = internalTIN + "cas_complet_petit/tinedges.shp";

                pathNodes = internalTIN + "cas_complet_petit/tinnodes.shp";

                createDatasources(pathNodes, pathEdges, pathFaces);
        }

        // Cas avec données Mercier
        private static void buildMercierTopo() throws Exception {

                pathFaces = internalTIN + "cas_mercier_topo/tinfaces.shp";

                pathEdges = internalTIN + "cas_mercier_topo/tinedges.shp";

                pathNodes = internalTIN + "cas_mercier_topo/tinnodes.shp";

                createDatasources(pathNodes, pathEdges, pathFaces);
        }

        // Cas avec face parallele
        private static void buildParalleleFace() throws Exception {

                pathFaces = internalTIN + "cas_face_parallele/tinfaces.shp";

                pathEdges = internalTIN + "cas_face_parallele/tinedges.shp";

                pathNodes = internalTIN + "cas_face_parallele/tinnodes.shp";

                createDatasources(pathNodes, pathEdges, pathFaces);
        }

        // Cas simple de modélisation
        private static void buildSimpleCase() throws Exception {

                pathFaces = internalTIN + "cas_simple/tinfaces.shp";

                pathEdges = internalTIN + "cas_simple/tinedges.shp";

                pathNodes = internalTIN + "cas_simple/tinnodes.shp";

                createDatasources(pathNodes, pathEdges, pathFaces);
        }

        // Cas talweg continu
        private static void buildTalweg() {

                pathFaces = internalTIN + "cas_talweg/tinfaces.shp";

                pathEdges = internalTIN + "cas_talweg/tinedges.shp";

                pathNodes = internalTIN + "cas_talweg/tinnodes.shp";



        }

        private static void buildFullTest() throws Exception {

                pathFaces = internalTIN + "cas_complet/tinfaces.shp";
                pathEdges = internalTIN + "cas_complet/tinedges.shp";
                pathNodes = internalTIN + "cas_complet/tinnodes.shp";

                createDatasources(pathNodes, pathEdges, pathFaces);

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

        private static void createDatasources(String pathNodes, String pathEdges, String pathFaces) throws Exception {
                sdsNodes = new SpatialDataSourceDecorator(dsf.getDataSource(new File(pathNodes)));
                sdsEdges = new SpatialDataSourceDecorator(dsf.getDataSource(new File(pathEdges)));
                sdsFaces = sdsNodes = new SpatialDataSourceDecorator(dsf.getDataSource(new File(pathFaces)));
        }
}
