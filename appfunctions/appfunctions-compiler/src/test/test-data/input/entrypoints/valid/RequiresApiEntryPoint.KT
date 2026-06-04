package com.testdata

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunction

@RequiresApi(34)
@AppFunctionServiceEntryPoint("MyRequiresApiService", "my_requires_api_service.xml")
abstract class RequiresApiEntryPoint: AppFunctionService() {
    @AppFunction
    internal fun simpleFunction(int: Int, string: String): String {
        TODO("Not implemented")
    }
}
