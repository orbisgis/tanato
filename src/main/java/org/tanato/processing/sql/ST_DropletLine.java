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
    public final String getName() {
        return "ST_DropletLine";
    }

    @Override
    public final String getDescription() {
        return "get the line a droplet will follow on the TIN.";
    }

    @Override
    public final String getSqlOrder() {
        return "SELECT ST_DropletLine(pointGeom) FROM out_point, out_edges, out_triangles";
    }

    @Override
    protected final DiskBufferDriver createDataSource(DataSourceFactory dsf, ArrayList<DPoint> result) throws DriverException {

        DiskBufferDriver writer = new DiskBufferDriver(dsf, getMetadata(null));
        int resultSize = result.size();
        GeometryFactory gf = new GeometryFactory();
        Coordinate[] coords = new Coordinate[resultSize];
        int i = 0;
        for (DPoint aPoint : result) {
            coords[i] = aPoint.getCoordinate();
            i++;
        }
        CoordinateSequence cs = new CoordinateArraySequence(coords);

        if (resultSize == 1) {
            Point thePoint = new Point(cs, gf);
            writer.addValues(new Value[]{ValueFactory.createValue(thePoint)});
        }
        else {
            LineString mp = new LineString(cs, gf);
            writer.addValues(new Value[]{ValueFactory.createValue(mp)});
        }
        writer.writingFinished();

        return writer;
    }
}
