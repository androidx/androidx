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

package androidx.room3.compiler.processing.ksp

import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.InternalXAnnotationValue
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.isArray
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument

internal class KspAnnotationValue(
    val env: KspProcessingEnv,
    val valueArgument: KSValueArgument,
    private val method: XMethodElement,
    override val valueType: XType,
    private val valueProvider: () -> Any?,
) : InternalXAnnotationValue() {

    override val name: String
        get() = method.propertyName ?: method.name

    override val jvmName: String
        get() = method.jvmName

    override val value: Any? by lazy { valueProvider.invoke() }

    companion object {
        fun create(
            env: KspProcessingEnv,
            annotation: KSAnnotation,
            valueArgument: KSValueArgument,
        ): KspAnnotationValue {
            val method = findMethod(env, annotation, valueArgument)
            val valueType = method.returnType
            return KspAnnotationValue(
                env,
                valueArgument,
                method,
                valueType,
                valueProvider = { unwrap(env, method, valueType, valueArgument) },
            )
        }

        private fun findMethod(
            env: KspProcessingEnv,
            annotation: KSAnnotation,
            valueArgument: KSValueArgument,
        ): XMethodElement {
            val ksDeclaration = annotation.annotationType.resolve().declaration.replaceTypeAliases()
            val typeElement =
                env.wrapClassDeclarationForNonEnumEntry(ksDeclaration as KSClassDeclaration)
            val valueName =
                valueArgument.name?.asString()
                    ?: error("Value $valueArgument does not have a name.")
            return typeElement.getDeclaredMethods().single {
                // Whether "name" or "propertyName" is used just depends on whether the annotation
                // is declared in a Java or Kotlin file, respectively. However, things get more
                // complicated when there is an @JvmName on the annotation value. In this case, if
                // the usage is a Java file then the "jvmName" is used. If the usage site is a
                // Kotlin file then "propertyName" or "jvmName" is used depending on whether the
                // usage site is being viewed from source or classpath (due to a bug in KSP,
                // https://github.com/google/ksp/issues/2620). For simplicity, just allow matches
                // from either rather than trying to figure out exactly which scenario we're in.
                valueName == (it.propertyName ?: it.name) || valueName == it.jvmName
            }
        }
    }
}

internal fun unwrap(
    env: KspProcessingEnv,
    method: XMethodElement,
    valueType: XType,
    valueArgument: KSValueArgument,
): Any? {
    fun unwrap(value: Any?): Any? {
        return when (value) {
            // Enums in KSP2
            is KSClassDeclaration -> {
                KspEnumEntry.create(env, value)
            }
            is KSType -> {
                val declaration = value.declaration
                // Wrap enum entries in enum specific type elements
                if (
                    declaration is KSClassDeclaration &&
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
            is List<*> -> value.filterNotNull().map { unwrap(it) }
            // TODO: https://github.com/google/ksp/issues/429
            // If the enum value is from compiled code KSP gives us the actual value an not
            // the KSType, so we wrap it as KspEnumEntry for consistency.
            is Enum<*> -> {
                val declaration =
                    env.resolver.getClassDeclarationByName(value::class.java.canonicalName)
                        ?: error("Cannot find KSClassDeclaration for Enum '$value'.")
                val valueDeclaration =
                    declaration.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .filter { it.classKind == ClassKind.ENUM_ENTRY }
                        .firstOrNull { it.simpleName.getShortName() == value.name }
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
                        //       return type should be Class<?>[] (only in sources).
                        //       https://github.com/google/ksp/issues/172
                        //       https://github.com/google/ksp/issues/214
                        !is List<*> -> listOf(result)
                        else -> result
                    }
                    .filterNotNull()
                    .map { value ->
                        KspAnnotationValue(
                            env,
                            valueArgument,
                            method,
                            valueType = valueType.componentType,
                            valueProvider = { convertValueToType(value, valueType.componentType) },
                        )
                    }
            }
            else -> convertValueToType(result, valueType)
        }
    }
}

private fun convertValueToType(value: Any?, valueType: XType): Any? {
    // Unlike Javac, KSP does not convert the value to the type declared on the annotation class's
    // annotation value automatically so we have to do that conversion manually here.
    if (value == null) return null
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
