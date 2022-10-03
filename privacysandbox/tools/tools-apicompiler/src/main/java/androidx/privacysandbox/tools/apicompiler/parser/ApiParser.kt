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

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.Type
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier

/** Convenience extension to get the full qualifier + name from a [KSName]. */
internal fun KSName.getFullName(): String {
    if (getQualifier() == "") return getShortName()
    return "${getQualifier()}.${getShortName()}"
}

/** Top-level entry point to parse a complete user-defined sandbox SDK API into a [ParsedApi]. */
class ApiParser(private val resolver: Resolver, private val logger: KSPLogger) {
    private val validator: ApiValidator = ApiValidator(logger, resolver)

    fun parseApi(): ParsedApi {
        return ParsedApi(services = parseAllServices())
    }

    private fun parseAllServices(): Set<AnnotatedInterface> {
        val symbolsWithServiceAnnotation =
            resolver.getSymbolsWithAnnotation(PrivacySandboxService::class.qualifiedName!!)
        val interfacesWithServiceAnnotation =
            symbolsWithServiceAnnotation.filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.INTERFACE }
        if (symbolsWithServiceAnnotation.count() != interfacesWithServiceAnnotation.count()) {
            logger.error("Only interfaces can be annotated with @PrivacySandboxService.")
            return setOf()
        }
        if (interfacesWithServiceAnnotation.count() > 1) {
            logger.error(
                "Multiple interfaces annotated with @PrivacySandboxService are not supported " +
                    "(${
                        interfacesWithServiceAnnotation.joinToString {
                            it.simpleName.getShortName()
                        }
                    })."
            )
            return setOf()
        }
        return interfacesWithServiceAnnotation.map(this::parseInterface).toSet()
    }

    private fun parseInterface(classDeclaration: KSClassDeclaration): AnnotatedInterface {
        validator.validateInterface(classDeclaration)
        return AnnotatedInterface(
            type = parseType(classDeclaration),
            methods = getAllMethods(classDeclaration),
        )
    }

    private fun getAllMethods(classDeclaration: KSClassDeclaration) =
        classDeclaration.getDeclaredFunctions().map(::parseMethod).toList()

    private fun parseMethod(method: KSFunctionDeclaration): Method {
        validator.validateMethod(method)
        // TODO: returnType "Can be null if an error occurred during resolution".
        val returnType = method.returnType!!.resolve()
        validator.validateType(method, returnType)
        return Method(
            name = method.simpleName.getFullName(),
            parameters = getAllParameters(method),
            returnType = parseType(returnType.declaration),
            isSuspend = method.modifiers.contains(Modifier.SUSPEND)
        )
    }

    private fun getAllParameters(method: KSFunctionDeclaration) =
        method.parameters.map { parameter -> parseParameter(method, parameter) }.toList()

    private fun parseParameter(method: KSFunctionDeclaration, parameter: KSValueParameter):
        Parameter {
        validator.validateParameter(method, parameter)
        val type = parameter.type.resolve()
        validator.validateType(method, type)
        return Parameter(
            name = parameter.name!!.getFullName(),
            type = parseType(type.declaration),
        )
    }

    private fun parseType(declaration: KSDeclaration): Type {
        return Type(
            packageName = declaration.packageName.getFullName(),
            simpleName = declaration.simpleName.getShortName(),
        )
    }
}