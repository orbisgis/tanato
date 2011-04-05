package org.tanato.utilities;

import com.vividsolutions.jts.geom.LineString;

public class GeomUtil {

	/**
	 * We don't want to encounter any GeomUtil instance.
	 */
	private GeomUtil(){
	}

	/**
	 * Reverse a linestring according to z value.
	 * The z first point must be greater than the z end point
	 * @param lineString
	 * @return
	 */
	public static LineString zReverse(LineString lineString){

		double startZ = lineString.getStartPoint().getCoordinate().z;
		double endZ = lineString.getEndPoint().getCoordinate().z;
		if ((!Double.isNaN(startZ) && !Double.isNaN(endZ)) && startZ < endZ){
				return (LineString) lineString.reverse();
		}

		return lineString;
	}
}
