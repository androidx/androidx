package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.AppFunctionEntryPoint
import androidx.appfunctions.AppFunction

@AppFunctionEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class ImplementedExecuteFunctionEntryPoint: AppFunctionService() {
    @AppFunction fun simpleFunction() {}

    override suspend fun executeFunction(
      request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse {
      throw NotImplementedError()
    }
}
