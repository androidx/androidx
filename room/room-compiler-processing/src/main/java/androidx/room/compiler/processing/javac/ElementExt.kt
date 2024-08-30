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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XNullability
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ExecutableType
import javax.lang.model.util.Types
import kotlin.coroutines.Continuation

private val NONNULL_ANNOTATIONS =
    arrayOf("androidx.annotation.NonNull", "org.jetbrains.annotations.NotNull")

private val NULLABLE_ANNOTATIONS =
    arrayOf("androidx.annotation.Nullable", "org.jetbrains.annotations.Nullable")

@Suppress("UnstableApiUsage")
private fun Element.hasAnyOf(annotations: Array<String>) =
    annotationMirrors.any { annotationMirror ->
        val annotationTypeElement = MoreElements.asType(annotationMirror.annotationType.asElement())
        annotations.any { annotationTypeElement.qualifiedName.contentEquals(it) }
    }

internal val Element.nullability: XNullability
    get() {
        // Get the type of the element: if this is a method, use the return type instead of the full
        // method type since the return is what determines nullability.
        val asType =
            asType().let {
                when (it) {
                    is ExecutableType -> it.returnType
                    else -> it
                }
            }
        return if (asType.kind.isPrimitive || hasAnyOf(NONNULL_ANNOTATIONS)) {
            XNullability.NONNULL
        } else if (hasAnyOf(NULLABLE_ANNOTATIONS)) {
            XNullability.NULLABLE
        } else {
            XNullability.UNKNOWN
        }
    }

internal fun Element.requireEnclosingType(env: JavacProcessingEnv): JavacTypeElement {
    return checkNotNull(enclosingType(env)) { "Cannot find required enclosing type for $this" }
}

@Suppress("UnstableApiUsage")
internal fun Element.enclosingType(env: JavacProcessingEnv): JavacTypeElement? {
    return if (MoreElements.isType(enclosingElement)) {
        env.wrapTypeElement(MoreElements.asType(enclosingElement))
    } else {
        null
    }
}

/**
 * Tests whether one suspend function, as a member of a given types, overrides another suspend
 * function.
 *
 * This method assumes function one and two are suspend methods, i.e. they both return Object, have
 * at least one parameter and the last parameter is of type Continuation. This method is similar to
 * MoreElements.overrides() but doesn't check isSubsignature() due to Continuation's type arg being
 * covariant, instead the equivalent is done by checking each parameter explicitly.
 */
internal fun suspendOverrides(
    overrider: ExecutableElement,
    overridden: ExecutableElement,
    owner: TypeElement,
    typeUtils: Types
): Boolean {
    if (overrider.simpleName != overridden.simpleName) {
        return false
    }
    if (overrider.enclosingElement == overridden.enclosingElement) {
        return false
    }
    if (overridden.modifiers.contains(Modifier.STATIC)) {
        return false
    }
    if (overridden.modifiers.contains(Modifier.PRIVATE)) {
        return false
    }
    val overriddenType = overridden.enclosingElement as? TypeElement ?: return false
    if (
        !typeUtils.isSubtype(
            typeUtils.erasure(owner.asType()),
            typeUtils.erasure(overriddenType.asType())
        )
    ) {
        return false
    }
    val ownerType = MoreTypes.asDeclared(owner.asType())
    val overriderExecutable = MoreTypes.asExecutable(typeUtils.asMemberOf(ownerType, overrider))
    val overriddenExecutable = MoreTypes.asExecutable(typeUtils.asMemberOf(ownerType, overridden))
    if (overriderExecutable.parameterTypes.size != overriddenExecutable.parameterTypes.size) {
        return false
    }
    val continuationTypeName = TypeName.get(Continuation::class.java)
    val overriderLastParamTypeName =
        (TypeName.get(overriderExecutable.parameterTypes.last()) as? ParameterizedTypeName)?.rawType
    check(overriderLastParamTypeName == continuationTypeName) {
        "Expected $overriderLastParamTypeName to be $continuationTypeName"
    }
    val overriddenLastParamTypeName =
        (TypeName.get(overriddenExecutable.parameterTypes.last()) as? ParameterizedTypeName)
            ?.rawType
    check(overriddenLastParamTypeName == continuationTypeName) {
        "Expected $overriddenLastParamTypeName to be $continuationTypeName"
    }
    val overriderContinuationTypeArg =
        MoreTypes.asDeclared(overriderExecutable.parameterTypes.last()).typeArguments.single().let {
            it.extendsBound() ?: it
        }
    val overriddenContinuationTypeArg =
        MoreTypes.asDeclared(overriddenExecutable.parameterTypes.last())
            .typeArguments
            .single()
            .let { it.extendsBound() ?: it }
    if (
        !typeUtils.isSameType(
            typeUtils.erasure(overriderContinuationTypeArg),
            typeUtils.erasure(overriddenContinuationTypeArg)
        )
    ) {
        return false
    }
    if (overriddenExecutable.parameterTypes.size >= 2) {
        overriderExecutable.parameterTypes
            .zip(overriddenExecutable.parameterTypes)
            .dropLast(1)
            .forEach { (overriderParam, overriddenParam) ->
                if (
                    !typeUtils.isSameType(
                        typeUtils.erasure(overriderParam),
                        typeUtils.erasure(overriddenParam)
                    )
                ) {
                    return false
                }
            }
    }
    return true
}
