//package org.tanato.model;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.ListIterator;
//
//import org.gdms.data.SpatialDataSourceDecorator;
//import org.gdms.driver.DriverException;
//
//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.Geometry;
//import com.vividsolutions.jts.geom.GeometryFactory;
//import com.vividsolutions.jts.geom.LineString;
//import com.vividsolutions.jts.geom.Point;
//import com.vividsolutions.jts.geom.Polygon;
//
//public class HydroTINModel {
//
//        ArrayList<TCell> tcells;
//        ArrayList<ECell> ecells;
//        ArrayList<NCell> ncells;
//        private SpatialDataSourceDecorator sdsEdges;
//        private SpatialDataSourceDecorator sdsFaces;
//        private SpatialDataSourceDecorator sdsNodes;
//        private long sdsEdgesCount;
//        private long sdsFacesCount;
//        private long sdsNodesCount;
//        private static final String ID = "gid";
//        private static final String RIGHT_FACE = "right_t";
//        private static final String LEFT_FACE = "left_t";
//        private static final String START_NODE = "start_n";
//        private static final String END_NODE = "end_n";
//        private static final String PROPERTY_FIELD = "property";
//        private static final String HEIGHT = "height";
//        private static final String GID_SOURCE = "gid_source";
//        private static final String SLOPE = "slope";
//        GeometryFactory gf = new GeometryFactory();
//
//        /**
//         * La datasource d'entrée correspond au TIN. Celui-ci est composé de trois
//         * datasources noeud, arc et triangle. Un champ nommé type est ajouté pour
//         * qualifier les objets du TIN. Obstacle, collecteur... des triangles :
//         * rural, urbain des noeuds : connecteur
//         *
//         * Une seconde qualification est réalisée automatiquement en construisant le
//         * graph. Le graph est ordonné et valué.
//         *
//         * @param ds
//         */
//        public HydroTINModel(SpatialDataSourceDecorator sdsFaces,
//                SpatialDataSourceDecorator sdsEdges,
//                SpatialDataSourceDecorator sdsNodes) throws DriverException {
//                this.sdsFaces = sdsFaces;
//                this.sdsEdges = sdsEdges;
//                this.sdsNodes = sdsNodes;
//
//                createHydroCells();
//                buildTINGraph();
//                linkTalweg();
//                buildProportion();
//        }
//
//        // Etape 1 initialisation des structures de données pour le stockage
//        // non-redondant des hydrocells du graphe
//        public void createHydroCells() throws DriverException {
//                tcells = new ArrayList<TCell>();
//                ecells = new ArrayList<ECell>();
//                ncells = new ArrayList<NCell>();
//                createHydroCells();
//                buildTINGraph();
//                linkTalweg();
//                buildProportion();
//        }
//
//        // Etape 1 initialisation des structures de données pour le stockage
//        // non-redondant des hydrocells du graphe
//        public final void createHydroCells() throws DriverException {
//                tcells = new ArrayList<TCell>();
//                ecells = new ArrayList<ECell>();
//                ncells = new ArrayList<NCell>();
//
//                sdsFaces.open();
//                sdsEdges.open();
//                sdsNodes.open();
//
//                sdsEdgesCount = sdsEdges.getRowCount();
//
//                sdsFacesCount = sdsFaces.getRowCount();
//
//                sdsNodesCount = sdsNodes.getRowCount();
//
//
//                // Création des structures de données vides
//                ECell ec;
//                for (int i = 0; i < sdsEdgesCount; i++) {
//                        //We fill the list with the desired values.
//                        ec = new ECell();
//                        ecells.add(ec);
//                }
//
//                sdsEdges.close();
//
//
//                TCell tc;
//                for (int i = 0; i < sdsFacesCount; i++) {
//                        tc = new TCell();
//                        tcells.add(tc);
//                }
//                sdsFaces.close();
//
//                NCell nc;
//                for (int i = 0; i < sdsNodesCount; i++) {
//                        nc = new NCell();
//                        ncells.add(nc);
//                }
//                sdsNodes.close();
//
//
//
//        }
//
//        /**
//         * Construction du graphe à partir des 3 datasources nodes, edges et faces.
//         * Note : Nous considerons que les identifiants (GID) du graphe commemcent
//         * tous à 1.
//         *
//         *
//         */
//        public final void buildTINGraph() {
//
//
//                try {
//                        sdsFaces.open();
//                        sdsEdges.open();
//                        sdsNodes.open();
//
//                        int gidField = sdsEdges.getFieldIndexByName(ID);
//                        int rightFace = sdsEdges.getFieldIndexByName(RIGHT_FACE);
//                        int leftFace = sdsEdges.getFieldIndexByName(LEFT_FACE);
//                        int propertyIndex = sdsEdges.getFieldIndexByName(PROPERTY_FIELD);
//                        int startNodeFieldIndex = sdsEdges.getFieldIndexByName(START_NODE);
//                        int endNodeFieldIndex = sdsEdges.getFieldIndexByName(END_NODE);
//                        int heightIndex = sdsEdges.getFieldIndexByName(HEIGHT);
//                        int gidSourceIndex = sdsEdges.getFieldIndexByName(GID_SOURCE);
//                        double slopeIndex = sdsEdges.getFieldIndexByName(SLOPE);
//
//                        boolean rightTtoEdge = false;
//                        boolean rightTColinear = false;
//                        boolean righTFlat = false;
//                        boolean leftTtoEdge = false;
//                        boolean leftTColinear = false;
//                        boolean leftTFlat = false;
//                        boolean rightBorder = false;
//                        boolean leftBorder = false;
//
//                        // Triangle à droite
//                        TCell dTCell = null;
//                        // Triangle à gauche
//                        TCell gTCell = null;
//
//                        // Noeud haut
//                        NCell hautNCell = null;
//                        // Noeud bas
//                        NCell basNCell = null;
//
//                        for (int i = 0; i < sdsEdgesCount; i++) {
//
//                                // Les identifiants des gid noeuds start et end
//                                int gidStartNode = sdsEdges.getFieldValue(i,
//                                        startNodeFieldIndex).getAsInt();
//                                int gidEndNode = sdsEdges.getFieldValue(i, endNodeFieldIndex).getAsInt();
//
//                                // Les identifiants des triangles à droite et à gauche
//                                int gidTcellRight = sdsEdges.getFieldValue(i, rightFace).getAsInt();
//                                int gidTcellLeft = sdsEdges.getFieldValue(i, leftFace).getAsInt();
//
//                                int edgeGID = sdsEdges.getFieldValue(i, gidField).getAsInt();
//                                int property = sdsEdges.getFieldValue(i, propertyIndex).getAsInt();
//                                double heigh = sdsEdges.getFieldValue(i, heightIndex).getAsDouble();
//
//
//                                // Recupere la geom de l'edge
//                                Geometry geomEdge = sdsEdges.getGeometry(i);
//                                LineString edgeGeom = (LineString) geomEdge.getGeometryN(0);
//
//                                ECell eCell = ecells.get(i);
//
//                                // Ajout des topologies sur eCell
//                                eCell.setGID(edgeGID);
//                                eCell.setRightGID(gidTcellRight);
//                                eCell.setLeftGID(gidTcellLeft);
//                                eCell.setStartNodeGID(gidStartNode);
//                                eCell.setEndNodeGID(gidEndNode);
//                                eCell.setHeight(heigh);
//
//
//                                // Affectation des TCells
//
//                                // On teste si le triangle à droite existe
//                                if (gidTcellRight != -1) {
//                                        int indexTcellRight = gidTcellRight - 1;
//                                        dTCell = tcells.get(gidTcellRight - 1);
//                                        dTCell.setGID(gidTcellRight);
//                                } else {
//                                        rightBorder = true;
//                                }
//
//                                // On teste si le triangle à gauche existe
//                                if (gidTcellLeft != -1) {
//                                        int indexTcellLeft = gidTcellLeft - 1;
//                                        gTCell = tcells.get(indexTcellLeft);
//                                        gTCell.setGID(gidTcellLeft);
//
//                                } else {
//                                        leftBorder = true;
//                                }
//
//                                // Qualification de la pente de l'edge parcouru et construction du graphe
//
//                                if (!leftBorder && !rightBorder) {
//                                        // Traitement des ridges
//                                        if ((!rightTtoEdge && !leftTtoEdge)
//                                                && (!righTFlat && !leftTFlat)) {
//                                                //TODO: A completer
//                                        } // Cas d'un edge talweg. Les deux triangles pointent vers l'edge
//                                        else if (rightTtoEdge && leftTtoEdge) {
//                                                eCell.setParent(dTCell);
//                                                eCell.setParent(gTCell);
//                                        }
//                                }
//
//
//                                boolean constraint = false;
//                                if (property.equalsIgnoreCase(HydroProperties.toString(HydroProperties.DITCH))
//                                        || (property.equalsIgnoreCase(HydroProperties.toString(HydroProperties.RIVER)))
//                                        || (property.equalsIgnoreCase(HydroProperties.toString(HydroProperties.SEWER)))) {
//
//                                        constraint = true;
//                                        hautNCell = ncells.get(gidStartNode - 1);
//                                        hautNCell.setGID(gidStartNode);
//                                        basNCell = ncells.get(gidEndNode - 1);
//                                        basNCell.setGID(gidEndNode);
//                                        eCell.setTalweg(true);
//
//                                } else {
//                                        // Hierarchisation des noeuds haut et bas en fonction de
//                                        // leur Z.
//                                        // 1 = down, -1 = up 0 = flat regarding to the topology
//                                        int edgeGradient = 0;
//                                        if (geomStartNode.getCoordinate().z > geomEndNode.getCoordinate().z) {
//
//                                                hautNCell = ncells.get(gidStartNode - 1);
//                                                hautNCell.setGID(gidStartNode);
//                                                basNCell = ncells.get(gidEndNode - 1);
//                                                basNCell.setGID(gidEndNode);
//                                                edgeGradient = 1;
//
//                                        } else if (geomStartNode.getCoordinate().z < geomEndNode.getCoordinate().z) {
//
//                                                hautNCell = ncells.get(gidEndNode - 1);
//                                                hautNCell.setGID(gidEndNode);
//                                                basNCell = ncells.get(gidStartNode - 1);
//                                                basNCell.setGID(gidStartNode);
//                                                edgeGradient = -1;
//                                        } else {
//                                                edgeGradient = 0;
//                                                hautNCell = ncells.get(gidStartNode - 1);
//                                                hautNCell.setGID(gidStartNode);
//                                                basNCell = ncells.get(gidEndNode - 1);
//                                                basNCell.setGID(gidEndNode);
//                                        }
//                                }
//
//                                eCell.setNodeHaut(hautNCell);
//                                eCell.setBasNcell(basNCell);
//
//                                // On construit le graphe à partir des qualifications topo
//
//                                /**
//                                 * Traitement des talwegs
//                                 */
//                                // Cas des talwegs simple
//                                if (edgeTopoType == HydroProperties.TALWEG) {
//                                        eCell.setTalweg(true);
//                                        basNCell.setTalweg(true);
//                                        hautNCell.setTalweg(true);
//
//                                        // Graph
//                                        eCell.setParent(hautNCell);
//                                        hautNCell.setChildren(eCell);
//                                        basNCell.setParent(eCell);
//                                        eCell.setChildren(basNCell);
//
//                                        eCell.setParent(dTCell);
//                                        eCell.setParent(gTCell);
//                                        dTCell.setChildren(eCell);
//                                        gTCell.setChildren(eCell);
//                                } // Talweg colineaire à droite
//                                else if (edgeTopoType == HydroProperties.RIGHTCOLINEAR) {
//
//                                        eCell.setTalweg(true);
//                                        basNCell.setTalweg(true);
//                                        hautNCell.setTalweg(true);
//
//                                        // Graph
//
//                                        eCell.setParent(hautNCell);
//                                        hautNCell.setChildren(eCell);
//                                        basNCell.setParent(eCell);
//                                        eCell.setChildren(basNCell);
//
//                                        eCell.setParent(gTCell);
//                                        gTCell.setChildren(eCell);
//
//                                } // Talweg colinéaire à gauche
//                                else if (edgeTopoType == HydroProperties.LEFTCOLINEAR) {
//
//                                        eCell.setTalweg(true);
//                                        basNCell.setTalweg(true);
//                                        hautNCell.setTalweg(true);
//
//                                        // Graph
//                                        eCell.setParent(hautNCell);
//                                        hautNCell.setChildren(eCell);
//                                        basNCell.setParent(eCell);
//                                        eCell.setChildren(basNCell);
//
//                                        eCell.setParent(dTCell);
//                                        dTCell.setChildren(eCell);
//
//                                } // Double talweg colineaire
//                                else if (edgeTopoType == HydroProperties.DOUBLECOLINEAR) {
//
//                                        eCell.setTalweg(true);
//                                        basNCell.setTalweg(true);
//                                        hautNCell.setTalweg(true);
//
//                                        // Graph
//                                        eCell.setParent(hautNCell);
//                                        hautNCell.setChildren(eCell);
//                                        basNCell.setParent(eCell);
//                                        eCell.setChildren(basNCell);
//
//                                } /**
//                                 * Traitement des ridges
//                                 */
//                                else if (edgeTopoType == HydroProperties.RIDGE) {
//
//                                        if (constraint) {
//
//                                                eCell.setTalweg(true);
//                                                basNCell.setTalweg(true);
//                                                hautNCell.setTalweg(true);
//
//                                                // Graph
//                                                eCell.setParent(hautNCell);
//                                                hautNCell.setChildren(eCell);
//                                                basNCell.setParent(eCell);
//                                                eCell.setChildren(basNCell);
//
//                                        } else {
//                                                eCell.setRidge(true);
//
//                                                // Graph
//                                                eCell.setParent(hautNCell);
//                                                hautNCell.setChildren(eCell);
//                                                basNCell.setParent(eCell);
//                                                eCell.setChildren(basNCell);
//
//                                                if (dTCell.getSlopeInDegree() > gTCell.getSlopeInDegree()) {
//
//                                                        hautNCell.setChildren(dTCell);
//                                                        dTCell.setParent(hautNCell);
//                                                } else if (dTCell.getSlopeInDegree() < gTCell.getSlopeInDegree()) {
//                                                        hautNCell.setChildren(gTCell);
//                                                        gTCell.setParent(hautNCell);
//                                                } else {
//                                                }
//                                        }
//
//                                } /**
//                                 * Traitement des pentes à droite
//                                 */
//                                else if (edgeTopoType == HydroProperties.RIGHTSLOPE) {
//
//                                        if (constraint) {
//
//                                                eCell.setTalweg(true);
//                                                basNCell.setTalweg(true);
//                                                hautNCell.setTalweg(true);
//
//                                                // Graph
//                                                eCell.setParent(hautNCell);
//                                                hautNCell.setChildren(eCell);
//                                                basNCell.setParent(eCell);
//                                                eCell.setChildren(basNCell);
//                                                eCell.setParent(gTCell);
//                                                gTCell.setChildren(eCell);
//
//                                        } else {
//                                                eCell.setTransfluent(true);
//
//                                                hautNCell.setChildren(dTCell);
//                                                dTCell.setParent(hautNCell);
//                                                dTCell.setParent(eCell);
//                                                eCell.setChildren(dTCell);
//                                                eCell.setParent(gTCell);
//                                                gTCell.setChildren(eCell);
//                                        }
//                                } /**
//                                 * Traitement des pentes à gauche
//                                 */
//                                else if (edgeTopoType == HydroProperties.LEFTTSLOPE) {
//
//                                        if (constraint) {
//
//                                                eCell.setTalweg(true);
//                                                basNCell.setTalweg(true);
//                                                hautNCell.setTalweg(true);
//
//                                                // Graph
//                                                eCell.setParent(hautNCell);
//                                                hautNCell.setChildren(eCell);
//                                                basNCell.setParent(eCell);
//                                                eCell.setChildren(basNCell);
//                                                eCell.setParent(dTCell);
//                                                dTCell.setChildren(eCell);
//
//                                        } else {
//                                                eCell.setTransfluent(true);
//
//                                                // Graph
//
//                                                hautNCell.setChildren(gTCell);
//                                                gTCell.setParent(hautNCell);
//                                                gTCell.setParent(eCell);
//                                                eCell.setChildren(gTCell);
//                                                eCell.setParent(dTCell);
//                                                dTCell.setChildren(eCell);
//
//                                        }
//                                } /**
//                                 * Traitement du rebord droit Attention l'edge n'est pas
//                                 * connecté au rebord
//                                 */
//                                /*
//                                 *
//                                 * else if (edgeTopoType.equals(TopoType.RIGHTSIDE)) { // Graph
//                                 *
//                                 * hautNCell.setFilsCells(dTCell);
//                                 * dTCell.setPeresCells(hautNCell); }
//                                 *//**
//                                 * Traitement du rebord gauche
//                                 */
//                                /*
//                                 *
//                                 * else if (edgeTopoType.equals(TopoType.LEFTSIDE)) { // Graph
//                                 * hautNCell.setFilsCells(gTCell);
//                                 * gTCell.setPeresCells(hautNCell); }
//                                 */ /**
//                                 * Traitement du fond droit
//                                 */
//                                else if (edgeTopoType == HydroProperties.RIGHTWELL) {
//
//                                        if (constraint) {
//
//                                                eCell.setTalweg(true);
//                                                basNCell.setTalweg(true);
//                                                hautNCell.setTalweg(true);
//
//                                                // Graph
//                                                eCell.setParent(hautNCell);
//                                                hautNCell.setChildren(eCell);
//                                                basNCell.setParent(eCell);
//                                                eCell.setChildren(basNCell);
//                                                eCell.setParent(dTCell);
//                                                dTCell.setChildren(eCell);
//
//                                        } else {
//                                                eCell.setTransfluent(true);
//                                                // Graph
//
//                                                gTCell.setChildren(eCell);
//                                                eCell.setParent(gTCell);
//                                                eCell.setChildren(dTCell);
//                                                dTCell.setParent(eCell);
//                                        }
//                                } /**
//                                 * Traitement du fond gauche
//                                 */
//                                else if (edgeTopoType == HydroProperties.LEFTWELL) {
//
//                                        if (constraint) {
//
//                                                eCell.setTalweg(true);
//                                                basNCell.setTalweg(true);
//                                                hautNCell.setTalweg(true);
//
//                                                // Graph
//                                                eCell.setParent(hautNCell);
//                                                hautNCell.setChildren(eCell);
//                                                basNCell.setParent(eCell);
//                                                eCell.setChildren(basNCell);
//                                                eCell.setParent(dTCell);
//                                                dTCell.setChildren(eCell);
//
//                                        } else {
//                                                eCell.setTransfluent(true);
//
//                                                // Graph
//                                                dTCell.setChildren(eCell);
//                                                eCell.setParent(dTCell);
//                                                eCell.setChildren(dTCell);
//                                                dTCell.setParent(eCell);
//
//                                        }
//                                }
//
//                        }
//
//                        sdsEdges.close();
//                        sdsFaces.close();
//                        sdsNodes.close();
//
//                } catch (DriverException e) {
//                        e.printStackTrace();
//                }
//
//        }
//
//        public void linkTalweg() {
//
//                // Traitement des lignes de crêtes
//                for (ECell ecell : ecells) {
//
//                        if (ecell.isRidge()) {
//
//                                NCell hautNCell = ecell.getHautNcell();
//
//                                boolean existFilsTalweg = false;
//
//                                for (HydroCellValued hydrocell : ecell.getChildrenCells()) {
//
//                                        if (hydrocell.getHydroCell().isTalweg()) {
//                                                existFilsTalweg = true;
//                                        }
//                                }
//
//                                if (!existFilsTalweg) {
//
//                                        for (HydroCellValued hydrocell : ecell.getChildrenCells()) {
//
//                                                if (hydrocell.getHydroCell() instanceof TCell) {
//                                                        TCell tCell = (TCell) hydrocell.getHydroCell();
//                                                        hautNCell.setChildren(tCell);
//                                                        tCell.setParent(hautNCell);
//
//                                                }
//                                        }
//                                }
//                        }
//
//                }
//
//                // Pour rechercher le triangle talweg d'un noeud nous parcourons
//                // l'ensemble des fils des triangles.
//
//                for (NCell nCell : ncells) {
//
//
//                        LinkedList<HydroCell> cellsList =
//                                new LinkedList<HydroCell>();
//
//                        for (HydroCellValued fCell : nCell.getChildrenCells()) {
//
//                                if (fCell.getHydroCell() instanceof TCell) {
//
//                                        if (!fCell.getHydroCell().isTalweg()) {
//                                                cellsList.add(fCell.getHydroCell());
//                                        }
//
//                                }
//                        }
//
//                        ListIterator<HydroCell> iter = cellsList.listIterator();
//
//                        while (iter.hasNext()) {
//
//                                boolean connected = false;
//
//                                HydroCell cell = iter.next();
//
//                                for (HydroCellValued hydroCell2 : cell.getChildrenCells()) {
//
//                                        if (hydroCell2.getHydroCell() instanceof ECell) {
//                                                ECell eFMaCell = (ECell) hydroCell2.getHydroCell();
//
//                                                // Regarde si l'edge est connecté au noeud
//                                                if (eFMaCell.getHautNcell().getGID() == nCell.getGID()) {
//                                                        connected = true;
//
//                                                } else if (eFMaCell.getBasNcell().getGID() == nCell.getGID()) {
//                                                        connected = true;
//                                                }
//                                        }
//                                }
//                                if (connected) {
//                                        iter.remove();
//                                }
//                        }
//                        for (HydroCell hydroCell : cellsList) {
//
//                                hydroCell.setTalweg(true);
//
//                        }
//                }
//        }
//
//        private void buildProportion() {
//
//                try {
//
//                        sdsFaces.open();
//                        sdsNodes.open();
//
//
//
//
//                        for (TCell cell : tcells) {
//
//                                if (cell.getChildrenCells().size() == 2) {
//
//                                        Polygon polygon = (Polygon) sdsFaces.getGeometry(
//                                                cell.getGID() - 1).getGeometryN(0);
//
//                                        ECell e1 = (ECell) cell.getChildrenCells().get(0).getHydroCell();
//
//                                        NCell nCell1 = e1.getHautNcell();
//                                        NCell nCell2 = e1.getBasNcell();
//
//                                        ECell e2 = (ECell) cell.getChildrenCells().get(1).getHydroCell();
//
//                                        NCell nCell3 = e2.getBasNcell();
//
//                                        NCell nCell4;
//
//
//
//
//
//                                        if (nCell1.getGID() == nCell3.getGID()) {
//                                                nCell4 = e2.getHautNcell();
//
//
//
//
//                                        } else if (nCell3.getGID() == nCell2.getGID()) {
//
//                                                nCell4 = nCell2;
//                                                nCell2 = nCell1;
//                                                nCell1 = nCell4;
//                                                nCell4 = e2.getHautNcell();
//
//
//
//
//                                        } else {
//
//                                                nCell4 = nCell3;
//                                                nCell3 = e2.getHautNcell();
//
//
//
//
//
//                                                if (nCell1.getGID() == nCell2.getGID()) {
//
//                                                        NCell nCell = nCell1;
//                                                        nCell1 = nCell2;
//                                                        nCell2 = nCell;
//
//
//
//
//                                                }
//
//                                        }
//
//                                        Coordinate geomNCell1 = sdsNodes.getGeometry(
//                                                nCell1.getGID() - 1).getCoordinates()[0];
//
//                                        Coordinate geomNCell2 = sdsNodes.getGeometry(
//                                                nCell2.getGID() - 1).getCoordinates()[0];
//
//                                        Coordinate geomNCell4 = sdsNodes.getGeometry(
//                                                nCell4.getGID() - 1).getCoordinates()[0];
//
//                                        Coordinate coordResult = MathUtil.getIntersection(geomNCell2, geomNCell4,
//                                                geomNCell1, cell.getSlope());
//
//                                        Geometry geom = null;
//
//
//
//
//                                        double area = 0;
//
//
//
//
//                                        if (coordResult != null) {
//                                                geom = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
//                                                                geomNCell1, geomNCell2, coordResult,
//                                                                geomNCell1}), null);
//                                                area = geom.getArea();
//
//
//
//
//                                        }
//
//                                        double contribution = 1;
//
//
//
//
//                                        double polygonArea = polygon.getArea();
//
//                                        contribution = area / polygonArea;
//
//
//
//
//
//                                        if (contribution > 1) {
//                                                contribution = 1;
//
//
//
//
//                                        }
//                                        cell.getChildrenCells().get(0).setContribution(
//                                                (float) contribution);
//
//                                        cell.getChildrenCells().get(1).setContribution(
//                                                1 - (float) contribution);
//
//
//
//
//
//                                } else if (cell.getChildrenCells().size() == 1) {
//
//                                        cell.getChildrenCells().getFirst().setContribution(1.0f);
//
//
//
//
//
//                                }
//
//                        }
//
//                        for (NCell cell : ncells) {
//
//                                int nb = cell.getChildrenCells().size();
//
//
//
//
//
//                                for (HydroCellValued fcell : cell.getChildrenCells()) {
//                                        fcell.setContribution(1.f / nb);
//
//
//
//
//                                }
//                        }
//
//                        sdsFaces.close();
//                        sdsNodes.close();
//
//
//
//
//
//                } catch (DriverException e) {
//                        e.printStackTrace();
//
//
//
//
//                }
//
//                normalizeProportion();
//
//
//
//
//
//        }
//
//        private void normalizeProportion() {
//
//                for (TCell cell : tcells) {
//
//                        float sumContribution = 0;
//
//
//
//
//                        for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                float contribution = fcell.getContribution();
//
//                                sumContribution = sumContribution + contribution;
//
//
//
//
//                        }
//
//                        for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                float contribution = fcell.getContribution() / sumContribution;
//
//                                fcell.setContribution(contribution);
//
//
//
//
//
//                        }
//
//                }
//
//                for (NCell cell : ncells) {
//
//                        float sumContribution = 0;
//
//
//
//
//                        for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                float contribution = fcell.getContribution();
//
//
//
//
//
//                                if (fcell.isTcell()) {
//                                        sumContribution = sumContribution + contribution;
//
//
//
//
//                                }
//
//                        }
//
//                        if (sumContribution > 0) {
//                                for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                        if (fcell.isTcell()) {
//                                                float contribution = fcell.getContribution()
//                                                        / sumContribution;
//                                                fcell.setContribution(contribution);
//
//
//
//
//                                        } else {
//                                                fcell.setContribution(0);
//
//
//
//
//                                        }
//
//                                }
//
//                        } else {
//
//                                for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                        float contribution = fcell.getContribution();
//                                        sumContribution = sumContribution + contribution;
//
//
//
//
//
//                                }
//
//                                for (HydroCellValued fcell : cell.getChildrenCells()) {
//                                        float contribution = fcell.getContribution()
//                                                / sumContribution;
//                                        fcell.setContribution(contribution);
//
//
//
//
//                                }
//
//                        }
//
//                }
//
//                for (ECell cell : ecells) {
//
//                        float sumContribution = 0;
//
//
//
//
//                        for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                float contribution = fcell.getContribution();
//
//
//
//
//
//                                if (fcell.isTcell()) {
//                                        sumContribution = sumContribution + contribution;
//
//
//
//
//                                }
//
//                        }
//
//                        if (sumContribution > 0) {
//                                for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                        if (fcell.isTcell()) {
//                                                float contribution = fcell.getContribution()
//                                                        / sumContribution;
//                                                fcell.setContribution(contribution);
//
//
//
//
//                                        } else {
//                                                fcell.setContribution(0);
//
//
//
//
//                                        }
//
//                                }
//
//                        } else {
//
//                                for (HydroCellValued fcell : cell.getChildrenCells()) {
//
//                                        float contribution = fcell.getContribution();
//                                        sumContribution = sumContribution + contribution;
//
//
//
//
//
//                                }
//
//                                for (HydroCellValued fcell : cell.getChildrenCells()) {
//                                        float contribution = fcell.getContribution()
//                                                / sumContribution;
//                                        fcell.setContribution(contribution);
//
//
//
//
//                                }
//
//                        }
//
//                }
//
//        }
//
//        private void ditcheIntegration() {
//
//                for (ECell cell : ecells) {
//
//                        if (cell.getType().equalsIgnoreCase("ditche")) {
//
//                                cell.setTalweg(true);
//                                cell.setTransfluent(false);
//
//                                ListIterator<HydroCellValued> iter = cell.getChildrenCells().listIterator();
//
//
//
//
//
//                                while (iter.hasNext()) {
//
//                                        HydroCellValued fCell = iter.next();
//
//
//
//
//
//                                        if (fCell.isTcell()) {
//                                                iter.remove();
//                                                fCell.getHydroCell().getParent().remove(cell);
//
//
//
//
//
//                                        }
//
//                                }
//
//                                cell.setChildren(cell.getBasNcell());
//                                cell.getBasNcell().setParent(cell);
//
//
//
//
//
//                        }
//
//                }
//
//                for (NCell cell : ncells) {
//
//                        boolean isConnectWithADitche = false;
//
//
//
//
//                        for (HydroCellValued fCell : cell.getChildrenCells()) {
//
//                                if (fCell.isEcell()) {
//
//                                        if (fCell.getHydroCell().getType().equals("ditche")) {
//
//                                                isConnectWithADitche = true;
//
//
//
//
//
//                                        }
//
//                                }
//
//                        }
//                        if (isConnectWithADitche) {
//
//                                ListIterator<HydroCellValued> iter = cell.getChildrenCells().listIterator();
//
//
//
//
//
//                                while (iter.hasNext()) {
//
//                                        HydroCellValued fCell = iter.next();
//
//
//
//
//
//                                        if (fCell.isEcell()) {
//
//                                                if (!fCell.getHydroCell().getType().equals("ditche")) {
//                                                        iter.remove();
//                                                        fCell.getHydroCell().getParent().remove(cell);
//
//
//
//
//                                                }
//                                        } else {
//                                                iter.remove();
//                                                fCell.getHydroCell().getParent().remove(cell);
//
//
//
//
//                                        }
//                                }
//
//                        }
//
//                }
//
//        }
//
//        public boolean isTalwegNodeOutlet(NCell nCell) {
//
//                boolean answer = false;
//
//
//
//
//
//                for (HydroCellValued nCellFils : nCell.getChildrenCells()) {
//
//                        if (nCellFils.isEcell()) {
//                        } else {
//                                answer = true;
//
//
//
//
//                        }
//
//                }
//
//                return answer;
//
//
//
//
//
//        }
//
//        public ArrayList<TCell> getTcells() {
//                return tcells;
//
//
//
//
//        }
//
//        public ArrayList<ECell> getEcells() {
//                return ecells;
//
//
//
//
//        }
//
//        public ArrayList<NCell> getNcells() {
//                return ncells;
//
//
//        }
//}
