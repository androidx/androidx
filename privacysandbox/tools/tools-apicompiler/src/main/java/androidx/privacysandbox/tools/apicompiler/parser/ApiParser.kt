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

import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.validator.ModelValidator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import kotlin.reflect.KClass

/** Convenience extension to get the full qualifier + name from a [KSName]. */
internal fun KSName.getFullName(): String {
    if (getQualifier() == "") return getShortName()
    return "${getQualifier()}.${getShortName()}"
}

/** Top-level entry point to parse a complete user-defined sandbox SDK API into a [ParsedApi]. */
class ApiParser(private val resolver: Resolver, private val logger: KSPLogger) {
    private val typeParser = TypeParser(logger)
    private val interfaceParser = InterfaceParser(logger, typeParser)
    private val valueParser = ValueParser(logger, typeParser)

    fun parseApi(): ParsedApi {
        val services = parseAllServices()
        if (services.count() > 1) {
            logger.error(
                "Multiple interfaces annotated with @PrivacySandboxService are not supported (${
                    services.joinToString {
                        it.type.simpleName
                    }
                }).")
        }
        val values = parseAllValues()
        val callbacks = parseAllCallbacks()
        val interfaces = parseAllInterfaces()
        return ParsedApi(services, values, callbacks, interfaces).also(::validate)
    }

    private fun parseAllValues(): Set<AnnotatedValue> {
        return resolver.getSymbolsWithAnnotation(PrivacySandboxValue::class.qualifiedName!!)
            .mapNotNull(valueParser::parseValue).toSet()
    }

    private fun parseAllServices(): Set<AnnotatedInterface> {
        return getInterfacesWithAnnotation(PrivacySandboxService::class)
            .map(interfaceParser::parseInterface)
            .toSet()
    }

    private fun parseAllCallbacks(): Set<AnnotatedInterface> {
        return getInterfacesWithAnnotation(PrivacySandboxCallback::class)
            .map(interfaceParser::parseInterface)
            .toSet()
    }

    private fun parseAllInterfaces(): Set<AnnotatedInterface> {
        return getInterfacesWithAnnotation(PrivacySandboxInterface::class)
            .map(interfaceParser::parseInterface)
            .toSet()
    }

    private fun getInterfacesWithAnnotation(annotationName: KClass<*>):
        Sequence<KSClassDeclaration> {
        val symbolsWithAnnotation =
            resolver.getSymbolsWithAnnotation(annotationName.qualifiedName!!)
        if (symbolsWithAnnotation.any {
                it !is KSClassDeclaration ||
                    it.classKind != ClassKind.INTERFACE
            }) {
            logger.error("Only interfaces can be annotated with @${annotationName.simpleName}.")
            return emptySequence()
        }
        return symbolsWithAnnotation.filterIsInstance<KSClassDeclaration>()
    }

    private fun validate(api: ParsedApi) {
        val validationResult = ModelValidator.validate(api)
        for (error in validationResult.errors) {
            logger.error(error)
        }
    }
}
