/* Ce package est inclus dans la distribution logicielle FlowTIN.
 * Il permet de charger et de travailler sur un réseau de triangles irréguliers (TIN), construit à partir
 * de l'application TopoTIN.
 *
 * Ce package a été développé par Olivier Bedel dans le cadre de la thèse de doctorat en géographie
 * d'Erwan Bocher, intitulée : Impacts des activités humaines sur le parcours des écoulements de surface dans un bassin versant bocager :
 * essai de modélisation spatiale.  Application au Bassin versant du Jaudy-Guindy-Bizien.
 *
 *  Le manuscrit est disponible en tééléchargement sur le site thèse en ligne du CNRS.
 *
 *
 * Cette thèse a bénéficié du soutien financier : de l'Union européenne, du Conseil Régional de Bretagne, de l'Agence de l'eau Loire-Bretagne,
 * du Conseil Général des Côtes-d'Armor et du Bassin versant du Jaudy-Guindy-Bizien[2] par l'intermédiaire du Syndicat intercommunal d'adduction d�eau du Tr�gor, ma�tre d�ouvrage.
 * Elle a été réalisée à l'Université de Rennes 2 Haute-Bretagne au Laboratoire RESO UMR ESO CNRS 6590 en partenariat avec le Laboratoire COSTEL UMR LETG 6554.
 *
 *
 * Created on 25 août 2004
 *
 * Olivier BEDEL et Erwan Bocher
 * Bassin Versant du Jaudy-Guindy-Bizien,
 * Laboratoire RESO UMR ESO 6590 CNRS, Université de Rennes 2
 *
 * Ce programme est distribué sous la licence logiciel libre du CNRS CeCILL.
 * Pour utiliser ce programme ou le modifier vous devez accepter pleinement et respecter les conditions de cette licence.
 *
 *
 * @revision 2010-10-04
 */

package org.tanato.utilities;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * utility class that contains basic mathematical methods.
 * We use coordinates here to represent vectors.
 * @author alexis, jean-yves
 */
public final class MathUtil {

	public static final double EPSILON = 1.0E-8;

	/**
	 * The default constructor must not be used on this class.
	 */
	private MathUtil(){

	}

	/**
	 * get the vector which results from the points v1 and v2
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static Coordinate getVector(Coordinate v1, Coordinate v2) {
		Coordinate v3 = new Coordinate(0, 0, 0);
		v3.x = v2.x - v1.x;
		v3.y = v2.y - v1.y;
		v3.z = v2.z - v1.z;

		return v3;
	}

	/**
	 * Compute the vector product from the vectors v1 and v2
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static Coordinate vectorProduct(Coordinate v1, Coordinate v2) {
		Coordinate v3 = new Coordinate(0, 0, 0);
		v3.x = v1.y * v2.z - v1.z * v2.y;
		v3.y = v1.z * v2.x - v1.x * v2.z;
		v3.z = v1.x * v2.y - v1.y * v2.x;
		return v3;
	}

	/**
	 * Checks that the vectors resulting from v1 and v2 are colinear.
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean isColinear(Coordinate v1, Coordinate v2) {
		double res = 0;
		res += Math.abs(v1.y * v2.z - v1.z * v2.y);
		res += Math.abs(v1.z * v2.x - v1.x * v2.z);
		res += Math.abs(v1.x * v2.y - v1.y * v2.x);
		return res < EPSILON;
	}

	/**
	 * Compute the difference between two vectors.
	 * @param v1
	 * @param v2
	 * @return v1 - v2
	 */
	public static Coordinate differenceVectoriel(Coordinate v1, Coordinate v2) {
		Coordinate v3 = new Coordinate(0, 0, 0);
		v3.x = v1.x - v2.x;
		v3.y = v1.y - v2.y;
		v3.z = v1.z - v2.z;
		return v3;
	}

	/**
	 * Compute the sum of two vectors.
	 * @param v1
	 * @param v2
	 * @return v1 + v2
	 */
	public static Coordinate sommeVectoriel(Coordinate v1, Coordinate v2) {
		Coordinate v3 = new Coordinate(0, 0, 0);
		v3.x = v1.x + v2.x;
		v3.y = v1.y + v2.y;
		v3.z = v1.z + v2.z;
		return v3;
	}

	/**
	 * Compute the scalar product of v1 and v2.
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double scalarProduct(Coordinate v1, Coordinate v2) {
		double p;
		p = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
		return p;
	}

	/**
	 * Multiply the vector using the constant.
	 * @param v1
	 * @param k
	 * @return k * v1
	 */
	public static Coordinate produit(Coordinate v1, double k) {
		Coordinate v3 = new Coordinate(0, 0, 0);
		v3.x = k * v1.x;
		v3.y = k * v1.y;
		v3.z = k * v1.z;
		return v3;
	}

