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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.indexes.DefaultAlphaQuery;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jdelaunay.delaunay.tools.Tools;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.NullProgressMonitor;
import org.tanato.factory.TINFeatureFactory;
import org.tanato.model.TINSchema;

/**
 * This class is used to build basin graphs from a given TIN, and a given TIn feature
 * used as a start for our processing.
 * @author alexis
 */
public class BasinBuilder {

	private int firstGID;
	private int startType;
        private final DataSet sdsEdges;
        private final DataSet sdsTriangles;
        private final DataSet sdsPoints;
        private GeometryFactory gf = new GeometryFactory();
        //Used to set the dimension of the TIN feature we use to start our processing.
        public static final int TIN_POINT = 0;
        public static final int TIN_EDGE = 1;
        public static final int TIN_TRIANGLE = 2;
        private final SQLDataSourceFactory dsf;
	private EdgePartManager remainingEP;
	private LinkedList<PointPart> remainingPoints;
	private Geometry basin;
	private MultiLineString lines;
	private int geomTriIndex;
	private int gidTriE0Index;
	private int gidTriE1Index;
	private int gidTriE2Index;
	private int gidEdgeLeft;
	private int gidEdgeRight;
	private int gidEdgeStart;
	private int gidEdgeEnd;
	private int geomPointIndex;
	private int geomEdgeIndex;
	private int edgeProperty;

