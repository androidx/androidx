package com.testdata

import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunction

@AppFunctionServiceEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class MissingSuperClassEntryPoint {
  @AppFunction fun simpleFunction() {}
}
