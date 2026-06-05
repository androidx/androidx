package com.testdata

import android.app.Service
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint

import androidx.appfunctions.AppFunction

@AndroidEntryPoint(Service::class)
@AppFunctionServiceEntryPoint("MyHiltAppFunctionServiceEntryPoint", "app_functions_v2.xml")
abstract class HiltNonAppFunctionServiceEntryPoint: Hilt_HiltNonAppFunctionServiceEntryPoint() {
    @AppFunction
    fun helloWorld(): String {
        return "Hello World"
    }
}

abstract class Hilt_HiltNonAppFunctionServiceEntryPoint : Service()