package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("FakeAllPrimitiveParamsService", "app_functions_v2")
abstract class FakeAllPrimitiveParamsImpl :
    AppFunctionService(), FakeAllPrimitiveParams, FakeAllPrimitiveArrayParams {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun allPrimitiveParams(
        arg1: Int,
        arg2: Long,
        arg3: Float,
        arg4: Double,
        arg5: String,
        arg6: Boolean,
    ) {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun allPrimitiveArrayParams(
        arg1: IntArray,
        arg2: LongArray,
        arg3: FloatArray,
        arg4: DoubleArray,
        arg5: List<String>,
        arg6: BooleanArray,
        arg7: ByteArray,
        arg8: List<String?>,
    ) {}
}
