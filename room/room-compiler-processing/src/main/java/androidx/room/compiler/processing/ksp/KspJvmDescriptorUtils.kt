/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XConstructorType
import androidx.room.compiler.processing.XExecutableType
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isBoolean
import androidx.room.compiler.processing.isByte
import androidx.room.compiler.processing.isChar
import androidx.room.compiler.processing.isDouble
import androidx.room.compiler.processing.isFloat
import androidx.room.compiler.processing.isInt
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.compiler.processing.isLong
import androidx.room.compiler.processing.isShort
import androidx.room.compiler.processing.isTypeVariable
import androidx.room.compiler.processing.isVoid
import androidx.room.compiler.processing.isVoidObject
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement

/**
 * Returns the method descriptor of this KSP field element.
 *
 * For reference, see the
 * [JVM specification, section 4.3.2](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.2).
 */
internal fun KspFieldElement.jvmDescriptor() = name + ":" + type.jvmDescriptor()

/**
 * Returns the method descriptor of this KSP method element.
 *
 * For reference, see the
 * [JVM specification, section 4.3.3](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3).
 */
internal fun KspExecutableElement.jvmDescriptor() =
    when (this) {
        is KspMethodElement -> jvmName + executableType.jvmDescriptor()
        else -> name + executableType.jvmDescriptor()
    }

/**
 * Returns the method descriptor of this KSP method element.
 *
 * For reference, see the
 * [JVM specification, section 4.3.3](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3).
 */
internal fun KspSyntheticPropertyMethodElement.jvmDescriptor() =
    jvmName + executableType.jvmDescriptor()

/**
 * Returns the method descriptor of this constructor element.
 *
 * For reference, see the
 * [JVM specification, section 4.3.3](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3).
 */
internal fun KspConstructorElement.jvmDescriptor() = name + executableType.jvmDescriptor()

private fun XExecutableType.jvmDescriptor(): String {
    val parameterTypeDescriptors = parameterTypes.joinToString("") { it.jvmDescriptor() }
    val returnTypeDescriptor =
        when (this) {
            is XMethodType -> returnType.jvmDescriptor()
            is XConstructorType -> "V"
            else -> error("Unexpected executable type: $javaClass")
        }
    return "($parameterTypeDescriptors)$returnTypeDescriptor"
}

private fun XType.jvmDescriptor(): String {
    return when {
        isKotlinUnit() || isNone() || isVoid() || isVoidObject() -> "V"
        isArray() -> "[${componentType.jvmDescriptor()}"
        // See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.2
        typeElement != null -> "L${typeElement!!.asClassName().reflectionName.replace('.', '/')};"
        // For a type variable with multiple bounds: "the erasure of a type variable is determined
        // by the first type in its bound" - JLS Sec 4.4
        // See https://docs.oracle.com/javase/specs/jls/se16/html/jls-4.html#jls-4.4
        isTypeVariable() -> upperBounds.first().jvmDescriptor()
        isInt() -> "I"
        isLong() -> "J"
        isByte() -> "B"
        isShort() -> "S"
        isDouble() -> "D"
        isFloat() -> "F"
        isBoolean() -> "Z"
        isChar() -> "C"
        else -> error("Unexpected type: $javaClass")
    }
}
