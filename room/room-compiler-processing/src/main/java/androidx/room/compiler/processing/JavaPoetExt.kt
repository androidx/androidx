/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.room.compiler.processing

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import java.lang.Character.isISOControl
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Javapoet does not model NonType, unlike javac, which makes it hard to rely on TypeName for
 * common functionality (e.g. ability to implement XType.isLong as typename() == TypeName.LONG
 * instead of in the base class)
 *
 * For those cases, we have this hacky type so that we can always query TypeName on an XType.
 *
 * We should still strive to avoid these cases, maybe turn it to an error in tests.
 */
internal val JAVA_NONE_TYPE_NAME: JClassName =
    JClassName.get("androidx.room.compiler.processing.error", "NotAType")

@JvmOverloads
fun XAnnotation.toAnnotationSpec(includeDefaultValues: Boolean = true): AnnotationSpec {
  val builder = AnnotationSpec.builder(className)
  if (includeDefaultValues) {
    annotationValues.forEach { builder.addAnnotationValue(it) }
  } else {
    declaredAnnotationValues.forEach { builder.addAnnotationValue(it) }
  }
  return builder.build()
}

private fun AnnotationSpec.Builder.addAnnotationValue(annotationValue: XAnnotationValue) {
  annotationValue.apply {
    requireNotNull(value) { "value == null, constant non-null value expected for $name" }
    require(SourceVersion.isName(name)) { "not a valid name: $name" }
    when {
        hasListValue() -> asAnnotationValueList().forEach { addAnnotationValue(it) }
        hasAnnotationValue() -> addMember(name, "\$L", asAnnotation().toAnnotationSpec())
        hasEnumValue() -> addMember(
            name, "\$T.\$L", asEnum().enclosingElement.asClassName().java, asEnum().name
        )
        hasTypeValue() -> addMember(name, "\$T.class", asType().asTypeName().java)
        hasStringValue() -> addMember(name, "\$S", asString())
        hasFloatValue() -> addMember(name, "\$Lf", asFloat())
        hasCharValue() -> addMember(
            name, "'\$L'", characterLiteralWithoutSingleQuotes(asChar())
        )
        else -> addMember(name, "\$L", value)
    }
  }
}

private fun characterLiteralWithoutSingleQuotes(c: Char): String? {
    // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
    return when (c) {
        '\b' -> "\\b" /* \u0008: backspace (BS) */
        '\t' -> "\\t" /* \u0009: horizontal tab (HT) */
        '\n' -> "\\n" /* \u000a: linefeed (LF) */
        '\u000c' -> "\\u000c" /* \u000c: form feed (FF) */
        '\r' -> "\\r" /* \u000d: carriage return (CR) */
        '\"' -> "\"" /* \u0022: double quote (") */
        '\'' -> "\\'" /* \u0027: single quote (') */
        '\\' -> "\\\\" /* \u005c: backslash (\) */
        else -> if (isISOControl(c)) String.format("\\u%04x", c.code) else Character.toString(c)
    }
}

internal fun TypeMirror.safeTypeName(): TypeName = if (kind == TypeKind.NONE) {
    JAVA_NONE_TYPE_NAME
} else {
    TypeName.get(this)
}

/**
 * Adds the given element as the originating element for compilation.
 * see [TypeSpec.Builder.addOriginatingElement].
 */
fun TypeSpec.Builder.addOriginatingElement(element: XElement): TypeSpec.Builder {
    element.originatingElementForPoet()?.let(this::addOriginatingElement)
    return this
}

internal fun TypeName.rawTypeName(): TypeName {
    return if (this is ParameterizedTypeName) {
        this.rawType
    } else {
        this
    }
}

/**
 * Returns the unboxed TypeName for this if it can be unboxed, otherwise, returns this.
 */
internal fun TypeName.tryUnbox(): TypeName {
    return if (isBoxedPrimitive) {
        unbox()
    } else {
        this
    }
}

/**
 * Returns the boxed TypeName for this if it can be unboxed, otherwise, returns this.
 */
internal fun TypeName.tryBox(): TypeName {
    return try {
        box()
    } catch (err: AssertionError) {
        this
    }
}

/**
 * Helper class to create overrides for XExecutableElements with final parameters and correct
 * parameter names read from Kotlin Metadata.
 */
object MethodSpecHelper {
    /**
     * Creates an overriding [MethodSpec] for the given [XMethodElement] that
     * does everything in [overriding] and also mark all parameters as final
     */
    @JvmStatic
    fun overridingWithFinalParams(
        elm: XMethodElement,
        owner: XType
    ): MethodSpec.Builder {
        val asMember = elm.asMemberOf(owner)
        return overriding(
            executableElement = elm,
            resolvedType = asMember,
            Modifier.FINAL
        )
    }

    /**
     * Creates an overriding [MethodSpec] for the given [XMethodElement] where:
     * * parameter names are copied from KotlinMetadata when available
     * * [Override] annotation is added and other annotations are dropped
     * * thrown types are copied if the backing element is from java
     */
    @JvmStatic
    @JvmOverloads
    fun overriding(
        elm: XMethodElement,
        owner: XType =
            checkNotNull(elm.enclosingElement.type) {
                "Cannot override method without enclosing class"
            }
    ): MethodSpec.Builder {
        val asMember = elm.asMemberOf(owner)
        return overriding(
            executableElement = elm,
            resolvedType = asMember
        )
    }

    private fun overriding(
        executableElement: XMethodElement,
        resolvedType: XMethodType = executableElement.executableType,
        vararg paramModifiers: Modifier
    ): MethodSpec.Builder {
        return MethodSpec.methodBuilder(executableElement.jvmName).apply {
            addTypeVariables(
                resolvedType.typeVariables.map { it.asTypeName().java as JTypeVariableName }
            )
            resolvedType.parameterTypes.forEachIndexed { index, paramType ->
                addParameter(
                    ParameterSpec.builder(
                        paramType.asTypeName().java,
                        // The parameter name isn't guaranteed to be a valid java name, so we use
                        // the jvmName instead, which should be a valid java name.
                        executableElement.parameters[index].jvmName,
                        *paramModifiers
                    ).build()
                )
            }
            if (executableElement.isPublic()) {
                addModifiers(Modifier.PUBLIC)
            } else if (executableElement.isProtected()) {
                addModifiers(Modifier.PROTECTED)
            }
            addAnnotation(Override::class.java)
            // In Java, only the last argument can be a vararg so for suspend functions, it is never
            // a vararg function.
            varargs(!executableElement.isSuspendFunction() && executableElement.isVarArgs())
            executableElement.thrownTypes.forEach {
                addException(it.asTypeName().java)
            }
            returns(resolvedType.returnType.asTypeName().java)
        }
    }
}
