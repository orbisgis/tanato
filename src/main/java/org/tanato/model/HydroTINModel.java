package org.tanato.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.tanato.utilities.MathUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.jhydrocell.hydronetwork.HydroProperties;

public class HydroTINModel {

	ArrayList<TCell> tcells;
	ArrayList<ECell> ecells;
	ArrayList<NCell> ncells;

	private SpatialDataSourceDecorator sdsEdges;
	private SpatialDataSourceDecorator sdsFaces;
	private SpatialDataSourceDecorator sdsNodes;
	private long sdsEdgesCount;
	private long sdsFacesCount;
	private long sdsNodesCount;

	private static final String ID = "gid";

	private static final String RIGHT_FACE = "right_t";

	private static final String LEFT_FACE = "left_t";

	private static final String START_NODE = "start_n";

	private static final String END_NODE = "end_n";
	private static final String PROPERTY_FIELD = "property";
        private static final String HEIGHT = "height";
        private static final String GID_SOURCE = "gid_source";

	GeometryFactory gf = new GeometryFactory();
	private boolean ditche = false;

	/**
	 * La datasource d'entrée correspond au TIN. Celui-ci est composé de trois
	 * datasources noeud, arc et triangle. Un champ nommé type est ajouté pour
	 * qualifier les objets du TIN. Obstacle, collecteur... des triangles :
	 * rural, urbain des noeuds : connecteur
	 * 
	 * Une seconde qualification est réalisée automatiquement en construisant le
	 * graph. Le graph est ordonné et valué.
	 * 
	 * @param ds
	 */

	public HydroTINModel(SpatialDataSourceDecorator sdsFaces,
			SpatialDataSourceDecorator sdsEdges,
			SpatialDataSourceDecorator sdsNodes) throws DriverException {
		this.sdsFaces = sdsFaces;
		this.sdsEdges = sdsEdges;
		this.sdsNodes = sdsNodes;

		createHydroCells();
		buildTINGraph();
		linkTalweg();
		buildProportion();
	}

	// Etape 1 initialisation des structures de données pour le stockage
	// non-redondant des hydrocells du graphe
	public final void createHydroCells() throws DriverException{
		tcells = new ArrayList<TCell>();
		ecells = new ArrayList<ECell>();
		ncells = new ArrayList<NCell>();

                sdsFaces.open();
                sdsEdges.open();
                sdsNodes.open();

                sdsEdgesCount = sdsEdges.getRowCount();

                sdsFacesCount = sdsFaces.getRowCount();

                sdsNodesCount = sdsNodes.getRowCount();

                int gidField = sdsEdges.getFieldIndexByName(ID);
                int rightFace = sdsEdges.getFieldIndexByName(RIGHT_FACE);
                int leftFace = sdsEdges.getFieldIndexByName(LEFT_FACE);
                int propertyIndex = sdsEdges.getFieldIndexByName(PROPERTY_FIELD);
                int startNodeFieldIndex = sdsEdges.getFieldIndexByName(START_NODE);
                int endNodeFieldIndex = sdsEdges.getFieldIndexByName(END_NODE);
                int heightIndex = sdsEdges.getFieldIndexByName(HEIGHT);
                int gidSourceIndex = sdsEdges.getFieldIndexByName(GID_SOURCE);
                // Création des structures de données vides
                ECell ec;
                for (int i = 0; i < sdsEdgesCount; i++) {
                        //We fill the list with the desired values.
                        ec = new ECell();
                        ec.setGID(sdsEdges.getInt(i, gidField));
                        ec.setHeight(sdsEdges.getDouble(i, heightIndex));
                        ec.setProperty(sdsEdges.getInt(i, propertyIndex));
                        ec.setGidSource(sdsEdges.getInt(i, gidSourceIndex));
                        ec.setLeftGID(sdsEdges.getInt(i, leftFace));
                        ec.setRightGID(sdsEdges.getInt(i, rightFace));
                        ec.setStartNodeGID(sdsEdges.getInt(i, startNodeFieldIndex));
                        ec.setEndNodeGID(sdsEdges.getInt(i, endNodeFieldIndex));
                        ecells.add(ec);
                }

                sdsEdges.close();

                gidField = sdsFaces.getFieldIndexByName(ID);
                propertyIndex = sdsFaces.getFieldIndexByName(PROPERTY_FIELD);
                heightIndex = sdsFaces.getFieldIndexByName(HEIGHT);
                gidSourceIndex = sdsFaces.getFieldIndexByName(GID_SOURCE);
                TCell tc;
                for (int i = 0; i < sdsFacesCount; i++) {
                        tc = new TCell();
                        tc.setGID(sdsFaces.getInt(i, gidField));
                        tc.setHeight(sdsFaces.getDouble(i, heightIndex));
                        tc.setProperty(sdsFaces.getInt(i, propertyIndex));
                        tc.setGidSource(sdsFaces.getInt(i, gidSourceIndex));

                        tcells.add(tc);
                }
                sdsFaces.close();

                gidField = sdsNodes.getFieldIndexByName(ID);
                propertyIndex = sdsNodes.getFieldIndexByName(PROPERTY_FIELD);
                heightIndex = sdsNodes.getFieldIndexByName(HEIGHT);
                gidSourceIndex = sdsNodes.getFieldIndexByName(GID_SOURCE);
                NCell nc;
                for (int i = 0; i < sdsNodesCount; i++) {
                        nc = new NCell();
                        nc.setGID(sdsFaces.getInt(i, gidField));
                        nc.setHeight(sdsFaces.getDouble(i, heightIndex));
                        nc.setProperty(sdsFaces.getInt(i, propertyIndex));
                        nc.setGidSource(sdsFaces.getInt(i, gidSourceIndex));

                        ncells.add(nc);
                }
                sdsNodes.close();



	}

