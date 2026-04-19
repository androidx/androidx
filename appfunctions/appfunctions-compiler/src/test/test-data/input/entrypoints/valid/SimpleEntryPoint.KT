package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.service.AppFunctionEntryPoint
import androidx.appfunctions.service.AppFunction

@AppFunctionEntryPoint("MySimpleService", "my_simple_service.xml")
abstract class SimpleEntryPoint: AppFunctionService() {
    @AppFunction
    internal fun simpleFunction(int: Int, string: String): String {
        TODO("Not implemented")
    }

    override fun onCreate() {

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}