package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Coordinate;
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
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.sql.customQuery.CustomQuery;
import org.gdms.sql.customQuery.TableDefinition;
import org.gdms.sql.function.Argument;
import org.gdms.sql.function.Arguments;
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
 *
 *
 * @author kwyhr
 */
public abstract class DropletFollower implements CustomQuery {

        private static final Logger logger = Logger.getLogger(DropletFollower.class.getName());
        // Table informations to navigate
        private SpatialDataSourceDecorator sdsPoints = null;
        private SpatialDataSourceDecorator sdsEdges = null;
        private SpatialDataSourceDecorator sdsTriangles = null;
        private static final int maxStagnation = 10;       // to stop iterations on the same point

        @Override
        public final ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {
                if (tables.length < 3) {
                        // There MUST be at least 3 tables
                        throw new ExecutionException("needs points, edges and triangles.");
                } else {
                        ArrayList<DPoint> result = null;

                        try {
                                // Set informations from tables and Values
                                populateData(dsf, pm, tables);
                                Geometry testPoint = getInitiaPoint(values[0]);

                                // process path
                                result = dropletFollows(testPoint);

                        } catch (DriverException ex) {
                                logger.log(Level.SEVERE, "There has been an error while opening a table, or counting its lines.\n", ex);
                        } catch (DelaunayError ex) {
                                logger.log(Level.SEVERE, null, ex);
                        }

                        // close drivers
                        closeData();

                        // create value
                        if (result != null) {
                                try {
                                        return createDataSource(dsf, result);
                                } catch (DriverException ex) {
                                        logger.log(Level.SEVERE, "There has been an error while opening a table, or counting its lines.\n", ex);
                                }
                        }
                }
                return null;
        }

