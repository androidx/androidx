package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.ExecuteAppFunctionRequest

@AppFunctionServiceEntryPoint("MyExecuteRequestParamService", "my_execute_request_param_service")
abstract class ExecuteRequestParamEntryPoint : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    internal fun requestOnly(request: ExecuteAppFunctionRequest): String {
        TODO("Not implemented")
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    internal fun requestWithParam(request: ExecuteAppFunctionRequest, string: String): String {
        TODO("Not implemented")
    }
}
