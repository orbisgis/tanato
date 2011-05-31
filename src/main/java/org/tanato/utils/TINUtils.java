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
 * Copyright (C) 2011 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
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
package org.tanato.utils;

import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;

/**
 *
 * @author ebocher
 */
public class TINUtils {

        /**
         * Computes the contribution of a triangle .
         * The constribution will be between 0 or 1.
         * @param dTriangle
         * @return
         * @throws DelaunayError
         */
        public static float getProportion(DTriangle dTriangle, DEdge dEdge) throws DelaunayError {
                double contribution = 0;


                if (!dTriangle.isFlatSlope()) {
                        DEdge edge2 = dTriangle.getOppositeEdge(dEdge.getEndPoint());
                        DEdge edge3 = dTriangle.getOppositeEdge(dEdge.getStartPoint());

                        boolean goToEdge1 = dTriangle.isTopoOrientedToEdge(dEdge);
                        boolean goToEdge2 = dTriangle.isTopoOrientedToEdge(edge2);
                        boolean goToEdge3 = dTriangle.isTopoOrientedToEdge(edge3);

                        DPoint pt;
                        DPoint ptRidge;
                        DTriangle triangle;
                        double area = 0;
                        if (goToEdge1) {
                                if (goToEdge2) {
                                        // the ridge crosses edge1 and edge2
                                        pt = dTriangle.getOppositePoint(edge3);
                                        ptRidge = dTriangle.getCounterSteepestIntersection(pt);
                                        triangle = new DTriangle(dEdge.getStartPoint(), dEdge.getEndPoint(), ptRidge);
                                        area = triangle.getArea();
                                        double polygonArea = dTriangle.getArea();
                                        contribution = area / polygonArea;
                                } else if (goToEdge3) {
                                        // the ridge crosses edge1 and edge3
                                        pt = dTriangle.getOppositePoint(edge2);
                                        ptRidge = dTriangle.getCounterSteepestIntersection(pt);
                                        triangle = new DTriangle(dEdge.getStartPoint(), dEdge.getEndPoint(), ptRidge);
                                        area = triangle.getArea();
                                        double polygonArea = dTriangle.getArea();
                                        contribution = area / polygonArea;
                                } else {
                                        contribution = 1;
                                }
                        }


                }


                if (contribution > 1) {
                        contribution = 1;
                }

                return (float) contribution;
        }

        /**
         * This method is used to compute the ridge line on a triangle
         * @param dTriangle
         * @return
         * @throws DelaunayError
         */
        public static DEdge getRidgeLine(DTriangle dTriangle) throws DelaunayError {
                DEdge[] edges = dTriangle.getEdges();

                DEdge edge1 = edges[0];
                DEdge edge2 = edges[1];
                DEdge edge3 = edges[2];

                boolean goToEdge1 = dTriangle.isTopoOrientedToEdge(edge1);
                boolean goToEdge2 = dTriangle.isTopoOrientedToEdge(edge2);
                boolean goToEdge3 = dTriangle.isTopoOrientedToEdge(edge3);
                DEdge ridgeLine = null;
                DPoint pt;
                DPoint ptRidge;
                if (goToEdge1) {
                        if (goToEdge2) {
                                // the ridge crosses edge1 and edge2
                                pt = dTriangle.getOppositePoint(edge3);
                                ptRidge = dTriangle.getCounterSteepestIntersection(pt);
                                ridgeLine = new DEdge(ptRidge, pt);
                        } else if (goToEdge3) {
                                // the ridge crosses edge1 and edge3
                                pt = dTriangle.getOppositePoint(edge2);
                                ptRidge = dTriangle.getCounterSteepestIntersection(pt);
                                ridgeLine = new DEdge(ptRidge, pt);
                        } else {
                                pt = dTriangle.getOppositePoint(edge1);
                                ridgeLine = new DEdge(edge1.getMiddle(), pt);
                        }
                } else if (goToEdge2) {
                        if (goToEdge3) {
                                // the ridge crosses edge2 and edge3
                                pt = dTriangle.getOppositePoint(edge1);
                                ptRidge = dTriangle.getCounterSteepestIntersection(pt);
                                ridgeLine = new DEdge(ptRidge, pt);
                        }
                }
                return ridgeLine;

        }
}
