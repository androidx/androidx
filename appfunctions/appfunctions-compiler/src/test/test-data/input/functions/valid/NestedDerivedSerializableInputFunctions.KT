package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class NestedDerivedSerializableInputFunctions {
    @AppFunction
    fun functionWithDerivedSerializable(
        appFunctionContext: AppFunctionContext,
        serializable: NestedDerivedSerializable
    ) {}

    @AppFunction
    fun functionWithDerivedSerializableList(
        appFunctionContext: AppFunctionContext,
        serializable: List<NestedDerivedSerializable>,
    ) {}
}
