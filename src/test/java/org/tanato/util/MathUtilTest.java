package org.tanato.util;

import org.tanato.utilities.MathUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import junit.framework.TestCase;

public class MathUtilTest extends TestCase {

	/**
	 * Test d'intersection 3D
	 * @throws ParseException
	 */
	public void testIntersection3D() throws ParseException {

		//Edge a intersecter
		LineString oppositEdge = (LineString) new WKTReader().read("MULTILINESTRING ((780267.68 2086737 450, 780262.21 2086727.49 450))").getGeometryN(0);

		//Coordonnée du vecteur de la face en 3D
		Coordinate vector  = new Coordinate(0.8497015481482858, -0.4887347495732649, -0.19785253001658681);

		//Point de départ
		Point point =  (Point) new WKTReader().read("POINT(780306.44 2086705.02 440)");



		Coordinate coordResultat = MathUtil.getIntersection(oppositEdge.getStartPoint().getCoordinate(), oppositEdge.getEndPoint().getCoordinate(), point.getCoordinate(), vector);


		Coordinate coordValid =  new Coordinate(780306.44, 2086705.02, 440);
		assertTrue(coordResultat.equals3D(coordValid ));
	}

}
