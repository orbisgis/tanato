package org.tanato.processing.sql;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.ExecutionException;
import org.gdms.data.NoSuchTableException;
import org.gdms.data.SpatialDataSourceDecorator;
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
import org.jdelaunay.delaunay.DPoint;
import org.jdelaunay.delaunay.DelaunayError;
import org.orbisgis.progress.IProgressMonitor;
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

    @Override
    public ObjectDriver evaluate(DataSourceFactory dsf, DataSource[] tables, Value[] values, IProgressMonitor pm) throws ExecutionException {
        if (tables.length < 3) {
            // There MUST be at least 3 tables
            throw new ExecutionException("needs points, edges and triangles.");
        } else {
            try {
                // Create follower
                DropletFollower aDroplet = new DropletFollower(dsf, pm);

                // Set informations from tables
                aDroplet.populateData(tables);

                // Get argument : a point geometry and the pathName
                Geometry testPoint = values[0].getAsGeometry();
                if (!testPoint.isValid()) {
                    throw new ExecutionException("invalid point geometry.");
                } else if (!testPoint.getGeometryType().equals("Point")) {
                    throw new ExecutionException("invalid point geometry.");
                }

                // process path
                ArrayList<DPoint> Result = aDroplet.dropletFollows(testPoint);

                // close drivers
                aDroplet.closeData();

                // create value
                return createDataSource(dsf, Result);
            } catch (NoSuchTableException ex) {
                Logger.getLogger(ST_DropletPath.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IndexException ex) {
                Logger.getLogger(ST_DropletPath.class.getName()).log(Level.SEVERE, null, ex);
            } catch (DriverException ex) {
                logger.log(Level.SEVERE, "There has been an error while opening a table, or counting its lines.\n", ex);
            } catch (DelaunayError ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to write the file.\n", ex);
            }
        }
        return null;
    }

    @Override
    public Metadata getMetadata(Metadata[] tables) throws DriverException {
        Metadata md = new DefaultMetadata(
                new Type[]{TypeFactory.createType(Type.GEOMETRY),
                    TypeFactory.createType(Type.INT)},
                new String[]{TINSchema.GEOM_FIELD, TINSchema.GID});
        return md;
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
        return "SELECT ST_DropletPath(pointGeom) FROM out_point, out_edges, out_triangles";
    }

    @Override
    public Arguments[] getFunctionArguments() {
        return new Arguments[]{new Arguments(Argument.GEOMETRY)};
    }

    /**
     * save Results in a file
     *
     * @param pathName
     * @param Result
     */
    private DiskBufferDriver createDataSource(DataSourceFactory dsf, ArrayList<DPoint> Result) throws IOException, DriverException {

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