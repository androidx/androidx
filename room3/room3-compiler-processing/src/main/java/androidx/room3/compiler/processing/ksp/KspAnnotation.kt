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

import androidx.room3.compiler.processing.InternalXAnnotation
import androidx.room3.compiler.processing.XType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Origin

internal class KspAnnotation(val env: KspProcessingEnv, val ksAnnotation: KSAnnotation) :
    InternalXAnnotation() {

    val ksType: KSType by lazy { ksAnnotation.annotationType.resolve() }

    override val name: String
        get() = ksAnnotation.shortName.asString()

    override val qualifiedName: String
        get() = ksType.declaration.qualifiedName?.asString() ?: ""

    override val type: XType by lazy { env.wrap(ksType, allowPrimitives = true) }

    override val declaredAnnotationValues: List<KspAnnotationValue> by lazy {
        annotationValues.filterNot { it.valueArgument.origin == Origin.SYNTHETIC }
    }

    override val defaultValues: List<KspAnnotationValue> by lazy {
        wrap(ksAnnotation.defaultArguments)
    }

    override val annotationValues: List<KspAnnotationValue> by lazy {
        val defaultValuesByName by lazy { defaultValues.associateBy { it.name } }
        wrap(ksAnnotation.arguments).mapNotNull {
            if (it.value != null) {
                return@mapNotNull it
            }
            val default = defaultValuesByName[it.name]
            if (default?.value != null) {
                return@mapNotNull default
            }
            null
        }
    }

    private fun wrap(values: List<KSValueArgument>): List<KspAnnotationValue> {
        // KSAnnotation.arguments / KSAnnotation.defaultArguments isn't guaranteed to have the same
        // ordering as declared in the annotation declaration, so we order it manually using a map
        // from name to index (see https://github.com/google/ksp/issues/2616).
        return values
            .map { env.wrapAnnotationValue(annotation = ksAnnotation, value = it) }
            .sortedBy { indexByName[it.name] }
    }

    // A map of annotation value name to index.
    private val indexByName: Map<String, Int> by lazy {
        typeElement
            .getDeclaredMethods()
            .mapIndexed { index, method -> (method.propertyName ?: method.name) to index }
            .toMap()
    }
}
