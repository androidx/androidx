package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.service.AppFunctionEntryPoint
import androidx.appfunctions.service.AppFunction

@AppFunctionEntryPoint("MySimpleService", "my_simple_service.xml")
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