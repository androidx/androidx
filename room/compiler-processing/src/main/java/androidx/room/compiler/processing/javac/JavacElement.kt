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

import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XEquality
import com.google.auto.common.MoreElements
import java.util.Locale
import javax.lang.model.element.Element
import kotlin.reflect.KClass

@Suppress("UnstableApiUsage")
internal abstract class JavacElement(
    protected val env: JavacProcessingEnv,
    open val element: Element
) : XElement, XEquality {
    override fun <T : Annotation> toAnnotationBox(annotation: KClass<T>): XAnnotationBox<T>? {
        return MoreElements
            .getAnnotationMirror(element, annotation.java)
            .orNull()
            ?.box(env, annotation.java)
    }

    override fun hasAnnotation(annotation: KClass<out Annotation>): Boolean {
        return MoreElements.isAnnotationPresent(element, annotation.java)
    }

    override fun toString(): String {
        return element.toString()
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun kindName(): String {
        return element.kind.name.toLowerCase(Locale.US)
    }

    override fun hasAnnotationWithPackage(pkg: String): Boolean {
        return element.annotationMirrors.any {
            MoreElements.getPackage(it.annotationType.asElement()).toString() == pkg
        }
    }
}