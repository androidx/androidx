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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.InternalXAnnotation
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XValueArgument
import com.google.auto.common.AnnotationMirrors
import javax.lang.model.element.AnnotationMirror

internal class JavacAnnotation(
    val env: JavacProcessingEnv,
    val mirror: AnnotationMirror
) : InternalXAnnotation {

    override val name: String
        get() = mirror.annotationType.asElement().simpleName.toString()

    override val type: XType by lazy {
        JavacDeclaredType(env, mirror.annotationType, XNullability.NONNULL)
    }

    override val valueArguments: List<XValueArgument>
        get() {
            return AnnotationMirrors.getAnnotationValuesWithDefaults(mirror)
                .map { (executableElement, annotationValue) ->
                    JavacValueArgument(env, executableElement, annotationValue)
                }
        }

    override fun <T : Annotation> asAnnotationBox(annotationClass: Class<T>): XAnnotationBox<T> {
        return mirror.box(env, annotationClass)
    }
}