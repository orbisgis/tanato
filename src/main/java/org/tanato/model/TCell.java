/**
 *
 */
package org.tanato.model;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author bocher
 *
 */
public class TCell extends HydroCell {

	private double slopeInDegree;

	private Coordinate slope;

	private double area;



	public void setSlopeInDegree(double slopeInPercent) {
		this.slopeInDegree = slopeInPercent;

	}

	public double getSlopeInDegree() {
		return slopeInDegree;
	}

	public void setSlope(Coordinate slope) {
		this.slope = slope;
	}

	public Coordinate getSlope() {
		return slope;
	}

	public double getArea() {

		return area;
	}

	public void setArea(double area) {
		this.area = area;
	}

}
