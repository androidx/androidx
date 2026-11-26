package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.ExecuteAppFunctionRequest

@AppFunctionServiceEntryPoint("InvalidService", "invalid_service")
abstract class ExecuteRequestNotFirstParamEntryPoint : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    internal fun invalidFunction(string: String, request: ExecuteAppFunctionRequest): String {
        TODO("Not implemented")
    }
}
