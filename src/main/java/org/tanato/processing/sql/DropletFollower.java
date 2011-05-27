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
package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.indexes.DefaultAlphaQuery;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.jdelaunay.delaunay.Element;
import org.jdelaunay.delaunay.Tools;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.factory.TINFeatureFactory;
import org.tanato.model.TINSchema;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a droplet path on an existing triangularization.
 * Two options can be set to constraint the drop path processing.
 * The two options implement the same object definition : a property.
 * A property describes a TIN features as a road , a sewer...
 * The user can set that kind of properties will be used to build the path (second argument).
 * It can also set if the process must stop when a property is found (last argument).
 *
 * Note if the property option is used the datasources must contains two fields : property and heigth.
 * Take a look the Tanato model user documentation.
 *
 *
 *
 * @author kwyhr
 */
public class DropletFollower {

        private static final Logger logger = Logger.getLogger(DropletFollower.class.getName());
        // Table informations to navigate
        private SpatialDataSourceDecorator sdsPoints = null;
        private SpatialDataSourceDecorator sdsEdges = null;
        private SpatialDataSourceDecorator sdsTriangles = null;
        // List of reached points
        private ArrayList<DPoint> theList = null;
        // Count each times we stay on the same point and stop when max is reached
        private int currentStagnation = 0;
        private static final int MAX_STAGNATION = 10;
        private DPoint lastPoint;
        // to follow walls
        private int wallSide = EDGE_NO_WALL;
        private DTriangle previousTriangle = null;
        private static final int EDGE_NO_WALL = 0;
        private static final int EDGE_WALL_LEFT = 1;
        private static final int EDGE_WALL_RIGHT = 2;
        // to follow sewers
        private boolean isInSewers;
        // using properties
        private int autorizedProperties;
        private int endingProperties;
        private boolean requieredAdditionalFields = false;

