/* 
 * TANATO  is a library dedicated to the modelling of water pathways based on 
 * triangulate irregular network. TANATO takes into account anthropogenic and 
 * natural artifacts to evaluate their impacts on the watershed response. 
 * It ables to compute watershed, main slope directions and water flow pathways.
 * 
 * This library has been originally created  by Erwan Bocher during his thesis 
 * “Impacts des activités humaines sur le parcours des écoulements de surface dans 
 * un bassin versant bocager : essai de modélisation spatiale. Application au 
 * Bassin versant du Jaudy-Guindy-Bizien (France)”. It has been funded by the 
 * Bassin versant du Jaudy-Guindy-Bizien and Syndicat d’Eau du Trégor.
 * 
 * The new version is developed at French IRSTV institut as part of the 
 * AvuPur project, funded by the French Agence Nationale de la Recherche 
 * (ANR) under contract ANR-07-VULN-01.
 * 
 * TANATO is distributed under GPL 3 license. It is produced by the "Atelier SIG" team of
 * the IRSTV Institute <http://www.irstv.cnrs.fr/> CNRS FR 2488.
 * Copyright (C) 2010 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
 * Copyright (C) 2011 Erwan BOCHER, , Alexis GUEGANNO, Jean-Yves MARTIN
 * 
 * TANATO is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * TANATO is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * TANATO. If not, see <http://www.gnu.org/licenses/>.
 * 
 * For more information, please consult: <http://trac.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.tanato.processing.postprocessing;

import java.io.File;
import java.util.ArrayList;

import org.gdms.data.DataSourceCreationException;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.DataSource;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DriverException;
import org.gdms.driver.driverManager.DriverLoadException;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.gdms.driver.memory.MemoryDataSetDriver;

public class TestFonctionLargeur {

	public static SQLDataSourceFactory dsf = new SQLDataSourceFactory();

	//public static String path = "/home/bocher/Bureau/avupur/modelisation1/resultat/fossetalweg_network.shp";

	public static String path = "/home/bocher/Bureau/avupur/TIN_resultat_amont/rivertopo network.shp";

	//public static String path = "/home/bocher/Bureau/avupur/modelisation3/resultat/fosse_talweg_network.shp";

//	public static String path = "/home/bocher/Bureau/avupur/modelisation3/resultat/fosse_talweg_sewer_network.shp";

	//public static String path = "/home/bocher/Bureau/avupur/modelisation3/resultat/fossetalweg_network.shp";

	private static DataSource sds;

	public static GeometryFactory gf = new GeometryFactory();

	public static void main(String[] args) throws DriverLoadException,
			DataSourceCreationException, DriverException {

		sds = dsf.getDataSource(new File(path));

		// Fosse talweg network idOutlet = 57;
		//int gidOutlet = 46;

		//int gidOutlet = 792;

		int gidOutlet = 85;

		buildWidthFunction(gidOutlet);


	}

	private static void buildWidthFunction(int gidOutlet) throws DriverException,
			DriverLoadException, DataSourceCreationException {

		sds.open();

		long count = sds.getRowCount();

		ArrayList<Integer> listPoint = new ArrayList<Integer>();
		ArrayList<LineString> listEdges = new ArrayList<LineString>();

		int nb = 0;



		int startgid = sds.getFieldIndexByName("start_node");
		int endgid = sds.getFieldIndexByName("end_node");
		for (int i = 0; i < count; i++) {

			LineString edge = (LineString) sds.getGeometry(i).getGeometryN(0);

			listPoint.add(sds.getInt(i, startgid));
			listEdges.add(edge);
			nb++;

			listPoint.add(sds.getInt(i, endgid));
			listEdges.add(edge);
			nb++;

		}
		sds.close();

		double[] listDist = new double[nb + 1];

		for (int i = 0; i < nb; i++) {

			if (listPoint.get(i) == gidOutlet) {
				listDist[i] = 0;

			} else {
				listDist[i] = -1;
			}

		}

		double maxDist = 0d;

		boolean fini = false;

		while (!fini) {

			fini = true;

			for (int i = 0; i < nb; i = i + 2) {

				double d1 = listDist[i];
				double d2 = listDist[i + 1];

				double d = listEdges.get(i).getLength();

				boolean traiter = false;
				// Gid 2eme point
				int p = 0;
				double dist = 0d;
				if ((d1 >= 0) && (d2 == -1)) {
					dist = d1 + d;
					traiter = true;
					p = listPoint.get(i + 1);
				} else if ((d1 == -1) && (d2 >= 0)) {
					dist = d2 + d;
					traiter = true;
					p = listPoint.get(i);
				}

				if (traiter) {
					fini = false;

					for (int j = 0; j < nb; j++) {

						if (listPoint.get(j) == p) {
							listDist[j] = dist;
						}

					}

					if (dist > maxDist) {
						maxDist = dist;
					}
				}

			}

		}

		int nbCases = (int) ((maxDist / 100) + 1);

		int[] range = new int[nbCases+1];

		/*for (int i = 0; i < listDist.length; i++) {

			range[i] = 0;
		}*/

		for (int i = 0; i < nb; i = i + 2) {

			if ((listDist[i] >= 0) && (listDist[i + 1] >= 0)) {
				int v1 = (int) (listDist[i] / 100);
				int v2 = (int) (listDist[i + 1] / 100);

				if (v1 > v2) {

					int v = v1;
					v1 = v2;
					v2 = v;
				}

				for (int j = v1; j <= v2 + 1; j++) {

					range[j]++;
				}
			}

		}

		Metadata metadata = new DefaultMetadata(new Type[] {
				TypeFactory.createType(Type.INT),
				TypeFactory.createType(Type.INT) }, new String[] { "x", "y" });

		MemoryDataSetDriver driver = new MemoryDataSetDriver(metadata);

		for (int i = 0; i < range.length; i++) {

			driver.addValues(new Value[] {
					ValueFactory.createValue((i + 1) * 100),
					ValueFactory.createValue(range[i]) });

		}
		DataSource ds = dsf.getDataSource(driver,"result");

		File gdmsFile = new File("/tmp/largeur.csv");
		gdmsFile.delete();
		dsf.getSourceManager().register("result", gdmsFile);

		ds.open();
		dsf.saveContents("result", ds);
		ds.close();

	}
}
