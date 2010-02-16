package org.tanato.processing.preprocessing;

import java.util.ArrayList;

import junit.framework.TestCase;
import org.tanato.processing.preprocessing.sewer.Sewer;

import com.vividsolutions.jts.geom.Geometry;

public class SewerTest extends TestCase {


		public void testsewer() throws Exception {
		Sewer s = new Sewer();
		ArrayList<Geometry> geoms = s.getSewer();
		}

}