	/**
	 * Compute the length of the vector defined by v.
	 * @param v
	 * @return
	 */
	public static double norme(Coordinate v) {
		return Double.isNaN(v.z) ? Math.sqrt(v.x * v.x + v.y * v.y) : Math
				.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
	}

	/**
	 * Compute the vector of length one that is colinear to v.
	 * @param v
	 * @return
	 */
	public static Coordinate normeVectoriel(Coordinate v) {
		double norme = norme(v);
		Coordinate vNorme = (Coordinate) v.clone();
		if (norme != 0.0) {
			vNorme = new Coordinate(v.x / norme, v.y / norme, v.z / norme);
		}
		return vNorme;
	}

	/**
	 * Calcule le point d'intersection de 2 droites coplanaires
	 *
	 * @param p1
	 *            point de la premiere droite
	 * @param v1
	 *            vecteur directeur de la premiere droite
	 * @param p2
	 *            point de la seconde droite
	 * @param v2
	 *            vecteur directeur de la seconde droite
	 * @return coordonnees du point d'intersection des 2 droites ou null si les
	 *         droites sont parrallèles
	 */

	public static Coordinate calculIntersection(Coordinate p1, Coordinate v1,
			Coordinate p2, Coordinate v2) {
		double delta;
		double k;
		Coordinate i = null;
		// methode de Cramer pour determiner l'intersection de 2 droites du plan
		delta = v1.x * (-v2.y) - (-v1.y) * v2.x;
		if (delta != 0) {
			k = ((p2.x - p1.x) * (-v2.y) - (p2.y - p1.y) * (-v2.x)) / delta;
			i = new Coordinate();
			i.x = p1.x + k * v1.x;
			i.y = p1.y + k * v1.y;
			i.z = p1.z + k * v1.z;
		}
		return i;
	}

	/**
	 * Approximate the double v with 10^-p precision.
	 *
	 * @param v
	 *            double to approximate
	 * @param p
	 *            precision
	 * @return an approximation of v.
	 */

	public static double roundDouble(double v, int p) {
		double vRounded;
		double m = 1.0;
		for (int i = 0; i < p; i++) {
			m = m * 10.0;
		}
		vRounded = Math.rint(v * m) / m;

		return vRounded;
	}

	/**
	 * Compute the intersection between [p1 p2] and [p3 v]
	 * @param p1
	 * @param p2
	 * @param p3
	 * @param v
	 * @return
	 */
	 public static Coordinate getIntersection(Coordinate p1, Coordinate p2, Coordinate p3, Coordinate v) {
	        Coordinate intersection = null;

	        // (x2 - x1) t1 - vx t2 = (x3 - x1)
	        // (y2 - y1) t1 - vIy t2 = (y3 - y1)

	        double a1 = p2.x - p1.x;
	        double b1 = v.x;
	        double c1 = p3.x - p1.x;
	        double a2 = p2.y - p1.y;
	        double b2 = v.y;
	        double c2 = p3.y - p1.y;

	        // d = vx (y2 - y1) - (x2 - x1) * vy
	        double d = b1 * a2 - b2 * a1;
	        if (d != 0) {
	            // t1 = ((y3 - y1) vx - (x3 - x1) vy) / d
	            // t2 = ((x2 - x1) (y3 - y1) - (y2 - y1) (x3 - x1)) / d

	            double t1 = (c2 * b1 - c1 * b2) / d;
//	            double t2 = (a1 * c2 - a2 * c1) / d;

	            if ((-EPSILON <= t1) && (t1 <= 1 + EPSILON)) {
	                // it intersects
	                if (t1 <= EPSILON) {
					intersection = p1;
				}
	                else if (t1 >= 1 - EPSILON) {
					intersection = p2;
				}
	                else {
	                    // x = x2 t1 + (1 - t1) x1
	                    // y = y2 t1 + (1 - t1) y1
	                    // z = z2 t1 + (1 - t1) z1
	                    double x = p2.x * t1 + (1 - t1) * p1.x;
	                    double y = p2.y * t1 + (1 - t1) * p1.y;
	                    double z = p2.z * t1 + (1 - t1) * p1.z;

	                    intersection = new Coordinate(x, y, z);

	                    // Last verification
	                    if (p1.distance(intersection) < EPSILON) {
						intersection = p1;
					}
	                    else if (p2.distance(intersection) < EPSILON) {
						intersection = p2;
					}
	                    else if (p3.distance(intersection) < EPSILON) {
						intersection = p3;
					}
	                }
	            }
	        }

	        return intersection;
	    }


}
