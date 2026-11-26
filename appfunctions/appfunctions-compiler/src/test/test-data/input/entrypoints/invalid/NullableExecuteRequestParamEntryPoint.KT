package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.ExecuteAppFunctionRequest

@AppFunctionServiceEntryPoint("InvalidService", "invalid_service")
abstract class NullableExecuteRequestParamEntryPoint : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    internal fun invalidFunction(request: ExecuteAppFunctionRequest?): String {
        TODO("Not implemented")
    }
}
