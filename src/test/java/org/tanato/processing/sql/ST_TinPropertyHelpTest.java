/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.processing.sql;

import org.gdms.data.DataSource;
import junit.framework.TestCase;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.driver.ObjectDriver;
import org.orbisgis.progress.IProgressMonitor;

/**
 *
 * @author kwyhr
 */
public class ST_TinPropertyHelpTest extends TestCase {

        public void test_property() throws Exception {
                // Generate object
                ST_TINPropertyHelp function2 = new ST_TINPropertyHelp();

                // Generate parameters
                DataSourceFactory dsf = null;
                DataSource[] tables = null;
                Value[] values = null;
                IProgressMonitor pm = null;

                // Call function
                ObjectDriver drvr = function2.evaluate(dsf, tables, values, pm);

                // always passes
                assertTrue(true);
        }
}
