package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("MyMultiModuleAppService", "multi_module_app.xml")
abstract class MultiModuleAppService: AppFunctionService() {
    @AppFunction(isDescribedByKDoc = true)
    fun useLibrarySerializable(
        serializable: MultiModuleLibrarySerializable
    ) {}
}
