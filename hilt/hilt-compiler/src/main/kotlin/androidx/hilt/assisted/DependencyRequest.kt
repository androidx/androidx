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
import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.compiler.processing.XVariableElement
import androidx.room.compiler.processing.toAnnotationSpec
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

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

internal fun XVariableElement.toDependencyRequest(): DependencyRequest {
    val qualifier = getAllAnnotations().find {
        it.qualifiedName == "javax.inject.Qualifier"
    }?.toAnnotationSpec(includeDefaultValues = false)
    return DependencyRequest(
        name = this.name,
        type = this.type.asTypeName().toJavaPoet(),
        isAssisted = (
            this.hasAnnotation(ClassNames.ANDROIDX_ASSISTED) ||
                this.hasAnnotation(ClassNames.ASSISTED)
            ) && qualifier == null,
        qualifier = qualifier
    )
}