	/**
	 * Construction du graphe à partir des 3 datasources nodes, edges et faces.
	 * Note : Nous considerons que les identifiants (GID) du graphe commemcent
	 * tous à 1.
	 * 
	 * 
	 */
	public final void buildTINGraph() {

		try {
			sdsFaces.open();
			sdsEdges.open();
			sdsNodes.open();

			int gidField = sdsEdges.getFieldIndexByName(ID);
			int rightFace = sdsEdges.getFieldIndexByName(RIGHT_FACE);
			int leftFace = sdsEdges.getFieldIndexByName(LEFT_FACE);
			int fieldTypeIndex = sdsEdges.getFieldIndexByName(PROPERTY_FIELD);
			int startNodeFieldIndex = sdsEdges.getFieldIndexByName(START_NODE);
			int endNodeFieldIndex = sdsEdges.getFieldIndexByName(END_NODE);

			// Triangle à droite
			TCell dTCell = null;
			// Triangle à gauche
			TCell gTCell = null;

			// Noeud haut
			NCell hautNCell = null;
			// Noeud bas
			NCell basNCell = null;

			for (int i = 0; i < sdsEdgesCount; i++) {

				// Les identifiants des gid noeuds start et end
				int gidStartNode = sdsEdges.getFieldValue(i,
						startNodeFieldIndex).getAsInt();
				int gidEndNode = sdsEdges.getFieldValue(i, endNodeFieldIndex)
						.getAsInt();

				// Les identifiants des triangles à droite et à gauche
				int gidTcellRight = sdsEdges.getFieldValue(i, rightFace)
						.getAsInt();
				int gidTcellLeft = sdsEdges.getFieldValue(i, leftFace)
						.getAsInt();

				int edgeGID = sdsEdges.getFieldValue(i, gidField).getAsInt();

				
				String edgeType = sdsEdges.getFieldValue(i, fieldTypeIndex)
						.getAsString();

				if (edgeType == null) {
					edgeType = "";
				}

				// Les parametres topographiques
				double edgeSlopeDeg = sdsEdges.getFieldValue(i,
						sdsEdges.getFieldIndexByName("slopedeg")).getAsDouble();

				Coordinate edgeSlope = sdsEdges.getFieldValue(i,
						sdsEdges.getFieldIndexByName("slope")).getAsGeometry()
						.getCoordinate();

				int edgeTopoType = sdsEdges.getFieldValue(i,
						sdsEdges.getFieldIndexByName("topo")).getAsInt();

				// Recupere la geom de l'edge

				// Geometry geom = sdsEdges.getGeometry(i);
				// LineString edgeGeom = (LineString) geom.getGeometryN(0);

				ECell eCell = ecells.get(i);

				// Ajout des parametres topo
				eCell.setSlopeInDegree(edgeSlopeDeg);
				eCell.setSlope(edgeSlope);

				// Ajout des topologies sur eCell
				eCell.setGID(edgeGID);
				eCell.setRightGID(gidTcellRight);
				eCell.setLeftGID(gidTcellLeft);
				eCell.setStartNodeGID(gidStartNode);
				eCell.setEndNodeGID(gidEndNode);

				// Geometry des noeuds de debut et de fin
				Point geomStartNode = (Point) sdsNodes.getGeometry(
						gidStartNode - 1).getGeometryN(0);
				Point geomEndNode = (Point) sdsNodes
						.getGeometry(gidEndNode - 1).getGeometryN(0);

				// Affectation des TCells

				// On test si le triangle à droite existe
				if (gidTcellRight != -1) {

					int indexTcellRight = gidTcellRight - 1;

					Geometry geom = sdsFaces.getGeometry(indexTcellRight);

					// Récupération des parametres topographiques
					double faceSlopeDeg = sdsFaces.getFieldValue(
							indexTcellRight,
							sdsFaces.getFieldIndexByName("slopedeg"))
							.getAsDouble();

					Coordinate faceSlope = sdsFaces.getFieldValue(
							indexTcellRight,
							sdsFaces.getFieldIndexByName("slope"))
							.getAsGeometry().getCoordinate();

					dTCell = tcells.get(gidTcellRight - 1);
					dTCell.setGID(gidTcellRight);
					dTCell.setSlopeInDegree(faceSlopeDeg);
					dTCell.setSlope(faceSlope);
					dTCell.setArea(geom.getArea());

				}
				// On test si le triangle à gauche existe
				if (gidTcellLeft != -1) {

					int indexTcellLeft = gidTcellLeft - 1;

					Geometry geom = sdsFaces.getGeometry(indexTcellLeft);

					// Récupération des parametres topographiques
					double faceSlopeDeg = sdsFaces.getFieldValue(
							indexTcellLeft,
							sdsFaces.getFieldIndexByName("slopedeg"))
							.getAsDouble();

					Coordinate faceSlope = sdsFaces.getFieldValue(
							indexTcellLeft,
							sdsFaces.getFieldIndexByName("slope"))
							.getAsGeometry().getCoordinate();

					gTCell = tcells.get(indexTcellLeft);
					gTCell.setGID(gidTcellLeft);
					gTCell.setSlopeInDegree(faceSlopeDeg);
					gTCell.setSlope(faceSlope);
					gTCell.setArea(geom.getArea());
				}

				boolean constraint = false;
				if (edgeType.equalsIgnoreCase(HydroProperties
						.toString(HydroProperties.DITCH))
						|| (edgeType.equalsIgnoreCase(HydroProperties
								.toString(HydroProperties.RIVER)))
						|| (edgeType.equalsIgnoreCase(HydroProperties
								.toString(HydroProperties.SEWER)))) {

					constraint = true;
					hautNCell = ncells.get(gidStartNode - 1);
					hautNCell.setGID(gidStartNode);
					basNCell = ncells.get(gidEndNode - 1);
					basNCell.setGID(gidEndNode);
					eCell.setTalweg(true);

				} else {
					// Hierarchisation des noeuds haut et bas en fonction de
					// leur Z.
					// 1 = down, -1 = up 0 = flat regarding to the topology
					int edgeGradient = 0;
					if (geomStartNode.getCoordinate().z > geomEndNode
							.getCoordinate().z) {

						hautNCell = ncells.get(gidStartNode - 1);
						hautNCell.setGID(gidStartNode);
						basNCell = ncells.get(gidEndNode - 1);
						basNCell.setGID(gidEndNode);
						edgeGradient = 1;

					}

					else if (geomStartNode.getCoordinate().z < geomEndNode
							.getCoordinate().z) {

						hautNCell = ncells.get(gidEndNode - 1);
						hautNCell.setGID(gidEndNode);
						basNCell = ncells.get(gidStartNode - 1);
						basNCell.setGID(gidStartNode);
						edgeGradient = -1;
					}

					else {
						edgeGradient = 0;
						hautNCell = ncells.get(gidStartNode - 1);
						hautNCell.setGID(gidStartNode);
						basNCell = ncells.get(gidEndNode - 1);
						basNCell.setGID(gidEndNode);
					}
				}

				eCell.setNodeHaut(hautNCell);
				eCell.setBasNcell(basNCell);

				// On construit le graphe à partir des qualifications topo

				/**
				 * Traitement des talwegs
				 */

				// Cas des talwegs simple
				if (edgeTopoType == HydroProperties.TALWEG) {
					eCell.setTalweg(true);
					basNCell.setTalweg(true);
					hautNCell.setTalweg(true);

					// Graph
					eCell.setParent(hautNCell);
					hautNCell.setChildren(eCell);
					basNCell.setParent(eCell);
					eCell.setChildren(basNCell);

					eCell.setParent(dTCell);
					eCell.setParent(gTCell);
					dTCell.setChildren(eCell);
					gTCell.setChildren(eCell);
				}

				// Talweg colineaire à droite
				else if (edgeTopoType == HydroProperties.RIGHTCOLINEAR) {

					eCell.setTalweg(true);
					basNCell.setTalweg(true);
					hautNCell.setTalweg(true);

					// Graph

					eCell.setParent(hautNCell);
					hautNCell.setChildren(eCell);
					basNCell.setParent(eCell);
					eCell.setChildren(basNCell);

					eCell.setParent(gTCell);
					gTCell.setChildren(eCell);

				}

				// Talweg colinéaire à gauche
				else if (edgeTopoType == HydroProperties.LEFTCOLINEAR) {

					eCell.setTalweg(true);
					basNCell.setTalweg(true);
					hautNCell.setTalweg(true);

					// Graph
					eCell.setParent(hautNCell);
					hautNCell.setChildren(eCell);
					basNCell.setParent(eCell);
					eCell.setChildren(basNCell);

					eCell.setParent(dTCell);
					dTCell.setChildren(eCell);

				}

				// Double talweg colineaire

				else if (edgeTopoType == HydroProperties.DOUBLECOLINEAR) {

					eCell.setTalweg(true);
					basNCell.setTalweg(true);
					hautNCell.setTalweg(true);

					// Graph
					eCell.setParent(hautNCell);
					hautNCell.setChildren(eCell);
					basNCell.setParent(eCell);
					eCell.setChildren(basNCell);

				}

				/**
				 * Traitement des ridges
				 */

				else if (edgeTopoType == HydroProperties.RIDGE) {

					if (constraint) {

						eCell.setTalweg(true);
						basNCell.setTalweg(true);
						hautNCell.setTalweg(true);

						// Graph
						eCell.setParent(hautNCell);
						hautNCell.setChildren(eCell);
						basNCell.setParent(eCell);
						eCell.setChildren(basNCell);

					} else {
						eCell.setRidge(true);

						// Graph
						eCell.setParent(hautNCell);
						hautNCell.setChildren(eCell);
						basNCell.setParent(eCell);
						eCell.setChildren(basNCell);

						if (dTCell.getSlopeInDegree() > gTCell
								.getSlopeInDegree()) {

							hautNCell.setChildren(dTCell);
							dTCell.setParent(hautNCell);
						} else if (dTCell.getSlopeInDegree() < gTCell
								.getSlopeInDegree()) {
							hautNCell.setChildren(gTCell);
							gTCell.setParent(hautNCell);
						}

						else {

						}
					}

				}

				/**
				 * Traitement des pentes à droite
				 */

				else if (edgeTopoType == HydroProperties.RIGHTSLOPE) {

					if (constraint) {

						eCell.setTalweg(true);
						basNCell.setTalweg(true);
						hautNCell.setTalweg(true);

						// Graph
						eCell.setParent(hautNCell);
						hautNCell.setChildren(eCell);
						basNCell.setParent(eCell);
						eCell.setChildren(basNCell);
						eCell.setParent(gTCell);
						gTCell.setChildren(eCell);

					} else {
						eCell.setTransfluent(true);

						hautNCell.setChildren(dTCell);
						dTCell.setParent(hautNCell);
						dTCell.setParent(eCell);
						eCell.setChildren(dTCell);
						eCell.setParent(gTCell);
						gTCell.setChildren(eCell);
					}
				}

				/**
				 * Traitement des pentes à gauche
				 */

				else if (edgeTopoType == HydroProperties.LEFTTSLOPE) {

					if (constraint) {

						eCell.setTalweg(true);
						basNCell.setTalweg(true);
						hautNCell.setTalweg(true);

						// Graph
						eCell.setParent(hautNCell);
						hautNCell.setChildren(eCell);
						basNCell.setParent(eCell);
						eCell.setChildren(basNCell);
						eCell.setParent(dTCell);
						dTCell.setChildren(eCell);

					} else {
						eCell.setTransfluent(true);

						// Graph

						hautNCell.setChildren(gTCell);
						gTCell.setParent(hautNCell);
						gTCell.setParent(eCell);
						eCell.setChildren(gTCell);
						eCell.setParent(dTCell);
						dTCell.setChildren(eCell);

					}
				}

				/**
				 * Traitement du rebord droit Attention l'edge n'est pas
				 * connecté au rebord
				 */
				/*
				 * 
				 * else if (edgeTopoType.equals(TopoType.RIGHTSIDE)) { // Graph
				 * 
				 * hautNCell.setFilsCells(dTCell);
				 * dTCell.setPeresCells(hautNCell); }
				 *//**
				 * Traitement du rebord gauche
				 */
				/*
				 * 
				 * else if (edgeTopoType.equals(TopoType.LEFTSIDE)) { // Graph
				 * hautNCell.setFilsCells(gTCell);
				 * gTCell.setPeresCells(hautNCell); }
				 */

				/**
				 * Traitement du fond droit
				 */

				else if (edgeTopoType == HydroProperties.RIGHTWELL) {

					if (constraint) {

						eCell.setTalweg(true);
						basNCell.setTalweg(true);
						hautNCell.setTalweg(true);

						// Graph
						eCell.setParent(hautNCell);
						hautNCell.setChildren(eCell);
						basNCell.setParent(eCell);
						eCell.setChildren(basNCell);
						eCell.setParent(dTCell);
						dTCell.setChildren(eCell);

					} else {
						eCell.setTransfluent(true);
						// Graph

						gTCell.setChildren(eCell);
						eCell.setParent(gTCell);
						eCell.setChildren(dTCell);
						dTCell.setParent(eCell);
					}
				}

				/**
				 * Traitement du fond gauche
				 */

				else if (edgeTopoType == HydroProperties.LEFTWELL) {

					if (constraint) {

						eCell.setTalweg(true);
						basNCell.setTalweg(true);
						hautNCell.setTalweg(true);

						// Graph
						eCell.setParent(hautNCell);
						hautNCell.setChildren(eCell);
						basNCell.setParent(eCell);
						eCell.setChildren(basNCell);
						eCell.setParent(dTCell);
						dTCell.setChildren(eCell);

					} else {
						eCell.setTransfluent(true);

						// Graph
						dTCell.setChildren(eCell);
						eCell.setParent(dTCell);
						eCell.setChildren(dTCell);
						dTCell.setParent(eCell);

					}
				}

			}

			sdsEdges.close();
			sdsFaces.close();
			sdsNodes.close();

		} catch (DriverException e) {
			e.printStackTrace();
		}

	}

