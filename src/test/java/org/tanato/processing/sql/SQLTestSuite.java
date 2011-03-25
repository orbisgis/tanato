package org.tanato.processing.sql;

import junit.framework.Test;
import junit.framework.TestSuite;

public class SQLTestSuite extends TestSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite("Test for TANATO SQL function");
                suite.addTestSuite(Tanato2SQLTest.class);
		return suite;
	}
}
