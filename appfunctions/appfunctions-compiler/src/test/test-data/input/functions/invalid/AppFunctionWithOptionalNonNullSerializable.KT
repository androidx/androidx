package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

@AppFunctionSerializable(isDescribedByKDoc = false)
class SerializableData(val intValue: Int)

class AppFunctionWithOptionalNonNullSerializable {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    fun withOptionalNonNullSerializable(
        appFunctionContext: AppFunctionContext,
        optionalNonNullSerializable: SerializableData = SerializableData(0)
    ) {}
}