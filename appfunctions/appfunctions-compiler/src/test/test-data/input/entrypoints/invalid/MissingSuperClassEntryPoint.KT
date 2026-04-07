package com.testdata

import androidx.appfunctions.service.AppFunctionEntryPoint
import androidx.appfunctions.service.AppFunction

@AppFunctionEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class MissingSuperClassEntryPoint {
  @AppFunction fun simpleFunction() {}
}
