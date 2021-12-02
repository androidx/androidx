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

import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticFileMemberContainer
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Finds the class that contains this declaration and throws [IllegalStateException] if it cannot
 * be found.
 * @see [findEnclosingAncestorClassDeclaration]
 */
internal fun KSDeclaration.requireEnclosingMemberContainer(
    env: KspProcessingEnv
): KspMemberContainer {
    return checkNotNull(findEnclosingMemberContainer(env)) {
        "Cannot find required enclosing type for $this"
    }
}

/**
 * Find the class that contains this declaration.
 *
 * Node that this is not necessarily the parent declaration. e.g. when a property is declared in
 * a constructor, its containing type is actual two levels up.
 */
@OptIn(KspExperimental::class)
internal fun KSDeclaration.findEnclosingMemberContainer(
    env: KspProcessingEnv
): KspMemberContainer? {
    val memberContainer = findEnclosingAncestorClassDeclaration()?.let {
        env.wrapClassDeclaration(it)
    } ?: this.containingFile?.let {
        env.wrapKSFile(it)
    }
    memberContainer?.let {
        return it
    }
    // in compiled files, we may not find it. Try using the binary name

    val ownerJvmClassName = when (this) {
        is KSPropertyDeclaration -> env.resolver.getOwnerJvmClassName(this)
        is KSFunctionDeclaration -> env.resolver.getOwnerJvmClassName(this)
        else -> null
    } ?: return null
    // Binary name of a top level type is its canonical name. So we just load it directly by
    // that value
    env.findTypeElement(ownerJvmClassName)?.let {
        return it
    }
    // When a top level function/property is compiled, its containing class does not exist in KSP,
    // neither the file. So instead, we synthesize one
    return KspSyntheticFileMemberContainer(ownerJvmClassName)
}

private fun KSDeclaration.findEnclosingAncestorClassDeclaration(): KSClassDeclaration? {
    var parent = parentDeclaration
    while (parent != null && parent !is KSClassDeclaration) {
        parent = parent.parentDeclaration
    }
    return parent as? KSClassDeclaration
}

internal fun KSDeclaration.isStatic(): Boolean {
    return modifiers.contains(Modifier.JAVA_STATIC) || hasJvmStaticAnnotation() ||
        // declarations in the companion object move into the enclosing class as statics.
        // https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-fields
        this.findEnclosingAncestorClassDeclaration()?.isCompanionObject == true ||
        when (this) {
            is KSPropertyAccessor -> this.receiver.findEnclosingAncestorClassDeclaration() == null
            is KSPropertyDeclaration -> this.findEnclosingAncestorClassDeclaration() == null
            is KSFunctionDeclaration -> this.findEnclosingAncestorClassDeclaration() == null
            else -> false
        }
}

internal fun KSDeclaration.isTransient(): Boolean {
    return modifiers.contains(Modifier.JAVA_TRANSIENT) || hasJvmTransientAnnotation()
}