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

import org.jetbrains.kotlin.ksp.getAllSuperTypes
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSName
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSType
import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.KSTypeParameter

/**
 * Returns the type of a property as if it is member of the given [ksType].
 *
 * This is a temporary / inefficient implementation until KSP provides the API. It also does not
 * handle inner classes properly.
 * TODO: remove once https://github.com/android/kotlin/issues/26 is implemented
 */
internal fun KSPropertyDeclaration.typeAsMemberOf(ksType: KSType): KSType {
    val myType: KSType = checkNotNull(type?.requireType()) {
        "Cannot find type of Kotlin property: $this"
    }
    val parent = checkNotNull(findEnclosingAncestorClassDeclaration()) {
        "Cannot find containing class for property. $this"
    }
    // TODO traverse grandparents if parent is an inner class as TypeArguments might be declared
    //  there as well.
    val matchingParentType: KSType = (ksType.declaration as? KSClassDeclaration)
        ?.getAllSuperTypes()
        ?.firstOrNull {
            it.starProjection().declaration == parentDeclaration
        } ?: return myType
    // create a map of replacements.
    val replacements = parent.typeParameters.mapIndexed { index, ksTypeParameter ->
        ksTypeParameter.name to matchingParentType.arguments.getOrNull(index)
    }.toMap()
    return myType.replaceFromMap(replacements)
}

private fun KSTypeArgument.replaceFromMap(arguments: Map<KSName, KSTypeArgument?>): KSTypeArgument {
    val myTypeDeclaration = type?.resolve()?.declaration
    if (myTypeDeclaration is KSTypeParameter) {
        return arguments[myTypeDeclaration.name] ?: this
    }
    return this
}

private fun KSType.replaceFromMap(arguments: Map<KSName, KSTypeArgument?>): KSType {
    val myDeclaration = this.declaration
    if (myDeclaration is KSTypeParameter) {
        return arguments[myDeclaration.name]?.type?.resolve() ?: this
    }
    if (this.arguments.isEmpty()) {
        return this
    }
    return replace(this.arguments.map {
        it.replaceFromMap(arguments)
    })
}
