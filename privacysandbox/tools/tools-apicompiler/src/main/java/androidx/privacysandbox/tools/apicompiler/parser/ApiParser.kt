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
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ParsedApi
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSName

/** Convenience extension to get the full qualifier + name from a [KSName]. */
internal fun KSName.getFullName(): String {
    if (getQualifier() == "") return getShortName()
    return "${getQualifier()}.${getShortName()}"
}

/** Top-level entry point to parse a complete user-defined sandbox SDK API into a [ParsedApi]. */
class ApiParser(private val resolver: Resolver, private val logger: KSPLogger) {
    private val interfaceParser = InterfaceParser(resolver, logger)
    private val valueParser = ValueParser(logger)

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
        return ParsedApi(services, values)
    }

    private fun parseAllValues(): Set<AnnotatedValue> {
        return resolver.getSymbolsWithAnnotation(PrivacySandboxValue::class.qualifiedName!!)
            .mapNotNull(valueParser::parseValue).toSet()
    }

    private fun parseAllServices(): Set<AnnotatedInterface> {
        return resolver.getSymbolsWithAnnotation(PrivacySandboxService::class.qualifiedName!!)
            .mapNotNull(interfaceParser::parseInterface).toSet()
    }
}