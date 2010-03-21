package org.tanato.processing.preprocessing.road;
import java.io.Serializable;

import com.vividsolutions.jts.algorithm.*;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Represents a line segment defined by two {@link Coordinate}s.
 * Provides methods to compute various geometric properties
 * and relationships of line segments.
 * <p>
 * This class is designed to be easily mutable (to the extent of
 * having its contained points public).
 * This supports a common pattern of reusing a single LineSegment
 * object as a way of computing segment properties on the
 * segments defined by arrays or lists of {@link Coordinate}s.
 *
 *@version 1.7
 */
public class LineSegment3D
implements Comparable, Serializable
{
	private static final long serialVersionUID = 3252005833466256227L;

	public Coordinate p0, p1;

	public LineSegment3D(Coordinate p0, Coordinate p1) {
		this.p0 = p0;
		this.p1 = p1;
	}

	public LineSegment3D(double x0, double y0, double x1, double y1) {
		this(new Coordinate(x0, y0), new Coordinate(x1, y1));
	}

	public LineSegment3D(LineSegment3D ls) {
		this(ls.p0, ls.p1);
	}

	public LineSegment3D() {
		this(new Coordinate(), new Coordinate());
	}

	public Coordinate getCoordinate(int i)
	{
		if (i == 0) return p0;
		return p1;
	}

	public void setCoordinates(LineSegment3D ls)
	{
		setCoordinates(ls.p0, ls.p1);
	}

	public void setCoordinates(Coordinate p0, Coordinate p1)
	{
		this.p0.x = p0.x;
		this.p0.y = p0.y;
		this.p0.z = p0.z;
		this.p1.x = p1.x;
		this.p1.y = p1.y;
		this.p1.z = p1.z;
	}

	/**
	 * Computes the length of the line segment.
	 * @return the length of the line segment
	 */
	public double getLength()
	{
		return p0.distance(p1);
	}
	/**
	 * Determines the orientation index of a {@link Coordinate} relative to this segment.
	 * The orientation index is as defined in {@link CGAlgorithms#computeOrientation}.
	 *
	 * @param seg the LineSegment3D to compare
	 *
	 * @return 1 if <code>p</code> is to the left of this segment
	 * @return -1 if <code>p</code> is to the right of this segment
	 * @return 0 if <code>p</code> is collinear with this segment
	 * 
	 * @see CGAlgorithms#computeOrientation(Coordinate, Coordinate, Coordinate)
	 */
	public int orientationIndex(Coordinate p)
	{
		return CGAlgorithms.orientationIndex(p0, p1, p);
	}

	/**
	 * Reverses the direction of the line segment.
	 */
	public void reverse()
	{
		Coordinate temp = p0;
		p0 = p1;
		p1 = temp;
	}

	/**
	 * Puts the line segment into a normalized form.
	 * This is useful for using line segments in maps and indexes when
	 * topological equality rather than exact equality is desired.
	 * A segment in normalized form has the first point smaller
	 * than the second (according to the standard ordering on {@link Coordinate}).
	 */
	public void normalize()
	{
		if (p1.compareTo(p0) < 0) reverse();
	}

	/**
	 * Computes the angle that the vector defined by this segment
	 * makes with the X-axis.
	 * The angle will be in the range [ -PI, PI ] radians.
	 *
	 * @return the angle this segment makes with the X-axis (in radians)
	 */
	public double angle()
	{
		return Math.atan2(p1.y - p0.y, p1.x - p0.x);
	}

	/**
	 * Computes the midpoint of the segment
	 *
	 * @return the midpoint of the segment
	 */
	public Coordinate midPoint()
	{
		return new Coordinate( (p0.x + p1.x) / 2,
				(p0.y + p1.y) / 2,(p0.z + p1.z) / 2 );
	}
	/**
	 * Computes the {@link Coordinate} that lies a given
	 * fraction along the line defined by this segment.
	 * A fraction of <code>0.0</code> returns the start point of the segment;
	 * a fraction of <code>1.0</code> returns the end point of the segment.
	 * If the fraction is < 0.0 or > 1.0 the point returned 
	 * will lie before the start or beyond the end of the segment. 
	 *
	 * @param segmentLengthFraction the fraction of the segment length along the line
	 * @return the point at that distance
	 */
	public Coordinate pointAlong(double segmentLengthFraction)
	{
		Coordinate coord = new Coordinate();
		coord.x = p0.x + segmentLengthFraction * (p1.x - p0.x);
		coord.y = p0.y + segmentLengthFraction * (p1.y - p0.y);
		coord.z = p0.z + segmentLengthFraction * (p1.z - p0.z);
		return coord;
	}

	/**
	 * Computes the {@link Coordinate} that lies a given
	 * fraction along the line defined by this segment and offset from 
	 * the segment by a given distance.
	 * A fraction of <code>0.0</code> offsets from the start point of the segment;
	 * a fraction of <code>1.0</code> offsets from the end point of the segment.
	 * The computed point is offset to the left of the line if the offset distance is
	 * positive, to the right if negative.
	 *
	 * @param segmentLengthFraction the fraction of the segment length along the line
	 * @param offsetDistance the distance the point is offset from the segment
	 *    (positive is to the left, negative is to the right)
	 * @return the point at that distance and offset
	 * 
	 * @throws IllegalStateException if the segment has zero length
	 */
	public Coordinate pointAlongOffset(double segmentLengthFraction, double offsetDistance)
	{
		// the point on the segment line
		double segx = p0.x + segmentLengthFraction * (p1.x - p0.x);
		double segy = p0.y + segmentLengthFraction * (p1.y - p0.y);
		double segz = p0.z + segmentLengthFraction * (p1.z - p0.z);

		double dx = p1.x - p0.x;
		double dy = p1.y - p0.y;
		double len = Math.sqrt(dx * dx + dy * dy);
		double ux = 0.0;
		double uy = 0.0;
		if (offsetDistance != 0.0) {
			if (len <= 0.0)
				throw new IllegalStateException("Cannot compute offset from zero-length line segment");

			// u is the vector that is the length of the offset, in the direction of the segment
			ux = offsetDistance * dx / len;
			uy = offsetDistance * dy / len;
		}

		// the offset point is the seg point plus the offset vector rotated 90 degrees CCW
		double offsetx = segx - uy;
		double offsety = segy + ux;
		double offsetz = segz;

		Coordinate coord = new Coordinate(offsetx, offsety,offsetz);
		return coord;
	}
	/**
	 * Creates a LineString with the same coordinates as this segment
	 * 
	 * @param geomFactory the geometery factory to use
	 * @return a LineString with the same geometry as this segment
	 */
	public LineString toGeometry(GeometryFactory geomFactory)
	{
		return geomFactory.createLineString(new Coordinate[] { p0, p1 });
	}

	/**
	 *  Returns <code>true</code> if <code>other</code> has the same values for
	 *  its points.
	 *
	 *@param  other  a <code>LineSegment3D</code> with which to do the comparison.
	 *@return        <code>true</code> if <code>other</code> is a <code>LineSegment3D</code>
	 *      with the same values for the x and y ordinates.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof LineSegment3D)) {
			return false;
		}
		LineSegment3D other = (LineSegment3D) o;
		return p0.equals(other.p0) && p1.equals(other.p1);
	}
	/**
	 *  Compares this object with the specified object for order.
	 *  Uses the standard lexicographic ordering for the points in the LineSegment3D.
	 *
	 *@param  o  the <code>LineSegment3D</code> with which this <code>LineSegment3D</code>
	 *      is being compared
	 *@return    a negative integer, zero, or a positive integer as this <code>LineSegment3D</code>
	 *      is less than, equal to, or greater than the specified <code>LineSegment3D</code>
	 */
	public int compareTo(Object o) {
		LineSegment3D other = (LineSegment3D) o;
		int comp0 = p0.compareTo(other.p0);
		if (comp0 != 0) return comp0;
		return p1.compareTo(other.p1);
	}

	/**
	 *  Returns <code>true</code> if <code>other</code> is
	 *  topologically equal to this LineSegment3D (e.g. irrespective
	 *  of orientation).
	 *
	 *@param  other  a <code>LineSegment3D</code> with which to do the comparison.
	 *@return        <code>true</code> if <code>other</code> is a <code>LineSegment3D</code>
	 *      with the same values for the x and y ordinates.
	 */
	public boolean equalsTopo(LineSegment3D other)
	{
		return
		p0.equals(other.p0) && p1.equals(other.p1)
		|| p0.equals(other.p1) && p1.equals(other.p0);
	}

	public String toString()
	{
		return "LINESTRING( " +
		p0.x + " " + p0.y + " " + p0.z
		+ ", " +
		p1.x + " " + p1.y + " " + p1.z + ")";
	}
}
