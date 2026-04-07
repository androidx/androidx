package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.service.AppFunctionEntryPoint
import androidx.appfunctions.service.AppFunction

@AppFunctionEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class ImplementedExecuteFunctionEntryPoint: AppFunctionService() {
    @AppFunction fun simpleFunction() {}

    override suspend fun executeFunction(
      request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse {
      throw NotImplementedError()
    }
}
