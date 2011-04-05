package org.tanato.utilities;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Triangle;
import com.vividsolutions.jts.util.Assert;

public class HydroPolygonUtil {

        private double valeurPente = -1.0;
        private double orientationPente = -1.0;
        private Coordinate pente;
        private Polygon p;
        private Triangle triangle;
        private GeometryFactory gf = new GeometryFactory();

        public HydroPolygonUtil(Polygon p) {
                this.p = p;
                triangle = JTSUtilities.createClockwiseTriangle(p.getCoordinates()[0],
                        p.getCoordinates()[1], p.getCoordinates()[2]);

        }

        /**
         *
         * @return normal vector (that pointing to the sky --> z>0) of the face
         *
         */
        public final Coordinate getNormal() {

                Coordinate n = new Coordinate();
                // vecteurs directeurs du triangle
                Coordinate v1 = new Coordinate(triangle.p1.x - triangle.p0.x,
                        triangle.p1.y - triangle.p0.y, triangle.p1.z - triangle.p0.z);
                Coordinate v2 = new Coordinate(triangle.p2.x - triangle.p0.x,
                        triangle.p2.y - triangle.p0.y, triangle.p2.z - triangle.p0.z);

                // calcul de la normale par produit vectoriel
                n.x = v1.y * v2.z - v1.z * v2.y;
                n.y = v1.z * v2.x - v1.x * v2.z;
                n.z = v1.x * v2.y - v1.y * v2.x;
                // normage du vecteur
                double norme = Math.sqrt(n.x * n.x + n.y * n.y + n.z * n.z);
                n.x = n.x / norme;
                n.y = n.y / norme;
                n.z = n.z / norme;
                // on veut que la normale pointe vers le ciel
                if (n.z < 0) {
                        n.x = -n.x;
                        n.y = -n.y;
                        n.z = -n.z;
                }
                return n;
        }

        /**
         *
         * @return steepest path vector of the face also direction
         */
        public final Coordinate get3DVector() {
                if (pente == null) {
                        pente = new Coordinate(0, 0, 0);
                        // on recupere le vecteur normal
                        Coordinate n = getNormal();
                        // on en deduit le vecteur de ligne de niveau
                        // Coordinate l = new Coordinate(n.y , -n.x, 0);
                        // on ne traite pas les cas degeneres
                        if (n.x == 0 && n.y == 0) {
                                pente = new Coordinate(0, 0, 0);
                        } else {
                                if (n.x == 0) {
                                        pente = new Coordinate(0, 1, -n.y / n.z);
                                } else if (n.y == 0) {
                                        pente = new Coordinate(1, 0, -n.x / n.z);
                                } else {
                                        pente = new Coordinate(n.x / n.y, 1, -1 / n.z
                                                * (n.x * n.x / n.y + n.y));
                                }
                                // on choisit le sens descendant
                                if (pente.z > 0) {
                                        pente.x = -pente.x;
                                        pente.y = -pente.y;
                                        pente.z = -pente.z;
                                }
                                double norme = Math.sqrt(pente.x * pente.x + pente.y * pente.y
                                        + pente.z * pente.z);
                                // normage du vecteur
                                if (norme > 0) {
                                        pente.x = pente.x / norme;
                                        pente.y = pente.y / norme;
                                        pente.z = pente.z / norme;
                                }
                        }
                }
                return pente;
        }

        /**
         *
         * @return pente du vecteur de plus forte pente de la face (dz/distance
         *         horizontale)
         */
        public final double getSlope() {
                if (Math.abs(valeurPente + 1.0) < MathUtil.EPSILON) {
                        double d = 0.0;
                        Coordinate pc = get3DVector();
                        if (pc != null) {
                                // calcul de la distance horizontale
                                // separant les 2 extremites du vecteur pente
                                d = Math.sqrt(pc.x * pc.x + pc.y * pc.y);
                        }
                        valeurPente = d == 0.0 ? 0.0 : pc.z / d;
                }

                // debug obedel
                if (Math.abs(valeurPente) > 1.0) {
                        System.out.println("valeur de pente surprenante : "
                                + valeurPente);
//			Coordinate pt = get3DVector();

                }

                return valeurPente;
        }

        /**
         * Returns true if the slope of the triangle associated to this object is
         * oriented to the edge myEdge
         * @param myEdge
         * @return
         */
        public final boolean getPenteVersEdge(LineString lineSegment) {
                boolean res = false;

                // on determine les sommets A,B et C du triangle et on calle AB (ou BA)
                // sur e
                Coordinate a = lineSegment.getStartPoint().getCoordinate();
                Coordinate b = lineSegment.getEndPoint().getCoordinate();
                int i = 0;
                while (!(i == 4 || (!p.getCoordinates()[i].equals3D(a) && !p.getCoordinates()[i].equals3D(b)))) {
                        i++;
                }
                // Assert.isTrue(i!=4,"edge n'appartenant pas au triangle");
                if (i == 4) {
                        return res;
                }// e n'appartient pas au triangle

                Coordinate c = p.getCoordinates()[i];
                Coordinate ab = MathUtil.differenceVectoriel(b, a);
                Coordinate ac = MathUtil.differenceVectoriel(c, a);
                // orientation CCW
                if (MathUtil.vectorProduct(ab, ac).z < 0) {
                        // echange A et B
                        Coordinate d = a;
                        a = b;
                        b = d;
                        ab = MathUtil.differenceVectoriel(b, a);
                }
                // test d'intersection entre AB et P
                Coordinate pv = this.get3DVector();

                res = MathUtil.vectorProduct(ab, pv).z < 0;

                return res;
        }

