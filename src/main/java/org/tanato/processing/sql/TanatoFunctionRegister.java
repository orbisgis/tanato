package org.tanato.processing.sql;

import org.gdms.sql.function.FunctionManager;

/**
 *
 * @author ebocher
 */
public class TanatoFunctionRegister {

        private TanatoFunctionRegister() {
        }

        /**
         * Register all gdms-topology functions.
         */
        public static void register() {
                FunctionManager.addFunction(ST_CreateHydroProperties.class);
                FunctionManager.addFunction(ST_DropletLine.class);
                FunctionManager.addFunction(ST_DropletPath.class);
                FunctionManager.addFunction(ST_GetHydroProperty.class);
                FunctionManager.addFunction(ST_HydroTIN.class);
                FunctionManager.addFunction(ST_SetZFromTriangles.class);
                FunctionManager.addFunction(ST_TIN.class);
                FunctionManager.addFunction(ST_TINPropertyHelp.class);
                FunctionManager.addFunction(ST_TINSlopeDirection.class);
                FunctionManager.addFunction(ST_TopoGraph.class);
                FunctionManager.addFunction(ST_TriangleSlope.class);

        }
}
