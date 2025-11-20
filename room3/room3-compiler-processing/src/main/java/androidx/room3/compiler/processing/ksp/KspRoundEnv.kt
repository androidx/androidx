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

import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XRoundEnv
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSValueParameter
import kotlin.reflect.KClass

internal class KspRoundEnv(
    private val env: KspProcessingEnv? // null on last round (i.e. isProcessingOver == true)
) : XRoundEnv {
    override val isProcessingOver: Boolean
        get() = env == null

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
        if (isProcessingOver) {
            return emptySet()
        }
        checkNotNull(env)
        return buildSet {
                env.resolver.getSymbolsWithAnnotation(annotationQualifiedName).forEach { symbol ->
                    when (symbol) {
                        is KSPropertyDeclaration -> add(env.wrapPropertyDeclaration(symbol))
                        is KSClassDeclaration -> add(env.wrapClassDeclaration(symbol))
                        is KSFunctionDeclaration ->
                            env.wrapFunctionDeclaration(symbol).let { method ->
                                add(method)
                                (method as? KspMethodElement)?.syntheticStaticMethod?.let {
                                    add(it)
                                }
                            }
                        is KSValueParameter -> add(env.wrapValueParameter(symbol))
                        is KSPropertyGetter ->
                            env.wrapPropertyDeclaration(symbol.receiver).let { property ->
                                property.getter?.let { add(it) }
                                property.getter?.syntheticStaticAccessor?.let { add(it) }
                            }
                        is KSPropertySetter ->
                            env.wrapPropertyDeclaration(symbol.receiver).let { property ->
                                property.setter?.let { add(it) }
                                property.setter?.syntheticStaticAccessor?.let { add(it) }
                            }
                        else ->
                            error("Unsupported $symbol with annotation $annotationQualifiedName")
                    }
                }

                env.resolver.getPackagesWithAnnotation(annotationQualifiedName).forEach {
                    packageName ->
                    add(KspPackageElement(env, packageName))
                }
            }
            .filter {
                // Due to the bug in https://github.com/google/ksp/issues/1198, KSP may incorrectly
                // copy annotations from a constructor KSValueParameter to its KSPropertyDeclaration
                // which we remove manually, so check here to make sure this is in sync with the
                // actual annotations on the element.
                it.getAllAnnotations().any { it.qualifiedName == annotationQualifiedName }
            }
            .toSet()
    }
}
