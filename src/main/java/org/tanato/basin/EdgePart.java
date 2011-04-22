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
 * Copyright (C) 2011 Erwan BOCHER, , Alexis GUEGANNO, Jean-Yves MARTIN
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
package org.tanato.basin;

/**
 * EdgePart are used to process just the area that really interests us when computing
 * a basin graph.
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

	/**
	 * Instanciate a new EdgePart
	 * @param gid
	 *	The GID of the edge in the TIN
	 * @param start
	 *	The start of the EdgePart in the Edge. Parametric value between 0 and 1.
	 * @param end
	 *	The end of the EdgePart in the Edge. Parametric value between 0 and 1. Always greater than start.
	 * @param gidStart
	 *	The GID of the start point of the edge.
	 * @param gidEnd
	 *	The GID of the end point of the edge.
	 * @param gidLeft
	 *	The GID of the left triangle of the edge.
	 * @param gidRight
	 *	The GID of the right triangle of the edge.
	 */
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

	@Override
	public String toString(){
		return "Defining edge GID : "+gid;
	}


}