	public final void linkTalweg() {

		// Traitement des lignes de crêtes
		for (ECell ecell : ecells) {

			if (ecell.isRidge()) {

				NCell hautNCell = ecell.getHautNcell();

				boolean existFilsTalweg = false;

				for (HydroCellValued hydrocell : ecell.getChildrenCells()) {

					if (hydrocell.getHydroCell().isTalweg()) {
						existFilsTalweg = true;
					}
				}

				if (!existFilsTalweg) {

					for (HydroCellValued hydrocell : ecell.getChildrenCells()) {

						if (hydrocell.getHydroCell() instanceof TCell) {
							TCell tCell = (TCell) hydrocell.getHydroCell();
							hautNCell.setChildren(tCell);
							tCell.setParent(hautNCell);

						}
					}
				}
			}

		}

		// Pour rechercher le triangle talweg d'un noeud nous parcourons
		// l'ensemble des fils des triangles.

		for (NCell nCell : ncells) {

			if (nCell.isTalweg()) {

				LinkedList<HydroCell> cellsList = new LinkedList<HydroCell>();

				for (HydroCellValued fCell : nCell.getChildrenCells()) {

					if (fCell.getHydroCell() instanceof TCell) {

						if (!fCell.getHydroCell().isTalweg()) {
							cellsList.add(fCell.getHydroCell());
						}

					}
				}

				ListIterator<HydroCell> iter = cellsList.listIterator();

				while (iter.hasNext()) {

					boolean connected = false;

					HydroCell cell = iter.next();

					for (HydroCellValued hydroCell2 : cell.getChildrenCells()) {

						if (hydroCell2.getHydroCell() instanceof ECell) {
							ECell eFMaCell = (ECell) hydroCell2.getHydroCell();

							// Regarde si l'edge est connecté au noeud
							if (eFMaCell.getHautNcell().getGID() == nCell
									.getGID()) {
								connected = true;

							}

							else if (eFMaCell.getBasNcell().getGID() == nCell
									.getGID()) {
								connected = true;
							}
						}
					}
					if (connected) {
						iter.remove();
					}
				}
				for (HydroCell hydroCell : cellsList) {

					hydroCell.setTalweg(true);

				}
			}

		}

		// Assure la continuité des noeuds entre talweg. Un noeud talweg se
		// deverse prioritairement dans un edge talweg.
		for (NCell cell : ncells) {

			if (cell.isTalweg()) {

				boolean talweg = false;

				for (HydroCellValued fcell : cell.getChildrenCells()) {

					if (fcell.getHydroCell().isTalweg()) {
						talweg = true;

					}

				}

				if (talweg) {

					ListIterator<HydroCellValued> iter = cell.getChildrenCells()
							.listIterator();

					while (iter.hasNext()) {

						HydroCellValued hydroCellValued = iter.next();
						HydroCell fCell = hydroCellValued.getHydroCell();

						if (!fCell.isTalweg()) {
							fCell.removeParent(cell);
							iter.remove();
						}

					}
				}

			}

		}

	}

