package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.IOException;
import java.util.ArrayList;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.jdelaunay.delaunay.DPoint;

/**
 * This class designs a custom query for GDMS. The goal of the query is to process
 * a droplet path on an existing triangularization.
 *
 *
 * @author kwyhr
 */
public class ST_DropletPath extends DropletFollower {

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
                return "SELECT ST_DropletPath(pointGeom) FROM out_point, out_edges, out_triangles";
        }

        @Override
        protected DiskBufferDriver createDataSource(DataSourceFactory dsf, ArrayList<DPoint> Result) throws IOException, DriverException {

                DiskBufferDriver writer = new DiskBufferDriver(dsf, getMetadata(null));
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
                writer.writingFinished();

                return writer;
        }
}
