/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.model;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.generic.GenericObjectDriver;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.IProgressMonitor;

/**
 *
 * @author ebocher
 */
public class HydroGraphBuilder {

        private SpatialDataSourceDecorator sdsEdges;
        private SpatialDataSourceDecorator sdsTriangles;
        private SpatialDataSourceDecorator sdsPoints;
        private GeometryFactory gf = new GeometryFactory();
        //Used to set the dimension of the TIN feature
        private int TIN_POINT = 0;
        private int TIN_EDGE = 1;
        private int TIN_TRIANGLE = 2;

        public HydroGraphBuilder(SpatialDataSourceDecorator sdsPoints,
                SpatialDataSourceDecorator sdsEdges, SpatialDataSourceDecorator sdsTriangles) {
                this.sdsTriangles = sdsTriangles;
                this.sdsEdges = sdsEdges;
                this.sdsPoints = sdsPoints;

        }

        /**
         * Create the graph structure based on edge loop.
         * @param name
         * @param pm
         */
        public ObjectDriver[] createGraph(IProgressMonitor pm) throws DriverException, IOException {

                sdsEdges.open();
                long edgesCount = sdsEdges.getRowCount();
                int gidField = sdsEdges.getFieldIndexByName(TINSchema.GID);
                int propertyIndex = sdsEdges.getFieldIndexByName(TINSchema.PROPERTY_FIELD);
                int startNodeFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.STARTPOINT_NODE_FIELD);
                int endNodeFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.ENDPOINT_NODE_FIELD);
                int leftTriangleFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.LEFT_TRIANGLE_FIELD);
                int rightTriangleFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.RIGHT_TRIANGLE_FIELD);
                int heightIndex = sdsEdges.getFieldIndexByName(TINSchema.HEIGHT_FIELD);

                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID,
                                TINSchema.PROPERTY_FIELD, TINSchema.HEIGHT_FIELD, TINSchema.TIN_GID, TINSchema.TIN_FEATURE});

                GenericObjectDriver nodes_graphDriver = new GenericObjectDriver(md);
                md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.FLOAT)},
                        new String[]{TINSchema.GEOM_FIELD, TINSchema.GID,
                                TINSchema.STARTPOINT_NODE_FIELD, TINSchema.ENDPOINT_NODE_FIELD, TINSchema.PROPORTION});

                GenericObjectDriver edges_graphDriver = new GenericObjectDriver(md);

                int nodeID = 1;
                int edgeID = 1;
                for (int i = 0; i < edgesCount; i++) {
                        int edgeGID = sdsEdges.getFieldValue(i, gidField).getAsInt();
                        int edgeProperty = sdsEdges.getFieldValue(i, propertyIndex).getAsInt();
                        double edgeHeight = sdsEdges.getFieldValue(i, heightIndex).getAsDouble();
                        int leftGid = sdsEdges.getFieldValue(i, leftTriangleFieldIndex).getAsInt();
                        int rightGid = sdsEdges.getFieldValue(i, rightTriangleFieldIndex).getAsInt();
                        Point point = sdsEdges.getGeometry(i).getCentroid();

                        if (HydroProperties.check(HydroProperties.TALWEG, edgeProperty)) {
                                //On ajoute le noeud du triangle de gauche                                                        

                                int nodeIdLeft = nodeID;
                                addNodesValue(nodes_graphDriver, null, nodeIdLeft, -1, 0, leftGid, TIN_TRIANGLE);
                                nodeID++;

                                //On ajoute le noeud de l'edge

                                int nodeIdEdge = nodeID;
                                addNodesValue(nodes_graphDriver, point, nodeIdEdge, edgeProperty, edgeHeight, edgeGID, TIN_EDGE);
                                nodeID++;

                                //On ajoute la connexion entre le noeud du triangle de gauche et le noeud de l'edge

                                addEdgesValue(edges_graphDriver, null, edgeID, nodeIdLeft, nodeIdEdge, 1.0f);
                                edgeID++;

                                //On ajoute le noeud du triangle de droite

                                int nodeIdRight = nodeID;
                                addNodesValue(nodes_graphDriver, null, nodeIdRight, -1, 0, rightGid, TIN_TRIANGLE);
                                nodeID++;

                                //On ajoute la connexion entre le noeud du  triangle de droite et le noeud de l'edge

                                addEdgesValue(edges_graphDriver, null, edgeID, nodeIdRight, nodeIdEdge, 1.0f);
                                edgeID++;

                        } else if (HydroProperties.check(HydroProperties.RIDGE, edgeProperty)) {
                                System.out.println("Ridge");
                        } else if (HydroProperties.check(HydroProperties.RIGHTSLOPE, edgeProperty)) {
                                System.out.println("Right slope");
                        } else if (HydroProperties.check(HydroProperties.LEFTTSLOPE, edgeProperty)) {
                                System.out.println("Left slope");
                        } else {
                                System.out.println("The rest");
                        }

                }

                sdsEdges.close();

                return new ObjectDriver[] {nodes_graphDriver, edges_graphDriver};

        }

        /**
         * This method is used to populate a node row.
         * @param nodes_graphDriver
         * @param nodeGeom
         * @param gid
         * @param property
         * @param height
         * @param tin_gid
         * @param tin_feature
         */
        private void addNodesValue(GenericObjectDriver nodes_graphDriver, Geometry nodeGeom, int gid, int property, double height, int tin_gid, int tin_feature) {

                nodes_graphDriver.addValues(
                        ValueFactory.createValue(nodeGeom), ValueFactory.createValue(gid),
                        ValueFactory.createValue(property), ValueFactory.createValue(height),
                        ValueFactory.createValue(tin_gid), ValueFactory.createValue(tin_feature));
        }

        /**
         * Get the ground surface linear constraint that affect the run off pathway.
         * @param property
         * @return
         */
        private int getHydroGroundSurfaceLinearConstraint(int property) {

                if (HydroProperties.check(HydroProperties.WALL, property)) {
                        return HydroProperties.WALL;
                } else if (HydroProperties.check(HydroProperties.RIVER, property)) {
                        return HydroProperties.RIVER;
                } else if (HydroProperties.check(HydroProperties.DITCH, property)) {
                        return HydroProperties.DITCH;
                }
                return HydroProperties.NONE;

        }

        /**
         * This method is used to populate an edge row.
         * @param edges_graphDriver
         * @param edgeGeom
         * @param gid
         * @param gidStart
         * @param gidEnd
         * @param proportion
         */
        private void addEdgesValue(GenericObjectDriver edges_graphDriver, Geometry edgeGeom, int gid, int gidStart, int gidEnd, float proportion) {
                edges_graphDriver.addValues(
                        ValueFactory.createValue(edgeGeom), ValueFactory.createValue(gid),
                        ValueFactory.createValue(gidStart), ValueFactory.createValue(gidEnd),
                        ValueFactory.createValue(proportion));
        }
}
