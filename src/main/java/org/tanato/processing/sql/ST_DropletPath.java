package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.gdms.GdmsWriter;
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
public class ST_DropletPath implements CustomQuery {

        private static final Logger logger = Logger.getLogger(ST_DropletPath.class.getName());
        // Table informations to navigate
        private SpatialDataSourceDecorator sds_points = null;
        private SpatialDataSourceDecorator sds_edges = null;
        private SpatialDataSourceDecorator sds_triangles = null;
        private HashMap<Integer, Long> pointsGID2Index = null;
        private HashMap<Integer, Long> edgesGID2Index = null;
        private HashMap<Integer, Long> trianglesGID2Index = null;
        private final int maxStagnation = 10;       // to stop iterations on the same point

        @Override
        public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {
                ObjectDriver driver = null;
                if (tables.length < 3) {
                        // There MUST be at least 3 tables
                        throw new ExecutionException("needs points, edges and triangles.");
                } else if (values.length != 2) {
                        // There MUST be 2 value : the start point and the pathName
                        throw new ExecutionException("needs 1 start point and a pathName.");
                } else {
                        try {
                                // Get informations from tables
                                populateData(tables);

                                // Get argument : a point geometry and the pathName
                                Geometry testPoint = values[0].getAsGeometry();
                                if (!testPoint.isValid()) {
                                        throw new ExecutionException("invalid point geometry.");
                                } else if (!testPoint.getGeometryType().equals("Point")) {
                                        throw new ExecutionException("invalid point geometry.");
                                }
                                String pathName = values[1].getAsString();

                                // process path
                                ArrayList<DPoint> Result = dropletFollows(testPoint);

                                // create value
                                savePath(dsf, pathName, Result);

                        } catch (DriverException ex) {
                                logger.log(Level.SEVERE, "There has been an error while opening a table, or counting its lines.\n", ex);
                        } catch (DelaunayError ex) {
                                logger.log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                                logger.log(Level.SEVERE, "Failed to write the file.\n", ex);
                        }
                }
                return driver;
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                return null;
        }

        @Override
        public TableDefinition[] getTablesDefinitions() {
                return new TableDefinition[]{TableDefinition.GEOMETRY, TableDefinition.GEOMETRY, TableDefinition.GEOMETRY};
        }

        @Override
        public String getName() {
                return "ST_DropletPath";
        }

        @Override
        public String getDescription() {
                return "get the path a droplet will follow on the TIN.";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_DropletPath(pointGeom, pathName) FROM out_point, out_edges, out_triangles";
        }

        @Override
        public Arguments[] getFunctionArguments() {
                return new Arguments[]{new Arguments(Argument.GEOMETRY,Argument.STRING)};
        }

        // ----------------------------------------------------------------
        // UTILITIES
        // ----------------------------------------------------------------
        /**
         * get Table Informations
         * @param tables
         * @throws ExecutionException
         * @throws DriverException
         */
        private void populateData(DataSource[] tables) throws ExecutionException, DriverException {
                // First is points
                DataSource ds_points = tables[0];
                sds_points = new SpatialDataSourceDecorator(ds_points);
                sds_points.open();
                long count_points = sds_points.getRowCount();
                if (count_points < 3) {
                        throw new ExecutionException("not enough points.");
                }
                pointsGID2Index = new HashMap<Integer, Long>();
                for (long i = 0; i < count_points; i++) {
                        int GID = sds_points.getInt(i, TINSchema.GID);
                        pointsGID2Index.put(GID, new Long(i));
                }

                // second is edges
                DataSource ds_edges = tables[1];
                sds_edges = new SpatialDataSourceDecorator(ds_edges);
                sds_edges.open();
                long count_edges = sds_edges.getRowCount();
                if (count_edges < 3) {
                        throw new ExecutionException("not enough edges.");
                }
                edgesGID2Index = new HashMap<Integer, Long>();
                for (long i = 0; i < count_edges; i++) {
                        int GID = sds_edges.getInt(i, TINSchema.GID);
                        edgesGID2Index.put(GID, new Long(i));
                }

                // third is triangles
                DataSource ds_triangles = tables[2];
                sds_triangles = new SpatialDataSourceDecorator(ds_triangles);
                sds_triangles.open();
                long count_triangles = sds_triangles.getRowCount();
                if (count_triangles < 1) {
                        throw new ExecutionException("not enough triangles.");
                }
                trianglesGID2Index = new HashMap<Integer, Long>();
                for (long i = 0; i < count_triangles; i++) {
                        int GID = sds_triangles.getInt(i, TINSchema.GID);
                        trianglesGID2Index.put(GID, new Long(i));
                }
        }

