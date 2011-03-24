/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.factory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.jdelaunay.delaunay.DEdge;
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DTriangle;
import org.jdelaunay.delaunay.DelaunayError;

/**
 *
 * @author ebocher
 */
public class TINFeatureFactory {

        /**
         * A factory to create a DTriangle from a Geometry
         * @param geom
         * @return
         * @throws DelaunayError
         */
        public static DTriangle createDTriangle(Geometry geom) throws DelaunayError {

                Coordinate[] coords = geom.getCoordinates();
                if (coords.length != 4) {
                        throw new IllegalArgumentException("The geometry must be a triangle");
                }
                return new DTriangle(new DEdge(coords[0].x, coords[0].y, coords[0].z, coords[1].x, coords[1].y, coords[1].z), new DEdge(coords[1].x, coords[1].y, coords[1].z, coords[2].x, coords[2].y, coords[2].z), new DEdge(coords[2].x, coords[2].y, coords[2].z, coords[0].x, coords[0].y, coords[0].z));
        }

        public static DPoint createDPoint(Geometry geom) throws DelaunayError{
                Coordinate[] coords = geom.getCoordinates();
                if (coords.length != 1) {
                        throw new IllegalArgumentException("The geometry must be a triangle");
                }
                return new DPoint(coords[0]);
        }

        public static DPoint createDPoint(Coordinate coord) throws DelaunayError {
                return new DPoint(coord);
        }

       

}
