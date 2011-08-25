/* 
 * TANATO  is a library dedicated to the modelling of water pathways based on 
 * triangulate irregular network. TANATO takes into account anthropogenic and 
 * natural artifacts to evaluate their impacts on the watershed response. 
 * It ables to compute watershed, main slope directions and water flow pathways.
 * 
 * This library has been originally created  by Erwan Bocher during his thesis 
 * “Impacts des activités humaines sur le parcours des écoulements de surface dans 
 * un bassin versant bocager : essai de modélisation spatiale. Application au 
 * Bassin versant du Jaudy-Guindy-Bizien (France)”. It has been funded by the 
 * Bassin versant du Jaudy-Guindy-Bizien and Syndicat d’Eau du Trégor.
 * 
 * The new version is developed at French IRSTV institut as part of the 
 * AvuPur project, funded by the French Agence Nationale de la Recherche 
 * (ANR) under contract ANR-07-VULN-01.
 * 
 * TANATO is distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 * Copyright (C) 2010 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
 * Copyright (C) 2011 Erwan BOCHER, , Alexis GUEGANNO, Jean-Yves MARTIN
 * 
 * TANATO is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * TANATO is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * TANATO. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, please consult: <http://trac.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.tanato;

import java.io.File;
import junit.framework.TestCase;

import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.DataSource;

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
        private static DataSource sdsFaces;
        private static DataSource sdsEdges;
        private static DataSource sdsNodes;
        public static SQLDataSourceFactory dsf = new SQLDataSourceFactory();

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

        public static DataSource getSdsFaces() {
                return sdsFaces;
        }

        public static DataSource getSdsEdges() {
                return sdsEdges;
        }

        public static DataSource getSdsNodes() {
                return sdsNodes;
        }

        private static void createDatasources(String pathNodes, String pathEdges, String pathFaces) throws Exception {
                sdsNodes = dsf.getDataSource(new File(pathNodes));
                sdsEdges = dsf.getDataSource(new File(pathEdges));
                sdsFaces = sdsNodes = dsf.getDataSource(new File(pathFaces));
        }
}
