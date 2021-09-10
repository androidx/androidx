/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.hilt.assisted

import androidx.hilt.ClassNames
import androidx.hilt.ext.hasAnnotation
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.VariableElement

/**
 * Data class that represents a binding request for an assisted injected type.
 */
internal data class DependencyRequest(
    val name: String,
    val type: TypeName,
    val isAssisted: Boolean,
    val qualifier: AnnotationSpec? = null
) {
    val isProvider = type is ParameterizedTypeName && type.rawType == ClassNames.PROVIDER

    val providerTypeName: TypeName = let {
        val type = if (isProvider) {
            type // Do not wrap a Provider inside another Provider.
        } else {
            ParameterizedTypeName.get(
                ClassNames.PROVIDER,
                type.box()
            )
        }
        if (qualifier != null) {
            type.annotated(qualifier)
        } else {
            type
        }
    }
}

internal fun VariableElement.toDependencyRequest(): DependencyRequest {
    val qualifier = annotationMirrors.find {
        it.annotationType.asElement().hasAnnotation("javax.inject.Qualifier")
    }?.let { AnnotationSpec.get(it) }
    val type = TypeName.get(asType())
    return DependencyRequest(
        name = simpleName.toString(),
        type = type,
        isAssisted = (
            hasAnnotation(ClassNames.ANDROIDX_ASSISTED.canonicalName()) ||
                hasAnnotation(ClassNames.ASSISTED.canonicalName())
            ) && qualifier == null,
        qualifier = qualifier
    )
}