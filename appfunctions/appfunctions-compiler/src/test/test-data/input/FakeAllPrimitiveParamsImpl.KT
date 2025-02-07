package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class FakeAllPrimitiveParamsImpl : FakeAllPrimitiveParams {
    @AppFunction
    override fun allPrimitiveParams(
        appFunctionContext: AppFunctionContext,
        arg1: Int,
        arg2: Long,
        arg3: Float,
        arg4: Double,
        arg5: String,
        arg6: Boolean,
        arg7: ByteArray,
    ) {}

    @AppFunction
    override fun allPrimitiveArrayParams(
        appFunctionContext: AppFunctionContext,
        arg1: IntArray,
        arg2: LongArray,
        arg3: FloatArray,
        arg4: DoubleArray,
        arg5: List<String>,
        arg6: BooleanArray,
        arg7: List<ByteArray>,
    ) {}
}
