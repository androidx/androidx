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
import androidx.privacysandbox.tools.apicompiler.model.ParsedApi
import androidx.privacysandbox.tools.apicompiler.model.AnnotatedInterface
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName

/** Convenience extension to get the full qualifier + name from a [KSName]. */
internal fun KSName.getFullName(): String {
    if (getQualifier() == "") return getShortName()
    return "${getQualifier()}.${getShortName()}"
}

/** Top-level entry point to parse a complete user-defined sandbox SDK API into a [ParsedApi]. */
class ApiParser(private val resolver: Resolver) {

    fun parseApi(): ParsedApi {
        return ParsedApi(services = getAllServices())
    }

    private fun getAllServices() =
        resolver
            .getSymbolsWithAnnotation(PrivacySandboxService::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .map(this::parseInterface)
            .toSet()

    private fun parseInterface(classDeclaration: KSClassDeclaration): AnnotatedInterface {
        return AnnotatedInterface(
            className = classDeclaration.simpleName.getShortName(),
            packageName = classDeclaration.packageName.getFullName()
        )
    }
}