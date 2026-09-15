package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint

import androidx.appfunctions.AppFunctionDeclaration

@AndroidEntryPoint(AppFunctionService::class)
@AppFunctionServiceEntryPoint("MyHiltAppFunctionServiceEntryPoint", "app_functions_v2")
abstract class HiltAppFunctionServiceEntryPoint: Hilt_HiltAppFunctionServiceEntryPoint() {
    @AppFunctionDeclaration
    fun helloWorld(): String {
        return "Hello World"
    }
}

abstract class Hilt_HiltAppFunctionServiceEntryPoint : AppFunctionService()
