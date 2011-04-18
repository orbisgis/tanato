package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
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
public class ST_DropletLine extends DropletFollower {

        @Override
        public String getName() {
                return "ST_DropletLine";
        }

        @Override
        public String getDescription() {
                return "get the line a droplet will follow on the TIN.";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_DropletLine(pointGeom [, autorizedProperties [, endingproperties]]) FROM out_point, out_edges, out_triangles";
        }

        @Override
        protected final DiskBufferDriver createDataSource(DataSourceFactory dsf, ArrayList<DPoint> result) throws DriverException {
                // Create writer
                DiskBufferDriver writer = new DiskBufferDriver(dsf, getMetadata(null));
                int ResultSize = result.size();

                // Process points to build a line
                GeometryFactory gf = new GeometryFactory();
                Coordinate[] coords = new Coordinate[ResultSize];
                int i = 0;
                for (DPoint aPoint : result) {
                        coords[i] = aPoint.getCoordinate();
                        i++;
                }

                // save line
                CoordinateSequence cs = new CoordinateArraySequence(coords);
                if (ResultSize == 1) {
                        Point thePoint = new Point(cs, gf);
                        writer.addValues(new Value[]{ValueFactory.createValue(thePoint)});
                } else {
                        LineString mp = new LineString(cs, gf);
                        writer.addValues(new Value[]{ValueFactory.createValue(mp)});
                }
                // Close driver
                writer.writingFinished();

                return writer;
        }
}
