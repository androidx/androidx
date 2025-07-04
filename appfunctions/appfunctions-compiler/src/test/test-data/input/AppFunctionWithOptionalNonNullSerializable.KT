package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.AppFunctionContext

@AppFunctionSerializable
class SerializableData(val intValue: Int)

class AppFunctionWithOptionalNonNullSerializable {
    @AppFunction
    fun withOptionalNonNullSerializable(
        appFunctionContext: AppFunctionContext,
        optionalNonNullSerializable: SerializableData = SerializableData(0)
    ) {}
}