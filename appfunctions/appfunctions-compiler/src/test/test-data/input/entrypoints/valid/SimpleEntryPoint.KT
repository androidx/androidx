package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.service.AppFunctionEntryPoint
import androidx.appfunctions.service.AppFunction

@AppFunctionEntryPoint("MySimpleService", "app_functions_v2.xml")
abstract class SimpleEntryPoint: AppFunctionService() {
    @AppFunction fun simpleFunction() {}

    override fun onCreate() {

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}