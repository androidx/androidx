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
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability

internal fun Resolver.findClass(qName: String) = getClassDeclarationByName(
    getKSNameFromString(qName)
)

internal fun Resolver.requireClass(qName: String) = checkNotNull(findClass(qName)) {
    "cannot find class $qName"
}

internal fun Resolver.requireType(qName: String) = requireClass(qName).asStarProjectedType()

internal fun Resolver.requireContinuationClass() = requireClass("kotlin.coroutines.Continuation")

private fun XExecutableElement.getDeclarationForOverride(): KSDeclaration = when (this) {
    is KspExecutableElement -> this.declaration
    is KspSyntheticPropertyMethodElement -> this.field.declaration
    else -> throw IllegalStateException("unexpected XExecutableElement type. $this")
}

internal fun Resolver.overrides(
    overriderElement: XMethodElement,
    overrideeElement: XMethodElement
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
    // do a quick check on name before doing the more expensive operations
    if (overriderElement.name != overrideeElement.name) {
        return false
    }
    val ksOverrider = overriderElement.getDeclarationForOverride()
    val ksOverridee = overrideeElement.getDeclarationForOverride()
    if (overrides(ksOverrider, ksOverridee)) {
        return true
    }
    // workaround for: https://github.com/google/ksp/issues/175
    if (ksOverrider is KSFunctionDeclaration && ksOverridee is KSFunctionDeclaration) {
        return ksOverrider.overrides(ksOverridee)
    }
    if (ksOverrider is KSPropertyDeclaration && ksOverridee is KSPropertyDeclaration) {
        return ksOverrider.overrides(ksOverridee)
    }
    return false
}

private fun KSFunctionDeclaration.overrides(other: KSFunctionDeclaration): Boolean {
    val overridee = try {
        findOverridee()
    } catch (ignored: IllegalStateException) {
        // workaround for https://github.com/google/ksp/issues/248
        null
    }
    // before accepting this override, check if we have a primitive parameter that was a type
    // reference in overridee. In those cases, kotlin will actually generate two jvm methods.
    if (overridee == other && this.overridesInJvm(other)) {
        return true
    }
    return overridee?.overrides(other) ?: false
}

/**
 * If the overrider specifies a primitive value for a type argument, ignore the override as
 * kotlin will generate two class methods for them.
 *
 * see: b/160258066 for details
 */
private fun KSFunctionDeclaration.overridesInJvm(
    other: KSFunctionDeclaration
): Boolean {
    parameters.forEachIndexed { index, myParam ->
        val myParamType = myParam.type.resolve()
        if (myParamType.nullability == Nullability.NOT_NULL) {
            val myParamDecl = myParamType.declaration
            val paramQName = myParamDecl.qualifiedName?.asString()
            if (paramQName != null &&
                KspTypeMapper.getPrimitiveJavaTypeName(paramQName) != null
            ) {
                // parameter is a primitive. Check if the parent declared it as a type argument,
                // in which case, we should ignore the override.
                val otherParamDeclaration = other.parameters
                    .getOrNull(index)?.type?.resolve()?.declaration
                if (otherParamDeclaration is KSTypeParameter) {
                    return false
                }
            }
        }
    }
    return true
}

private fun KSPropertyDeclaration.overrides(other: KSPropertyDeclaration): Boolean {
    val overridee = findOverridee()
    if (overridee == other) {
        return true
    }
    return overridee?.overrides(other) ?: false
}

@OptIn(KspExperimental::class)
internal fun Resolver.safeGetJvmName(
    declaration: KSFunctionDeclaration
): String {
    return try {
        getJvmName(declaration)
    } catch (ignored: ClassCastException) {
        // TODO remove this catch once that issue is fixed.
        // workaround for https://github.com/google/ksp/issues/164
        return declaration.simpleName.asString()
    } catch (cannotFindDeclaration: IllegalStateException) {
        // workaround for https://github.com/google/ksp/issues/240
        return declaration.simpleName.asString()
    }
}
