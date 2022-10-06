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
import androidx.privacysandbox.tools.core.model.Type
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability

internal class InterfaceParser(resolver: Resolver, private val logger: KSPLogger) {
    private val primitiveTypes = getPrimitiveTypes(resolver.builtIns)
    private val validInterfaceModifiers = setOf(Modifier.PUBLIC)
    private val validMethodModifiers = setOf(Modifier.PUBLIC, Modifier.SUSPEND)

    fun parseInterface(interfaceDeclaration: KSClassDeclaration): AnnotatedInterface {
        check(interfaceDeclaration.classKind == ClassKind.INTERFACE) {
            "${interfaceDeclaration.qualifiedName} is not an interface."
        }
        val name = interfaceDeclaration.qualifiedName?.getFullName()
            ?: interfaceDeclaration.simpleName.getFullName()
        if (!interfaceDeclaration.isPublic()) {
            logger.error("Error in $name: annotated interfaces should be public.")
        }
        if (interfaceDeclaration.getDeclaredProperties().any()) {
            logger.error(
                "Error in $name: annotated interfaces cannot declare properties.")
        }
        if (interfaceDeclaration.declarations.filterIsInstance<KSClassDeclaration>()
                .any(KSClassDeclaration::isCompanionObject)
        ) {
            logger.error(
                "Error in $name: annotated interfaces cannot declare companion objects.")
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
            logger.error(
                "Error in $name: annotated interfaces cannot declare type parameters (${
                    interfaceDeclaration.typeParameters.map { it.simpleName.getShortName() }
                        .sorted().joinToString(limit = 3)
                })."
            )
        }

        val methods = interfaceDeclaration.getDeclaredFunctions().map(::parseMethod).toList()
        return AnnotatedInterface(
            type = Converters.typeFromDeclaration(interfaceDeclaration),
            methods = methods,
        )
    }

    private fun parseMethod(method: KSFunctionDeclaration): Method {
        val name = method.qualifiedName?.getFullName() ?: method.simpleName.getFullName()
        if (!method.isAbstract) {
            logger.error("Error in $name: method cannot have default implementation.")
        }
        if (method.typeParameters.isNotEmpty()) {
            logger.error("Error in $name: method cannot declare type parameters (<${
                method.typeParameters.joinToString(limit = 3) { it.name.getShortName() }
            }>).")
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
        val returnType = parseType(method, method.returnType!!.resolve())

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
            logger.error(
                "Error in $name: parameters cannot have default values."
            )
        }

        return Parameter(
            name = parameter.name!!.getFullName(),
            type = parseType(method, parameter.type.resolve()),
        )
    }

    private fun parseType(method: KSFunctionDeclaration, type: KSType): Type {
        val name = method.qualifiedName?.getFullName() ?: method.simpleName.getFullName()
        if (type.nullability == Nullability.NULLABLE) {
            logger.error("Error in $name: nullable types are not supported.")
        }
        if (!primitiveTypes.contains(type)) {
            logger.error("Error in $name: only primitive types are supported.")
        }
        return Converters.typeFromDeclaration(type.declaration)
    }

    private fun getPrimitiveTypes(builtIns: KSBuiltIns) = listOf(
        builtIns.booleanType,
        builtIns.shortType,
        builtIns.intType,
        builtIns.longType,
        builtIns.floatType,
        builtIns.doubleType,
        builtIns.charType,
        builtIns.stringType,
        builtIns.unitType,
    )
}