	/**
	 * Build a basin, using the couple gid-type to determine which object to use as a start.
	 * @param dsf
	 * @param sdsPoints
	 * @param sdsEdges
	 * @param sdsTriangles
	 * @param gid
	 *	The GID of the object in the table where it is.
	 * @param type
	 *	The type of the object, used to choose the table where to pick the object up.
	 */
	public BasinBuilder(SQLDataSourceFactory dsf , DataSet sdsPoints,
                DataSet sdsEdges, DataSet sdsTriangles, int gid, int type){
		firstGID = gid;
		startType = type;
                this.sdsTriangles = sdsTriangles;
                this.sdsEdges = sdsEdges;
                this.sdsPoints = sdsPoints;
                this.dsf=dsf;
		remainingEP = new EdgePartManager();
		remainingPoints = new LinkedList<PointPart>();
		lines = new MultiLineString(new LineString[0], gf);
		basin = gf.createPolygon(gf.createLinearRing(new Coordinate[0]), new LinearRing[0]);
		try {
			geomTriIndex = sdsTriangles.getMetadata().getFieldIndex(TINSchema.GEOM_FIELD);
			gidTriE0Index = sdsTriangles.getMetadata().getFieldIndex(TINSchema.EDGE_0_GID_FIELD);
			gidTriE1Index = sdsTriangles.getMetadata().getFieldIndex(TINSchema.EDGE_1_GID_FIELD);
			gidTriE2Index = sdsTriangles.getMetadata().getFieldIndex(TINSchema.EDGE_2_GID_FIELD);
			gidEdgeLeft = sdsEdges.getMetadata().getFieldIndex(TINSchema.LEFT_TRIANGLE_FIELD);
			gidEdgeRight = sdsEdges.getMetadata().getFieldIndex(TINSchema.RIGHT_TRIANGLE_FIELD);
			gidEdgeStart = sdsEdges.getMetadata().getFieldIndex(TINSchema.STARTPOINT_NODE_FIELD);
			gidEdgeEnd = sdsEdges.getMetadata().getFieldIndex(TINSchema.ENDPOINT_NODE_FIELD);
			geomPointIndex = sdsPoints.getMetadata().getFieldIndex(TINSchema.GEOM_FIELD);
			geomEdgeIndex = sdsEdges.getMetadata().getFieldIndex(TINSchema.GEOM_FIELD);
			edgeProperty = sdsEdges.getMetadata().getFieldIndex(TINSchema.PROPERTY_FIELD);

		} catch (DriverException ex) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * This method actually computes the basin from the informations given to build the BasinBuilder.
	 * @throws DriverException
         *      If we encounter a problem while handling a datasource.
	 */
	public final void computeBasin() throws DriverException {
		try {
                        //We start by building the indexes we'll need for each data structure. Note that
                        //if they already exist, we acn use tem directly.
			if (!dsf.getIndexManager().isIndexed(sdsTriangles, TINSchema.GEOM_FIELD)) {
				dsf.getIndexManager().buildIndex(sdsTriangles, TINSchema.GEOM_FIELD, new NullProgressMonitor());
			}
			if (!dsf.getIndexManager().isIndexed(sdsEdges, TINSchema.GEOM_FIELD)) {
				dsf.getIndexManager().buildIndex(sdsEdges, TINSchema.GEOM_FIELD, new NullProgressMonitor());
			}
			if (!dsf.getIndexManager().isIndexed(sdsPoints, TINSchema.GEOM_FIELD)) {
				dsf.getIndexManager().buildIndex(sdsPoints, TINSchema.GEOM_FIELD, new NullProgressMonitor());
			}
			if (!dsf.getIndexManager().isIndexed(sdsTriangles, TINSchema.GID)) {
				dsf.getIndexManager().buildIndex(sdsTriangles, TINSchema.GID, new NullProgressMonitor());
			}
			if (!dsf.getIndexManager().isIndexed(sdsEdges, TINSchema.GID)){
				dsf.getIndexManager().buildIndex(sdsEdges, TINSchema.GID, new NullProgressMonitor());
			}
			if(!dsf.getIndexManager().isIndexed(sdsEdges, TINSchema.ENDPOINT_NODE_FIELD)){
				dsf.getIndexManager().buildIndex(sdsEdges, TINSchema.ENDPOINT_NODE_FIELD, new NullProgressMonitor());
			}
			if(!dsf.getIndexManager().isIndexed(sdsPoints, TINSchema.GID)){
				dsf.getIndexManager().buildIndex(sdsPoints, TINSchema.GID, new NullProgressMonitor());
			}
                        //we define the type of input that has been given.
			if(startType==TIN_POINT){
				processMeshPoint(firstGID);
			} else if(startType == TIN_EDGE) {
				Value[] edLine = retrieveLine(firstGID, sdsEdges);
				DEdge ed = retrieveEdge(firstGID);
				if(ed.getGradient() != DEdge.FLATSLOPE){
					ed.forceTopographicOrientation();
				}
				EdgePart ep = buildEdgePart(ed, ed.getStartPoint(), ed.getEndPoint(),
					firstGID, edLine[gidEdgeLeft].getAsInt(),
						edLine[gidEdgeRight].getAsInt(),
						edLine[gidEdgeStart].getAsInt(),
						edLine[gidEdgeEnd].getAsInt());
				processEdgePart(ep);
			} else if (startType ==TIN_TRIANGLE){
				throw new IllegalArgumentException("Triangles are not supported as an input currently");
			}
                        //This loop is the one that actually build the geometry we'll return
                        //as an output. We'll always process edges before points,
                        //as they give more often a polygon as a result.
			while(!remainingEP.isEmpty()||!remainingPoints.isEmpty()){
				if(!remainingEP.isEmpty()){
					List<EdgePart> ep =remainingEP.getEdgeParts();
					processEdgeParts(ep);
				} else {
					PointPart pp = remainingPoints.getFirst();
					remainingPoints.removeFirst();
					processPoint(pp);
				}
			}
			
		} catch (NoSuchTableException ex) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}catch (IndexException ex) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Return the basin graph computed previously. The resulting geometry will be a collection
	 * containing a MultiPolygon and a MultiLineString.
	 * @return
         *      The computed geometry
	 */
	public final Geometry getBasin(){
		return gf.createGeometryCollection(new Geometry[] {basin, lines});
	}

	/**
	 * Process a PointPart from the remaining points.
	 * @param pp
	 */
	private void processPoint(PointPart pp){
		//We determine if our point is a point of the mesh, ot if it is an intermediate
		//point that lies on an edge of the mesh.
		int type = pp.getOwnerType();
		if(type == 0){
			//We're on a point of the mesh.
			processMeshPoint(pp.getOwnerGID());
		} else if(type == 1){
			//We're on an edge of the mesh.
			//We retrieve the GID of the said edge.
			int edgeGid = pp.getOwnerGID();
			try{
				//We retrieve the line of Values in the GDMS file.
				Value[] edLine = retrieveLine(edgeGid, sdsEdges);
				//We will analyse the property, to know what to do.
				int edProp = edLine[edgeProperty].getAsInt();
				//In the following cases, we will analyze a part of an edge in the mesh.
				//More precisely, we will study what area of the triangles that lies
				//behind the edge directly comes in the part of the edge that is upper
				//than pp.
				//if we have a well, we must do this analysis
				boolean isWell = HydroProperties.check(edProp,  HydroProperties.LEFTWELL) ||
						HydroProperties.check(edProp, HydroProperties.RIGHTWELL);
				//If we have a river or a ditch, we must do this analysis
				boolean isRiverOrDitch =HydroProperties.check(edProp, HydroProperties.RIVER) ||
						HydroProperties.check(edProp, HydroProperties.DITCH) ;
				//if we have a talweg, we must do this analysis
				boolean isTalweg = HydroProperties.check(edProp, HydroProperties.LEFTCOLINEAR) ||
						HydroProperties.check(edProp, HydroProperties.RIGHTCOLINEAR)||
						HydroProperties.check(edProp, HydroProperties.TALWEG);
				if(isWell || isRiverOrDitch || isTalweg){
					//We must create a new EdgePart that will be used later.
					DEdge ed = retrieveEdge(edgeGid);
					if(ed.getGradient() != DEdge.FLATSLOPE){
						ed.forceTopographicOrientation();
					}
					//We build an EdgePart. its startPoint will be
					// the startPoint of the DEdge, as we've ensured topographic orientation,
					//its end will be pp.
					EdgePart ep = buildEdgePart(ed,
						ed.getStartPoint(), new DPoint(pp.getPt()),
						edgeGid,
						edLine[gidEdgeLeft].getAsInt(),
						edLine[gidEdgeRight].getAsInt(),
						edLine[gidEdgeStart].getAsInt(),
						edLine[gidEdgeEnd].getAsInt());
					//We've added an element that needs to be analyzed
					remainingEP.addEdgePart(ep);
				} else if(HydroProperties.check(edProp, HydroProperties.RIGHTSLOPE)){
					//We analyze the left triangle
					//We can have only a line, here.
					if(!basin.covers(gf.createPoint(pp.getPt()))){
						analyzeTriangleLine(pp.getPt(),edLine[gidEdgeLeft].getAsInt() );
					}
				} else if(HydroProperties.check(edProp, HydroProperties.LEFTTSLOPE)
						&& !basin.covers(gf.createPoint(pp.getPt()))){
					//We analyze the right triangle
					//We can have only a line, here.
					analyzeTriangleLine(pp.getPt(),edLine[gidEdgeRight].getAsInt() );
				}

			} catch (DriverException ex) {
				Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
			} catch (DelaunayError d) {
				Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, d);
			}
		}
	}

