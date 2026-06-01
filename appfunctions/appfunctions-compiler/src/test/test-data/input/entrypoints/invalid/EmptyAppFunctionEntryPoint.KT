package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class EmptyAppFunctionEntryPoint: AppFunctionService() {

}