	private void buildProportion() {

		try {

			sdsFaces.open();
			sdsNodes.open();
			for (TCell cell : tcells) {

				if (cell.getChildrenCells().size() == 2) {

					Polygon polygon = (Polygon) sdsFaces.getGeometry(
							cell.getGID() - 1).getGeometryN(0);

					ECell e1 = (ECell) cell.getChildrenCells().get(0)
							.getHydroCell();

					NCell nCell1 = e1.getHautNcell();
					NCell nCell2 = e1.getBasNcell();

					ECell e2 = (ECell) cell.getChildrenCells().get(1)
							.getHydroCell();

					NCell nCell3 = e2.getBasNcell();

					NCell nCell4;

					if (nCell1.getGID() == nCell3.getGID()) {
						nCell4 = e2.getHautNcell();
					} else if (nCell3.getGID() == nCell2.getGID()) {

						nCell4 = nCell2;
						nCell2 = nCell1;
						nCell1 = nCell4;
						nCell4 = e2.getHautNcell();
					} else {

						nCell4 = nCell3;
						nCell3 = e2.getHautNcell();

						if (nCell1.getGID() == nCell2.getGID()) {

							NCell nCell = nCell1;
							nCell1 = nCell2;
							nCell2 = nCell;
						}

					}

					Coordinate geomNCell1 = sdsNodes.getGeometry(
							nCell1.getGID() - 1).getCoordinates()[0];

					Coordinate geomNCell2 = sdsNodes.getGeometry(
							nCell2.getGID() - 1).getCoordinates()[0];

					Coordinate geomNCell4 = sdsNodes.getGeometry(
							nCell4.getGID() - 1).getCoordinates()[0];

					Coordinate coordResult = MathUtil
							.getIntersection(geomNCell2, geomNCell4,
									geomNCell1, cell.getSlope());

					Geometry geom = null;
					double area = 0;
					if (coordResult != null) {
						geom = gf.createPolygon(gf
								.createLinearRing(new Coordinate[] {
										geomNCell1, geomNCell2, coordResult,
										geomNCell1 }), null);
						area = geom.getArea();
					}

					double contribution = 1;
					double polygonArea = polygon.getArea();

					contribution = area / polygonArea;

					if (contribution > 1) {
						contribution = 1;
					}
					cell.getChildrenCells().get(0).setContribution(
							(float) contribution);

					cell.getChildrenCells().get(1).setContribution(
							1 - (float) contribution);

				}

				else if (cell.getChildrenCells().size() == 1) {

					cell.getChildrenCells().getFirst().setContribution(1.0f);

				}

			}

			for (NCell cell : ncells) {

				int nb = cell.getChildrenCells().size();

				for (HydroCellValued fcell : cell.getChildrenCells()) {
					fcell.setContribution(1.f / nb);
				}
			}

			sdsFaces.close();
			sdsNodes.close();

		} catch (DriverException e) {
			e.printStackTrace();
		}

		normalizeProportion();

	}