	/**
	 * Process a point that is an actual part of the mesh.
	 * @param ptGID
	 */
	private void processMeshPoint(int ptGID){
		//We retrieve all the edges that end in pp, if any.
		DefaultAlphaQuery daq = new DefaultAlphaQuery(TINSchema.ENDPOINT_NODE_FIELD, ValueFactory.createValue(ptGID));
		DefaultAlphaQuery ptquery = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(ptGID));
		try{
			Iterator<Integer> it = sdsEdges.queryIndex(dsf, daq);
			Iterator<Integer> ipt = sdsPoints.queryIndex(dsf, ptquery);
			Value[] ptValue = sdsPoints.getRow(ipt.next());

			while(it.hasNext()){
				//We can be sure that talwegs, leftwells, rightwells, rivers and ditches lead
				//the water to the current node.
				Integer cur = it.next();
				int edProp = sdsEdges.getInt(cur, TINSchema.PROPERTY_FIELD);
				//Wells, rivers, ditches and talwegs must be processed as EdgeParts.
				boolean isWell = HydroProperties.check(edProp,  HydroProperties.LEFTWELL) ||
						HydroProperties.check(edProp, HydroProperties.RIGHTWELL);
				boolean isRiverOrDitch = HydroProperties.check(edProp, HydroProperties.RIVER) ||
						HydroProperties.check(edProp, HydroProperties.DITCH);
				boolean isTalweg = HydroProperties.check(edProp, HydroProperties.LEFTCOLINEAR) ||
						HydroProperties.check(edProp, HydroProperties.RIGHTCOLINEAR) ||
						HydroProperties.check(edProp, HydroProperties.TALWEG);
				if(isWell || isRiverOrDitch || isTalweg){
					//These edges will have to be processed as EdgeParts later.
					DEdge ed = TINFeatureFactory.createDEdge(sdsEdges.getGeometry(cur, geomEdgeIndex));
					if(ed.getGradient() != DEdge.FLATSLOPE){
						ed.forceTopographicOrientation();
					}
					//We need a new EdgePart, that begins in the start of the DEdge
					//and end in the end of the DEdge.
					EdgePart ep = buildEdgePart(ed,
						ed.getStartPoint(),
						ed.getEndPoint(),
						sdsEdges.getInt(cur, TINSchema.GID),
						sdsEdges.getInt(cur, TINSchema.LEFT_TRIANGLE_FIELD),
						sdsEdges.getInt(cur, TINSchema.RIGHT_TRIANGLE_FIELD),
						sdsEdges.getInt(cur, TINSchema.STARTPOINT_NODE_FIELD),
						sdsEdges.getInt(cur, TINSchema.ENDPOINT_NODE_FIELD));
					Coordinate[] cs = new Coordinate[] {ed.getStartPoint().getCoordinate(),ed.getEndPoint().getCoordinate()};
					LineString ls = gf.createLineString(cs);
					if(!basin.covers(ls)&& !lines.covers(ls)){
						remainingEP.addEdgePart(ep);
					}
				}
				//If we have a right or left slope, we must go a little further to know
				//if the triangle point to the point
				else if(HydroProperties.check(edProp, HydroProperties.RIGHTSLOPE)){
					//We must analyze the left triangle.
					int leftGID = sdsEdges.getInt(cur, TINSchema.LEFT_TRIANGLE_FIELD);
					Coordinate coord = ptValue[geomPointIndex].getAsGeometry().getCoordinates()[0];
					if(!basin.covers(gf.createPoint(coord))){
						analyzeTriangleLine(coord, leftGID);
					}
				} else if(HydroProperties.check(edProp, HydroProperties.LEFTTSLOPE)){
					//We must analyze the right triangle.
					int rightGID = sdsEdges.getInt(cur, TINSchema.RIGHT_TRIANGLE_FIELD);
					Coordinate coord = ptValue[geomPointIndex].getAsGeometry().getCoordinates()[0];
					if(!basin.covers(gf.createPoint(coord))){
						analyzeTriangleLine(coord, rightGID);
					}
				}
			}
		} catch (DriverException ex) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
		} catch (DelaunayError d) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, d);
		}

	}

	/**
	 * Try to get a line from a couple point-triangle. we are in the case that some wate comes
	 * from the triangle and end in the point. We must potentially add a line
	 * to our basin graph.
	 * @param cd
	 * @param triangleGID
	 */
	private void analyzeTriangleLine(Coordinate cd, int triangleGID){
		try{
			//We must know the triangle we need to analyze. To do so, let's retrieve
			//the good line in the table of triangles.
			DefaultAlphaQuery triquery = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(triangleGID));
			Iterator<Integer> itri = sdsTriangles.queryIndex(dsf, triquery);
			//We actually rtrieve the line that interests us.
			Value[] triline = sdsTriangles.getRow(itri.next());
			DTriangle dtr = TINFeatureFactory.createDTriangle(triline[geomTriIndex].getAsGeometry());
			DPoint pt = TINFeatureFactory.createDPoint(cd);
			//We retrieve the point that will be used to build the line we'll add to the basin graph.
			DPoint si = dtr.getCounterSteepestIntersection(pt);
			if(!pt.equals(si) && si!=null){
				//We must add a line from cd to the found point
				LineString ls = gf.createLineString(new Coordinate[] {cd, si.getCoordinate()});
				//we process the union between the current line, and the LineString
				//we've just created.
				addLineString(ls);

				//We must add the new coordinate to the points that have to be treated.
				//For that, we retrieve the three edges of the triangle, and search the
				//one that contains the new PointPart.
				List<Integer> edgeGids = new LinkedList<Integer>();
				edgeGids.add(triline[gidTriE0Index].getAsInt());
				edgeGids.add(triline[gidTriE1Index].getAsInt());
				edgeGids.add(triline[gidTriE2Index].getAsInt());
				for(Integer i : edgeGids){
					//We know the GID of the edge, we must retrieve its geometry as a DEdge.
					DefaultAlphaQuery search = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(i));
					Iterator<Integer> it = sdsEdges.queryIndex(dsf, search);
					Value[] val = sdsEdges.getRow(it.next());
					DEdge cur = TINFeatureFactory.createDEdge(val[geomEdgeIndex].getAsGeometry());
					//We check that the current edge contains si.
					if(cur.contains(si)){
						if(cur.getGradient()!=DEdge.FLATSLOPE){
							cur.forceTopographicOrientation();
						}
						//It will be more efficient to test first if the point is
						//an extremity of the edge, and if it is not, if it is inside.
						if(cur.getStartPoint().equals(si)){
							//the new PointPart contains a point of the mesh.
							PointPart pp = new PointPart(si.getCoordinate(), val[gidEdgeStart].getAsInt(),0);
							remainingPoints.add(pp);
							break;
						} else if (cur.getEndPoint().equals(si)){
							//the new PointPart contains a point of the mesh.
							PointPart pp = new PointPart(si.getCoordinate(), val[gidEdgeEnd].getAsInt(),0 );
							remainingPoints.add(pp);
							break;
						} else {
							//the new PointPart does not contain a point of the mesh. We
							//instanciate it with the GID of the edge that contains it,
							//and with the value 1 to says that it is on an edge.
							PointPart pp = new PointPart(si.getCoordinate(), i, 1);
							remainingPoints.add(pp);
							break;
						}
					}
				}
			}
		} catch (DriverException ex) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
		} catch (DelaunayError d) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, d);
		}
		
	}

	private void addLineString(LineString ls){
		ArrayList<Geometry> un = new ArrayList<Geometry>();
		un.add(ls);
		un.add(lines);
		Geometry res = UnaryUnionOp.union(un);
		if(res instanceof MultiLineString){
			lines =  (MultiLineString) res;
		}else if (res instanceof LineString) {
			lines = gf.createMultiLineString(new LineString[] {(LineString) res});

		}
	}
        
        private void processEdgeParts(List<EdgePart> list){
                for(EdgePart e : list){
                        processEdgePart(e);
                }
        }

	/**
	 * This method determines which behaviour to have when processing an EdgePart,
	 * ie which calls to analyzeTriangle we must make.
	 * @param ep
	 */
	private void processEdgePart(EdgePart ep){
		int epGID = ep.getGid();
		DefaultAlphaQuery daq = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(epGID));
		try {
			Iterator<Integer> it = sdsEdges.queryIndex(dsf, daq);
			long edgeIndex = it.next();
			int epProp = sdsEdges.getInt(edgeIndex, TINSchema.PROPERTY_FIELD);
			//If the edge is a ridge, we just add it.
			if(HydroProperties.check(epProp, HydroProperties.RIDGE)){
				//We process the union.
				ArrayList<Geometry> geom = new ArrayList<Geometry>();
				geom.add(lines);
				geom.add((LineString) sdsEdges.getGeometry(edgeIndex, geomEdgeIndex));
				Geometry res = UnaryUnionOp.union(geom);
				if(res instanceof MultiLineString){
					lines =  (MultiLineString) res;
				}else if (res instanceof LineString) {
					lines = gf.createMultiLineString(new LineString[] {(LineString) res});

				}
			//When processing a talweg, we must analyze the two neigbour triangles.
			} else if(HydroProperties.check(epProp, HydroProperties.TALWEG)) {
				analyzeTriangle(ep, edgeIndex, sdsEdges.getInt(edgeIndex, TINSchema.LEFT_TRIANGLE_FIELD));
				analyzeTriangle(ep, edgeIndex, sdsEdges.getInt(edgeIndex, TINSchema.RIGHT_TRIANGLE_FIELD));
			//For rightslopes and rightwells, we just analyse the left triangle.
			} else if(HydroProperties.check(epProp, HydroProperties.RIGHTSLOPE)||
					HydroProperties.check(epProp, HydroProperties.RIGHTWELL)){
				analyzeTriangle(ep, edgeIndex, sdsEdges.getInt(edgeIndex, TINSchema.LEFT_TRIANGLE_FIELD));
			//For leftslopes and leftwells, we just analyze the right triangle.
			} else if (HydroProperties.check(epProp, HydroProperties.LEFTTSLOPE)||
					HydroProperties.check(epProp, HydroProperties.LEFTWELL)) {
				analyzeTriangle(ep, edgeIndex, sdsEdges.getInt(edgeIndex, TINSchema.RIGHT_TRIANGLE_FIELD));
			}
		} catch (DriverException ex) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * When processing an edge part, we must sometimes analyze the triangles
	 * that are associated to it, to know which part of the triangle's area
	 * pour into the EdgePart.
	 * @param ep
	 * @param edgeIndex
	 * @param triangleGID
	 */
	private void analyzeTriangle(EdgePart ep, long edgeIndex, int triangleGID){
		DefaultAlphaQuery daq = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(triangleGID));
		try {
			Iterator<Integer> it = sdsTriangles.queryIndex(dsf, daq);
			if(it.hasNext()){
				//We retrieve the line of values associated to the triangle.
				Value [] triLine = sdsTriangles.getRow(it.next());
				//We build the matching DTriangle.
				DTriangle left =  TINFeatureFactory.createDTriangle(triLine[geomTriIndex].getAsGeometry());
				//We build the DEdge.
				DEdge current = TINFeatureFactory.createDEdge(sdsEdges.getGeometry(edgeIndex,geomEdgeIndex));
				if(current.getGradient() != DEdge.FLATSLOPE){
					current.forceTopographicOrientation();
				}
				//We build the exact DPoint that is the start of the EdgePart.
				double s = ep.getStart();
				double xstart = (1-s)*current.getStartPoint().getX()+s*current.getEndPoint().getX();
				double ystart = (1-s)*current.getStartPoint().getY()+s*current.getEndPoint().getY();
				double zstart = (1-s)*current.getStartPoint().getZ()+s*current.getEndPoint().getZ();
				DPoint p1 = new DPoint(xstart, ystart, zstart);
				//We build the exact DPoint that is the end of the EdgePart.
				double e = ep.getEnd();
				double xend = (1-e)*current.getStartPoint().getX()+e*current.getEndPoint().getX();
				double yend = (1-e)*current.getStartPoint().getY()+e*current.getEndPoint().getY();
				double zend = (1-e)*current.getStartPoint().getZ()+e*current.getEndPoint().getZ();
				DPoint p2 = new DPoint(xend, yend, zend);
				//We need the two other DEdge that form this DTriangle.
				List<DEdge> others = retrieveOtherEdges(current, triLine);
				DEdge e1 = others.get(0);
				DEdge e2 = others.get(1);
				//The ancestor of p1 in left.
				DPoint proj1 = left.getCounterSteepestIntersection(p1);
				//the ancestor of p2 in left.
				DPoint proj2 = left.getCounterSteepestIntersection(p2);
				//lastPoint is the point of the triangle currently under analysis
				//that is not on the DEdge current.
				DPoint lastPoint = left.getOppositePoint(current);
				//We retrieve the line associated to e1 in the table of edges, and use it to fill some
				//useful values.
				Value[] lineE1 = retrieveLine(e1.getGID(), sdsEdges);
				int gidE1Left = lineE1[gidEdgeLeft].getAsInt();
				int gidE1Right = lineE1[gidEdgeRight].getAsInt();
				int gidE1Start = lineE1[gidEdgeStart].getAsInt();
				int gidE1End = lineE1[gidEdgeEnd].getAsInt();
				//We must set the gid of the last point of left.
				lastPoint.setGID(gidE1End == ep.getGidStart() || gidE1End == ep.getGidEnd() ? gidE1Start : gidE1End);
				//We retrieve the line associated to e2 in the table of edges, and use it to fill some
				//useful values.
				Value[] lineE2 = retrieveLine(e2.getGID(), sdsEdges);
				int gidE2Left = lineE2[gidEdgeLeft].getAsInt();
				int gidE2Right = lineE2[gidEdgeRight].getAsInt();
				int gidE2Start = lineE2[gidEdgeStart].getAsInt();
				int gidE2End = lineE2[gidEdgeEnd].getAsInt();
//				List<Geometry> union = new ArrayList<Geometry>();
//				union.add(basin);
				//We'll use buildEdgePart to build our EdgeParts,
				//as it will use our points in the right order.
				if(e1.contains(proj1)){
					if(e1.contains(proj2)){
						//We have just one edgepart to build
						//It is based on e1
						//We check that we're not already in the basin.
						//We build the polygon we're about to add, to perform tests on it.
						Coordinate[] cs = new Coordinate[] {
									p1.getCoordinate(),
									p2.getCoordinate(),
									proj2.getCoordinate(),
									proj1.getCoordinate(),
									p1.getCoordinate()};
						Polygon poly = gf.createPolygon(gf.createLinearRing(cs), new LinearRing[]{});
						if(!isCoveredByBasin(poly)&&!poly.isEmpty()){
							remainingEP.addEdgePart(buildEdgePart(e1, 
											proj1,
											proj2,
											e1.getGID(),
											gidE1Left,
											gidE1Right,
											gidE1Start,
											gidE1End));
							//We create a new Polygon, and add it to the list we'll use for the union.
							addPolygonToBasin( poly);
						}
					} else {
						//e2 contains proj2. We must add two edgeparts to the remaining elements.
						//projCoord is the coordinate of the intersection between the line supported
						//by the steepest path vecto and that contains the last point, and the
						//edge that contains the current EdgePart.
						Coordinate projCoord = left.getSteepestIntersectionPoint(lastPoint).getCoordinate();
						//We check that we're not already in the basin.
						//We build the polygon we're about to add, to perform tests on it.
						Coordinate[] cs = new Coordinate[] {
									p1.getCoordinate(),
									projCoord,
									lastPoint.getCoordinate(),
									proj1.getCoordinate(),
									p1.getCoordinate()};
						Polygon poly = gf.createPolygon(gf.createLinearRing(cs), new LinearRing[]{});
						if(!isCoveredByBasin(poly) && !poly.isEmpty()){
							remainingEP.addEdgePart(buildEdgePart(e1, 
										proj1,
										lastPoint,
										e1.getGID(),
										gidE1Left,
										gidE1Right,
										gidE1Start,
										gidE1End));
							//We create a new Polygon, and add it to the list we'll use for the union.
							addPolygonToBasin( poly);

						}
						//We build the polygon we're about to add, to perform tests on it.
						cs = new Coordinate[] {
									p2.getCoordinate(),
									proj2.getCoordinate(),
									lastPoint.getCoordinate(),
									projCoord,
									p2.getCoordinate()};
						poly = gf.createPolygon(gf.createLinearRing(cs), new LinearRing[]{});
						//We check that we're not already in the basin.
						if(!isCoveredByBasin(poly) && !poly.isEmpty()){
							remainingEP.addEdgePart(buildEdgePart(e2, 
											proj2,
											lastPoint,
											e2.getGID(),
											gidE2Left,
											gidE2Right,
											gidE2Start,
											gidE2End));
							//We create a new Polygon, and add it to the list we'll use for the union.
							addPolygonToBasin( poly);
						}
					}
				} else if (e2.contains(proj1)){
					if(e2.contains(proj2)){
						//e2 contains both proj1 and proj2
						//We check that we're not already in the basin.
						//We build the polygon we're about to add, to perform tests on it.
						Coordinate[] cs = new Coordinate[] {p1.getCoordinate(),p2.getCoordinate(),
								proj2.getCoordinate(),proj1.getCoordinate(),p1.getCoordinate()};
						Polygon poly = gf.createPolygon(gf.createLinearRing(cs), new LinearRing[]{});
						if(!isCoveredByBasin(poly) && !poly.isEmpty()){
							remainingEP.addEdgePart(buildEdgePart(e2, 
											proj1,
											proj2,
											e2.getGID(),
											gidE2Left,
											gidE2Right,
											gidE2Start,
											gidE2End));
							//We create a new Polygon, and add it to the list we'll use for the union.
							addPolygonToBasin( poly);			}
					} else {
						//e1 contains proj2, and e2 contains proj1.
						//We must add two edgeparts to the remaining elements.
						//We build the polygon we're about to add, to perform tests on it.
						Coordinate projCoord = left.getSteepestIntersectionPoint(lastPoint).getCoordinate();
						Coordinate[] cs = new Coordinate[] {
									p1.getCoordinate(),
									projCoord,
									lastPoint.getCoordinate(),
									proj1.getCoordinate(),
									p1.getCoordinate()};
						Polygon poly = gf.createPolygon(gf.createLinearRing(cs), new LinearRing[]{});
						//We check that we're not already in the basin.
						if(!isCoveredByBasin(poly) && !poly.isEmpty()){
							remainingEP.addEdgePart(buildEdgePart(e2, 
											proj1,
											lastPoint,
											e2.getGID(),
											gidE2Left,
											gidE2Right,
											gidE2Start,
											gidE2End));
							//We create a new Polygon, and add it to the list we'll use for the union.
							addPolygonToBasin( poly);

						}
						//We build the polygon we're about to add, to perform tests on it.
						cs = new Coordinate[] {
									p2.getCoordinate(),
									proj2.getCoordinate(),
									lastPoint.getCoordinate(),
									projCoord,
									p2.getCoordinate()};
						poly = gf.createPolygon(gf.createLinearRing(cs), new LinearRing[]{});
						//We check that we're not already in the basin.
						if(!isCoveredByBasin(poly) && !poly.isEmpty()){
							remainingEP.addEdgePart(buildEdgePart(e1, 
											proj2,
											lastPoint,
											e1.getGID(),
											gidE1Left,
											gidE1Right,
											gidE1Start,
											gidE1End));
							//We create a new Polygon, and add it to the list we'll use for the union.
							addPolygonToBasin( poly);
						}
					}
					
				}
//				basin = UnaryUnionOp.union(union);
			}
		} catch (DriverException ex) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, ex);
		} catch (DelaunayError d) {
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, d);
		}

	}

	private void addPolygonToBasin(Polygon poly){
		if(!poly.isEmpty()){
			basin = EnhancedPrecisionOp.union(basin, poly.convexHull());
			if(basin instanceof GeometryCollection && !(basin instanceof MultiPolygon)){
				Geometry gn ;
				Geometry out = gf.createMultiPolygon(new Polygon[]{});
				for(int i=0; i<basin.getNumGeometries(); i++){
					gn = basin.getGeometryN(i);
					if(gn instanceof Polygonal){
						out = EnhancedPrecisionOp.union(out, gn);
					} else if(gn instanceof LineString){
						addLineString((LineString) gn);
					}
				}
				basin = out;
			}
		}
	}

	/**
	 * Knowing a DEdge and a a line in the table of triangles, we try to retrieve
	 * the two other edges of the triangles as DEdge instances.
	 * @param first
	 * @param triLine
	 * @return
	 * @throws DriverException
	 */
	private List<DEdge> retrieveOtherEdges( DEdge first, Value[] triLine) throws DriverException{
		List<DEdge> ret = new ArrayList<DEdge>();
		int gid0 = triLine[gidTriE0Index].getAsInt();
		DEdge e0 = retrieveEdge(gid0);
		if(!e0.equals(first)){
			ret.add(e0);
		}
		int gid1 = triLine[gidTriE1Index].getAsInt();
		DEdge e1 = retrieveEdge(gid1);
		if(!e1.equals(first)){
			ret.add(e1);
		}
		int gid2 = triLine[gidTriE2Index].getAsInt();
		DEdge e2 = retrieveEdge(gid2);
		if(!e2.equals(first)){
			ret.add(e2);
		}
		if(ret.size()!=2){
			throw new DriverException("We are trying to retrieve exactly two edges in a triangle, not more, not less !");
		}
		return ret;
	}

	/**
	 * Retrieve the array of values in the table associated to sds, matching the GID gid.
	 * @param gid
	 * @param sds
	 * @return
	 * @throws DriverException
	 */
	private Value[] retrieveLine(int gid, DataSet sds) throws DriverException{
		DefaultAlphaQuery daq = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(gid));
		Iterator<Integer> it = sds.queryIndex(dsf, daq);
		return sds.getRow(it.next());
	}

	/**
	 * Retrieve an edge int the edges table. Its gid and its properties are associated to it.
	 * @param gid
	 * @return
	 */
	private DEdge retrieveEdge(int gid) throws DriverException{
		DefaultAlphaQuery daq = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(gid));
		try {
			Iterator<Integer> it = sdsEdges.queryIndex(dsf, daq);
			long edgeIndex = it.next();
			int epProp = sdsEdges.getInt(edgeIndex, TINSchema.PROPERTY_FIELD);
			DEdge ret = TINFeatureFactory.createDEdge(sdsEdges.getGeometry(edgeIndex, geomEdgeIndex));
			if(ret.getGradient() != DEdge.FLATSLOPE){
				ret.forceTopographicOrientation();
			}
			ret.setGID(gid);
			ret.setProperty(epProp);
                        ret.getStartPoint().setGID(sdsEdges.getInt(edgeIndex, TINSchema.STARTPOINT_NODE_FIELD));
                        ret.getEndPoint().setGID(sdsEdges.getInt(edgeIndex, TINSchema.ENDPOINT_NODE_FIELD));
			return ret;
		} catch (DelaunayError d){
			Logger.getLogger(BasinBuilder.class.getName()).log(Level.SEVERE, null, d);
		}
		return null;
	}

	/**
	 * Build an EdgePart with the given arguments. be careful when using it, coherence
	 * of the input is not checked for effciency reasons.
	 * Methods that call this one are supposed to have ensured that ptStart and ptEnd
	 * actually lies on ed. It is private, and there are good reasons for that ;-)
	 * @param ed
	 * @param pt
	 * @return
	 */
	private EdgePart buildEdgePart(DEdge ed, DPoint ptStart, DPoint ptEnd, int gid, int gidLeft, int gidRight, int gidStart, int gidEnd){
		double ratiostart;
		double ratioend;
                ed.getStartPoint().setGID(gidStart);
                ed.getEndPoint().setGID(gidEnd);
                ed.setGID(gid);
		if(!ed.isVertical()){
			ratiostart = (ptStart.getX()-ed.getStartPoint().getX())/(ed.getEndPoint().getX()-ed.getStartPoint().getX());
			ratioend = (ptEnd.getX()-ed.getStartPoint().getX())/(ed.getEndPoint().getX()-ed.getStartPoint().getX());
		} else {
			ratiostart = (ptStart.getY()-ed.getStartPoint().getY())/(ed.getEndPoint().getY()-ed.getStartPoint().getY());
			ratioend = (ptEnd.getY()-ed.getStartPoint().getY())/(ed.getEndPoint().getY()-ed.getStartPoint().getY());
		}
		if(ratiostart<ratioend){
			if(Math.abs(ratiostart)<Tools.EPSILON){
				PointPart pp = new PointPart(ptStart.getCoordinate(), gidStart, 0);
				if(!basin.covers(gf.createPoint(pp.getPt())) && !lines.covers(gf.createPoint(pp.getPt()))){
					remainingPoints.add(pp);
				}
			}
			if(Math.abs(1-ratioend)<Tools.EPSILON){
				PointPart pp = new PointPart(ptEnd.getCoordinate(), gidEnd, 0);
				if(!basin.covers(gf.createPoint(pp.getPt())) && !lines.covers(gf.createPoint(pp.getPt()))){
					remainingPoints.add(pp);
				}

			}
			return new EdgePart(ed, ratiostart, ratioend, gidLeft, gidRight);
		} else {
			if(Math.abs(ratioend)<Tools.EPSILON){
				PointPart pp = new PointPart(ptStart.getCoordinate(), gidStart, 0);
				if(!basin.covers(gf.createPoint(pp.getPt())) && !lines.covers(gf.createPoint(pp.getPt()))){
					remainingPoints.add(pp);
				}
			}
			if(Math.abs(1-ratiostart)<Tools.EPSILON){
				PointPart pp = new PointPart(ptEnd.getCoordinate(), gidEnd, 0);
				if(!basin.covers(gf.createPoint(pp.getPt())) && !lines.covers(gf.createPoint(pp.getPt()))){
					remainingPoints.add(pp);
				}
			}
			return new EdgePart(ed, ratioend, ratiostart, gidLeft, gidRight);
		}
	}

	private boolean isCoveredByBasin(Geometry geom){
		int n = basin.getNumGeometries();
		Geometry ng ;
		for(int i=0; i<n; i++){
			ng = basin.getGeometryN(i);
			if(ng.covers(geom)){
				return true;
			}
		}
		return false;
	}
}
