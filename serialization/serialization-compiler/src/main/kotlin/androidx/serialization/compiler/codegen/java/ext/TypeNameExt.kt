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

package androidx.serialization.compiler.codegen.java.ext

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.TypeElement

/** Convert a type element to a class name. */
internal fun TypeElement.toClassName(): ClassName {
    return ClassName.get(this)
}

/** This class name with type parameters.  */
internal fun ClassName.parameterized(vararg typeArguments: TypeName): ParameterizedTypeName {
    return ParameterizedTypeName.get(this, *typeArguments)
}

/** This type name annotated with NonNull. */
internal val TypeName.nonNull: TypeName
    get() {
        require(!isPrimitive) { "@NonNull is not applicable to primitive type: $this" }
        require(annotations.none { it.type == NULLABLE.type }) {
            "@NonNull conflicts with @Nullable present on type: ${withoutAnnotations()}"
        }
        return if (annotations.any { it.type == NON_NULL.type }) this else annotated(
            NON_NULL
        )
    }

private val NON_NULL: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("androidx.annotation", "NonNull")).build()

/** This type name annotated with Nullable. */
internal val TypeName.nullable: TypeName
    get() {
        require(!isPrimitive) { "@Nullable is not applicable to primitive type: $this" }
        require(annotations.none { it.type == NON_NULL.type }) {
            "@Nullable conflicts with @NonNull present on type: ${withoutAnnotations()}"
        }
        return if (annotations.any { it.type == NULLABLE.type }) this else annotated(NULLABLE)
    }

private val NULLABLE: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("androidx.annotation", "Nullable")).build()