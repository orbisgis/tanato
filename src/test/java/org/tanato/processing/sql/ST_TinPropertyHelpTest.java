/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import java.lang.reflect.Field;
import org.gdms.data.DataSource;
import junit.framework.TestCase;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.metadata.Metadata;
import org.gdms.data.values.Value;
import org.gdms.driver.ObjectDriver;
import org.jhydrocell.hydronetwork.HydroProperties;
import org.orbisgis.progress.IProgressMonitor;
import org.orbisgis.progress.NullProgressMonitor;

/**
 *
 * @author kwyhr, alexis
 */
public class ST_TinPropertyHelpTest extends TestCase {

        public void testProperty() throws Exception {
                // Generate object
                ST_TINPropertyHelp function2 = new ST_TINPropertyHelp();
                assertNull(function2.getMetadata(new Metadata[]{}));
                // Generate parameters
                DataSourceFactory dsf = null;
                DataSource[] tables = null;
                Value[] values = null;
                IProgressMonitor pm = new NullProgressMonitor();

                // Call function
                ObjectDriver drvr = function2.evaluate(dsf, tables, values, pm);
                
                Field[] fields = HydroProperties.class.getFields();
                long intCount = 0;
                for (int i = 0; i < fields.length; i++) {
                        Field field = fields[i];
                        if(field.getType().isAssignableFrom(Integer.TYPE)){
                                intCount++;
                        }
                        
                }
                
                // always passes
                assertTrue(drvr.getRowCount()==intCount);
        }
}
