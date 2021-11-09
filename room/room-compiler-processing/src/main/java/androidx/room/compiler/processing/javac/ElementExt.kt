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
import javax.lang.model.element.Element

private val NONNULL_ANNOTATIONS = arrayOf(
    "androidx.annotation.NonNull",
    "org.jetbrains.annotations.NotNull"
)

private val NULLABLE_ANNOTATIONS = arrayOf(
    "androidx.annotation.Nullable",
    "org.jetbrains.annotations.Nullable"
)

@Suppress("UnstableApiUsage")
private fun Element.hasAnyOf(annotations: Array<String>) =
    annotationMirrors.any { annotationMirror ->
        val annotationTypeElement = MoreElements.asType(annotationMirror.annotationType.asElement())
        annotations.any { annotationTypeElement.qualifiedName.contentEquals(it) }
    }

internal val Element.nullability: XNullability
    get() = if (asType().kind.isPrimitive || hasAnyOf(NONNULL_ANNOTATIONS)) {
        XNullability.NONNULL
    } else if (hasAnyOf(NULLABLE_ANNOTATIONS)) {
        XNullability.NULLABLE
    } else {
        XNullability.UNKNOWN
    }

internal fun Element.requireEnclosingType(env: JavacProcessingEnv): JavacTypeElement {
    return checkNotNull(enclosingType(env)) {
        "Cannot find required enclosing type for $this"
    }
}

@Suppress("UnstableApiUsage")
internal fun Element.enclosingType(env: JavacProcessingEnv): JavacTypeElement? {
    return if (MoreElements.isType(enclosingElement)) {
        env.wrapTypeElement(MoreElements.asType(enclosingElement))
    } else {
        null
    }
}