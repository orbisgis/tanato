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
        public static String PROPERTY_FIELD = "proprety";
        public static String WEIGTH_FIELD = "weight";
        public static String HEIGHT_FIELD = "height";
        public static String STARTPOINT_NODE_FIELD = "startp_n";
        public static String ENDPOINT_NODE_FIELD = "endp_n";
        public static String LEFT_TRIANGLE_FIELD = "left_t";
        public static String RIGHT_TRIANGLE_FIELD = "right_t";
        public static String GID_SOURCE_FIELD = "gid_source";
        public static String EDGE_0_GID_FIELD = "edge0_gid";
        public static String EDGE_1_GID_FIELD = "edge1_gid";
        public static String EDGE_2_GID_FIELD = "edge2_gid";
        public static String GID = "gid";
        public static String GEOM_FIELD="the_geom";
        static String TIN_FEATURE = "tin_feature";
        static String TIN_GID = "tin_gid";
        static String PROPORTION = "proportion";
}
