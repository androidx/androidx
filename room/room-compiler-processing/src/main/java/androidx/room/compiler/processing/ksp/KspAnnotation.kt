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

import androidx.room.compiler.processing.InternalXAnnotation
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Origin

internal class KspAnnotation(
    val env: KspProcessingEnv,
    val ksAnnotated: KSAnnotation
) : InternalXAnnotation() {

    val ksType: KSType by lazy {
        ksAnnotated.annotationType.resolve()
    }

    override val name: String
        get() = ksAnnotated.shortName.asString()

    override val qualifiedName: String
        get() = ksType.declaration.qualifiedName?.asString() ?: ""

    override val type: XType by lazy {
        env.wrap(ksType, allowPrimitives = true)
    }

    override val declaredAnnotationValues: List<XAnnotationValue> by lazy {
        annotationValues.filterNot {
          (it as KspAnnotationValue).valueArgument.origin == Origin.SYNTHETIC
        }
    }

    override val defaultValues: List<XAnnotationValue> by lazy {
        wrap(ksAnnotated.defaultArguments)
    }

    override val annotationValues: List<XAnnotationValue> by lazy {
        wrap(ksAnnotated.arguments)
    }

    private fun wrap(source: List<KSValueArgument>): List<XAnnotationValue> {
        // KSAnnotated.arguments / KSAnnotated.defaultArguments isn't guaranteed to have the same
        // ordering as declared in the annotation declaration, so we order it manually using a map
        // from name to index.
        val indexByName = typesByName.keys.mapIndexed { index, name -> name to index }.toMap()
        return source.map {
            val valueName = it.name?.asString()
                ?: error("Value argument $it does not have a name.")
            val valueType = typesByName[valueName]
                ?: error("Value type not found for $valueName.")
            KspAnnotationValue(env, this, valueType, it)
        }.sortedBy { indexByName[it.name] }
    }

    // A map of annotation value name to type.
    private val typesByName: Map<String, XType> by lazy {
        buildMap {
            typeElement.getDeclaredMethods()
                .filter {
                      // Whether the annotation value is being treated as property or
                      // abstract method depends on the actual usage of the annotation.
                      // If the annotation is being used on Java source, then the annotation
                      // value will have a corresponding method element, otherwise, it
                      // will become a kotlin property.
                    if ((typeElement as KspTypeElement).declaration
                            .getConstructors()
                            .single().parameters
                            .isNotEmpty()) {
                        it.isKotlinPropertyMethod()
                    } else {
                        it.isAbstract()
                    }
                }.forEach {
                    put(it.name, it.returnType)
                    put(it.jvmName, it.returnType)
                }
            }
    }

    override fun <T : Annotation> asAnnotationBox(annotationClass: Class<T>): XAnnotationBox<T> {
        return KspAnnotationBox(
            env = env,
            annotationClass = annotationClass,
            annotation = ksAnnotated
        )
    }
}
