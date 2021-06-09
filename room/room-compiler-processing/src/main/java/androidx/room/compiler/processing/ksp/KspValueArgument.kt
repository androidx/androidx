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

import androidx.room.compiler.processing.XAnnotationValue
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument

internal class KspValueArgument(
    val env: KspProcessingEnv,
    val valueArgument: KSValueArgument,
    val isListType: () -> Boolean,
) : XAnnotationValue {

    override val name: String
        get() = valueArgument.name?.asString()
            ?: error("Value argument $this does not have a name.")

    override val value: Any? by lazy { valueArgument.unwrap() }

    private fun KSValueArgument.unwrap(): Any? {
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
                else -> value
            }
        }
        return unwrap(value).let { result ->
            // TODO: 5/24/21 KSP does not wrap a single item in a list, even though the
            // return type should be Class<?>[] (only in sources).
            // https://github.com/google/ksp/issues/172
            // https://github.com/google/ksp/issues/214
            if (result !is List<*> && isListType()) {
                listOf(result)
            } else {
                result
            }
        }
    }
}
