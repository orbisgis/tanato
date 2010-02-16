package org.tanato.processing.preprocessing.sewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class KDTree3D {
	 /**
	 * file KDTree3D.java, adapted from http://geom-java.sourceforge.net
	 */
	/**
	 * A data structure for storing a great number of points. During construction
	 * of the tree, median point in current coordinate is chosen for each step,
	 * ensuring the final tree is balanced. The cost for retrieving a point is 
	 * O(log n).<br>
	 * The cost for building the tree is O(n log^2 n), that can take some time for
	 * large points sets.<br>
	 * This implementation is semi-dynamic: points can be added, but can not be
	 * removed.
	 *
	 */
	public int MAXVALUE=10000000;
	
  	    public class Node{
	        private  Point 	point;
	        private Node left;
	        private Node right;
	        
	        public Node(Point point){
	            this.point  = point;
	            this.left   = null;
	            this.right  = null;
	        }

	        public Node(Point point, Node left, Node right){
	        	this.point  = point;
	            this.left   = left;
	            this.right  = right;
	        }
	        
	        public Point getPoint() {
	            return point;
	        }
	        
	        public Node getLeftChild() {
	            return left;
	        }
	        
	        public Node getRightChild() {
	            return right;
	        }
	        
	        public boolean isLeaf() {
	            return left==null && right==null;
	        }
	    }
	        
	    private class XComparator implements Comparator<Point> {
	        /* (non-Javadoc)
	         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	         */
	        public int compare(Point p1, Point p2){
	            if(p1.getCoordinate().x<p2.getCoordinate().x)
	                return -1;
	            if(p1.getCoordinate().x>p2.getCoordinate().x)
	                return +1;
	            return Double.compare(p1.getCoordinate().y, p2.getCoordinate().y);
	        }
	    }
	    
	    private class YComparator implements Comparator<Point> {
	        /* (non-Javadoc)
	         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	         */
	        public int compare(Point p1, Point p2){
	        	if(p1.getCoordinate().y<p2.getCoordinate().y)
		            return -1;
		        if(p1.getCoordinate().y>p2.getCoordinate().y)
		            return +1;
	            return Double.compare(p1.getCoordinate().x, p2.getCoordinate().x);
	        }
	    }
	   
	    private Node root;
	    
	    private Comparator<Point> xComparator;
	    private Comparator<Point> yComparator;
	    
	    public KDTree3D(ArrayList<Geometry> sl) {
	        this.xComparator = new XComparator();
	        this.yComparator = new YComparator();
	        
	        ArrayList<Point> points =new ArrayList<Point>();
	         for (int i=0;i<(sl.size());i++)
	        {
	        	 Geometry geom =sl.get(i);
	        	 if (geom instanceof Point)
	        	 {
	        		 points.add((Point)geom);
	        	 }
	        	 if (geom instanceof LineString)
	        	 {
	 				for (int k=0;k<geom.getNumPoints();k++)
					{points.add(((LineString)geom).getPointN(k));
					}
	        	 }
		    }
	        
	        root = makeTree(points, 0);
	    }
	    
	    private Node makeTree(List<Point> points, int depth) {
	        // Add a leaf
	        if(points.size()==0)
	            return null;
	        
	        // select direction
	        int dir = depth%2;
	        
	        // sort points according to i-th dimension
	        if(dir==0){
	            // Compare points based on their x-coordinate
	            Collections.sort(points, xComparator);
	        }else{
	            // Compare points based on their x-coordinate
	            Collections.sort(points, yComparator);
	        }
	        
	        int n = points.size();
	        int med = n/2;    // compute median
	        
	        return new Node(
	        		points.get(med),
	                makeTree(points.subList(0, med), depth+1),
	                makeTree(points.subList(med+1, n), depth+1));
	    }

	    public Node getRoot() {
	        return root;
	    }
	    
	    public Node getNode(Point point) {
	        return getNode(point, root, 0);
	    }
	    
	    private Node getNode(Point point, Node node, int depth){
	        if(node==null) return null;
	        // select direction
	        int dir = depth%2;
	        
	        // sort points according to i-th dimension
	        int res;
	        if(dir==0){
	            // Compare points based on their x-coordinate
	            res = xComparator.compare(point, node.point);
	        }else{
	            // Compare points based on their x-coordinate
	            res = yComparator.compare(point, node.point);
	        }
	        
	        if(res<0)
	            return getNode(point, node.left, depth+1);
	        if(res>0)
	            return getNode(point, node.right, depth+1);
	        
	        return node;
	    }

	    public void add(Point point){
	       add(point, root, 0);
	    }
	    
	    private void add(Point point, Node node, int depth) {
	        // select direction
	        int dir = depth%2;
	        
	        // sort points according to i-th dimension
	        int res;
	        if(dir==0){
	            // Compare points based on their x-coordinate
	            res = xComparator.compare(point, node.point);
	        }else{
	            // Compare points based on their x-coordinate
	            res = yComparator.compare(point, node.point);
	        }
	        
	        if(res<0){
	            if(node.left==null)
	                node.left = new Node(point);
	            else
	                add(point, node.left, depth+1);
	        }
	        if(res>0)
	            if(node.right==null)
	                node.right = new Node(point);
	            else
	                add(point, node.right, depth+1);
	    }
	    
	    public Point nearestNeighbor(Point point) {
	        return nearestNeighbor(point, root, root, 0).getPoint();
	    }
	    
	    /**
	     * Return either the same node as candidate, or another node whose point
	     * is closer.
	     */
	    private Node nearestNeighbor(Point point, Node candidate, Node node, 
	            int depth) {
	    	
	        // Check if the current node is closest that current candidate
	        double distCand = candidate.point.distance(point);
	        double dist     = node.point.distance(point);
	        if(dist<distCand){
	            candidate = node;
	        }
	        
	        // select direction
	        int dir = depth%2;

	        Node node1, node2;
	        
	        // First try on the canonical side,
	        // the result is the closest node found by depth-firth search
	        Point anchor = node.getPoint();
	        GeometryFactory gf= new GeometryFactory();
	        LineString line;
	        if(dir==0){
	            boolean b = point.getCoordinate().x<anchor.getCoordinate().x;
	            node1 = b ? node.left : node.right;
	            node2 = b ? node.right : node.left;
	           
	           Coordinate[] coord = new Coordinate[2];
	           	coord[0]= new Coordinate();
	           	coord[0].x=anchor.getCoordinate().x;
	           	coord[0].y=MAXVALUE;
				coord[1]= new Coordinate();
				coord[1].x=anchor.getCoordinate().x;
	           	coord[1].y=-MAXVALUE;
	           line =gf.createLineString(coord);
	        } else {
	            boolean b = point.getCoordinate().y<anchor.getCoordinate().y;
	            node1 = b ? node.left : node.right;
	            node2 = b ? node.right : node.left;
	            //line = StraightLine2D.create(anchor, new Vector2D(1, 0)); 
		           Coordinate[] coord = new Coordinate[2];
		           	coord[0]= new Coordinate();
		           	coord[0].x=MAXVALUE;
		           	coord[0].y=anchor.getCoordinate().y;
					coord[1]= new Coordinate();
					coord[1].x=-MAXVALUE;
		           	coord[1].y=anchor.getCoordinate().y;
		           line =gf.createLineString(coord);
	        }
	        
	        if(node1!=null) {
	            // Try to find a better candidate
	            candidate = nearestNeighbor(point, candidate, node1, depth+1);

	            // recomputes distance to the (possibly new) candidate
	            distCand = candidate.getPoint().distance(point);
	        }
	        
	        // If line is close enough, there can be closer points to the other
	        // side of the line
	        if(line.distance(point)<distCand && node2!=null) {
	            candidate = nearestNeighbor(point, candidate, node2, depth+1);
	        }
	        
	        return candidate;
	    }
	    
	    public Point nearestNeighborWithInfZ(Point point) {
	        return nearestNeighborWithInfZ(point, root, root, 0).getPoint();
	    }
	    
	    /**
	     * Return either the same node as candidate, or another node whose point
	     * is closer.
	     */
	    private Node nearestNeighborWithInfZ(Point point, Node candidate, Node node, 
	            int depth) {
	    	int MAXVALUE=10000000;
	    	
	        // Check if the current node is closest that current candidate
	        double distCand = candidate.point.distance(point);
	        double dist     = node.point.distance(point);
	        if(dist<distCand && (point.getCoordinate().z>node.getPoint().getCoordinate().z)){
	            candidate = node;
	        }
	        
	        // select direction
	        int dir = depth%2;

	        Node node1, node2;
	        
	        // First try on the canonical side,
	        // the result is the closest node found by depth-firth search
	        Point anchor = node.getPoint();
	        GeometryFactory gf= new GeometryFactory();
	        LineString line;
	        if(dir==0){
	            boolean b = point.getCoordinate().x<anchor.getCoordinate().x;
	            node1 = b ? node.left : node.right;
	            node2 = b ? node.right : node.left;
	           
	           Coordinate[] coord = new Coordinate[2];
	           	coord[0]= new Coordinate();
	           	coord[0].x=anchor.getCoordinate().x;
	           	coord[0].y=MAXVALUE;
				coord[1]= new Coordinate();
				coord[1].x=anchor.getCoordinate().x;
	           	coord[1].y=-MAXVALUE;
	           line =gf.createLineString(coord);
	        } else {
	            boolean b = point.getCoordinate().y<anchor.getCoordinate().y;
	            node1 = b ? node.left : node.right;
	            node2 = b ? node.right : node.left;
	            //line = StraightLine2D.create(anchor, new Vector2D(1, 0)); 
		           Coordinate[] coord = new Coordinate[2];
		           	coord[0]= new Coordinate();
		           	coord[0].x=MAXVALUE;
		           	coord[0].y=anchor.getCoordinate().y;
					coord[1]= new Coordinate();
					coord[1].x=-MAXVALUE;
		           	coord[1].y=anchor.getCoordinate().y;
		           line =gf.createLineString(coord);
	        }
	        
	        if(node1!=null) {
	            // Try to find a better candidate
	            candidate = nearestNeighbor(point, candidate, node1, depth+1);

	            // recomputes distance to the (possibly new) candidate
	            distCand = candidate.getPoint().distance(point);
	        }
	        
	        // If line is close enough, there can be closer points to the other
	        // side of the line
	        if(line.distance(point)<distCand && node2!=null) {
	            candidate = nearestNeighbor(point, candidate, node2, depth+1);
	        }
	        
	        return candidate;
	    }
}

