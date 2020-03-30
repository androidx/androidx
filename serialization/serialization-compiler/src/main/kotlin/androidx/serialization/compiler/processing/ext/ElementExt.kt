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

package androidx.serialization.compiler.processing.ext

import com.google.auto.common.MoreElements
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.SimpleElementVisitor6
import kotlin.reflect.KClass

/** Casts this element to a [TypeElement] using [MoreElements.asType]. */
internal fun Element.asTypeElement(): TypeElement {
    return MoreElements.asType(this)
}

/** Casts this element to a [VariableElement] using [MoreElements.asVariable]. */
internal fun Element.asVariableElement(): VariableElement {
    return MoreElements.asVariable(this)
}

/** Determines if this element is private by the presence of [Modifier.PRIVATE] in it modifiers. */
internal fun Element.isPrivate(): Boolean {
    return Modifier.PRIVATE in modifiers
}

/**
 * Determines if this element is visible to its own package.
 *
 * A private element or an element enclosed within a private element is not visible to its package.
 */
internal fun Element.isVisibleToPackage(): Boolean {
    return accept(IsVisibleToPackageVisitor, null)
}

@Suppress("DEPRECATION")
private object IsVisibleToPackageVisitor : SimpleElementVisitor6<Boolean, Nothing?>() {
    override fun visitPackage(e: PackageElement, p: Nothing?): Boolean {
        return true
    }

    override fun defaultAction(e: Element, p: Nothing?): Boolean {
        return if (e.isPrivate()) {
            false
        } else {
            e.enclosingElement.accept(this, null)
        }
    }
}

/** Gets the enclosing package element using [MoreElements.getPackage]. */
internal val Element.packageElement: PackageElement
    get() = MoreElements.getPackage(this)

/** Get an annotation mirror if directly present on this element or else `null`. */
internal operator fun Element.get(annotationClass: KClass<out Annotation>): AnnotationMirror? {
    return MoreElements.getAnnotationMirror(this, annotationClass.java).orNull()
}
