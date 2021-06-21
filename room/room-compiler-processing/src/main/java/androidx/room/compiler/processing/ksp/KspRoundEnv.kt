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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import kotlin.reflect.KClass

internal class KspRoundEnv(
    private val env: KspProcessingEnv
) : XRoundEnv {
    override val rootElements: Set<XElement>
        get() = TODO("not supported")

    override fun getElementsAnnotatedWith(klass: KClass<out Annotation>): Set<XElement> {
        return getElementsAnnotatedWith(
            annotationQualifiedName = klass.qualifiedName ?: error("No qualified name for $klass")
        )
    }

    override fun getElementsAnnotatedWith(annotationQualifiedName: String): Set<XElement> {
        return env.resolver.getSymbolsWithAnnotation(annotationQualifiedName)
            .map { symbol ->
                when (symbol) {
                    is KSPropertyDeclaration -> {
                        KspFieldElement.create(env, symbol)
                    }
                    is KSClassDeclaration -> {
                        KspTypeElement.create(env, symbol)
                    }
                    is KSFunctionDeclaration -> {
                        KspExecutableElement.create(env, symbol)
                    }
                    is KSPropertyAccessor -> {
                        KspSyntheticPropertyMethodElement.create(env, symbol)
                    }
                    else -> error("Unsupported $symbol with annotation $annotationQualifiedName")
                }
            }.toSet()
    }
}