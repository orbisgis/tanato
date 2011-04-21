package org.tanato.utilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import junit.framework.TestCase;

/**
 * This class is intended to perform tests on the various utility calsses present in Tanato.
 * @author alexis
 */
public class TestUtilities extends TestCase {

	GeometryFactory gf = new GeometryFactory();

	/**
	 * we tet that LineString are well reverted when their end is upper
	 * than their start.
	 * @throws Exception
	 */
	public void testZReverse() throws Exception {
		LineString ls = gf.createLineString(new Coordinate[]{
				new Coordinate(0,2,2),
				new Coordinate(4,3,4),
				new Coordinate(9,8,5)});
		ls = GeomUtil.zReverse(ls);
		assertTrue(ls.getPointN(0).getCoordinate().z==5);
		ls = gf.createLineString(new Coordinate[]{
				new Coordinate(0,2,5),
				new Coordinate(4,3,4),
				new Coordinate(9,8,2)});
		ls = GeomUtil.zReverse(ls);
		assertTrue(ls.getPointN(0).getCoordinate().z==5);
	}

}
