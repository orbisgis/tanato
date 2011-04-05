/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tanato.model;

/**
 * This class contains only some constants that describe the names of the columns,
 * in the GDMS tables, that will be produced by the tanato GDMS functions and
 * custom queries.
 * @author ebocher
 */
public class TINSchema {

        /**
         * Field names used by TIN datasources
         */
        public static final String PROPERTY_FIELD = "property";
        public static final String WEIGTH_FIELD = "weight";
        public static final String HEIGHT_FIELD = "height";
        public static final String STARTPOINT_NODE_FIELD = "startp_n";
        public static final String ENDPOINT_NODE_FIELD = "endp_n";
        public static final String LEFT_TRIANGLE_FIELD = "left_t";
        public static final String RIGHT_TRIANGLE_FIELD = "right_t";
        public static final String GID_SOURCE_FIELD = "gid_source";
        public static final String EDGE_0_GID_FIELD = "edge0_gid";
        public static final String EDGE_1_GID_FIELD = "edge1_gid";
        public static final String EDGE_2_GID_FIELD = "edge2_gid";
        public static final String GID = "gid";
        public static final String GEOM_FIELD="the_geom";
        static final String TIN_FEATURE = "tin_feature";
        static final String TIN_GID = "tin_gid";
        static final String PROPORTION = "proportion";

	/**
	 * We don't want anyone to instanciate a TINSchema.
	 */
	private TINSchema(){
	}
}
