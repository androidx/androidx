package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("DerivedSerializableOutputFunctionsService", "app_functions_v2")
abstract class DerivedSerializableOutputFunctions : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunction(): DerivedSerializable {
        return DerivedSerializable("testValue", 1L)
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionSerializableList(): List<DerivedSerializable> {
        return listOf(DerivedSerializable("testValue", 1L))
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializableList(): List<DerivedSerializable>? {
        return null
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun simpleFunctionNullableSerializable(): DerivedSerializable? {
        return null
    }
}