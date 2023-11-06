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

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XRoundEnv
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import kotlin.reflect.KClass

internal class KspRoundEnv(
    private val env: KspProcessingEnv,
    override val isProcessingOver: Boolean
) : XRoundEnv {
    override val rootElements: Set<XElement>
        get() = TODO("not supported")

    override fun getElementsAnnotatedWith(klass: KClass<out Annotation>): Set<XElement> {
        return getElementsAnnotatedWith(
            annotationQualifiedName = klass.qualifiedName ?: error("No qualified name for $klass")
        )
    }

    @OptIn(KspExperimental::class)
    override fun getElementsAnnotatedWith(annotationQualifiedName: String): Set<XElement> {
        if (annotationQualifiedName == "*") {
            return emptySet()
        }
        return buildSet {
            env.resolver.getSymbolsWithAnnotation(annotationQualifiedName)
                .forEach { symbol ->
                    when (symbol) {
                        is KSPropertyDeclaration -> {
                           add(KspFieldElement.create(env, symbol))
                        }

                        is KSClassDeclaration -> {
                            when (symbol.classKind) {
                                ClassKind.ENUM_ENTRY ->
                                    add(KspEnumEntry.create(env, symbol))
                                else -> add(KspTypeElement.create(env, symbol))
                            }
                        }

                        is KSFunctionDeclaration -> {
                            add(KspExecutableElement.create(env, symbol))
                        }

                        is KSPropertyAccessor -> {
                            if (symbol.receiver.isStatic() &&
                                symbol.receiver.parentDeclaration is KSClassDeclaration &&
                                (symbol.receiver.hasJvmStaticAnnotation() ||
                                    symbol.hasJvmStaticAnnotation())) {
                                // Getter/setter can be copied from companion object to its
                                // outer class if the field is annotated with @JvmStatic.
                                add(
                                    KspSyntheticPropertyMethodElement.create(
                                        env, symbol, isSyntheticStatic = true
                                    )
                                )
                            }
                            // static fields are the properties that are coming from the companion.
                            // Whether we'll generate method for it or not depends on the JVMStatic
                            // annotation
                            if (!symbol.receiver.isStatic() ||
                                symbol.receiver.hasJvmStaticAnnotation() ||
                                symbol.hasJvmStaticAnnotation() ||
                                symbol.receiver.parentDeclaration !is KSClassDeclaration
                            ) {
                                add(
                                    KspSyntheticPropertyMethodElement.create(
                                        env, symbol, isSyntheticStatic = false
                                    )
                                )
                            }
                        }

                        is KSValueParameter -> {
                            add(KspExecutableParameterElement.create(env, symbol))
                        }

                        else ->
                            error("Unsupported $symbol with annotation $annotationQualifiedName")
                    }
                }

            env.resolver.getPackagesWithAnnotation(annotationQualifiedName)
                .forEach { packageName ->
                    add(KspPackageElement(env, packageName))
                }
        }
        .filter {
            // Due to the bug in https://github.com/google/ksp/issues/1198, KSP may incorrectly
            // copy annotations from a constructor KSValueParameter to its KSPropertyDeclaration
            // which we remove manually, so check here to make sure this is in sync with the
            // actual annotations on the element.
            it.getAllAnnotations().any { it.qualifiedName == annotationQualifiedName }
        }.toSet()
    }
}
