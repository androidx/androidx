package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunction

@AppFunctionServiceEntryPoint("MySimpleService", "my_simple_service")
abstract class SimpleEntryPoint: AppFunctionService() {
    /**
     * simple function.
     *
     * @param int Int param.
     * @param string String param.
     * @return Result.
     */
    @AppFunction(isDescribedByKDoc = true)
    internal fun simpleFunction(int: Int, string: String): String {
        TODO("Not implemented")
    }

    override fun onCreate() {

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}