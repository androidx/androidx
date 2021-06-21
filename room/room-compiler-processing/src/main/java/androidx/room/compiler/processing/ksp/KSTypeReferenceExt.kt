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

package androidx.room.compiler.processing.ksp

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin

/**
 * Creates a new TypeReference from [this] where the resolved type [replacement] but everything
 * else is the same (e.g. location).
 */
internal fun KSTypeReference.swapResolvedType(replacement: KSType): KSTypeReference {
    return DelegatingTypeReference(
        original = this,
        resolved = replacement
    )
}

/**
 * Creates a [NonExistLocation] type reference for [this].
 */
internal fun KSType.createTypeReference(): KSTypeReference {
    return NoLocationTypeReference(this)
}

private class DelegatingTypeReference(
    val original: KSTypeReference,
    val resolved: KSType
) : KSTypeReference by original {
    override fun resolve() = resolved
}

private class NoLocationTypeReference(
    val resolved: KSType
) : KSTypeReference {
    override val annotations: Sequence<KSAnnotation>
        get() = emptySequence()
    override val element: KSReferenceElement?
        get() = null
    override val location: Location
        get() = NonExistLocation
    override val modifiers: Set<Modifier>
        get() = emptySet()
    override val origin: Origin
        get() = Origin.SYNTHETIC

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun resolve(): KSType = resolved
}