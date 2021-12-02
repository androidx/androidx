/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.solver

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.solver.types.NullSafeTypeConverter
import androidx.room.solver.types.RequireNotNullTypeConverter
import androidx.room.solver.types.TypeConverter
import androidx.room.solver.types.UpCastTypeConverter

// Shared signatures for objects that make testing more readable
private fun XNullability.toSignature() = when (this) {
    XNullability.NONNULL -> "!"
    XNullability.NULLABLE -> "?"
    XNullability.UNKNOWN -> ""
}

fun XType.toSignature() =
    "$typeName${nullability.toSignature()}".substringAfter("java.lang.")

fun TypeConverter.toSignature(): String {
    return when (this) {
        is CompositeTypeConverter -> "${conv1.toSignature()} / ${conv2.toSignature()}"
        is CustomTypeConverterWrapper -> this.custom.methodName
        is NullSafeTypeConverter ->
            "(${this.from.toSignature()} == null " +
                "? null : ${this.delegate.toSignature()})"
        is RequireNotNullTypeConverter ->
            "checkNotNull(${from.toSignature()})"
        is UpCastTypeConverter ->
            "(${from.toSignature()} as ${to.toSignature()})"
        else -> "${from.toSignature()} -> ${to.toSignature()}"
    }
}