	private void normalizeProportion() {

		for (TCell cell : tcells) {

			float sumContribution = 0;
			for (HydroCellValued fcell : cell.getChildrenCells()) {

				float contribution = fcell.getContribution();

				sumContribution = sumContribution + contribution;
			}

			for (HydroCellValued fcell : cell.getChildrenCells()) {

				float contribution = fcell.getContribution() / sumContribution;

				fcell.setContribution(contribution);

			}

		}

		for (NCell cell : ncells) {

			float sumContribution = 0;
			for (HydroCellValued fcell : cell.getChildrenCells()) {

				float contribution = fcell.getContribution();

				if (fcell.isTcell()) {
					sumContribution = sumContribution + contribution;
				}

			}

			if (sumContribution > 0) {
				for (HydroCellValued fcell : cell.getChildrenCells()) {

					if (fcell.isTcell()) {
						float contribution = fcell.getContribution()
								/ sumContribution;
						fcell.setContribution(contribution);
					} else {
						fcell.setContribution(0);
					}

				}

			} else {

				for (HydroCellValued fcell : cell.getChildrenCells()) {

					float contribution = fcell.getContribution();
					sumContribution = sumContribution + contribution;

				}

				for (HydroCellValued fcell : cell.getChildrenCells()) {
					float contribution = fcell.getContribution()
							/ sumContribution;
					fcell.setContribution(contribution);
				}

			}

		}

		for (ECell cell : ecells) {

			float sumContribution = 0;
			for (HydroCellValued fcell : cell.getChildrenCells()) {

				float contribution = fcell.getContribution();

				if (fcell.isTcell()) {
					sumContribution = sumContribution + contribution;
				}

			}

			if (sumContribution > 0) {
				for (HydroCellValued fcell : cell.getChildrenCells()) {

					if (fcell.isTcell()) {
						float contribution = fcell.getContribution()
								/ sumContribution;
						fcell.setContribution(contribution);
					} else {
						fcell.setContribution(0);
					}

				}

			} else {

				for (HydroCellValued fcell : cell.getChildrenCells()) {

					float contribution = fcell.getContribution();
					sumContribution = sumContribution + contribution;

				}

				for (HydroCellValued fcell : cell.getChildrenCells()) {
					float contribution = fcell.getContribution()
							/ sumContribution;
					fcell.setContribution(contribution);
				}

			}

		}

	}

