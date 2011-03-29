/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.tanato.basin;

import junit.framework.TestCase;

/**
 *
 * @author alexis
 */
public class EdgePartTest extends TestCase{

	public void testConstructor() {
		EdgePart ep = new EdgePart(0,0.5,0.6, 8,9,40,41);
		assertEquals(ep.getEnd(), 0.6);
		assertEquals(ep.getStart(), 0.5);
		assertEquals(ep.getGid(), 0);
		assertEquals(ep.getGidStart(), 8);
		assertEquals(ep.getGidEnd(), 9);
		assertEquals(ep.getGidLeft(), 40);
		assertEquals(ep.getGidRight(), 41);
	}

}
