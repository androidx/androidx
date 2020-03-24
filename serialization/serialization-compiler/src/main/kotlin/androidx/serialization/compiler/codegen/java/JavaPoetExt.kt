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

package androidx.serialization.compiler.codegen.java

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/** This type name annotated with NonNull. */
internal val TypeName.nonNull: TypeName
    get() {
        require(!isPrimitive) { "@NonNull is not applicable to primitive type: $this" }
        require(annotations.none { it.type == NULLABLE.type }) {
            "@NonNull conflicts with @Nullable present on type: ${withoutAnnotations()}"
        }
        return if (annotations.any { it.type == NON_NULL.type }) this else annotated(NON_NULL)
    }

/** This type name annotated with Nullable. */
internal val TypeName.nullable: TypeName
    get() {
        require(!isPrimitive) { "@Nullable is not applicable to primitive type: $this" }
        require(annotations.none { it.type == NON_NULL.type }) {
            "@Nullable conflicts with @NonNull present on type: ${withoutAnnotations()}"
        }
        return if (annotations.any { it.type == NULLABLE.type }) this else annotated(NULLABLE)
    }

/** This class name with type parameters.  */
internal fun ClassName.parameterized(vararg typeArguments: TypeName): ParameterizedTypeName {
    return ParameterizedTypeName.get(this, *typeArguments)
}

/** Type-safe builder for a field. */
internal inline fun TypeSpec.Builder.field(
    type: TypeName,
    name: String,
    vararg modifiers: Modifier,
    init: FieldSpec.Builder.() -> Unit
) {
    addField(FieldSpec.builder(type, name, *modifiers).apply(init).build())
}

/** Type-safe builder for a method. */
internal inline fun TypeSpec.Builder.method(
    name: String,
    vararg modifiers: Modifier,
    init: MethodSpec.Builder.() -> Unit
) {
    addMethod(MethodSpec.methodBuilder(name).run {
        addModifiers(*modifiers)
        init()
        build()
    })
}

internal inline fun TypeSpec.Builder.overrideMethod(
    name: String,
    vararg modifiers: Modifier,
    init: MethodSpec.Builder.() -> Unit
) {
    method(name, *modifiers) {
        addAnnotation(OVERRIDE)
        init()
    }
}

/** Type-safe builder for a control flow within a method. */
internal inline fun MethodSpec.Builder.controlFlow(
    format: String,
    vararg args: Any?,
    body: MethodSpec.Builder.() -> Unit
) {
    beginControlFlow(format, *args)
    body()
    endControlFlow()
}

/** Type-safe builder for a `case` block within a `switch`. */
internal inline fun MethodSpec.Builder.switchCase(
    format: String,
    vararg args: Any?,
    body: MethodSpec.Builder.() -> Unit
) {
    addCode("case $format:\$>\n", *args)
    body()
    addCode("\$<")
}

/** Type-safe builder for a `default` block within a `switch`. */
internal inline fun MethodSpec.Builder.switchDefault(body: MethodSpec.Builder.() -> Unit) {
    addCode("default:\n\$>")
    body()
    addCode("\$<")
}
