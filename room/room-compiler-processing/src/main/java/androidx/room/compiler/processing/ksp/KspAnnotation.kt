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
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XAnnotationValue
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType

internal class KspAnnotation(
    val env: KspProcessingEnv,
    val ksAnnotated: KSAnnotation
) : InternalXAnnotation {

    val ksType: KSType by lazy { ksAnnotated.annotationType.resolve() }

    override val name: String
        get() = ksAnnotated.shortName.asString()

    override val qualifiedName: String
        get() = ksType.declaration.qualifiedName?.asString() ?: ""

    override val type: XType by lazy {
        env.wrap(ksType, allowPrimitives = true)
    }

    override val annotationValues: List<XAnnotationValue> by lazy {
        ksAnnotated.arguments.map { KspAnnotationValue(env, this, it) }
    }

    override fun <T : Annotation> asAnnotationBox(annotationClass: Class<T>): XAnnotationBox<T> {
        return KspAnnotationBox(
            env = env,
            annotationClass = annotationClass,
            annotation = ksAnnotated
        )
    }
}