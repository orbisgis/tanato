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

import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.tools.Tools;

/**
 * EdgePart are used to process just the area that really interests us when computing
 * a basin graph.
 * @author alexis
 */
class EdgePart implements Comparable<EdgePart>{
        /**
         * The default number of iterations that can be done on a single EdgePart
         * whose length is inferior to org.jdelaunay.jdelaunay.Tools.EPSILON
         */
        public static final int DEFAULT_MAX_ITER = 10;
        private DEdge edge;
	private double start;
	private double end;
        private int gidLeft;
        private int gidRight;
        private int occuredIter;
        private static int MAX_ITER = DEFAULT_MAX_ITER;

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
	public EdgePart(DEdge edge, double start, double end, int leftGID, int rightGID){
		this.edge = edge;
		this.start = start;
		this.end = end;
                gidLeft = leftGID;
                gidRight = rightGID;
                occuredIter=0;
	}

	/**
	 * Get the relative position of the start point of the part of the edge
	 * we are about to analyze in our study. Should always be inferior to 1.
	 * @return
	 */
	public final double getEnd() {
		return end;
	}

	/**
	 * Set the relative position of the start point of the interesting part
	 * of the edge.
	 * @param end
	 */
	public final void setEnd(double end) {
		this.end = end;
	}

	/**
	 * get the GID of the DEdge associated to this edge part in the mesh.
	 * @return
	 */
	public final int getGid() {
		return edge.getGID();
	}

	/**
	 * Get the GID of the endPoint of the underlying edge.
	 * @return
	 */
	public final int getGidEnd() {
		return edge.getEndPoint().getGID();
	}

	/**
	 * Get the GID of the left triangle of this edge.
	 * @return
	 */
	public final int getGidLeft() {
		return gidLeft;
	}

	/**
	 * Set the GID of the left triangle of this edge.
	 * @param gidLeft
	 */
	public final void setGidLeft(int gidLeft) {
		this.gidLeft = gidLeft;
	}

	/**
	 * Get the GID of the right triangle of this Edge
	 * @return
	 */
	public final int getGidRight() {
		return gidRight;
	}

	/**
	 * Set the GID of the right triangle of this edge.
	 * @param gidRight
	 */
	public final void setGidRight(int gidRight) {
		this.gidRight = gidRight;
	}

	/**
	 * Get the GID of the start point of this edge
	 * @return
	 */
	public final int getGidStart() {
		return edge.getStartPoint().getGID();
	}

	/**
	 * Get the relative position of the end point of the part we are interested
	 * in.
	 * @return
	 */
	public final double getStart() {
		return start;
	}

	/**
	 * Set the relative position of the end point of the part we are interested
	 * in.
	 * @param start
	 */
	public final void setStart(double start) {
		this.start = start;
	}

	@Override
	public final String toString(){
		return "Defining edge GID : "+getGid();
	}
        
        /**
         * Increase the number of iterations used to count the times we have tried
         * to process this EdgePart while its length was inferior to Tools.EPSILON.
         */
        public final void increaseIterNumber(){
                occuredIter++;
        }
        
        /**
         * Reset the iteration number, setting it to 0;
         */
        public final void resetIterNumber(){
                occuredIter=0;
        }
        
        /**
         * Get the number of times we've tried to process this EdgePart.
         * @return 
         */
        public final int getIterNumber(){
                return occuredIter;
        }

        /**
         * Return the maximum number of time we can try to process an EdgePart.
         * @return 
         */
        public static int getMaxIterNumber() {
                return MAX_ITER;
        }
        
        /**
         * Set the maximum number of time we can try to process an EdgePart. This is
         * a configuration at the system level, not at the object level.
         * @param max 
         */
        public static void setMaxIterNumber(int max){
                MAX_ITER = max;
        }
        
        public final boolean isMaxIterReached(){
                return occuredIter>=MAX_ITER;
        }
        
