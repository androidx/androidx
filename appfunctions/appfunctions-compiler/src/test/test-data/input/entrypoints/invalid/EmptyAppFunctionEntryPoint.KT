package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionEntryPoint

@AppFunctionEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class EmptyAppFunctionEntryPoint: AppFunctionService() {

}
