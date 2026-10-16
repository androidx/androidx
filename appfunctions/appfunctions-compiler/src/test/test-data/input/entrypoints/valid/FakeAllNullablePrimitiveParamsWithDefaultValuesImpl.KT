package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint(
    "FakeAllNullablePrimitiveParamsWithDefaultValuesService",
    "app_functions_v2",
)
abstract class FakeAllNullablePrimitiveParamsWithDefaultValuesImpl :
    AppFunctionService(),
    FakeAllNullablePrimitiveParamsWithDefaultValues,
    FakeAllNullablePrimitiveArrayParamsWithDefaultValues {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun allNullablePrimitiveParamsWithDefault(
        arg1: Int?,
        arg2: Long?,
        arg3: Float?,
        arg4: Double?,
        arg5: String?,
        arg6: Boolean?,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun allNullablePrimitiveArrayParamsWithDefault(
        arg1: IntArray?,
        arg2: LongArray?,
        arg3: FloatArray?,
        arg4: DoubleArray?,
        arg5: List<String>,
        arg6: BooleanArray?,
        arg7: ByteArray?,
    ) {}
}
