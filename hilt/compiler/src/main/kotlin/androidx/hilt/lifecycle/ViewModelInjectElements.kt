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

package androidx.hilt.lifecycle

import androidx.hilt.ClassNames
import androidx.hilt.ext.hasAnnotation
import com.google.auto.common.MoreElements
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * Data class that represents a Hilt injected ViewModel
 */
internal data class ViewModelInjectElements(
    val typeElement: TypeElement,
    val constructorElement: ExecutableElement
) {
    val className = ClassName.get(typeElement)

    val factoryClassName = ClassName.get(
        MoreElements.getPackage(typeElement).qualifiedName.toString(),
        "${className.simpleNames().joinToString("_")}_AssistedFactory")

    val factorySuperTypeName = ParameterizedTypeName.get(
        ClassNames.VIEW_MODEL_ASSISTED_FACTORY,
        className)

    val moduleClassName = ClassName.get(
        MoreElements.getPackage(typeElement).qualifiedName.toString(),
        "${className.simpleNames().joinToString("_")}_HiltModule")

    val dependencyRequests = constructorElement.parameters.map { it.toDependencyRequest() }
}

/**
 * Data class that represents a binding request from the injected ViewModel
 */
internal data class DependencyRequest(
    val name: String,
    val type: TypeName,
    val qualifier: AnnotationSpec? = null
) {
    val isProvider = type is ParameterizedTypeName && type.rawType == ClassNames.PROVIDER

    val providerTypeName: TypeName = let {
        val type = if (isProvider) {
            type // Do not wrap a Provider inside another Provider.
        } else {
            ParameterizedTypeName.get(ClassNames.PROVIDER, type.box())
        }
        if (qualifier != null) {
            type.annotated(qualifier)
        } else {
            type
        }
    }
}

internal fun VariableElement.toDependencyRequest() =
    DependencyRequest(
        name = simpleName.toString(),
        type = TypeName.get(asType()),
        qualifier = annotationMirrors.find {
            it.annotationType.asElement().hasAnnotation("javax.inject.Qualifier")
        }?.let { AnnotationSpec.get(it) }
    )