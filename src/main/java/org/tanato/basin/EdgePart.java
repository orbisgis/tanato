/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tanato.basin;

import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;

/**
 *
 * @author alexis
 */
class EdgePart {
	private int gid;
	private double start;
	private double end;
	private int gidStart;
	private int gidEnd;
	private int gidLeft;
	private int gidRight;

	public EdgePart(int gid, double start, double end, int gidStart, int gidEnd, int gidLeft, int gidRight){
		this.gid = gid;
		this.start = start;
		this.end = end;
		this.gidStart = gidStart;
		this.gidEnd = gidEnd;
		this.gidLeft = gidLeft;
		this.gidRight = gidRight;
	}

	/**
	 * Get the relative position of the start point of the part of the edge
	 * we are about to analyze in our study. Should always be inferior to 1.
	 * @return
	 */
	public double getEnd() {
		return end;
	}

	/**
	 * Set the relative position of the start point of the interesting part
	 * of the edge.
	 * @param end
	 */
	public void setEnd(double end) {
		this.end = end;
	}

	/**
	 * get the GID of the DEdge associated to this edge part in the mesh.
	 * @return
	 */
	public int getGid() {
		return gid;
	}

	/**
	 * Set the GID of the DEdge associated to this in the mesh.
	 * @param gid
	 */
	public void setGid(int gid) {
		this.gid = gid;
	}

	/**
	 * Get the GID of the endPoint of the underlying edge.
	 * @return
	 */
	public int getGidEnd() {
		return gidEnd;
	}

	/**
	 * Set the GID of the end point of the underlying edge.
	 * @param gidEnd
	 */
	public void setGidEnd(int gidEnd) {
		this.gidEnd = gidEnd;
	}

	/**
	 * Get the GID of the left triangle of this edge.
	 * @return
	 */
	public int getGidLeft() {
		return gidLeft;
	}

	/**
	 * Set the GID of the left triangle of this edge.
	 * @param gidLeft
	 */
	public void setGidLeft(int gidLeft) {
		this.gidLeft = gidLeft;
	}

	/**
	 * Get the GID of the right triangle of this Edge
	 * @return
	 */
	public int getGidRight() {
		return gidRight;
	}

	/**
	 * Set the GID of the right triangle of this edge.
	 * @param gidRight
	 */
	public void setGidRight(int gidRight) {
		this.gidRight = gidRight;
	}

	/**
	 * Get the GID of the start point of this edge
	 * @return
	 */
	public int getGidStart() {
		return gidStart;
	}

	/**
	 * Set the GID of the start point of the underlying edge in the mesh.
	 * @param gidStart
	 */
	public void setGidStart(int gidStart) {
		this.gidStart = gidStart;
	}

	/**
	 * Get the relative position of the end point of the part we are interested
	 * in.
	 * @return
	 */
	public double getStart() {
		return start;
	}

	/**
	 * Set the relative position of the end point of the part we are interested
	 * in.
	 * @param start
	 */
	public void setStart(double start) {
		this.start = start;
	}



}
