package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class NestedDerivedSerializableInputFunctions {
    @AppFunctionDeclaration
    fun functionWithDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: NestedDerivedSerializable
    ) {}

    @AppFunctionDeclaration
    fun functionWithDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<NestedDerivedSerializable>,
    ) {}
}
