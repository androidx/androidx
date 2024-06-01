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

import androidx.room.compiler.processing.InternalXAnnotated
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.javac.kotlin.KmData
import androidx.room.compiler.processing.javac.kotlin.KmVisibility
import androidx.room.compiler.processing.unwrapRepeatedAnnotationsFromContainer
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreElements.isAnnotationPresent
import com.google.auto.common.SuperficialValidation
import java.util.Locale
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

@Suppress("UnstableApiUsage")
internal abstract class JavacElement(
    internal val env: JavacProcessingEnv,
    open val element: Element
) : XElement, XEquality, InternalXAnnotated, XHasModifiers {

    abstract val kotlinMetadata: KmData?

    override fun <T : Annotation> getAnnotations(
        annotation: KClass<T>,
        containerAnnotation: KClass<out Annotation>?
    ): List<XAnnotationBox<T>> {
        // if there is a container annotation and annotation is repeated, we'll get the container.
        if (containerAnnotation != null) {
            MoreElements.getAnnotationMirror(element, containerAnnotation.java)
                .orNull()
                ?.box(env, containerAnnotation.java)
                ?.let { containerBox ->
                    // found a container, return
                    return containerBox.getAsAnnotationBoxArray<T>("value").toList()
                }
        }
        // if there is no container annotation or annotation is not repeated, we'll see the
        // individual value
        return MoreElements.getAnnotationMirror(element, annotation.java)
            .orNull()
            ?.box(env, annotation.java)
            ?.let { listOf(it) } ?: emptyList()
    }

    override fun getAllAnnotations(): List<XAnnotation> {
        return element.annotationMirrors
            .map { mirror -> JavacAnnotation(env, mirror) }
            .flatMap { annotation ->
                // TODO(b/313473892): Checking if an annotation needs to be unwrapped can be
                //  expensive with the XProcessing API, especially if we don't really care about
                //  annotation values, so do a quick check on the AnnotationMirror first to decide
                //  if its repeatable. Remove this once we've optimized the general solution in
                //  unwrapRepeatedAnnotationsFromContainer()
                if (annotation.mirror.isRepeatable()) {
                    annotation.unwrapRepeatedAnnotationsFromContainer() ?: listOf(annotation)
                } else {
                    listOf(annotation)
                }
            }
    }

    override fun hasAnnotation(
        annotation: KClass<out Annotation>,
        containerAnnotation: KClass<out Annotation>?
    ): Boolean {
        return isAnnotationPresent(element, annotation.java) ||
            (containerAnnotation != null && isAnnotationPresent(element, containerAnnotation.java))
    }

    override fun toString(): String {
        return element.toString()
    }

    final override val equalityItems: Array<out Any?> by lazy { arrayOf(element) }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun kindName(): String {
        return element.kind.name.lowercase(Locale.US)
    }

    override fun hasAnnotationWithPackage(pkg: String): Boolean {
        return element.annotationMirrors.any {
            MoreElements.getPackage(it.annotationType.asElement()).toString() == pkg
        }
    }

    override val docComment: String? by lazy { env.elementUtils.getDocComment(element) }

    override fun validate(): Boolean {
        return SuperficialValidation.validateElement(element)
    }

    override fun isPublic(): Boolean {
        return element.modifiers.contains(Modifier.PUBLIC)
    }

    override fun isInternal(): Boolean {
        return (kotlinMetadata as? KmVisibility)?.isInternal() ?: false
    }

    override fun isProtected(): Boolean {
        return element.modifiers.contains(Modifier.PROTECTED)
    }

    override fun isAbstract(): Boolean {
        return element.modifiers.contains(Modifier.ABSTRACT)
    }

    override fun isKtPrivate(): Boolean {
        return (kotlinMetadata as? KmVisibility)?.isPrivate() ?: false
    }

    override fun isPrivate(): Boolean {
        return element.modifiers.contains(Modifier.PRIVATE)
    }

    override fun isStatic(): Boolean {
        return element.modifiers.contains(Modifier.STATIC)
    }

    override fun isTransient(): Boolean {
        return element.modifiers.contains(Modifier.TRANSIENT)
    }

    override fun isFinal(): Boolean {
        return element.modifiers.contains(Modifier.FINAL)
    }
}
