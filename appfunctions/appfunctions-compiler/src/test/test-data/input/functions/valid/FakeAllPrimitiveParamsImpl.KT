package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext

class FakeAllPrimitiveParamsImpl : FakeAllPrimitiveParams {
    @AppFunctionDeclaration
    override fun allPrimitiveParams(
        appFunctionContext: AppFunctionContext,
        arg1: Int,
        arg2: Long,
        arg3: Float,
        arg4: Double,
        arg5: String,
        arg6: Boolean,
    ) {}
}

class FakeAllPrimitiveArrayParamsImpl : FakeAllPrimitiveArrayParams {
    @AppFunctionDeclaration
    override fun allPrimitiveArrayParams(
        appFunctionContext: AppFunctionContext,
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
