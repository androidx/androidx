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
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements

private val NONNULL_ANNOTATIONS = arrayOf(
    androidx.annotation.NonNull::class.java,
    org.jetbrains.annotations.NotNull::class.java
)

private val NULLABLE_ANNOTATIONS = arrayOf(
    androidx.annotation.Nullable::class.java,
    org.jetbrains.annotations.Nullable::class.java
)

/**
 * Returns all fields including private fields (including private fields in super). Removes
 * duplicate fields if class has a field with the same name as the parent.
 * Note that enum constants are not included in the list even thought they are fields in java.
 * To access enum constants, use [JavacTypeElement.JavacEnumTypeElement].
 */
internal fun TypeElement.getAllFieldsIncludingPrivateSupers(
    elementUtils: Elements
): Set<VariableElement> {
    val selection = ElementFilter
        .fieldsIn(elementUtils.getAllMembers(this))
        .filterIsInstance<VariableElement>()
        .filterNot { it.kind == ElementKind.ENUM_CONSTANT }
        .toMutableSet()
    val selectionNames = selection.mapTo(mutableSetOf()) {
        it.simpleName
    }
    if (superclass.kind != TypeKind.NONE) {
        val superFields = MoreTypes.asTypeElement(superclass)
            .getAllFieldsIncludingPrivateSupers(elementUtils)
        // accept super fields only if the name does not conflict
        superFields.forEach { superField ->
            if (selectionNames.add(superField.simpleName)) {
                selection.add(superField)
            }
        }
    }
    return selection
}

@Suppress("UnstableApiUsage")
private fun Element.hasAnyOf(annotations: Array<Class<out Annotation>>) = annotations.any {
    MoreElements.isAnnotationPresent(this, it)
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