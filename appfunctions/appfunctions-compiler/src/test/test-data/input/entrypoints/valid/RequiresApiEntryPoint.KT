package com.testdata

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionDeclaration

@RequiresApi(34)
@AppFunctionServiceEntryPoint("MyRequiresApiService", "my_requires_api_service")
abstract class RequiresApiEntryPoint: AppFunctionService() {
    @AppFunctionDeclaration
    internal fun simpleFunction(int: Int, string: String): String {
        TODO("Not implemented")
    }
}
