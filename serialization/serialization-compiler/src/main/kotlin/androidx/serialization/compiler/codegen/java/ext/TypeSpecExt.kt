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
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/** Builds a new class type spec. */
internal inline fun buildClass(
    className: ClassName,
    vararg modifiers: Modifier,
    init: TypeSpec.Builder.() -> Unit
): JavaFile {
    return TypeSpec.classBuilder(className.topLevelClassName()).run {
        addModifiers(*modifiers)
        init()
        build()
    }.toJavaFile(className.packageName())
}

internal fun TypeSpec.toJavaFile(packageName: String): JavaFile {
    return JavaFile.builder(packageName, this).indent(INDENTATION).build()
}

private const val INDENTATION = "    "

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

private val OVERRIDE: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("java.lang", "Override")).build()