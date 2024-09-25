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

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.Types.any
import androidx.privacysandbox.tools.core.model.Types.sandboxedUiAdapter
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier

internal class InterfaceParser(
    private val logger: KSPLogger,
    private val typeParser: TypeParser,
) {
    private val validInterfaceModifiers = setOf(Modifier.PUBLIC)
    private val validMethodModifiers = setOf(Modifier.PUBLIC, Modifier.SUSPEND)
    private val validInterfaceSuperTypes = setOf(sandboxedUiAdapter)

    fun parseInterface(interfaceDeclaration: KSClassDeclaration): AnnotatedInterface {
        check(interfaceDeclaration.classKind == ClassKind.INTERFACE) {
            "${interfaceDeclaration.qualifiedName} is not an interface."
        }
        val name =
            interfaceDeclaration.qualifiedName?.getFullName()
                ?: interfaceDeclaration.simpleName.getFullName()
        if (!interfaceDeclaration.isPublic()) {
            logger.error("Error in $name: annotated interfaces should be public.")
        }
        if (interfaceDeclaration.getDeclaredProperties().any()) {
            logger.error("Error in $name: annotated interfaces cannot declare properties.")
        }
        if (
            interfaceDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter {
                    listOf(
                            ClassKind.OBJECT,
                            ClassKind.INTERFACE,
                            ClassKind.ENUM_CLASS,
                            ClassKind.CLASS
                        )
                        .contains(it.classKind)
                }
                .any { !it.isCompanionObject }
        ) {
            logger.error("Error in $name: annotated interfaces cannot declare objects or classes.")
        }

        interfaceDeclaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.isCompanionObject }
            .forEach { validateCompanion(name, it, logger) }

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
            logger.error(
                "Error in $name: annotated interfaces cannot declare type parameters (${
                    interfaceDeclaration.typeParameters.map { it.simpleName.getShortName() }
                        .sorted().joinToString(limit = 3)
                })."
            )
        }
        val superTypes =
            interfaceDeclaration.superTypes
                .map { typeParser.parseFromDeclaration(it.resolve().declaration) }
                .filterNot { it == any }
                .toList()
        val invalidSuperTypes = superTypes.filterNot { validInterfaceSuperTypes.contains(it) }
        if (invalidSuperTypes.isNotEmpty()) {
            logger.error(
                "Error in $name: annotated interface inherits prohibited types (${
                    superTypes.map { it.simpleName }.sorted().joinToString(limit = 3)
                })."
            )
        }

        val methods = interfaceDeclaration.getDeclaredFunctions().map(::parseMethod).toList()
        return AnnotatedInterface(
            type = typeParser.parseFromDeclaration(interfaceDeclaration),
            superTypes = superTypes,
            methods = methods,
        )
    }

    private fun parseMethod(method: KSFunctionDeclaration): Method {
        val name = method.qualifiedName?.getFullName() ?: method.simpleName.getFullName()
        if (!method.isAbstract) {
            logger.error("Error in $name: method cannot have default implementation.")
        }
        if (method.typeParameters.isNotEmpty()) {
            logger.error(
                "Error in $name: method cannot declare type parameters (<${
                    method.typeParameters.joinToString(limit = 3) { it.name.getShortName() }
                }>)."
            )
        }
        val invalidModifiers = method.modifiers.filterNot(validMethodModifiers::contains)
        if (invalidModifiers.isNotEmpty()) {
            logger.error(
                "Error in $name: method contains invalid modifiers (${
                    invalidModifiers.map { it.name.lowercase() }.sorted().joinToString(limit = 3)
                })."
            )
        }

        if (method.returnType == null) {
            logger.error("Error in $name: failed to resolve return type.")
        }
        val returnType = typeParser.parseFromTypeReference(method.returnType!!, name)

        val parameters =
            method.parameters.map { parameter -> parseParameter(method, parameter) }.toList()

        return Method(
            name = method.simpleName.getFullName(),
            parameters = parameters,
            returnType = returnType,
            isSuspend = method.modifiers.contains(Modifier.SUSPEND)
        )
    }

    private fun parseParameter(
        method: KSFunctionDeclaration,
        parameter: KSValueParameter
    ): Parameter {
        val name = method.qualifiedName?.getFullName() ?: method.simpleName.getFullName()
        if (parameter.hasDefault) {
            logger.error("Error in $name: parameters cannot have default values.")
        }

        return Parameter(
            name = parameter.name!!.getFullName(),
            type = typeParser.parseFromTypeReference(parameter.type, name),
        )
    }
}

internal fun validateCompanion(name: String, companionDecl: KSClassDeclaration, logger: KSPLogger) {
    val nonConstValues =
        companionDecl.declarations
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { !it.modifiers.contains(Modifier.CONST) }
            .toList()
    if (nonConstValues.isNotEmpty()) {
        logger.error(
            "Error in $name: companion object cannot declare non-const values (${
                nonConstValues.joinToString(limit = 3) { it.simpleName.getShortName() }
            })."
        )
    }
    val methods =
        companionDecl.declarations
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { it.simpleName.getFullName() != "<init>" }
            .toList()
    if (methods.isNotEmpty()) {
        logger.error(
            "Error in $name: companion object cannot declare methods (${
                methods.joinToString(limit = 3) { it.simpleName.getShortName() }
            })."
        )
    }
    val classes = companionDecl.declarations.filterIsInstance<KSClassDeclaration>().toList()
    if (classes.isNotEmpty()) {
        logger.error(
            "Error in $name: companion object cannot declare classes (${
                classes.joinToString(limit = 3) { it.simpleName.getShortName() }
            })."
        )
    }
}
