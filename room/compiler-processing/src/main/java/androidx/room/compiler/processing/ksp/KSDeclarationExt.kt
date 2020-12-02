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

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Finds the class that contains this declaration and throws [IllegalStateException] if it cannot
 * be found.
 * @see [findEnclosingAncestorClassDeclaration]
 */
internal fun KSDeclaration.requireEnclosingTypeElement(env: KspProcessingEnv): KspTypeElement {
    return checkNotNull(findEnclosingTypeElement(env)) {
        "Cannot find required enclosing type for $this"
    }
}

/**
 * Find the class that contains this declaration.
 *
 * Node that this is not necessarily the parent declaration. e.g. when a property is declared in
 * a constructor, its containing type is actual two levels up.
 */
internal fun KSDeclaration.findEnclosingTypeElement(env: KspProcessingEnv): KspTypeElement? {
    return findEnclosingAncestorClassDeclaration()?.let {
        env.wrapClassDeclaration(it)
    }
}

internal fun KSDeclaration.findEnclosingAncestorClassDeclaration(): KSClassDeclaration? {
    var parent = parentDeclaration
    while (parent != null && parent !is KSClassDeclaration) {
        parent = parent.parentDeclaration
    }
    return parent as? KSClassDeclaration
}

internal fun KSDeclaration.isStatic(): Boolean {
    return modifiers.contains(Modifier.JAVA_STATIC) || hasJvmStaticAnnotation()
}