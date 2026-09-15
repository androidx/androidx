package com.testdata

import android.app.Service
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint

import androidx.appfunctions.AppFunctionDeclaration

@AndroidEntryPoint(Service::class)
@AppFunctionServiceEntryPoint("MyHiltAppFunctionServiceEntryPoint", "app_functions_v2.xml")
abstract class HiltNonAppFunctionServiceEntryPoint: Hilt_HiltNonAppFunctionServiceEntryPoint() {
    @AppFunctionDeclaration
    fun helloWorld(): String {
        return "Hello World"
    }
}

abstract class Hilt_HiltNonAppFunctionServiceEntryPoint : Service()