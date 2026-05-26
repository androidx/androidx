package com.testdata

import androidx.appfunctions.AppFunctionEntryPoint
import androidx.appfunctions.AppFunction

@AppFunctionEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class MissingSuperClassEntryPoint {
  @AppFunction fun simpleFunction() {}
}
