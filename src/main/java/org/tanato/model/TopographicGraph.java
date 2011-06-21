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
 * Copyright (C) 2011 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
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
package org.tanato.model;

import org.tanato.utils.TINUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.indexes.IndexException;
import org.gdms.data.indexes.rtree.DiskRTree;
import org.gdms.data.metadata.DefaultMetadata;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.types.GeometryConstraint;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.IProgressMonitor;
import org.tanato.factory.TINFeatureFactory;

/**
 *
 * @author ebocher
 */
public class TopographicGraph extends HydroGraph {

        private final SpatialDataSourceDecorator sdsEdges;
        private final SpatialDataSourceDecorator sdsTriangles;
        private final DataSourceFactory dsf;
        private GeometryFactory gf = new GeometryFactory();
        private int gidNode = 1;

        public TopographicGraph(DataSourceFactory dsf, SpatialDataSourceDecorator sdsEdges, SpatialDataSourceDecorator sdsTriangles) {
                this.sdsEdges = sdsEdges;
                this.sdsTriangles = sdsTriangles;
                this.dsf = dsf;
        }

        /**
         * Create a graph to connect all TIN features according the steepest downslope direction.
         * 
         */
        public void createGraph(IProgressMonitor pm) throws DriverException, IOException, NoSuchTableException, IndexException {

                sdsEdges.open();
                sdsTriangles.open();
                checkMetadata(sdsEdges);

                //Create the node table metadata.
                DefaultMetadata nodeMedata = new DefaultMetadata(new Type[]{
                                TypeFactory.createType(Type.GEOMETRY),
                                TypeFactory.createType(Type.INT)}, new String[]{"the_geom",
                                TINSchema.GID});

                //Create the diskbuffer datasource to store node rows.
                DiskBufferDriver nodesDriver = new DiskBufferDriver(dsf, nodeMedata);

                //Use a temporal rtree disk to perform topology gid order.
                String diskTreePath = dsf.getTempFile();
                DiskRTree diskRTree = new DiskRTree();
                diskRTree.newIndex(new File(diskTreePath));

                DefaultMetadata edgeMetadata = new DefaultMetadata(new Type[]{TypeFactory.createType(Type.GEOMETRY, new GeometryConstraint(
                                GeometryConstraint.LINESTRING)),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.INT),
                                TypeFactory.createType(Type.FLOAT)}, new String[]{TINSchema.GEOM_FIELD,
                                TINSchema.GID, TINSchema.STARTPOINT_NODE_FIELD, TINSchema.ENDPOINT_NODE_FIELD, "contribution"});

                DiskBufferDriver edgesDriver = new DiskBufferDriver(dsf, edgeMetadata);

