package org.tanato.processing.preprocessing.sewer;

import com.vividsolutions.jts.geom.Point;

public class ProjectedPoint {
	   private Point point;
	   private float dist;
	   private int index;
	   ProjectedPoint (Point point, float dist) {
	      this.point = point;
	      this.dist = dist;
	   }
	   ProjectedPoint (Point point, float dist, int index) {
		      this.point = point;
		      this.dist = dist;
		      this.index=index;
		   }
	   public Point getPoint() {
	      return this.point;
	   }
	   public void setPoint(Point p) {
		      this.point=p;
		   }
	   public float getDist() {
	      return this.dist;
	   }
	   public int getIndex() {
		      return this.index;
		   }
}