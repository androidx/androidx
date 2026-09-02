package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("MyMultiModuleAppService", "multi_module_app.xml")
abstract class MultiModuleAppService: AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    fun useLibrarySerializable(
        serializable: MultiModuleLibrarySerializable
    ) {}
}
