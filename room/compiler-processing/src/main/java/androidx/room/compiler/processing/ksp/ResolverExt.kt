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

import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration

internal fun Resolver.findClass(qName: String) = getClassDeclarationByName(
    getKSNameFromString(qName)
)

internal fun Resolver.requireClass(qName: String) = checkNotNull(findClass(qName)) {
    "cannot find class $qName"
}

internal fun Resolver.requireContinuationClass() = requireClass("kotlin.coroutines.Continuation")

private fun XExecutableElement.getDeclarationForOverride(): KSDeclaration = when (this) {
    is KspExecutableElement -> this.declaration
    is KspSyntheticPropertyMethodElement -> this.field.declaration
    else -> throw IllegalStateException("unexpected XExecutableElement type. $this")
}

internal fun Resolver.overrides(
    overriderElement: XExecutableElement,
    overrideeElement: XExecutableElement
): Boolean {
    // in addition to functions declared in kotlin, we also synthesize getter/setter functions for
    // properties which means we cannot simply send the declaration to KSP for override check
    // (otherwise, it won't give us a definitive answer when java methods override property
    // getters /setters or even we won't be able to distinguish between our own Getter/Setter
    // synthetics).
    // By cheaply checking parameter counts, we avoid all those cases and if KSP returns true, it
    // won't include false positives.
    if (overriderElement.parameters.size != overrideeElement.parameters.size) {
        return false
    }
    val ksOverrider = overriderElement.getDeclarationForOverride()
    val ksOverridee = overrideeElement.getDeclarationForOverride()
    if (!overrides(ksOverrider, ksOverridee)) {
        return false
    }
    // TODO Workaround for https://github.com/google/ksp/issues/123
    //  remove once that bug is fixed
    val subClass = ksOverrider.closestClassDeclaration() ?: return false
    val superClass = ksOverridee.closestClassDeclaration() ?: return false
    return subClass.getAllSuperTypes().any {
        it.declaration.closestClassDeclaration() == superClass
    }
}
