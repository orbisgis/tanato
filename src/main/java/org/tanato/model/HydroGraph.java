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
 * Copyright (C) 2011 Erwan BOCHER, Alexis GUEGANNO, Jean-Yves MARTIN
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
package org.tanato.model;

import org.gdms.data.DataSource;
import org.gdms.data.schema.Metadata;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;

/**
 *
 * @author ebocher
 */
public abstract class HydroGraph {

        /**
         * A method to check if the datasource contains the two fields :  property and height. The field is used to tag the TIN feature
         * @param dataSource
         * @throws DriverException
         */
        public void checkMetadata(DataSet dataSource) throws DriverException {
                Metadata md = dataSource.getMetadata();
                if ((md.getFieldIndex(TINSchema.PROPERTY_FIELD) == -1) || (md.getFieldIndex(TINSchema.HEIGHT_FIELD) == -1)) {
                        String name = dataSource instanceof DataSource ? ((DataSource) dataSource).getName() : "";
                        throw new IllegalArgumentException("The table " + name + 
                                " must contains two fields  : property and height");

                }
        }
}