        public DropletFollower(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {
                if (tables.length < 4) {
                        // There MUST be at least 3 tables
                        throw new ExecutionException("needs points, edges , triangles and start points.");
                } else if (values.length > 2) {
                        // There MUST be at least 1 value
                        throw new ExecutionException("number of parameters exceeded.");
                } else {
                        autorizedProperties = -1;
                        endingProperties = 0;
                        theList = null;

                        try {

                                if (values.length >= 1) {
                                        autorizedProperties = values[0].getAsInt();
                                        checkMetadata(tables);
                                        requieredAdditionalFields = true;
                                }
                                if (values.length >= 2) {
                                        endingProperties = values[1].getAsInt();
                                        requieredAdditionalFields = true;
                                }

                                // Set informations from tables and Values
                                populateData(dsf, pm, tables);

                        } catch (DriverException ex) {
                                logger.log(Level.SEVERE, "There has been an error while opening a table, or counting its lines.\n", ex);
                        }
                }
        }

        public ArrayList<DPoint> getPath(Geometry geom) throws ExecutionException, DriverException, DelaunayError {
                Geometry testPoint = getInitiaPoint(geom);
                // process path
                dropletFollows(testPoint);
                return theList;
        }

        // ----------------------------------------------------------------
        // UTILITIES
        // ----------------------------------------------------------------
        /**
         * Populate a decorator
         * @param dsf
         * @param pm
         * @param aTable
         * @return
         * @throws DriverException
         * @throws ExecutionException
         */
        private SpatialDataSourceDecorator populateDecorator(DataSourceFactory dsf, IProgressMonitor pm, DataSource aTable) throws DriverException, ExecutionException {
                // Open it
                SpatialDataSourceDecorator aSourceGenerator = new SpatialDataSourceDecorator(aTable);
                aSourceGenerator.open();

                // Generate index on GID
                try {
                        if (!dsf.getIndexManager().isIndexed(aTable.getName(), TINSchema.GID)) {
                                dsf.getIndexManager().buildIndex(aTable.getName(), TINSchema.GID, pm);
                        }
                } catch (IndexException ex) {
                        throw new ExecutionException("Unable to create index.", ex);
                } catch (NoSuchTableException ex) {
                        throw new ExecutionException("Unable to create index.", ex);
                }
                return aSourceGenerator;
        }

        /**
         * Get table informations and put them in appropriate structures
         * @param dsf
         * @param pm
         * @param tables
         * @throws ExecutionException
         * @throws DriverException
         */
        private void populateData(DataSourceFactory dsf, IProgressMonitor pm, DataSource[] tables) throws DriverException, ExecutionException {
                sdsPoints = populateDecorator(dsf, pm, tables[0]);
                sdsEdges = populateDecorator(dsf, pm, tables[1]);
                sdsTriangles = populateDecorator(dsf, pm, tables[2]);
        }

        /**
         * close decorator
         * @param aDecorator
         */
        private void closeDecorator(SpatialDataSourceDecorator aDecorator) {
                if (aDecorator != null) {
                        try {
                                aDecorator.close();
                        } catch (DriverException ex) {
                                logger.log(Level.SEVERE, "There has been an error while closing a table.\n", ex);
                        }
                }
        }

        /**
         * Close tables
         * @throws DriverException
         */
        public void closeData() {
                closeDecorator(this.sdsPoints);
                this.sdsPoints = null;

                closeDecorator(this.sdsEdges);
                this.sdsEdges = null;

                closeDecorator(this.sdsTriangles);
                this.sdsTriangles = null;
        }

        /**
         * Get initial point and force it to add a z
         * @param values
         * @return
         * @throws ExecutionException
         */
        private Geometry getInitiaPoint(Geometry testPoint) throws ExecutionException {
                if (!testPoint.isValid()) {
                        throw new ExecutionException("invalid point geometry.");
                } else if (!testPoint.getGeometryType().equals("Point")) {
                        throw new ExecutionException("invalid point geometry.");
                } else {
                        Coordinate coord = testPoint.getCoordinate();

                        if (Double.isNaN(testPoint.getCoordinate().z)) {
                                coord.z = 0;
                                testPoint = testPoint.getFactory().createPoint(coord);
                        }
                }
                return testPoint;

        } // ----------------------------------------------------------------
        // PRIVATE METHODS
        // ----------------------------------------------------------------

        /**
         * Get an ilement from its GID
         * @param sds
         * @param theGID
         * @return
         * @throws DriverException
         */
        private long getElementIndex(SpatialDataSourceDecorator sds, int theGID) throws DriverException {
                if (sds == null) {
                        // sds not set
                        return -1;
                } else {
                        DefaultAlphaQuery defaultAlphaQuery = new DefaultAlphaQuery(TINSchema.GID, ValueFactory.createValue(theGID));
                        Iterator<Integer> queryResult = sds.queryIndex(defaultAlphaQuery);

                        if (queryResult == null) {
                                return -1;

                        } else if (queryResult.hasNext()) {
                                return queryResult.next();
                        } else {
                                return -1;
                        }
                }
        }

        /**
         * Get index position of a Triangle
         * @param theGID
         * @return
         */
        private long getTriangleIndex(int theGID) throws DriverException {
                return getElementIndex(sdsTriangles, theGID);
        }

        /**
         * Get index position of an Edge
         * @param theGID
         * @return
         */
        private long getEdgeIndex(int theGID) throws DriverException {
                return getElementIndex(sdsEdges, theGID);
        }

        /**
         * Get index position of a Point
         * @param theGID
         * @return
         */
        private long getPointIndex(int theGID) throws DriverException {
                return getElementIndex(sdsPoints, theGID);
        }

        /**
         * create DTriangle structure with GDMS data
         *
         * @param aTriangle
         * @throws DriverException
         * @throws DelaunayError
         */
        private DTriangle populateTriangleWithGDMS(int theGID) throws DriverException, DelaunayError {
                DTriangle aTriangle;

                long tIndex = getTriangleIndex(theGID);

                // Create edges
                DEdge edge0 = new DEdge();

                int eGID0 = sdsTriangles.getInt(tIndex, TINSchema.EDGE_0_GID_FIELD);

                long e0Index = getEdgeIndex(eGID0);
                edge0.setGID(eGID0);

                DEdge edge1 = new DEdge();

                int eGID1 = sdsTriangles.getInt(tIndex, TINSchema.EDGE_1_GID_FIELD);

                long e1Index = getEdgeIndex(eGID1);
                edge1.setGID(eGID1);

                DEdge edge2 = new DEdge();

                int eGID2 = sdsTriangles.getInt(tIndex, TINSchema.EDGE_2_GID_FIELD);

                long e2Index = getEdgeIndex(eGID2);
                edge2.setGID(eGID2);

                // Create Points
                DPoint point0 = new DPoint();
                edge0.setStartPoint(point0);

                int pGID0 = sdsEdges.getInt(e0Index, TINSchema.STARTPOINT_NODE_FIELD);

                long p0Index = getPointIndex(pGID0);
                point0.setGID(pGID0);

                DPoint point1 = new DPoint();
                edge0.setEndPoint(point1);

                int pGID1 = sdsEdges.getInt(e0Index, TINSchema.ENDPOINT_NODE_FIELD);

                long p1Index = getPointIndex(pGID1);
                point1.setGID(pGID1);

                int pGID2;
                DPoint point2 = new DPoint();

                if (sdsEdges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD) == pGID0) {
                        // Edge1 is 0-2
                        edge1.setStartPoint(point0);
                        edge1.setEndPoint(point2);
                        // Edge2 cannot contain 0

                        if (sdsEdges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == pGID1) {
                                // Edge2 is 1-2
                                edge2.setStartPoint(point1);
                                edge2.setEndPoint(point2);
                        } else {
                                // Edge2 is 2-1
                                edge2.setStartPoint(point2);
                                edge2.setEndPoint(point1);
                        }
                        pGID2 = sdsEdges.getInt(e1Index, TINSchema.ENDPOINT_NODE_FIELD);

                } else if (sdsEdges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD) == pGID1) {
                        // Edge1 is 1-2
                        edge1.setStartPoint(point1);
                        edge1.setEndPoint(point2);
                        // Edge2 cannot contain 1

                        if (sdsEdges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == pGID0) {
                                // Edge2 is 0-2
                                edge2.setStartPoint(point0);
                                edge2.setEndPoint(point2);
                        } else {
                                // Edge2 is 2-0
                                edge2.setStartPoint(point2);
                                edge2.setEndPoint(point0);
                        }
                        pGID2 = sdsEdges.getInt(e1Index, TINSchema.ENDPOINT_NODE_FIELD);

                } else if (sdsEdges.getInt(e1Index, TINSchema.ENDPOINT_NODE_FIELD) == pGID0) {
                        // Edge1 is 2-0
                        edge1.setStartPoint(point2);
                        edge1.setEndPoint(point0);
                        // Edge2 cannot contain 0

                        if (sdsEdges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == pGID1) {
                                // Edge2 is 1-2
                                edge2.setStartPoint(point1);
                                edge2.setEndPoint(point2);
                        } else {
                                // Edge2 is 2-1
                                edge2.setStartPoint(point2);
                                edge2.setEndPoint(point1);
                        }
                        pGID2 = sdsEdges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD);

                } else {
                        // Edge1 is 2-1
                        edge1.setStartPoint(point2);
                        edge1.setEndPoint(point1);
                        // Edge2 cannot contain 1

                        if (sdsEdges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == pGID0) {
                                // Edge2 is 0-2
                                edge2.setStartPoint(point0);
                                edge2.setEndPoint(point2);
                        } else {
                                // Edge2 is 2-0
                                edge2.setStartPoint(point2);
                                edge2.setEndPoint(point0);
                        }
                        pGID2 = sdsEdges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD);
                }
                long p2Index = getPointIndex(pGID2);
                point2.setGID(pGID2);

                // Set points location
                Coordinate coord;
                coord = sdsPoints.getGeometry(p0Index).getCoordinate();
                point0.setX(coord.x);
                point0.setY(coord.y);
                point0.setZ(coord.z);

                coord = sdsPoints.getGeometry(p1Index).getCoordinate();
                point1.setX(coord.x);
                point1.setY(coord.y);
                point1.setZ(coord.z);

                coord = sdsPoints.getGeometry(p2Index).getCoordinate();
                point2.setX(coord.x);
                point2.setY(coord.y);
                point2.setZ(coord.z);

                // Create triangle
                aTriangle = new DTriangle(edge0, edge1, edge2);

                // Set edges triangles connection

                if (sdsEdges.getInt(e0Index, TINSchema.LEFT_TRIANGLE_FIELD) == theGID) {
                        edge0.setLeft(aTriangle);
                } else {
                        edge0.setRight(aTriangle);
                }
                if (sdsEdges.getInt(e1Index, TINSchema.LEFT_TRIANGLE_FIELD) == theGID) {
                        edge1.setLeft(aTriangle);
                } else {
                        edge1.setRight(aTriangle);
                }
                if (sdsEdges.getInt(e2Index, TINSchema.LEFT_TRIANGLE_FIELD) == theGID) {
                        edge2.setLeft(aTriangle);
                } else {
                        edge2.setRight(aTriangle);
                } // Set informations
                if (aTriangle != null) {
                        aTriangle.setGID(sdsTriangles.getInt(tIndex, TINSchema.GID));

                        if (requieredAdditionalFields) {
                                aTriangle.setHeight(sdsTriangles.getDouble(tIndex, TINSchema.HEIGHT_FIELD));
                                aTriangle.setProperty(sdsTriangles.getInt(tIndex, TINSchema.PROPERTY_FIELD));
                        }
                }

                return aTriangle;
        }

        /**
         * create DEdge structure with GDMS data
         *
         * @param aTriangle
         * @throws DriverException
         * @throws DelaunayError
         */
        private DEdge populateEdgeWithGDMS(int gid) throws DriverException, DelaunayError {
                DEdge theEdge = null;

                // Get GIDs
                long eIndex = getEdgeIndex(gid);
                int leftGID = sdsEdges.getInt(eIndex, TINSchema.LEFT_TRIANGLE_FIELD);
                int rightGID = sdsEdges.getInt(eIndex, TINSchema.RIGHT_TRIANGLE_FIELD);

                // Build left triangle if it exists
                DTriangle triangleLeft = null;

                if (leftGID >= 0) {
                        // Build it
                        triangleLeft = populateTriangleWithGDMS(leftGID);

                        // Get which edge has the edge GID

                        if (triangleLeft.getEdge(0).getGID() == gid) {
                                theEdge = triangleLeft.getEdge(0);
                        } else if (triangleLeft.getEdge(1).getGID() == gid) {
                                theEdge = triangleLeft.getEdge(1);
                        } else {
                                theEdge = triangleLeft.getEdge(2);
                        }
                }

                // Build right triangle if it exists
                DTriangle triangleRight = null;

                if (rightGID >= 0) {
                        // Build it
                        triangleRight = populateTriangleWithGDMS(rightGID);

                        // mix data with previous ones
                        if (theEdge == null) {
                                // Edge does not exist (edge on boundary

                                // Get which edge has the edge GID
                                if (triangleRight.getEdge(0).getGID() == gid) {
                                        theEdge = triangleRight.getEdge(0);
                                } else if (triangleRight.getEdge(1).getGID() == gid) {
                                        theEdge = triangleRight.getEdge(1);
                                } else {
                                        theEdge = triangleRight.getEdge(2);
                                }
                        } else {
                                // fing which has has the edge GID and change it
                                if (triangleRight.getEdge(0).getGID() == gid) {
                                        triangleRight.setEdge(0, theEdge);
                                } else if (triangleRight.getEdge(1).getGID() == gid) {
                                        triangleRight.setEdge(1, theEdge);
                                } else {
                                        triangleRight.setEdge(2, theEdge);
                                }
                                // Change points
                                for (int i = 0; i
                                        < 3; i++) {
                                        DEdge anEdge = triangleRight.getEdge(i);

                                        if (anEdge.getStartPoint().getGID() == theEdge.getStartPoint().getGID()) {
                                                anEdge.setStartPoint(theEdge.getStartPoint());
                                        }
                                        if (anEdge.getStartPoint().getGID() == theEdge.getEndPoint().getGID()) {
                                                anEdge.setStartPoint(theEdge.getEndPoint());
                                        }
                                        if (anEdge.getEndPoint().getGID() == theEdge.getStartPoint().getGID()) {
                                                anEdge.setEndPoint(theEdge.getStartPoint());
                                        }
                                        if (anEdge.getEndPoint().getGID() == theEdge.getEndPoint().getGID()) {
                                                anEdge.setEndPoint(theEdge.getEndPoint());
                                        }
                                }
                        }
                }

                // Set edge informations
                if (theEdge != null) {
                        theEdge.setLeft(triangleLeft);
                        theEdge.setRight(triangleRight);

                        theEdge.setGID(sdsEdges.getInt(eIndex, TINSchema.GID));

                        if (requieredAdditionalFields) {
                                theEdge.setHeight(sdsEdges.getDouble(eIndex, TINSchema.HEIGHT_FIELD));
                                theEdge.setProperty(sdsEdges.getInt(eIndex, TINSchema.PROPERTY_FIELD));
                        }
                }

                return theEdge;

        } // ----------------------------------------------------------------
        // METHODS
        // ----------------------------------------------------------------

        /**
         * Square distance between two elements
         * @param aPoint1
         * @param aPoint2
         * @return distance
         */
        private static double distanceBetween(DPoint aPoint1, DPoint aPoint2) {
                return (aPoint1.getX() - aPoint2.getX()) * (aPoint1.getX() - aPoint2.getX())
                        + (aPoint1.getY() - aPoint2.getY()) * (aPoint1.getY() - aPoint2.getY())
                        + (aPoint1.getZ() - aPoint2.getZ()) * (aPoint1.getZ() - aPoint2.getZ());
        }

        /**
         * Find the triangle thzt the point projects in (only x and y values are used).
         *
         * @param aPoint
         * @return theTriangleGID : the triangle the point projects in. null if there is none.
         */
        private DTriangle getSpottedTriangle(Geometry aPoint, DPoint initialPoint) {
                long countTriangles = 0;
                DTriangle found = null;

                try {
                        countTriangles = sdsTriangles.getRowCount();
                        DTriangle possibleTriangle = null;

                        // Process triangles until we find it
                        Geometry geom = null;

                        long i = 0;

                        while ((i < countTriangles) && (found == null)) {
                                // get geometry
                                geom = sdsTriangles.getGeometry(i);

                                if (geom instanceof MultiPolygon) {
                                        // we get a MultiPlolygon -> az triangle
                                        MultiPolygon mp = (MultiPolygon) geom;

                                        if (mp.intersects(aPoint)) {
                                                // Given point is in the triangle
                                                int gid = sdsTriangles.getInt(i, TINSchema.GID);
                                                found = populateTriangleWithGDMS(gid);

                                                double theSlope = getSlope(found, initialPoint);

                                                if (theSlope <= 0) {
                                                        possibleTriangle = found;
                                                        found = null;
                                                        i++;
                                                }

                                        } else {
                                                i++;
                                        }
                                } else {
                                        i++;
                                }
                        }
                        // Test if we founded a flat triangle
                        if (found == null) {
                                found = possibleTriangle;
                        }
                } catch (DelaunayError ex) {
                        logger.log(Level.SEVERE, "Can't retrieve the geometry.\n", ex);
                } catch (DriverException ex) {
                        logger.log(Level.SEVERE, "Can't retrieve the geometry.\n", ex);
                }

                return found;
        }

        /**
         * get edge slope from the point
         * @param anEdge
         * @param reference
         * @return
         */
        private static double getSlope(DEdge anEdge, DPoint reference) throws DelaunayError {
                double slope = 0.0;

                // Slope is the edge one
                slope = Math.abs(anEdge.getSlope());

                // if it is the point with the lowest Z, slope is negative. it is positive otherwise

                double minZ = anEdge.getStartPoint().getZ();

                if (minZ > anEdge.getEndPoint().getZ()) {
                        minZ = anEdge.getEndPoint().getZ();
                }
                if (Math.abs(minZ - reference.getZ()) < Tools.EPSILON) {
                        slope = -slope;
                }
                return slope;
        }

        /**
         * get triangle slope from the point
         * @param aTriangle
         * @param reference
         * @return
         */
        private static double getSlope(DTriangle aTriangle, DPoint reference) throws DelaunayError {
                double slope = 0.0;

                // Check if there is an intersection with one edhe
                DPoint theSlope = aTriangle.getSteepestVector();
                DEdge intersectedEdge = null;

                for (int i = 0; i < 3; i++) {
                        DEdge possibleEdge = aTriangle.getEdge(i);

                        if (!possibleEdge.isOnEdge(reference)) {
                                // Process only if point is not on that edge
                                DPoint intersectedPoint = getIntersection(reference, theSlope, possibleEdge);

                                if (intersectedPoint != null
                                        && !intersectedPoint.contains(possibleEdge.getStartPoint())
                                        && !intersectedPoint.contains(possibleEdge.getEndPoint())
                                        && !intersectedPoint.contains(reference)) {
                                        intersectedEdge = possibleEdge;
                                }
                        }
                }
                // If there is an intersection, the droplet can follow the triangle and slope is > 0
                if (intersectedEdge != null) {
                        slope = Math.abs(aTriangle.getSlope());
                } else {
                        slope = -Math.abs(aTriangle.getSlope());
                }
                return slope;
        }

        /**
         * Add an element in the list if it is not already present
         * 
         * @param elementToProcess
         * @param anElement
         */
        private static boolean isElementInArray(ArrayList<Element> elementToProcess, Element anElement) {
                int size = elementToProcess.size();
                int i = 0;
                boolean found = false;

                // look at all elements in the array
                while ((i < size) && (!found)) {
                        Element current = elementToProcess.get(i);

                        if (current.getGID() == anElement.getGID()) {
                                // the 2 elements have the save GID
                                if ((current instanceof DTriangle)
                                        && (anElement instanceof DTriangle)) {
                                        // both are triangles => they are the same
                                        found = true;
                                } else if ((current instanceof DEdge)
                                        && (anElement instanceof DEdge)) {
                                        // both are edges => they are the same
                                        found = true;
                                } else {
                                        // next element
                                        i++;
                                }
                        } else {
                                // next element
                                i++;
                        }
                }

                return found;
        }

        /**
         * return all elements to could be processed by turnAroundthePoint
         *
         * @param aPoint an extremity of an edge
         * @param anElement
         * @return the edge that leads to geatest slope
         * @throws DelaunayError
         */
        private ArrayList<Element> getElementsToProcess(DPoint aPoint, Element anElement, boolean withWallConstraint) throws DelaunayError, DriverException {
                ArrayList<Element> elementToProcess = new ArrayList<Element>();
                elementToProcess.add(anElement);

                int GID = aPoint.getGID();
                int currentElement = 0;

                while (currentElement < elementToProcess.size()) {
                        Element elementToTest = elementToProcess.get(currentElement);
                        currentElement++;

                        if (elementToTest instanceof DTriangle) {
                                // current element is a triangle
                                DTriangle aTriangle = (DTriangle) elementToTest;

                                // Check all edges : add edges that contains the point

                                for (int i = 0; i < 3; i++) {
                                        DEdge possibleEdge = aTriangle.getEdge(i);

                                        if ((possibleEdge.getStartPoint().getGID() == GID)
                                                || (possibleEdge.getEndPoint().getGID() == GID)) {
                                                // The edge contains aPoint => it is the one we look for.
                                                // generate full element
                                                if (!isElementInArray(elementToProcess, possibleEdge)) {
                                                        DEdge theEdge = populateEdgeWithGDMS(possibleEdge.getGID());
                                                        elementToProcess.add(theEdge);
                                                }
                                        }
                                }
                        } else {
                                // current element is an edge
                                DEdge anEdge = (DEdge) elementToTest;

                                if ((!anEdge.hasProperty(HydroProperties.WALL)) || (!withWallConstraint)) {
                                        // We stop on walls
                                        // Add left element
                                        DTriangle left = anEdge.getLeft();
                                        if (left != null) {
                                                if (!isElementInArray(elementToProcess, left)) {
                                                        DTriangle newElement = populateTriangleWithGDMS(left.getGID());
                                                        elementToProcess.add(newElement);

                                                }
                                        }

                                        // Add right element
                                        DTriangle right = anEdge.getRight();
                                        if (right != null) {
                                                if (!isElementInArray(elementToProcess, right)) {
                                                        DTriangle newElement = populateTriangleWithGDMS(right.getGID());
                                                        elementToProcess.add(newElement);
                                                }
                                        }
                                }
                        }
                }
                return elementToProcess;
        }

        /**
         * give the edge that leads to geatest slope when turning around aPoint
         *
         * @param aPoint an extremity of an edge
         * @param anElement
         * @return the edge that leads to geatest slope
         * @throws DelaunayError
         */
        private Element turnAroundthePoint(DPoint aPoint, DEdge anElement, DTriangle previousTriangle) throws DelaunayError, DriverException {
                // First get all elements around the point
                // anElement is an element we can start with
                Element selectedElement = null;         // the result
                ArrayList<Element> elementToProcess;
                Element startElement = anElement;

                boolean withWallConstraint = false;
                double maxSlope = 0;                    // Current value of geatest slope

                if (anElement.hasProperty(HydroProperties.RIVER)) {
                        // We are in a river
                        // We stay in the river
                        elementToProcess = getElementsToProcess(aPoint, anElement, false);

                        for (Element elementToTest : elementToProcess) {
                                if (elementToTest.hasProperty(HydroProperties.RIVER)) {
                                        if (elementToTest instanceof DTriangle) {
                                                // River goes to river, not in triangke
                                                DTriangle aTriangle = (DTriangle) elementToTest;

                                                double theSlope = getSlope(aTriangle, aPoint);

                                                if (theSlope > maxSlope) {
                                                        maxSlope = theSlope;
                                                        selectedElement = elementToTest;
                                                }
                                        } else if (anElement.getGID() != elementToTest.getGID()) {
                                                // Do not go back on the same element
                                                DEdge anEdge = (DEdge) elementToTest;

                                                double theSlope = getSlope(anEdge, aPoint);

                                                if (theSlope >= maxSlope) {
                                                        // We prefer edges to triangles when it is possible
                                                        maxSlope = theSlope;
                                                        selectedElement = elementToTest;
                                                }
                                        }
                                }
                        }
                } else if (anElement.hasProperty(HydroProperties.DITCH)) {
                        // We are in a ditch
                        // We may go in a river or in a ditch
                        elementToProcess = getElementsToProcess(aPoint, anElement, false);

                        for (Element elementToTest : elementToProcess) {
                                if ((elementToTest.hasProperty(HydroProperties.RIVER))
                                        || (elementToTest.hasProperty(HydroProperties.DITCH))) {
                                        if (elementToTest instanceof DTriangle) {
                                                // River goes to river, not in triangke
                                                DTriangle aTriangle = (DTriangle) elementToTest;

                                                double theSlope = getSlope(aTriangle, aPoint);

                                                if (theSlope > maxSlope) {
                                                        maxSlope = theSlope;
                                                        selectedElement = elementToTest;
                                                }
                                        } else if (anElement.getGID() != elementToTest.getGID()) {
                                                DEdge anEdge = (DEdge) elementToTest;

                                                double theSlope = getSlope(anEdge, aPoint);

                                                if ((theSlope >= maxSlope) && (theSlope > 0)) {
                                                        // We prefer edges to triangles when it is possible
                                                        maxSlope = theSlope;
                                                        selectedElement = elementToTest;
                                                }
                                        }
                                }
                        }
                } else if (anElement.hasProperty(HydroProperties.WALL)) {
                        // We were folowwing a wall
                        // Change start element
                        if (previousTriangle != null) {
                                startElement = previousTriangle;
                        }
                        withWallConstraint = true;

                }

                if (selectedElement == null) {
                        // No exit found => get the one we can with the greatest slope
                        elementToProcess = getElementsToProcess(aPoint, startElement, withWallConstraint);

                        int levelChange = 0;
                        maxSlope = 0;

                        // Now we process all elements in the array and we keep the one with th greastest slope
                        // Except that rivers goes to rivers and ditches goes to ditches or rivers
                        // Also, if there is a ditch or a river we prior go to then

                        for (Element elementToTest : elementToProcess) {
                                if (elementToTest instanceof DTriangle) {
                                        DTriangle aTriangle = (DTriangle) elementToTest;

                                        double theSlope = getSlope(aTriangle, aPoint);

                                        if ((theSlope > maxSlope) && (levelChange == 0)) {
                                                maxSlope = theSlope;
                                                selectedElement = elementToTest;
                                        }
                                } else if (anElement.getGID() != elementToTest.getGID()) {
                                        DEdge anEdge = (DEdge) elementToTest;

                                        double theSlope = getSlope(anEdge, aPoint);

                                        if (theSlope > 0) {
                                                if (anEdge.hasProperty(HydroProperties.RIVER)) {
                                                        // Go to river when it exists
                                                        if ((levelChange < 2) || (theSlope >= maxSlope)) {
                                                                maxSlope = theSlope;
                                                                selectedElement = elementToTest;
                                                                levelChange = 2;
                                                        }
                                                } else if (anEdge.hasProperty(HydroProperties.DITCH)) {
                                                        // Go to ditch when we are not in a river
                                                        if ((levelChange < 1) || ((levelChange == 1) && (theSlope >= maxSlope))) {
                                                                maxSlope = theSlope;
                                                                selectedElement = elementToTest;
                                                                levelChange = 1;
                                                        }
                                                } else if ((theSlope >= maxSlope) && (levelChange == 0)) {
                                                        // We prefer edges to triangles when it is possible
                                                        maxSlope = theSlope;
                                                        selectedElement = elementToTest;
                                                }
                                        }
                                }
                        }
                }
                if (maxSlope == 0) {
                        // If no slope => stop procesz
                        selectedElement = null;
                }
                return selectedElement;
        }

        /**
         * intersects the edge and the half-line that start in p1 directed with v1.
         * returns null if there is no intersection
         * NB : this method is not applied it the point is on  the edge => it should not happen
         *
         * @param p1 a point inside a triangle
         * @param v1 triangle's slope
         * @param anEdge an edge of the triangle
         * @return intersection
         * @throws DelaunayError
         */
        private static DPoint getIntersection(DPoint point1, DPoint v1, DEdge anEdge) throws DelaunayError {
                DPoint intersection = null;
                DPoint p3 = anEdge.getPointLeft();
                DPoint p4 = anEdge.getPointRight();
                DPoint p1 = point1;

                // (v1.x) t1 - (x4 - x3) t2 = (x3 - x1)
                // (v1.y) t1 - (y4 - y3) t2 = (y3 - y1)

                double deltaXO = v1.getX();
                double deltaXT = p4.getX() - p3.getX();
                double c1 = p3.getX() - p1.getX();
                double deltaYO = v1.getY();
                double deltaYT = p4.getY() - p3.getY();
                double c2 = p3.getY() - p1.getY();

                // d = (x4 - x3) (y2 - y1) - (x2 - x1) * (y4 - y3)
                double d = deltaXT * deltaYO - deltaYT * deltaXO;

                if (Math.abs(d) > Tools.EPSILON) {
                        //The two edges are not colinear.
                        // t1 = ((y3 - y1) (x4 - x3) - (x3 - x1) (y4 - y3)) / d
                        // t2 = ((v1.x) (y3 - y1) - (v1.y) (x3 - x1)) / d
                        double t1 = (c2 * deltaXT - c1 * deltaYT) / d;
                        double t2 = (deltaXO * c2 - deltaYO * c1) / d;

                        // There is no upper limit to t1 value
                        if ((-Tools.EPSILON <= t1) && (-Tools.EPSILON <= t2) && (t2 <= 1 + Tools.EPSILON)) {
                                // it intersects
                                if (t2 <= Tools.EPSILON) {
                                        intersection = p3;
                                } else if (t2 >= 1 - Tools.EPSILON) {
                                        intersection = p4;
                                } else if (t1 <= Tools.EPSILON) {
                                        intersection = p1;
                                } else {
                                        // We use t2 to compute values
                                        // x = x4 t2 + (1 - t2) x3
                                        // y = y4 t2 + (1 - t2) y3
                                        // z = z4 t2 + (1 - t2) z3
                                        double x = p4.getX() * t2 + (1 - t2) * p3.getX();
                                        double y = p4.getY() * t2 + (1 - t2) * p3.getY();
                                        double z = p4.getZ() * t2 + (1 - t2) * p3.getZ();
                                        intersection = new DPoint(x, y, z);
                                }
                        }
                } else {
                        //d==0 : the two edges are colinear
                        double test;

                        if (Math.abs(deltaXO) < Tools.EPSILON2) {
                                test = c1 / deltaXT - c2 / deltaYT;
                        } else {
                                test = c1 / deltaXO - c2 / deltaYO;
                        }
                        if (Math.abs(test) > Tools.EPSILON) {
                                //the two supporting lines are different
                                intersection = null;
                        } else {
                                // we have one supporting line
                                // So, p1 is between p3 and p4.
                                intersection = p1;
                        }
                }

                return intersection;
        }

        /**
         * Check if we can use a property
         * @param property
         * @return autorization
         */
        private boolean canUseProperty(int property) {
                return ((autorizedProperties & property) != 0);
        }

        // ----------------------------------------------------------------
        // MAIN METHOD
        // ----------------------------------------------------------------
        /**
         * add a point to the droplet path
         * Point is memorised only once. currentStagnation is incremented each time we have the same point
         * 
         * @param aPoint
         */
        private void addPointToDropletPath(DPoint aPoint) {
                if (lastPoint == null) {
                        // No previous point
                        theList.add(aPoint);
                        lastPoint = aPoint;
                        currentStagnation = 1;
                } else if (!lastPoint.contains(aPoint)) {
                        // The next point is not the previous one
                        theList.add(aPoint);
                        lastPoint = aPoint;
                        currentStagnation = 1;
                } else {
                        currentStagnation++;
                }
        }

        /**
         * Select next edge in the sewers
         * If lastEdge is null, it means we start with a sewer. Otherwise, we are following sewers
         * NB : we do not take into account the slope.
         * 
         * @param aPoint
         * @param lastEdge
         * @return
         */
        private Element selectNextSewer(DPoint aPoint, DEdge lastEdge) {
                Element theElement = null;

                if (lastEdge == null) {
                } else {
                }
                return theElement;

        }

        /**
         * Select next point in the sewers.
         * None of the two elements can be null
         *
         * @param anEdge
         * @param lastPoint
         * @return
         */
        private Element selectNextSewer(DEdge anEdge, DPoint lastPoint) {
                Element theElement = null;

                if (anEdge.getStartPoint().getGID() == lastPoint.getGID()) {
                        theElement = anEdge.getEndPoint();
                } else {
                        theElement = anEdge.getStartPoint();
                } // Take care we do not exit from sewers
                if ((!theElement.hasProperty(HydroProperties.SEWER_INPUT)) && (theElement.hasProperty(HydroProperties.SEWER_OUTPUT))) {
                        // The point exists from sewers
                        isInSewers = false;
                }
                return theElement;
        }

        /**
         * process a droplet on a Truangle.
         *
         * Current element is a triangle. We can be inside, or on an edge.
         * we go down the slope to find a point on an edge (even if start/end point)
         * next element is an edge
         *
         * @param aPoint
         * @param aTriangle
         * @return
         * @throws DelaunayError
         * @throws DriverException
         */
        private Element processDropletOnTriangle(DPoint aPoint, DTriangle aTriangle) throws DelaunayError, DriverException {
                Element theElement = null;

                // We take triangle's slope
                // We find the edge that intersects, and the intersection point
                DPoint intersection = null;
                DPoint theSlope = aTriangle.getSteepestVector();
                DEdge intersectedEdge = null;

                for (int i = 0; i
                        < 3; i++) {
                        DEdge possibleEdge = aTriangle.getEdge(i);

                        if (possibleEdge.isOnEdge(aPoint)) {
                                // aPoint is on the edge
                                // We keep the edge only if there is no other one
                                if (intersectedEdge == null) {
                                        // maybe we follow the edge
                                        intersectedEdge = possibleEdge;
                                        intersection = aPoint;
                                }
                        } else {
                                // Point is not on the edge
                                DPoint intersectedPoint = getIntersection(aPoint, theSlope, possibleEdge);

                                if (intersectedPoint != null) {
                                        // we've got an intersection
                                        if (intersectedEdge == null) {
                                                // none found yet
                                                intersectedEdge = possibleEdge;
                                                intersection = intersectedPoint;
                                        } else {
                                                // we take the one with the greater distance
                                                // in case we would have miss the isOnEdge function
                                                if (distanceBetween(intersectedPoint, aPoint) > distanceBetween(intersection, aPoint)) {
                                                        // this point is better
                                                        intersectedEdge = possibleEdge;
                                                        intersection = intersectedPoint;
                                                }
                                        }
                                }
                        }
                }

                if (intersection != null) {
                        // We memorise the point
                        intersection.setGID(-1);
                        addPointToDropletPath(intersection);

                        // set next element data
                        theElement = populateEdgeWithGDMS(intersectedEdge.getGID());

                } else {
                        // there is a problem
                        // there is no intersection with the triangle
                        // The triangle might be flat
                        theElement = null;
                }
                wallSide = EDGE_NO_WALL;

                return theElement;
        }

        /**
         * process a droplet on an edge.
         *
         * Current element is an Edge.
         * If we reach a river or a ditch, we follow it.
         * If we find a wall and we must follow it, we do it, depending on previous position of the droplet
         * At least, we can travel the edge to go to a triangle or follow the edge to go to lowest point.
         *
         * @param aPoint
         * @param anEdge
         * @param useHydroProperties
         * @return the element we go into
         * @throws DelaunayError
         */
        private Element processDropletOnAnEdge(final DPoint aPoint, final DEdge anEdge) throws DelaunayError {
                Element theElement = null;

                // current element is an edge
                // the next element follows the greatest slope
                Element nextElement = null;

                // First, get edge slope this value is positive or equal to zero

                double maxSlope = getSlope(anEdge, aPoint);
                DTriangle left = anEdge.getLeft();
                DTriangle right = anEdge.getRight();

                if (isInSewers) {
                        // We are in sewers - next element is a point
                        previousTriangle = null;
                        wallSide = EDGE_NO_WALL;
                        maxSlope = 1.0;
                        nextElement = selectNextSewer(anEdge, aPoint);
                } else if ((anEdge.hasProperty(HydroProperties.RIVER)) && (canUseProperty(HydroProperties.RIVER))) {
                        // do not have a look on triangles
                        previousTriangle = null;
                        wallSide = EDGE_NO_WALL;
                } else if ((anEdge.hasProperty(HydroProperties.DITCH)) && (canUseProperty(HydroProperties.DITCH))) {
                        // do not have a look on triangles
                        previousTriangle = null;
                        wallSide = EDGE_NO_WALL;
                } else if ((anEdge.hasProperty(HydroProperties.WALL)) && (canUseProperty(HydroProperties.WALL))) {
                        // following the wall
                        if (previousTriangle != null) {
                                // We arrived on the edge with a triangle
                                if (right == null) {
                                        // Come from left
                                        wallSide = EDGE_WALL_LEFT;
                                } else if (left == null) {
                                        // Come from right
                                        wallSide = EDGE_WALL_RIGHT;
                                } else if (previousTriangle.getGID() == left.getGID()) {
                                        // Come from left
                                        wallSide = EDGE_WALL_LEFT;
                                } else {
                                        // Come from right
                                        wallSide = EDGE_WALL_RIGHT;
                                }
                        } else {
                                // We come from another edge (last one was a point)
                                // Define the new value for previousTriangle
                                if (wallSide == EDGE_WALL_LEFT) {
                                        // Stay left
                                        if (anEdge.getStartPoint().getZ() > anEdge.getEndPoint().getZ()) {
                                                previousTriangle = left;
                                        } else {
                                                previousTriangle = right;
                                        }
                                } else {
                                        // Stay right
                                        if (anEdge.getStartPoint().getZ() > anEdge.getEndPoint().getZ()) {
                                                previousTriangle = right;
                                        } else {
                                                previousTriangle = left;
                                        }
                                }
                        }
                        // previousTriangle Always have a value => we can go to the point
                } else {
                        // Check the 2 triangle around the edge
                        if (left != null) {
                                // get slope
                                // slope is positive only if droplet can go doan the triangle
                                double slope = getSlope(left, aPoint);

                                if (slope > maxSlope) {
                                        maxSlope = slope;
                                        nextElement = left;
                                }
                        }

                        if (right != null) {
                                // get slope
                                // slope is positive only if droplet can go doan the triangle
                                double slope = getSlope(right, aPoint);

                                if (slope > maxSlope) {
                                        maxSlope = slope;
                                        nextElement = right;
                                }
                        }
                        previousTriangle = null;
                        wallSide = EDGE_NO_WALL;
                }

                if (maxSlope > 0) {
                        if (nextElement == null) {
                                // next step is on the edge
                                // => follow the edge
                                DPoint pt;

                                if (anEdge.getStartPoint().getZ() > anEdge.getEndPoint().getZ()) {
                                        pt = anEdge.getEndPoint();
                                } else {
                                        pt = anEdge.getStartPoint();
                                }

                                // We memorise the lowest point
                                addPointToDropletPath(pt);
                                theElement = pt;
                        } else {
                                // Next element is a triangle
                                theElement = nextElement;
                                // We stay on the same point => aPoint does not change
                        }
                } else {
                        // Slope is flat => stop process
                        theElement = null;
                }

                return theElement;
        }

        /**
         * process a droplet on a Point.
         *
         * Current element is a point.
         * If there is a sewer entry at this point, we stop.
         * Otherwise, we turn around the point, starting at previous element.
         * We go to the greatests slope (triangle / edge)
         *
         * @param aPoint
         * @param lastEdge
         * @return the element we go into.
         * @throws DelaunayError
         * @throws DriverException
         */
        private Element processDropletOnPoint(DPoint aPoint, DEdge lastEdge) throws DelaunayError, DriverException {
                Element theElement = null;

                if (((aPoint.hasProperty(HydroProperties.SEWER_INPUT)) || (isInSewers))
                        && (canUseProperty(HydroProperties.SEWER_INPUT))) {
                        // go into sewers
                        isInSewers = true;
                        wallSide = EDGE_NO_WALL;

                        // try to find the point connected that is in sewers
                        theElement = selectNextSewer(aPoint, lastEdge);
                } else {
                        // First, we get the element we come from. It might be an edge.
                        Element selectedElement = turnAroundthePoint(aPoint, lastEdge, previousTriangle);

                        if (selectedElement != null) {
                                // We go to a next edge
                                theElement = selectedElement;
                        } else {
                                // End of process: no successor
                                theElement = null;
                        }
                }
                return theElement;
        }

        /**
         * Droplet follower
         *
         * @param initialPoint
         * @param useHydroProperties
         * @throws DelaunayError
         */
        private void dropletFollows(Geometry initialGeometry) throws DriverException, DelaunayError {
                // First we have to find the triangle that contains the point
                // Then
                // - if we have a triangle, we foloow the slope to the next edge
                //      So, next element is the edge we intersect
                // - if we are on an edge, we go to the greatest slope
                //      + if it is a triangle, next element is the triangle
                //      + if it is the edge, we go down the edge. Next element is the lowest point
                //		NB : Except if the point has some properties (RIVER, DITCH, WALL, ...)
                // - if we are on a point, we look for the greatest slope
                //      + if is is a triangle, we go to the triangle
                //      + if it is an edge, we go to the edge
                //      + if there is none, it is ended
                //      NB : to turn around the point, we MUST have a connected element to the point (edge / triangle)
                //		NB2 : if point is a SEWER_INPUT, we stop
                //
                // It may happen that we stay on the same same point. That means we are in a hole => we stop iterations
                // if we find such a point

                theList = new ArrayList<DPoint>();
                lastPoint = null;

                // Find the point on the surface
                DPoint initialPoint = TINFeatureFactory.createDPoint(initialGeometry);
                DTriangle aTriangle = getSpottedTriangle(initialGeometry, initialPoint);

                if (aTriangle == null) {
                        // Droplet stays on initial point : it is outside mesh
                        initialPoint.setGID(-1);
                        addPointToDropletPath(initialPoint);

                } else {
                        // point is on the mesh, in a triangle
                        DEdge anEdge = null;            // doplet on an edge

                        // Project the point on the surface and memorise it
                        DPoint aPoint = new DPoint(initialPoint.getX(), initialPoint.getY(), aTriangle.interpolateZ(initialPoint));
                        aPoint.setGID(-1);
                        addPointToDropletPath(aPoint);

                        // The current element we are in
                        Element theElement = aTriangle;         // current processed element
                        Element lastElement = null;             // last Element we were in
                        previousTriangle = null;                // to manage walls we are following
                        wallSide = EDGE_NO_WALL;                  // To know on which side of the wall we are
                        isInSewers = false;

                        while ((theElement != null) && (currentStagnation < MAX_STAGNATION)) {
                                // we've got a Point (aPoint)
                                // and the element we are in (theElement)
                                // theElement can be a triangle, an edge or a point

                                if (theElement.hasProperty(endingProperties)) {
                                        // If we reach an ending property, we stop
                                        theElement = null;
                                } else if (theElement instanceof DTriangle) {
                                        // current element is a triangle
                                        aTriangle = (DTriangle) theElement;
                                        lastElement = theElement;
                                        theElement = processDropletOnTriangle(aPoint, aTriangle);
                                        aPoint = theList.get(theList.size() - 1);
                                        previousTriangle = aTriangle;
                                } else if (theElement instanceof DEdge) {
                                        // current element is an edge
                                        // the next element follows the greatest slope
                                        anEdge = (DEdge) theElement;
                                        lastElement = theElement;
                                        theElement = processDropletOnAnEdge(aPoint, anEdge);
                                        aPoint = theList.get(theList.size() - 1);
                                } else {
                                        // Current element is a point
                                        // the point comes from an edge. It CANNOT come from a triangle
                                        // We turn around aPoint to select the edge that leads to greatest slope
                                        aPoint = (DPoint) theElement;
                                        theElement = processDropletOnPoint(aPoint, (DEdge) lastElement);
                                        lastElement = aPoint;
                                        previousTriangle = null;
                                }
                        }
                }

        }

        /**
         * A method to check if the datasource contains two fields :  property and height. The field is used to tag the TIN feature
         * @param tables
         * @throws DriverException
         */
        private void checkMetadata(DataSource[] tables) throws DriverException {

                for (int i = 0; i
                        < tables.length - 1; i++) {
                        DataSource dataSource = tables[i];
                        Metadata md = dataSource.getMetadata();

                        if ((md.getFieldIndex(TINSchema.PROPERTY_FIELD) == -1) || (md.getFieldIndex(TINSchema.HEIGHT_FIELD) == -1)) {
                                throw new IllegalArgumentException("The table " + dataSource.getName() + " must contains two fields  : property and height");
                        }

                }
        }
}
