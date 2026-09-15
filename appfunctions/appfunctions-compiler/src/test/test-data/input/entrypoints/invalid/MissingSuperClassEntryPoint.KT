package com.testdata

import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionDeclaration

@AppFunctionServiceEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class MissingSuperClassEntryPoint {
  @AppFunctionDeclaration fun simpleFunction() {}
}