	private void ditcheIntegration() {

		for (ECell cell : ecells) {

			if (cell.getType().equalsIgnoreCase("ditche")) {

				cell.setTalweg(true);
				cell.setTransfluent(false);

				ListIterator<HydroCellValued> iter = cell.getChildrenCells()
						.listIterator();

				while (iter.hasNext()) {

					HydroCellValued fCell = iter.next();

					if (fCell.isTcell()) {
						iter.remove();
						fCell.getHydroCell().getParent().remove(cell);

					}

				}

				cell.setChildren(cell.getBasNcell());
				cell.getBasNcell().setParent(cell);

			}

		}

		for (NCell cell : ncells) {

			boolean isConnectWithADitche = false;
			for (HydroCellValued fCell : cell.getChildrenCells()) {

				if (fCell.isEcell()) {

					if (fCell.getHydroCell().getType().equals("ditche")) {

						isConnectWithADitche = true;

					}

				}

			}
			if (isConnectWithADitche) {

				ListIterator<HydroCellValued> iter = cell.getChildrenCells()
						.listIterator();

				while (iter.hasNext()) {

					HydroCellValued fCell = iter.next();

					if (fCell.isEcell()) {

						if (!fCell.getHydroCell().getType().equals("ditche")) {
							iter.remove();
							fCell.getHydroCell().getParent().remove(cell);
						}
					}

					else {
						iter.remove();
						fCell.getHydroCell().getParent().remove(cell);
					}
				}

			}

		}

	}

	public boolean isTalwegNodeOutlet(NCell nCell) {

		boolean answer = false;

		for (HydroCellValued nCellFils : nCell.getChildrenCells()) {

			if (nCellFils.isEcell()) {

			} else {
				answer = true;
			}

		}

		return answer;

	}

	public ArrayList<TCell> getTcells() {
		return tcells;
	}

	public ArrayList<ECell> getEcells() {
		return ecells;
	}

	public ArrayList<NCell> getNcells() {
		return ncells;
	}

}
