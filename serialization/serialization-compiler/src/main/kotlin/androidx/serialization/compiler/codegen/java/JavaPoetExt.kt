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

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Element

internal const val L = "\$L"
internal const val N = "\$N"
internal const val S = "\$S"
internal const val T = "\$T"

internal fun nameAllocatorOf(vararg names: String): NameAllocator {
    return NameAllocator().apply { names.forEach { newName(it, it) } }
}

internal inline fun buildClass(
    className: ClassName,
    javaGenEnv: JavaGenEnvironment,
    vararg originatingElements: Element?,
    init: TypeSpec.Builder.() -> Unit
): JavaFile {
    return TypeSpec.classBuilder(className).apply {
        init()
        afterInit(javaGenEnv, originatingElements)
    }.toJavaFile(className)
}

internal fun TypeSpec.Builder.toJavaFile(className: ClassName): JavaFile {
    return JavaFile.builder(className.packageName(), build()).indent("    ").build()
}

internal inline fun TypeSpec.Builder.constructor(init: MethodSpec.Builder.() -> Unit) {
    addMethod(MethodSpec.constructorBuilder().apply(init).build())
}

internal inline fun TypeSpec.Builder.method(name: String, init: MethodSpec.Builder.() -> Unit) {
    addMethod(MethodSpec.methodBuilder(name).apply(init).build())
}

internal fun MethodSpec.Builder.parameter(
    name: String,
    type: TypeName,
    vararg annotations: AnnotationSpec?
) {
    addParameter(ParameterSpec.builder(type, name).run {
        annotations.forEach { if (it != null) addAnnotation(it) }
        build()
    })
}

internal fun MethodSpec.Builder.returns(
    type: TypeName,
    vararg annotations: AnnotationSpec?
) {
    returns(type)
    annotations.forEach { if (it != null) addAnnotation(it) }
}

internal inline fun MethodSpec.Builder.controlFlow(
    format: String,
    vararg args: Any,
    body: MethodSpec.Builder.() -> Unit
) {
    beginControlFlow(format, *args)
    body()
    endControlFlow()
}

internal inline fun MethodSpec.Builder.switchCase(
    format: String,
    vararg args: Any,
    body: MethodSpec.Builder.() -> Unit
) {
    addCode("case $format:\n$>", *args)
    body()
    addCode("$<")
}

internal inline fun MethodSpec.Builder.switchDefault(
    body: MethodSpec.Builder.() -> Unit
) {
    addCode("default:\n$>")
    body()
    addCode("$<")
}

internal fun TypeSpec.Builder.afterInit(
    javaGenEnv: JavaGenEnvironment,
    originatingElements: Array<out Element?>
) {
    javaGenEnv.applyGenerated(this)
    originatingElements.forEach { if (it != null) addOriginatingElement(it) }
}
