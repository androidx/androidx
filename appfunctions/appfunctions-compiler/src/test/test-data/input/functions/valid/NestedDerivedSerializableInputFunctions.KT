package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class NestedDerivedSerializableInputFunctions {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: NestedDerivedSerializable
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun functionWithDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<NestedDerivedSerializable>,
    ) {}
}
