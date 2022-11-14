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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.InternalXAnnotationValue
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument

internal class KspAnnotationValue(
    val env: KspProcessingEnv,
    private val owner: KspAnnotation,
    override val valueType: XType,
    val valueArgument: KSValueArgument,
    private val valueProvider: () -> Any? = { owner.unwrap(valueType, valueArgument) },
) : InternalXAnnotationValue() {

    override val name: String
        get() = valueArgument.name?.asString()
            ?: error("Value argument $this does not have a name.")

    override val value: Any? by lazy { valueProvider.invoke() }
}

internal fun KspAnnotation.unwrap(valueType: XType, valueArgument: KSValueArgument): Any? {
    fun unwrap(value: Any?): Any? {
        return when (value) {
            is KSType -> {
                val declaration = value.declaration
                // Wrap enum entries in enum specific type elements
                if (declaration is KSClassDeclaration &&
                    declaration.classKind == ClassKind.ENUM_ENTRY
                ) {
                    KspEnumEntry.create(env, declaration)
                } else {
                    // And otherwise represent class types as generic XType
                    env.wrap(value, allowPrimitives = true)
                }
            }
            is KSAnnotation -> KspAnnotation(env, value)
            // The List implementation further wraps each value as a AnnotationValue.
            // We don't use arrays because we don't have a reified type to instantiate the array
            // with, and using "Any" prevents the array from being cast to the correct
            // type later on.
            is List<*> -> value.map { unwrap(it) }
            // TODO: https://github.com/google/ksp/issues/429
            // If the enum value is from compiled code KSP gives us the actual value an not
            // the KSType, so we wrap it as KspEnumEntry for consistency.
            is Enum<*> -> {
                val declaration =
                    env.resolver.getClassDeclarationByName(value::class.java.canonicalName)
                        ?: error("Cannot find KSClassDeclaration for Enum '$value'.")
                val valueDeclaration = declaration.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter { it.classKind == ClassKind.ENUM_ENTRY }
                    .firstOrNull() { it.simpleName.getShortName() == value.name }
                    ?: error("Cannot find ENUM_ENTRY '$value' in '$declaration'.")
                KspEnumEntry.create(env, valueDeclaration)
            }
            else -> value
        }
    }
    return unwrap(valueArgument.value).let { result ->
        when {
            // For array values, wrap each item in a KSPAnnotationValue. This models things similar
            // to javac, and allows us to report errors on each individual item rather than
            // just the list itself.
            valueType.isArray() -> {
                when (result) {
                    // TODO: 5/24/21 KSP does not wrap a single item in a list, even though the
                    // return type should be Class<?>[] (only in sources).
                    // https://github.com/google/ksp/issues/172
                    // https://github.com/google/ksp/issues/214
                    !is List<*> -> listOf(result)
                    else -> result
                }.map {
                    KspAnnotationValue(env, this, valueType.componentType, valueArgument) {
                        convertValueToType(it, valueType.componentType)
                    }
                }
            }
            else -> convertValueToType(result, valueType)
        }
    }
}

private fun convertValueToType(value: Any?, valueType: XType): Any? {
    // Unlike Javac, KSP does not convert the value to the type declared on the annotation class's
    // annotation value automatically so we have to do that conversion manually here.
    return when (valueType.asTypeName()) {
        XTypeName.PRIMITIVE_BYTE -> (value as Number).toByte()
        XTypeName.PRIMITIVE_SHORT -> (value as Number).toShort()
        XTypeName.PRIMITIVE_INT -> (value as Number).toInt()
        XTypeName.PRIMITIVE_LONG -> (value as Number).toLong()
        XTypeName.PRIMITIVE_FLOAT -> (value as Number).toFloat()
        XTypeName.PRIMITIVE_DOUBLE -> (value as Number).toDouble()
        else -> value
    }
}
