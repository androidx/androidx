/*
 * Copyright 2023 The Android Open Source Project
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

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import java.lang.annotation.Repeatable
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter

/** Returns true if the given [AnnotationMirror] represents a repeatable annotation. */
internal fun AnnotationMirror.isRepeatable(): Boolean {
    val valueType =
        ElementFilter.methodsIn(MoreTypes.asTypeElement(annotationType).enclosedElements)
            .singleOrNull { it.simpleName.toString() == "value" }
            ?.returnType

    // The contract of a repeatable annotation requires that the container annotation have a
    // single "default" method that returns an array typed with the repeatable annotation type.
    if (valueType == null || valueType.kind != TypeKind.ARRAY) {
        return false
    }
    val componentType = MoreTypes.asArray(valueType).componentType
    if (componentType.kind != TypeKind.DECLARED) {
        return false
    }
    val componentElement = MoreTypes.asDeclared(componentType).asElement()

    // Ideally we would read the value of the Repeatable annotation to get the container class
    // type and check that it matches "this" type. However, there seems to be a KSP bug where
    // the value of Repeatable is not present so the best we can do is check that all the nested
    // members are annotated with repeatable.
    // https://github.com/google/ksp/issues/358
    return MoreElements.isAnnotationPresent(componentElement, Repeatable::class.java) ||
        // The java and kotlin versions of Repeatable are not interchangeable.
        // https://github.com/google/ksp/issues/459 asks whether the built in type
        // mapper should convert them, but it may not be possible because there are
        // differences to how they work (eg different parameters).
        MoreElements.isAnnotationPresent(componentElement, kotlin.annotation.Repeatable::class.java)
}