                long sdsEdgesCount = sdsEdges.getRowCount();
                int propertyFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.PROPERTY_FIELD);
                int leftTriangleFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.LEFT_TRIANGLE_FIELD);
                int rightTriangleFieldIndex = sdsEdges.getFieldIndexByName(TINSchema.RIGHT_TRIANGLE_FIELD);

                pm.startTask("Create the topographic graph");
                LineString geomEdge;
                int lineGid = 1;
                for (int i = 0; i < sdsEdgesCount; i++) {
                        try {
                                if (i / 100 == i / 100.0) {
                                        if (pm.isCancelled()) {
                                                break;
                                        } else {
                                                pm.progressTo((int) (100 * i / sdsEdgesCount));
                                        }
                                }
                                //Get the geometry as a DEdge
                                DEdge line = TINFeatureFactory.createDEdge(sdsEdges.getGeometry(i));
                                Coordinate middlePointEdge = line.getMiddle().getCoordinate();
                                //Get the morphological property that describes if the edge is a talweg, a ridge, a flat slope...
                                int property = sdsEdges.getFieldValue(i, propertyFieldIndex).getAsInt();
                                line.setProperty(property);
                                int leftTriangleGID = sdsEdges.getFieldValue(i, leftTriangleFieldIndex).getAsInt();
                                int rightTriangleGID = sdsEdges.getFieldValue(i, rightTriangleFieldIndex).getAsInt();
                                DTriangle dTriangleLeft = getTriangle(leftTriangleGID);
                                DTriangle dTriangleRight = getTriangle(rightTriangleGID);
                                Coordinate startCoord, endCoord;
                                int startGID, endGID;
                                if (line.hasProperty(HydroProperties.TALWEG)) {
                                        startCoord = dTriangleLeft.getBarycenter().getCoordinate();
                                        endCoord = middlePointEdge;
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);
                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(TINUtils.getProportion(dTriangleLeft, line))});
                                        lineGid++;
                                        startCoord = dTriangleRight.getBarycenter().getCoordinate();
                                        endCoord = middlePointEdge;
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);
                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID), ValueFactory.createValue(endGID), ValueFactory.createValue(TINUtils.getProportion(dTriangleRight, line))});
                                        lineGid++;

                                        startCoord = middlePointEdge;
                                        endCoord = line.getEndPoint().getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID), ValueFactory.createValue(endGID), ValueFactory.createValue(1)});
                                        lineGid++;

                                } else if (line.hasProperty(HydroProperties.LEFTTSLOPE)) {
                                        // The left triangle point to the current edge
                                        startCoord = dTriangleRight.getBarycenter().getCoordinate();
                                        endCoord = dTriangleLeft.getBarycenter().getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(TINUtils.getProportion(dTriangleRight, line))});
                                        lineGid++;
                                } else if (line.hasProperty(HydroProperties.RIGHTSLOPE)) {
                                        //The right triangle point to the current edge
                                        startCoord = dTriangleLeft.getBarycenter().getCoordinate();
                                        endCoord = dTriangleRight.getBarycenter().getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(TINUtils.getProportion(dTriangleLeft, line))});
                                        lineGid++;
                                } else if (line.hasProperty(HydroProperties.LEFTCOLINEAR)) {
                                        //The rigth triangle point to the edge, the edge is a talweg, the left triangle point to the shared edge
                                        startCoord = dTriangleRight.getBarycenter().getCoordinate();
                                        endCoord = middlePointEdge;
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(TINUtils.getProportion(dTriangleRight, line))});
                                        lineGid++;
                                        DPoint endPoint = line.getEndPoint();
                                        startCoord = middlePointEdge;
                                        endCoord = endPoint.getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID), ValueFactory.createValue(endGID),
                                                        ValueFactory.createValue(1)});
                                        lineGid++;


                                } else if (line.hasProperty(HydroProperties.RIGHTCOLINEAR)) {
                                        //The left triangle point to the edge, the edge is a talweg and the right triangle go to the shared edge

                                        startCoord = dTriangleLeft.getBarycenter().getCoordinate();
                                        endCoord = middlePointEdge;
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(TINUtils.getProportion(dTriangleLeft, line))});
                                        lineGid++;
                                        DPoint endPoint = line.getEndPoint();
                                        startCoord = middlePointEdge;
                                        endCoord = endPoint.getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(1)});
                                        lineGid++;

                                } else if (line.hasProperty(HydroProperties.LEFTWELL)) {
                                        //The right triangle go to the edge
                                        startCoord = dTriangleRight.getBarycenter().getCoordinate();
                                        endCoord = middlePointEdge;
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(TINUtils.getProportion(dTriangleRight, line))});
                                        lineGid++;
                                        DPoint endPoint = line.getEndPoint();
                                        startCoord = middlePointEdge;
                                        endCoord = endPoint.getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(1)});
                                        lineGid++;

                                } else if (line.hasProperty(HydroProperties.RIGHTWELL)) {
                                        //The left triangle go to the edge
                                        startCoord = dTriangleLeft.getBarycenter().getCoordinate();
                                        endCoord = middlePointEdge;
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);
                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID),
                                                        ValueFactory.createValue(TINUtils.getProportion(dTriangleLeft, line))});
                                        lineGid++;
                                        DPoint endPoint = line.getEndPoint();
                                        startCoord = middlePointEdge;
                                        endCoord = endPoint.getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);

                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(1)});
                                        lineGid++;
                                } else if (line.hasProperty(HydroProperties.DOUBLECOLINEAR)) {
                                        //The left and right triangles are colinear to the edge
                                        //The edge is a tagged as a talweg
                                        startCoord = line.getStartPoint().getCoordinate();
                                        endCoord = line.getEndPoint().getCoordinate();
                                        startGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, startCoord);
                                        endGID = getGIDAndCreateNodesTable(diskRTree, nodesDriver, endCoord);
                                        geomEdge = gf.createLineString(new Coordinate[]{startCoord, endCoord});
                                        edgesDriver.addValues(new Value[]{ValueFactory.createValue(geomEdge), ValueFactory.createValue(lineGid),
                                                        ValueFactory.createValue(startGID),
                                                        ValueFactory.createValue(endGID), ValueFactory.createValue(1)});
                                        lineGid++;
                                }
                        } catch (DelaunayError ex) {
                                Logger.getLogger(TopographicGraph.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
                pm.endTask();
                String src_sds_Name = sdsEdges.getName().split("_")[0];
                sdsEdges.close();
                sdsTriangles.close();
                edgesDriver.writingFinished();
                nodesDriver.writingFinished();

                String ds_edges_name = dsf.getSourceManager().getUniqueName(src_sds_Name + "_graphTopo_edges");
                dsf.getSourceManager().register(ds_edges_name, edgesDriver);

                String ds_nodes_name = dsf.getSourceManager().getUniqueName(src_sds_Name + "_graphTopo_nodes");
                dsf.getSourceManager().register(ds_nodes_name, nodesDriver);

                //Remove the Rtree on disk
                new File(diskTreePath).delete();
        }

        /**
         * A method to obtain an unique GID node and populate the node tables.
         * @param diskRTree
         * @param nodesDriver
         * @param coordinate
         * @return
         * @throws IOException
         * @throws DriverException
         */
        public int getGIDAndCreateNodesTable(DiskRTree diskRTree, DiskBufferDriver nodesDriver, Coordinate coordinate) throws IOException, DriverException {
                int[] gidsStart = diskRTree.getRow(new Envelope(coordinate));
                int lastGID = gidNode;
                if (gidsStart.length == 0) {
                        nodesDriver.addValues(new Value[]{ValueFactory.createValue(gf.createPoint(coordinate)),
                                        ValueFactory.createValue(lastGID)});
                        diskRTree.insert(new Envelope(coordinate), lastGID);
                        gidNode++;
                        return lastGID;
                } else {
                        return gidsStart[0];
                }
        }

        

        /**
         * Build all indexes to improve the graph computing
         */
        public void createIndex(SpatialDataSourceDecorator sds, String field, IProgressMonitor pm) throws NoSuchTableException, IndexException {
                if (!dsf.getIndexManager().isIndexed(sds.getName(), field)) {
                        dsf.getIndexManager().buildIndex(sds.getName(), field, pm);
                }
        }

        /**
         * Get the interior point of triangle using an alphanumeric index
         * @param field
         * @param gid
         * @return
         * @throws DriverException
         */
        private DTriangle getTriangle(int gid) throws DriverException, DelaunayError {

                if (gid != -1) {
                        return TINFeatureFactory.createDTriangle(sdsTriangles.getGeometry(gid - 1));
                }
                return null;

        }
}