        /**
         * create DTriangle structure with GDMS data
         * 
         * @param aTriangle
         * @throws DriverException
         * @throws DelaunayError
         */
        private DTriangle populateTriangleWithGDMS(int GID) throws DriverException, DelaunayError {
                DTriangle aTriangle;
                long tIndex = trianglesGID2Index.get(GID);

                // Create edges
                DEdge Edge0 = new DEdge();
                int E_GID_0 = sds_triangles.getInt(tIndex, TINSchema.EDGE_0_GID_FIELD);
                long e0Index = edgesGID2Index.get(E_GID_0);
                Edge0.setGID(E_GID_0);

                DEdge Edge1 = new DEdge();
                int E_GID_1 = sds_triangles.getInt(tIndex, TINSchema.EDGE_1_GID_FIELD);
                long e1Index = edgesGID2Index.get(E_GID_1);
                Edge1.setGID(E_GID_1);

                DEdge Edge2 = new DEdge();
                int E_GID_2 = sds_triangles.getInt(tIndex, TINSchema.EDGE_2_GID_FIELD);
                long e2Index = edgesGID2Index.get(E_GID_2);
                Edge2.setGID(E_GID_2);

                // Create Points

                DPoint Point0 = new DPoint();
                Edge0.setStartPoint(Point0);
                int P_GID_0 = sds_edges.getInt(e0Index, TINSchema.STARTPOINT_NODE_FIELD);
                long p0Index = pointsGID2Index.get(P_GID_0);
                Point0.setGID(P_GID_0);

                DPoint Point1 = new DPoint();
                Edge0.setEndPoint(Point1);
                int P_GID_1 = sds_edges.getInt(e0Index, TINSchema.ENDPOINT_NODE_FIELD);
                long p1Index = pointsGID2Index.get(P_GID_1);
                Point1.setGID(P_GID_1);

                int P_GID_2;
                DPoint Point2 = new DPoint();
                if (sds_edges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD) == P_GID_0) {
                        // Edge1 is 0-2
                        Edge1.setStartPoint(Point0);
                        Edge1.setEndPoint(Point2);
                        // Edge2 cannot contain 0
                        if (sds_edges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == P_GID_1) {
                                // Edge2 is 1-2
                                Edge2.setStartPoint(Point1);
                                Edge2.setEndPoint(Point2);
                        } else {
                                // Edge2 is 2-1
                                Edge2.setStartPoint(Point2);
                                Edge2.setEndPoint(Point1);
                        }
                        P_GID_2 = sds_edges.getInt(e1Index, TINSchema.ENDPOINT_NODE_FIELD);

                } else if (sds_edges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD) == P_GID_1) {
                        // Edge1 is 1-2
                        Edge1.setStartPoint(Point1);
                        Edge1.setEndPoint(Point2);
                        // Edge2 cannot contain 1
                        if (sds_edges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == P_GID_0) {
                                // Edge2 is 0-2
                                Edge2.setStartPoint(Point0);
                                Edge2.setEndPoint(Point2);
                        } else {
                                // Edge2 is 2-0
                                Edge2.setStartPoint(Point2);
                                Edge2.setEndPoint(Point0);
                        }
                        P_GID_2 = sds_edges.getInt(e1Index, TINSchema.ENDPOINT_NODE_FIELD);

                } else if (sds_edges.getInt(e1Index, TINSchema.ENDPOINT_NODE_FIELD) == P_GID_0) {
                        // Edge1 is 2-0
                        Edge1.setStartPoint(Point2);
                        Edge1.setEndPoint(Point0);
                        // Edge2 cannot contain 0
                        if (sds_edges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == P_GID_1) {
                                // Edge2 is 1-2
                                Edge2.setStartPoint(Point1);
                                Edge2.setEndPoint(Point2);
                        } else {
                                // Edge2 is 2-1
                                Edge2.setStartPoint(Point2);
                                Edge2.setEndPoint(Point1);
                        }
                        P_GID_2 = sds_edges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD);

                } else {
                        // Edge1 is 2-1
                        Edge1.setStartPoint(Point2);
                        Edge1.setEndPoint(Point1);
                        // Edge2 cannot contain 1
                        if (sds_edges.getInt(e2Index, TINSchema.STARTPOINT_NODE_FIELD) == P_GID_0) {
                                // Edge2 is 0-2
                                Edge2.setStartPoint(Point0);
                                Edge2.setEndPoint(Point2);
                        } else {
                                // Edge2 is 2-0
                                Edge2.setStartPoint(Point2);
                                Edge2.setEndPoint(Point0);
                        }
                        P_GID_2 = sds_edges.getInt(e1Index, TINSchema.STARTPOINT_NODE_FIELD);
                }
                long p2Index = pointsGID2Index.get(P_GID_2);
                Point2.setGID(P_GID_2);

                // Set points location
                Coordinate coord;
                coord = sds_points.getGeometry(p0Index).getCoordinate();
                Point0.setX(coord.x);
                Point0.setY(coord.y);
                Point0.setZ(coord.z);

                coord = sds_points.getGeometry(p1Index).getCoordinate();
                Point1.setX(coord.x);
                Point1.setY(coord.y);
                Point1.setZ(coord.z);

                coord = sds_points.getGeometry(p2Index).getCoordinate();
                Point2.setX(coord.x);
                Point2.setY(coord.y);
                Point2.setZ(coord.z);

                // Create triangle
                aTriangle = new DTriangle(Edge0, Edge1, Edge2);

                // Set edges triangles connection
                if (sds_edges.getInt(e0Index, TINSchema.LEFT_TRIANGLE_FIELD) == GID) {
                        Edge0.setLeft(aTriangle);
                } else {
                        Edge0.setRight(aTriangle);
                }
                if (sds_edges.getInt(e1Index, TINSchema.LEFT_TRIANGLE_FIELD) == GID) {
                        Edge1.setLeft(aTriangle);
                } else {
                        Edge1.setRight(aTriangle);
                }
                if (sds_edges.getInt(e2Index, TINSchema.LEFT_TRIANGLE_FIELD) == GID) {
                        Edge2.setLeft(aTriangle);
                } else {
                        Edge2.setRight(aTriangle);
                }

                // Set informations
                if (aTriangle != null) {
                        aTriangle.setGID(sds_triangles.getInt(tIndex, TINSchema.GID));
                        aTriangle.setHeight(sds_triangles.getDouble(tIndex, TINSchema.HEIGHT_FIELD));
                        aTriangle.setProperty(sds_triangles.getInt(tIndex, TINSchema.PROPERTY_FIELD));
                        aTriangle.setExternalGID(sds_triangles.getInt(tIndex, TINSchema.GID_SOURCE_FIELD));
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
        private DEdge populateEdgeWithGDMS(int GID) throws DriverException, DelaunayError {
                DEdge theEdge = null;

                // Get GIDs
                long eIndex = edgesGID2Index.get(GID);
                int leftGID = sds_edges.getInt(eIndex, TINSchema.LEFT_TRIANGLE_FIELD);
                int rightGID = sds_edges.getInt(eIndex, TINSchema.RIGHT_TRIANGLE_FIELD);

                int P_GID_0 = sds_edges.getInt(eIndex, TINSchema.STARTPOINT_NODE_FIELD);
                int P_GID_1 = sds_edges.getInt(eIndex, TINSchema.ENDPOINT_NODE_FIELD);

                // Build left triangle if it exists
                DTriangle triangleLeft = null;
                if (leftGID >= 0) {
                        // Build it
                        triangleLeft = populateTriangleWithGDMS(leftGID);

                        // Get which edge has the edge GID
                        if (triangleLeft.getEdge(0).getGID() == GID) {
                                theEdge = triangleLeft.getEdge(0);
                        } else if (triangleLeft.getEdge(1).getGID() == GID) {
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
                                if (triangleRight.getEdge(0).getGID() == GID) {
                                        theEdge = triangleRight.getEdge(0);
                                } else if (triangleRight.getEdge(1).getGID() == GID) {
                                        theEdge = triangleRight.getEdge(1);
                                } else {
                                        theEdge = triangleRight.getEdge(2);
                                }
                        } else {
                                // fing which has has the edge GID and change it
                                if (triangleRight.getEdge(0).getGID() == GID) {
                                        triangleRight.setEdge(0, theEdge);
                                } else if (triangleRight.getEdge(1).getGID() == GID) {
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

                        theEdge.setGID(sds_edges.getInt(eIndex, TINSchema.GID));
                        theEdge.setHeight(sds_edges.getDouble(eIndex, TINSchema.HEIGHT_FIELD));
                        theEdge.setProperty(sds_edges.getInt(eIndex, TINSchema.PROPERTY_FIELD));
                        theEdge.setExternalGID(sds_edges.getInt(eIndex, TINSchema.GID_SOURCE_FIELD));
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
        private double distanceBetween(DPoint aPoint1, DPoint aPoint2) {
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
        private DTriangle getSpottedTriangle(Geometry aPoint) {
                long count_triangles = 0;
                DTriangle found = null;
                try {
                        count_triangles = sds_triangles.getRowCount();

                        // Process triangles until we find it
                        Geometry geom = null;
                        long i = 0;
                        while ((i < count_triangles) && (found == null)) {
                                // get geometry
                                geom = sds_triangles.getGeometry(i);
                                if (geom instanceof MultiPolygon) {
                                        // we get a MultiPlolygon -> az triangle
                                        MultiPolygon mp = (MultiPolygon) geom;
                                        if (mp.contains(aPoint)) {
                                                // Given point is in the triangle
                                                int GID = sds_triangles.getInt(i, TINSchema.GID);
                                                found = populateTriangleWithGDMS(GID);
                                        } else {
                                                i++;
                                        }
                                } else {
                                        i++;
                                }

                        }
                } catch (DelaunayError ex) {
                        logger.log(Level.SEVERE, "Can't retrieve the geometry.\n", ex);
                } catch (DriverException ex) {
                        logger.log(Level.SEVERE, "Can't retrieve the geometry.\n", ex);
                }

                return found;
        }

        /**
         * get slope from the point
         * @param anElement
         * @return
         */
        private double getSlope(Element anElement, DPoint Reference) throws DelaunayError {
                double slope = 0.0;

                if (anElement instanceof DEdge) {
                        // Slope is the edge one
                        DEdge anEdge = (DEdge) anElement;
                        slope = Math.abs(anEdge.getSlope());

                        // if it is the point with the lowest Z, slope is negative. it is positive otherwise
                        double minZ = anEdge.getStartPoint().getZ();
                        if (minZ > anEdge.getEndPoint().getZ()) {
                                minZ = anEdge.getEndPoint().getZ();
                        }
                        if (Math.abs(minZ - Reference.getZ()) < Tools.EPSILON) {
                                slope = -slope;
                        }

                } else if (anElement instanceof DTriangle) {
                        // get Slope vector
                        DTriangle aTriangle = (DTriangle) anElement;
                        DPoint slopeVector = aTriangle.getSteepestVector();

                        // get barycenter
                        DPoint barycenter = aTriangle.getBarycenter();

                        // Check scalar product of (Reference-barycenter) and slopeVector
                        // if it is positive, they go in the same direction (slope > 0)
                        // otherwise, they do not go in the same direction (slope < 0)
                        double scalarProduct = (barycenter.getX() - Reference.getX()) * slopeVector.getX()
                                + (barycenter.getY() - Reference.getY()) * slopeVector.getY()
                                + (barycenter.getZ() - Reference.getZ()) * slopeVector.getZ();
                        if (scalarProduct >= 0) {
                                slope = Math.abs(aTriangle.getSlope());
                        } else {
                                slope = -Math.abs(aTriangle.getSlope());
                        }
                }
                return slope;
        }

        /**
         * Add an element in the list if it is not in
         * @param ElementToProcess
         * @param anElement
         */
        private boolean isElementInArray(ArrayList<Element> ElementToProcess, Element anElement) {
                int size = ElementToProcess.size();
                int i = 0;
                boolean found = false;

                // look at all elements in the array
                while ((i < size) && (!found)) {
                        Element Current = ElementToProcess.get(i);
                        if (Current.getGID() == anElement.getGID()) {
                                // the 2 elements have the save GID
                                if ((Current instanceof DTriangle)
                                        && (anElement instanceof DTriangle)) {
                                        // both are triangles => they are the same
                                        found = true;
                                } else if ((Current instanceof DTriangle)
                                        && (anElement instanceof DTriangle)) {
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
         * give the edge that leads to geatest slope when turning around aPoint
         *
         * @param aPoint an extremity of an edge
         * @param anElement
         * @return the edge that leads to geatest slope
         * @throws DelaunayError
         */
        private Element turnAroundthePoint(DPoint aPoint, Element anElement) throws DelaunayError, DriverException {
                // First get all elements around the point
                // anElement is an element we can start with
                ArrayList<Element> ElementToProcess = new ArrayList<Element>();
                ElementToProcess.add(anElement);
                int currentElement = 0;
                while (currentElement < ElementToProcess.size()) {
                        Element ElementToTest = ElementToProcess.get(currentElement);
                        currentElement++;

                        if (ElementToTest instanceof DTriangle) {
                                // current element is a triangle
                                DTriangle aTriangle = (DTriangle) ElementToTest;

                                // Check all edges : add edges that contains the point
                                for (int i = 0; i < 3; i++) {
                                        DEdge possibleEdge = aTriangle.getEdge(i);
                                        if ((possibleEdge.getStart() == aPoint)
                                                || (possibleEdge.getEnd() == aPoint)) {
                                                // The edge contains aPoint => it is the one we look for.
                                                // generate full element
                                                if (!isElementInArray(ElementToProcess, possibleEdge)) {
                                                        DEdge theEdge = populateEdgeWithGDMS(possibleEdge.getGID());
                                                        ElementToProcess.add(theEdge);
                                                }
                                        }
                                }
                        } else {
                                // current element is an edge
                                DEdge anEdge = (DEdge) ElementToTest;

                                // Add left element
                                DTriangle Left = anEdge.getLeft();
                                if (!isElementInArray(ElementToProcess, Left)) {
                                        DTriangle newElement = populateTriangleWithGDMS(Left.getGID());
                                        ElementToProcess.add(newElement);
                                }

                                // Add right element
                                DTriangle Right = anEdge.getRight();
                                if (!isElementInArray(ElementToProcess, Right)) {
                                        DTriangle newElement = populateTriangleWithGDMS(Right.getGID());
                                        ElementToProcess.add(newElement);
                                }
                        }
                }

                // Now we process all elements in the array and we keep the one with th greastest slope
                double maxSlope = 0;                    // Current value of geatest slope
                Element selectedElement = null;         // the result

                for (Element ElementToTest : ElementToProcess) {
                        if (ElementToTest instanceof DTriangle) {
                                DTriangle aTriangle = (DTriangle) ElementToTest;
                                double theSlope = getSlope(aTriangle, aPoint);
                                if (theSlope > maxSlope) {
                                        maxSlope = theSlope;
                                        selectedElement = ElementToTest;
                                }
                        } else {
                                DEdge anEdge = (DEdge) ElementToTest;
                                double theSlope = getSlope(anEdge, aPoint);
                                if (theSlope >= maxSlope) {
                                        // We prefer edges to triangles when it is possible
                                        maxSlope = theSlope;
                                        selectedElement = ElementToTest;
                                }
                        }
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
                // - if we are on a point, we look for the greatest slope
                //      + if is is a triangle, we go to the triangle
                //      + if it is an edge, we go to the edge
                //      + if there is none, it is ended
                //      NB : to turn around the point, we MUST have a connected element to the point (edge / triangle)
                //
                // It may happen that we stay on the same same point. That means we are in a hole => we stop iterations
                // if we find such a point

                ArrayList<DPoint> theList = new ArrayList<DPoint>();

                // Find the point on the surface
                DPoint initialPoint = TINFeatureFactory.createDPoint(initialGeometry);
                DTriangle aTriangle = this.getSpottedTriangle(initialGeometry);
                if (aTriangle != null) {
                        // point is on the mesh, in a triangle
                        DEdge anEdge = null;            // doplet on an edge
                        DPoint aPoint = null;           // doplet on a point
                        DPoint lastPoint = null;        // last memorized point

                        // Project the point on the surface
                        double z_value = aTriangle.interpolateZ(initialPoint);
                        aPoint = new DPoint(initialPoint.getX(), initialPoint.getY(), z_value);
                        aPoint.setGID(-1);

                        // memorise it
                        theList.add(aPoint);
                        lastPoint = aPoint;
                        int stagnation = 1;     // we count how many times we are on the same point

                        // The current element we are in
                        Element theElement = aTriangle; // current processed element
                        Element lastElement = null;     // last Element we were in

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
                                                if (!lastPoint.contains(intersection)) {
                                                        // The next point is not the previous one
                                                        theList.add(intersection);
                                                        lastPoint = intersection;
                                                        stagnation = 1;
                                                } else {
                                                        stagnation++;
                                                }
                                                // set next element data
                                                theElement = populateEdgeWithGDMS(intersectedEdge.getGID());
                                        } else {
                                                // there is a problem
                                                // there is no intersection with the triangle
                                                theElement = null;
                                                ended = true;
                                        }
                                } else if (theElement instanceof DEdge) {
                                        // current element is an edge
                                        // the next element follows the greatest slope
                                        anEdge = (DEdge) theElement;
                                        Element nextElement = null;

                                        // First, get edge slope this value is positive or equal to zero
                                        double maxSlope = anEdge.getSlope();

                                        if (anEdge.hasProperty(HydroProperties.RIVER)) {
                                                // do not have a look on triangles
                                        } else if (anEdge.hasProperty(HydroProperties.DITCH)) {
                                                // do not have a look on triangles
                                        } else {
                                                // Check the 2 triangle around the edge
                                                DTriangle Left = anEdge.getLeft();
                                                DTriangle Right = anEdge.getRight();

                                                if (Left != null) {
                                                        // get slope
                                                        // slope is positive only if droplet can go doan the triangle
                                                        double slope = getSlope(Left, lastPoint);
                                                        if (slope > maxSlope) {
                                                                maxSlope = slope;
                                                                nextElement = Left;
                                                        }
                                                }

                                                if (Right != null) {
                                                        // get slope
                                                        // slope is positive only if droplet can go doan the triangle
                                                        double slope = getSlope(Right, lastPoint);
                                                        if (slope > maxSlope) {
                                                                maxSlope = slope;
                                                                nextElement = Right;
                                                        }
                                                }
                                        }

                                        lastElement = theElement;
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
                                        }

                                } else {
                                        // Current element is a point
                                        // the point comes from an edge. It CANNOT come from a triangle
                                        // We turn around aPoint to select the edge that leads to greatest slope
                                        aPoint = (DPoint) theElement;

                                        // First, we get the element we come from. It might be an edge.
                                        Element selectedElement;
                                        selectedElement = turnAroundthePoint(aPoint, lastElement);
                                        lastElement = theElement;
                                        if (selectedElement != null) {
                                                // We go to a next edge
                                                theElement = selectedElement;
                                        } else {
                                                // End of process: no successor
                                                theElement = null;
                                                ended = true;
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

        /**
         * save Results in a file
         * 
         * @param pathName
         * @param Result
         */
        private void savePath(DataSourceFactory dsf, String pathName, ArrayList<DPoint> Result) throws IOException, DriverException {
                File out = new File(pathName + ".gdms");
                GdmsWriter writer = new GdmsWriter(out);
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID});

                int pointsCount = Result.size();
                writer.writeMetadata(pointsCount, md);
                GeometryFactory gf = new GeometryFactory();
                Coordinate[] coords = new Coordinate[1];
                int i = 0;
                for (DPoint aPoint : Result) {
                        coords[0] = aPoint.getCoordinate();
                        CoordinateSequence cs = new CoordinateArraySequence(coords);
                        Point thePoint = new Point(cs, gf);
                        i++;
                        writer.addValues(new Value[]{ValueFactory.createValue(thePoint),
                                        ValueFactory.createValue(i)});
                }

                // write the row indexes
                writer.writeRowIndexes();
                // write envelope
                writer.writeExtent();
                writer.close();
                dsf.getSourceManager().register(dsf.getSourceManager().getUniqueName(pathName), out);
        }
}
