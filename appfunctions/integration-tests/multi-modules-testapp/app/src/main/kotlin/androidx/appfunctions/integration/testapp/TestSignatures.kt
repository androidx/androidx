/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appfunctions.integration.testapp

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "dynamic_signature_definitions",
)
fun interface DynamicAllPrimitivesInputsSignature {
    suspend fun processPrimitives(
        intValue: Int,
        longValue: Long,
        floatValue: Float,
        doubleValue: Double,
        booleanValue: Boolean,
        stringValue: String,
        intArrayValue: IntArray,
        longArrayValue: LongArray,
        floatArrayValue: FloatArray,
        doubleArrayValue: DoubleArray,
        booleanArrayValue: BooleanArray,
        byteArrayValue: ByteArray,
        stringListValue: List<String>,
    ): Boolean
}

@AppFunctionSerializable
data class InnerComplexData(val id: String, val scores: IntArray, val optionalTag: String? = null)

@AppFunctionSerializable
data class OuterComplexData(
    val title: String,
    val primaryInner: InnerComplexData,
    val innerList: List<InnerComplexData>,
    val optionalMetadata: String? = null,
)

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "dynamic_signature_definitions",
)
fun interface DynamicComplexSerializableSignature {
    suspend fun processComplex(input: OuterComplexData): OuterComplexData
}

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "dynamic_signature_definitions",
)
fun interface DynamicVoidReturnSignature {
    suspend fun processVoid(message: String)
}

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "dynamic_signature_definitions",
)
fun interface DynamicThrowingSignature {
    suspend fun processAndThrow(exceptionType: String): String
}
