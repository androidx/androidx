/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apicompiler.parser

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier

class ApiValidator(private val logger: KSPLogger) {
    companion object {
        val validInterfaceModifiers = setOf(Modifier.PUBLIC)
    }

    fun validateInterface(interfaceDeclaration: KSClassDeclaration) {
        val name = interfaceDeclaration.qualifiedName?.getFullName()
            ?: interfaceDeclaration.simpleName.getFullName()
        if (!interfaceDeclaration.isPublic()) {
            logger.error("Error in $name: annotated interfaces should be public.")
        }
        if (interfaceDeclaration.getDeclaredProperties().any()) {
            logger.error("Error in $name: annotated interfaces cannot declare properties.")
        }
        if (interfaceDeclaration.declarations.filterIsInstance<KSClassDeclaration>()
                .any(KSClassDeclaration::isCompanionObject)
        ) {
            logger.error("Error in $name: annotated interfaces cannot declare companion objects.")
        }
        val invalidModifiers =
            interfaceDeclaration.modifiers.filterNot(validInterfaceModifiers::contains)
        if (invalidModifiers.isNotEmpty()) {
            logger.error(
                "Error in $name: annotated interface contains invalid modifiers (${
                    invalidModifiers.map { it.name.lowercase() }.sorted().joinToString(limit = 3)
                })."
            )
        }
        if (interfaceDeclaration.typeParameters.isNotEmpty()) {
            interfaceDeclaration.typeParameters.map { it.simpleName.getShortName() }.sorted()
                .joinToString(limit = 3)
            logger.error(
                "Error in $name: annotated interfaces cannot declare type parameters (${
                    interfaceDeclaration.typeParameters.map { it.simpleName.getShortName() }
                        .sorted().joinToString(limit = 3)
                })."
            )
        }
    }
}