        /**
         * Pente topographique exprimée en degrées
         *
         * @return
         */
        public final double getSlopeInDegree() {
                return Math.abs(getSlope()) * 100;
        }

        /**
         * Retrieve the centroid of the triangle.
         * @return
         */
        public final Coordinate getCentroid() {
                return new Coordinate(p.getInteriorPoint().getX(), p.getInteriorPoint().getY());
        }

        /**
         *
         * @return angle entre le nord et la direction de plus forte pente (sens
         *         descendant) de la face (en degres)
         */
        public final double getSlopeAzimut() {
                if (orientationPente == -1.) {
                        Coordinate c1 = new Coordinate(0.0, 0.0, 0.0);
                        Coordinate c2 = this.get3DVector();
                        if (c2.z > 0.0) {
                                c2.setCoordinate(new Coordinate(-c2.x, -c2.y, -c2.z));
                        }
                        // l'ordre des coordonnees correspond a l'orientation de l'arc
                        // "sommet haut vers sommet bas"
                        double angleAxeXRad = Angle.angle(c1, c2);
                        // on considere que l'axe nord correspond a l'axe Y positif
                        double angleAxeNordRad = Angle.PI_OVER_2 - angleAxeXRad;
                        double angleAxeNordDeg = Angle.toDegrees(angleAxeNordRad);
                        // on renvoie toujours une valeur d'angle >= 0
                        orientationPente = angleAxeNordDeg < 0.0 ? 360.0 + angleAxeNordDeg
                                : angleAxeNordDeg;
                }
                return orientationPente;
        }

        public final LineString getLigneSeparatrice() {

                CoordinateList line = new CoordinateList();

		Coordinate coordA = triangle.p0;
		Coordinate coordB = triangle.p1;
		Coordinate coordC = triangle.p2;

		LineString edgeAB = gf.createLineString(new Coordinate[]{coordA,
				coordB});
		LineString edgeBC = gf.createLineString(new Coordinate[]{coordB,
				coordC});
		LineString edgeAC = gf.createLineString(new Coordinate[]{coordC,
				coordA});

		boolean pVersAB = getPenteVersEdge(edgeAB);
		boolean pVersBC = getPenteVersEdge(edgeBC);
		boolean pVersAC = getPenteVersEdge(edgeAC);

		if (pVersAB) {
			if (pVersAC) {
				// ligne de separation coupe BC
				Coordinate iBC = getPenteEdgeIntersect(edgeBC, coordA);

				if (coordA.z > iBC.z) {
					line.add(coordA);
					line.add(iBC);
				} else if (coordA.z < iBC.z) {
					line.add(iBC);
					line.add(coordA);
				} else {
					line.add(coordA);
					line.add(iBC);
				}

			} else if (pVersBC) {
				// ligne de separation coupe AC
				Coordinate iAC = getPenteEdgeIntersect(edgeAC, coordB);
				if (coordB.z > iAC.z) {
					line.add(coordB);
					line.add(iAC);
				} else if (coordB.z < iAC.z) {
					line.add(iAC);
					line.add(coordB);
				} else {
					line.add(coordB);
					line.add(iAC);
				}

			}
		} else if (pVersBC && pVersAC) {
			// ligne de separation porte par AB
			Coordinate iAB = getPenteEdgeIntersect(edgeAB, coordC);

			if (coordC.z > iAB.z) {
				line.add(coordC);
				line.add(iAB);
			} else if (coordC.z < iAB.z) {
				line.add(iAB);
				line.add(coordC);
			} else {
				line.add(coordC);
				line.add(iAB);
			}
		}

                return gf.createLineString(line.toCoordinateArray());

        }

        public final Coordinate getSharedPoint() {

                Coordinate sharedPOint = null;
                Coordinate coordA = triangle.p0;
                Coordinate coordB = triangle.p1;
                Coordinate coordC = triangle.p2;

                LineString edgeAB = gf.createLineString(new Coordinate[]{coordA,
                                coordB});
                LineString edgeBC = gf.createLineString(new Coordinate[]{coordB,
                                coordC});
                LineString edgeAC = gf.createLineString(new Coordinate[]{coordC,
                                coordA});

                boolean pVersAB = getPenteVersEdge(edgeAB);
                boolean pVersBC = getPenteVersEdge(edgeBC);
                boolean pVersAC = getPenteVersEdge(edgeAC);

                if (pVersAB) {
                        if (pVersAC) {
                                sharedPOint = coordA;

                        } else if (pVersBC) {
                                sharedPOint = coordB;

                        }
                } else if (pVersBC && pVersAC) {
			sharedPOint = coordC;
                }

                return sharedPOint;

        }

        public final Coordinate getPenteEdgeIntersect(LineString e, Coordinate pt) {
                Coordinate i = null;

                Coordinate eVector = MathUtil.getVector(e.getStartPoint().getCoordinate(), e.getEndPoint().getCoordinate());

                // Coordinate AB = MathUtil.DifferenceVectoriel(B, A);
                Coordinate pv = this.get3DVector();
                i = MathUtil.calculIntersection(e.getStartPoint().getCoordinate(),
                        eVector, pt, pv);
                Assert.isTrue(i != null,
                        "Intersection detectee mais non verifiee par calcul");

                return i;
        }
}
