package org.tanato.model;


import junit.framework.TestCase;

import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.driver.DriverException;
import org.gdms.sql.strategies.IncompatibleTypesException;
import org.tanato.SetUpData;

public class DataModelTest extends TestCase {

	private String FIELDNAME = "gid";

	/**
	 *
	 * All  gid in the datasource must be greater than 0
	 *
	 * @throws DriverException
	 * @throws IncompatibleTypesException
	 */
	public void testDatasSourcesGID() throws IncompatibleTypesException,
			DriverException {

		SetUpData.buildHydrologicalGraph(SetUpData.TMP_TEST);

		SpatialDataSourceDecorator sdsEdges = SetUpData.getSdsEdges();
		SpatialDataSourceDecorator sdsNodes = SetUpData.getSdsNodes();
		SpatialDataSourceDecorator sdsFaces = SetUpData.getSdsFaces();

		sdsEdges.open();

		for (int i = 0; i < sdsEdges.getRowCount(); i++) {

			assertTrue(sdsEdges.getFieldValue(i,
					sdsEdges.getFieldIndexByName(FIELDNAME)).getAsInt() > 0);
		}

		sdsEdges.close();

		sdsNodes.open();
		for (int i = 0; i < sdsNodes.getRowCount(); i++) {

			assertTrue(sdsNodes.getFieldValue(i,
					sdsNodes.getFieldIndexByName(FIELDNAME)).getAsInt() > 0);
		}
		sdsNodes.close();

		sdsFaces.open();
		for (int i = 0; i < sdsFaces.getRowCount(); i++) {

			assertTrue(sdsFaces.getFieldValue(i,
					sdsFaces.getFieldIndexByName(FIELDNAME)).getAsInt() > 0);
		}

		sdsFaces.close();
	}

}