        /**
         * Check if this EdgePart contains other. The two EdgeParts must share the 
         * same parent gid, ie they must lie on the same edge of the mesh,
         * and we must have :<br/>
         *  * this.start &lt;= other.start <br/>
         *  * this.end &gt;= other.end
         * @param other
         * @return 
         */
        public final boolean contains(EdgePart other){
                if(this.getGid() == other.getGid()){
                        boolean oStart = other.start - this.start > -Tools.EPSILON;
                        boolean oEnd = this.end - other.end > -Tools.EPSILON;
                        if(oStart && oEnd){
                                return true;
                        }
                }
                return false;
        }

        public final boolean isProcessable(){
                double length = (end - start)*edge.get2DLength();
                return length > Tools.EPSILON;
        }
        
        /**
         * Try to merge this EdgePart with other. The two <code>EdgePart</code>s can
         * be merged if they lie on the same edge of the mesh, and if they share
         * a part of this edge, ie if :<br/>
         *  this.start &lt;= other.end &amp;&amp; this.end &gt; other.start<br/><br/>
         * 
         * The resulting <code>EdgePart</code> is stored in this EdgePart. other is not modified.
         * To know if the two <code>EdgePart</code>s have been merged, you can call 
         * contains after the call to merge.
         * @param other 
         */
        public final void expandToInclude(EdgePart other){
                boolean s = other.start <= end+Tools.EPSILON;
                boolean e = other.end >= start -Tools.EPSILON;
                if(s && e){
                        start = start < other.start ? start : other.start;
                        end = end < other.end ? other.end : end;
                }
        }
        
        /**
         * Perform the comparison between this and another EdgePart.
         * We implement Comparable&lt;EdgePart&gt; for efficiency reasons, 
         * for the EdgePartManager.<br/>
         * To understand quickly the meaning of this realization, we can say that :<br/>
         * this.compareTo(other) == 0 if and only if this and other can be merged using
         * expandToInclude.<br/>
         * More precisely : <br/>
         *  * this.compareTo(other) == -1 if and only if this.gid&lt;other.gid OR 
         * (this.gid == other.gid AND this.end &lt; other.start)<br/>
         *  * this.compareTo(other) == 1 if and only if this.gid&gt;other.gid OR
         * (this.gid == other.gid AND this.start &gt; other.end)<br/>
         *  * this.compareTo(other) == 0 otherwise.<br/>
         * <b>Important note : </b> this method is not consistent with equality! Note 
         * that this method is intended to be used in well-formed sorted lists. Use with care.
         * 
         * @param other
         * @return 
         */
        @Override
        public final int compareTo(EdgePart other){
                if(this.getGid()<other.getGid()){
                        return -1;
                } else if(this.getGid()>other.getGid()){
                        return 1;
                } else  if(this.end < other.start-Tools.EPSILON){
                        return -1;
                } else if(this.start > other.end+Tools.EPSILON){
                        return 1;
                } else {
                        return 0;
                }
        }
        
        /**
         * Overrides the default equals method. Two EdgeParts are equals if and only if 
         * they share the same GID, start and end. As we work on a unique mesh, the 
         * other properties will be the same.
         * Not consistent with compareTo.
         * @param other
         * @return 
         */
        @Override
        public final boolean equals(Object other){
                if(other instanceof EdgePart){
                        EdgePart ep = (EdgePart) other;
                        return ep.getGid()==getGid() && Math.abs(end-ep.end)<Tools.EPSILON && 
                                Math.abs(start-ep.start)<Tools.EPSILON;
                } else {
                        return false;
                }
        }

        @Override
        public int hashCode() {
                final int base = 3;
                final int coef = 67;
                final int bitDec = 32;
                int hash = base;
                hash = coef * hash + this.getGid();
                hash = coef * hash + (int) (Double.doubleToLongBits(this.start) ^ (Double.doubleToLongBits(this.start) >>> bitDec));
                hash = coef * hash + (int) (Double.doubleToLongBits(this.end) ^ (Double.doubleToLongBits(this.end) >>> bitDec));
                return hash;
        }
}
