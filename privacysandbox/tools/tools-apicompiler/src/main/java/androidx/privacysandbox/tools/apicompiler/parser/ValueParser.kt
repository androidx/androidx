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

import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ValueProperty
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal class ValueParser(private val logger: KSPLogger, private val typeParser: TypeParser) {
    fun parseValue(value: KSAnnotated): AnnotatedValue? {
        if (value !is KSClassDeclaration ||
            value.classKind != ClassKind.CLASS ||
            !value.modifiers.contains(Modifier.DATA)
        ) {
            logger.error(
                "Only data classes can be annotated with @PrivacySandboxValue."
            )
            return null
        }

        val name = value.qualifiedName?.getFullName() ?: value.simpleName.getFullName()
        if (!value.isPublic()) {
            logger.error("Error in $name: annotated values should be public.")
        }

        if (value.declarations.filterIsInstance<KSClassDeclaration>()
                .any(KSClassDeclaration::isCompanionObject)
        ) {
            logger.error(
                "Error in $name: annotated values cannot declare companion objects."
            )
        }

        if (value.typeParameters.isNotEmpty()) {
            logger.error(
                "Error in $name: annotated values cannot declare type parameters (${
                    value.typeParameters.map { it.simpleName.getShortName() }.sorted()
                        .joinToString(limit = 3)
                })."
            )
        }

        return AnnotatedValue(
            type = typeParser.parseFromDeclaration(value),
            properties = value.getAllProperties().map(::parseProperty).toList()
        )
    }

    private fun parseProperty(property: KSPropertyDeclaration): ValueProperty {
        val name = property.qualifiedName?.getFullName() ?: property.simpleName.getFullName()
        if (property.isMutable) {
            logger.error("Error in $name: properties cannot be mutable.")
        }
        return ValueProperty(
            name = property.simpleName.getShortName(),
            type = typeParser.parseFromTypeReference(property.type, name),
        )
    }
}
