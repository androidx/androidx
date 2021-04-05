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

import androidx.annotation.VisibleForTesting
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XRoundEnv
import androidx.room.compiler.processing.XTypeElement
import com.google.devtools.ksp.symbol.KSClassDeclaration

@VisibleForTesting
internal class KspRoundEnv(
    private val env: KspProcessingEnv
) : XRoundEnv {
    override val rootElements: Set<XElement>
        get() = TODO("not supported")

    override fun getTypeElementsAnnotatedWith(annotationQualifiedName: String): Set<XTypeElement> {
        return env.resolver.getSymbolsWithAnnotation(
            annotationQualifiedName
        ).filterIsInstance<KSClassDeclaration>()
            .map {
                env.wrapClassDeclaration(it)
            }.toSet()
    }
}