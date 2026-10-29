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

package androidx.room3.compiler.processing

import androidx.room3.compiler.processing.ksp.KspFileMemberContainer
import androidx.room3.compiler.processing.ksp.synthetic.KspSyntheticFileMemberContainer

/** Property in an [XTypeElement]. */
interface XPropertyElement : XVariableElement, XHasModifiers {
    /**
     * The element that declared this property. For property declared in classes, this will be an
     * [XTypeElement].
     *
     * For top level properties in Kotlin:
     * * When running with KAPT, the value will be an [XTypeElement].
     * * When running with KSP, the value will **NOT** be an [XTypeElement]. It will be an
     *   [KspSyntheticFileMemberContainer] if this property is coming from the classpath or
     *   [KspFileMemberContainer] if this property is in source. If you need the generated synthetic
     *   java class name, you can use [XMemberContainer.asClassName] property.
     */
    override val enclosingElement: XMemberContainer

    override val fallbackLocationText: String
        get() = "$name in ${enclosingElement.fallbackLocationText}"

    /**
     * Returns the field associated with this property or `null` if there isn't one.
     *
     * Note: This is never `null` for Java sources.
     */
    val backingField: XFieldElement?

    /**
     * Returns the getter method associated with this property or `null` if there isn't one.
     *
     * Note: This is expected to be `null` for Java source, or if the field isn't associated with a
     * Kotlin property.
     */
    val getter: XMethodElement?

    /**
     * Returns the setter method associated with this field or `null` if there isn't one.
     *
     * Note: This is expected to be `null` for Java source, or if the field isn't associated with a
     * Kotlin property.
     */
    val setter: XMethodElement?

    /**
     * Returns `true` if this property is known to have annotations that are not visible to the
     * processor, in which case [getAllAnnotations] and related methods may be incomplete.
     *
     * When running with KAPT, annotations on Kotlin properties are stored on a synthetic
     * `$annotations` method. For properties of precompiled classes that method is marked
     * `ACC_SYNTHETIC` and is not visible to javac, see
     * https://youtrack.jetbrains.com/issue/KT-34684. In that case this returns `true` if the Kotlin
     * metadata says the property has annotations, so processors can fall back to another source of
     * information, or report an error, instead of silently missing an annotation.
     *
     * This is always `false` when running with KSP.
     */
    fun hasInaccessibleAnnotations(): Boolean = false
}