        @Override
        public final Metadata getMetadata(Metadata[] tables) throws DriverException {
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY)},
                        new String[]{TINSchema.GEOM_FIELD});
                return md;
        }

        @Override
        public final TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};
        }

        @Override
        public final Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY)};
        }

        /**
         * save Results in a file. This method is the one that will be overriden by the children.
         *
         * @param pathName
         * @param result
         */
        protected abstract DiskBufferDriver createDataSource(DataSourceFactory dsf, ArrayList<DPoint> result) throws DriverException;

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
                        throw new ExecutionException("Unable to create index.",ex);
                } catch (NoSuchTableException ex) {
                        throw new ExecutionException("Unable to create index.",ex);
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
        private void closeData() {
                closeDecorator(this.sdsPoints);
                this.sdsPoints = null;

                closeDecorator(this.sdsEdges);
                this.sdsEdges = null;

                closeDecorator(this.sdsTriangles);
                this.sdsTriangles = null;
        }

        /**
         * Get initia point from values
         * @param values
         * @return
         * @throws ExecutionException
         */
        private Geometry getInitiaPoint(Value theValue) throws ExecutionException {
                Geometry testPoint = theValue.getAsGeometry();
                if (!testPoint.isValid()) {
                        throw new ExecutionException("invalid point geometry.");
                } else if (!testPoint.getGeometryType().equals("Point")) {
                        throw new ExecutionException("invalid point geometry.");
                }
                return testPoint;
        }

        // ----------------------------------------------------------------
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
                }

                // Set informations
                if (aTriangle != null) {
                        aTriangle.setGID(sdsTriangles.getInt(tIndex, TINSchema.GID));
                        aTriangle.setHeight(sdsTriangles.getDouble(tIndex, TINSchema.HEIGHT_FIELD));
                        aTriangle.setProperty(sdsTriangles.getInt(tIndex, TINSchema.PROPERTY_FIELD));
                        aTriangle.setExternalGID(sdsTriangles.getInt(tIndex, TINSchema.GID_SOURCE_FIELD));
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
                                for (int i = 0; i < 3; i++) {
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
                        theEdge.setHeight(sdsEdges.getDouble(eIndex, TINSchema.HEIGHT_FIELD));
                        theEdge.setProperty(sdsEdges.getInt(eIndex, TINSchema.PROPERTY_FIELD));
                        theEdge.setExternalGID(sdsEdges.getInt(eIndex, TINSchema.GID_SOURCE_FIELD));
                }

                return theEdge;
        }

        // ----------------------------------------------------------------
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

                if (intersectedEdge != null) {
                        slope = Math.abs(aTriangle.getSlope());
                } else {
                        slope = -Math.abs(aTriangle.getSlope());
                }
                return slope;
        }

        /**
         * Add an element in the list if it is not in
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
                                        if ((possibleEdge.getStartPoint().getGID() == aPoint.getGID())
                                                || (possibleEdge.getEndPoint().getGID() == aPoint.getGID())) {
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
                                        if (!isElementInArray(elementToProcess, left)) {
                                                DTriangle newElement = populateTriangleWithGDMS(left.getGID());
                                                elementToProcess.add(newElement);
                                        }

                                        // Add right element
                                        DTriangle right = anEdge.getRight();
                                        if (!isElementInArray(elementToProcess, right)) {
                                                DTriangle newElement = populateTriangleWithGDMS(right.getGID());
                                                elementToProcess.add(newElement);
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

        // ----------------------------------------------------------------
        // MAIN METHOD
        // ----------------------------------------------------------------
        /**
         * Droplet follower
         *
         * @param initialPoint
         * @return thePath : list of DEdge that defines the path the droplet follows
         * @throws DelaunayError
         */
        private ArrayList<DPoint> dropletFollows(Geometry initialGeometry) throws DriverException, DelaunayError {
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

                ArrayList<DPoint> theList = new ArrayList<DPoint>();

                // Find the point on the surface
                DPoint initialPoint = TINFeatureFactory.createDPoint(initialGeometry);
                DTriangle aTriangle = this.getSpottedTriangle(initialGeometry, initialPoint);
                if (aTriangle != null) {
                        // point is on the mesh, in a triangle
                        DEdge anEdge = null;            // doplet on an edge
                        DPoint aPoint = null;           // doplet on a point
                        DPoint lastPoint = null;        // last memorized point

                        // Project the point on the surface
                        double zValue = aTriangle.interpolateZ(initialPoint);
                        aPoint = new DPoint(initialPoint.getX(), initialPoint.getY(), zValue);
                        aPoint.setGID(-1);

                        // memorise it
                        theList.add(aPoint);
                        lastPoint = aPoint;
                        int stagnation = 1;     // we count how many times we are on the same point

                        // The current element we are in
                        Element theElement = aTriangle;     // current processed element
                        Element lastElement = null;         // last Element we were in
                        DTriangle previousTriangle = null;  // to manawe walls we we are folowwing it
                        int wallSide = 0;

                        boolean ended = false;
                        while (!ended) {
                                // we've got a Point (aPoint)
                                // and the element we are in (theElement)
                                // theElement can be a triangle, an edge or a point

                                if (theElement instanceof DTriangle) {
                                        // current element is a triangle
                                        // We can be inside, or on an edge
                                        // we go down the slope to find a point on an edge (even if start/end point)
                                        // next element is an edge
                                        aTriangle = (DTriangle) theElement;

                                        // We take triangle's slope
                                        // We find the edge that intersects, and the intersection point
                                        DPoint intersection = null;
                                        DPoint theSlope = aTriangle.getSteepestVector();
                                        DEdge intersectedEdge = null;
                                        for (int i = 0; i < 3; i++) {
                                                DEdge possibleEdge = aTriangle.getEdge(i);
                                                if (possibleEdge.isOnEdge(aPoint)) {
                                                        // aPoint is on the edge
                                                        if (intersectedEdge == null) {
                                                                // maybe we follow the edge
                                                                intersectedEdge = possibleEdge;
                                                                intersection = aPoint;
                                                        } else {
                                                                // There is another intersection. Keep it
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

                                        lastElement = theElement;
                                        if (intersection != null) {
                                                // We memorise the point
                                                aPoint = intersection;
                                                if (!lastPoint.contains(aPoint)) {
                                                        // The next point is not the previous one
                                                        theList.add(aPoint);
                                                        lastPoint = aPoint;
                                                        stagnation = 1;
                                                } else {
                                                        stagnation++;
                                                }
                                                // set next element data
                                                theElement = populateEdgeWithGDMS(intersectedEdge.getGID());
                                                previousTriangle = aTriangle;
                                                wallSide = 0;
                                        } else {
                                                // there is a problem
                                                // there is no intersection with the triangle
                                                // The triangle might be flat
                                                theElement = null;
                                                ended = true;
                                        }
                                } else if (theElement instanceof DEdge) {
                                        // current element is an edge
                                        // the next element follows the greatest slope
                                        anEdge = (DEdge) theElement;
                                        Element nextElement = null;

                                        // First, get edge slope this value is positive or equal to zero
                                        double maxSlope = getSlope(anEdge, aPoint);
                                        DTriangle left = anEdge.getLeft();
                                        DTriangle right = anEdge.getRight();

                                        if (anEdge.hasProperty(HydroProperties.RIVER)) {
                                                // do not have a look on triangles
                                                previousTriangle = null;
                                                wallSide = 0;
                                        } else if (anEdge.hasProperty(HydroProperties.DITCH)) {
                                                // do not have a look on triangles
                                                previousTriangle = null;
                                                wallSide = 0;
                                        } else if (anEdge.hasProperty(HydroProperties.WALL)) {
                                                // following the wall
                                                if (previousTriangle != null) {
                                                        // We arrived on the edge with a triangle
                                                        if (right == null) {
                                                                // Come from left
                                                                wallSide = 1;
                                                        } else if (left == null) {
                                                                // Come from right
                                                                wallSide = 2;
                                                        } else if (previousTriangle.getGID() == left.getGID()) {
                                                                // Come from left
                                                                wallSide = 1;
                                                        } else {
                                                                // Come from right
                                                                wallSide = 2;
                                                        }
                                                } else {
                                                        // We come from another edge (last one was a point)
                                                        // Define the new value for previousTriangle
                                                        if (wallSide == 1) {
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
                                                wallSide = 0;
                                        }

                                        lastElement = theElement;
                                        if (maxSlope > 0) {
                                                if (nextElement == null) {
                                                        // next step is on the edge
                                                        // => follow the edge
                                                        if (anEdge.getStartPoint().getZ() > anEdge.getEndPoint().getZ()) {
                                                                aPoint = anEdge.getEndPoint();
                                                        } else {
                                                                aPoint = anEdge.getStartPoint();
                                                        }

                                                        // We memorise the lowest point
                                                        if (!lastPoint.contains(aPoint)) {
                                                                // The next point is not the previous one
                                                                theList.add(aPoint);
                                                                lastPoint = aPoint;
                                                                stagnation = 1;
                                                        } else {
                                                                stagnation++;
                                                        }
                                                        theElement = aPoint;
                                                } else {
                                                        // Next element is a triangle
                                                        theElement = nextElement;
                                                        // We stay on the same point => aPoint does not change
                                                }
                                        } else {
                                                // Slope is flat => stop process
                                                theElement = null;
                                                ended = true;
                                        }

                                } else {
                                        // Current element is a point
                                        // the point comes from an edge. It CANNOT come from a triangle
                                        // We turn around aPoint to select the edge that leads to greatest slope
                                        aPoint = (DPoint) theElement;

                                        if (aPoint.hasProperty(HydroProperties.SEWER_INPUT)) {
                                                // go in a hole
                                                theElement = null;
                                                ended = true;
                                        } else {
                                                // First, we get the element we come from. It might be an edge.
                                                Element selectedElement;
                                                selectedElement = turnAroundthePoint(aPoint, (DEdge) lastElement, previousTriangle);
                                                lastElement = theElement;
                                                if (selectedElement != null) {
                                                        // We go to a next edge
                                                        theElement = selectedElement;
                                                        previousTriangle = null;
                                                } else {
                                                        // End of process: no successor
                                                        theElement = null;
                                                        ended = true;
                                                }
                                        }
                                }
                                if (stagnation > maxStagnation) {
                                        // We are on the same point since maxStagnation iterations
                                        // => we are in a hole -> STOP
                                        ended = true;
                                }
                        }
                }

                return theList;
        }
}
