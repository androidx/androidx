/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.kotlin

import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.KmConstructorExtensionVisitor
import kotlinx.metadata.KmConstructorVisitor
import kotlinx.metadata.KmExtensionType
import kotlinx.metadata.KmFunctionExtensionVisitor
import kotlinx.metadata.KmFunctionVisitor
import kotlinx.metadata.KmValueParameterVisitor
import kotlinx.metadata.jvm.JvmConstructorExtensionVisitor
import kotlinx.metadata.jvm.JvmFunctionExtensionVisitor
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.KotlinClassMetadata

/**
 * Represents the kotlin metadata of a function
 */
data class KmFunction(
    val descriptor: String,
    private val flags: Flags,
    val parameters: List<KmValueParameter>
) {
    fun isSuspend() = Flag.Function.IS_SUSPEND(flags)
}

/**
 * Represents the kotlin metadata of a constructor
 */
data class KmConstructor(
    val descriptor: String,
    private val flags: Flags,
    val parameters: List<KmValueParameter>
) {
    fun isPrimary() = Flag.Constructor.IS_PRIMARY(flags)
}

/**
 * Represents the kotlin metadata of a parameter
 */
data class KmValueParameter(val name: String, private val flags: Flags)

internal fun KotlinClassMetadata.Class.readFunctions(): List<KmFunction> =
    mutableListOf<KmFunction>().apply { accept(FunctionReader(this)) }

private class FunctionReader(val result: MutableList<KmFunction>) : KmClassVisitor() {
    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        return object : KmFunctionVisitor() {

            lateinit var descriptor: String
            val parameters = mutableListOf<KmValueParameter>()

            override fun visitValueParameter(
                flags: Flags,
                name: String
            ): KmValueParameterVisitor? {
                parameters.add(KmValueParameter(name, flags))
                return super.visitValueParameter(flags, name)
            }

            override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
                if (type != JvmFunctionExtensionVisitor.TYPE) {
                    error("Unsupported extension type: $type")
                }
                return object : JvmFunctionExtensionVisitor() {
                    override fun visit(desc: JvmMethodSignature?) {
                        descriptor = desc!!.asString()
                    }
                }
            }

            override fun visitEnd() {
                result.add(KmFunction(descriptor, flags, parameters))
            }
        }
    }
}

internal fun KotlinClassMetadata.Class.readConstructors(): List<KmConstructor> =
    mutableListOf<KmConstructor>().apply { accept(ConstructorReader(this)) }

private class ConstructorReader(val result: MutableList<KmConstructor>) : KmClassVisitor() {
    override fun visitConstructor(flags: Flags): KmConstructorVisitor? {
        return object : KmConstructorVisitor() {

            lateinit var descriptor: String
            val parameters = mutableListOf<KmValueParameter>()

            override fun visitValueParameter(
                flags: Flags,
                name: String
            ): KmValueParameterVisitor? {
                parameters.add(KmValueParameter(name, flags))
                return super.visitValueParameter(flags, name)
            }

            override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? {
                if (type != JvmConstructorExtensionVisitor.TYPE) {
                    error("Unsupported extension type: $type")
                }
                return object : JvmConstructorExtensionVisitor() {
                    override fun visit(desc: JvmMethodSignature?) {
                        descriptor = desc!!.asString()
                    }
                }
            }

            override fun visitEnd() {
                result.add(KmConstructor(descriptor, flags, parameters))
            }
        }
    }
}