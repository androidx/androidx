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

import androidx.privacysandbox.tools.core.model.AnnotatedDataClass
import androidx.privacysandbox.tools.core.model.AnnotatedEnumClass
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
    fun parseValue(annotatedValue: KSAnnotated): AnnotatedValue? {
        val isDataClass =
            (annotatedValue is KSClassDeclaration &&
                annotatedValue.classKind == ClassKind.CLASS &&
                annotatedValue.modifiers.contains(Modifier.DATA))
        val isEnumClass =
            (annotatedValue is KSClassDeclaration &&
                annotatedValue.classKind == ClassKind.ENUM_CLASS &&
                annotatedValue.modifiers.contains(Modifier.ENUM))
        if (!isDataClass && !isEnumClass) {
            logger.error(
                "Only data classes and enum classes can be annotated with @PrivacySandboxValue."
            )
            return null
        }
        val value = annotatedValue as KSClassDeclaration
        val name = value.qualifiedName?.getFullName() ?: value.simpleName.getFullName()

        if (!value.isPublic()) {
            logger.error("Error in $name: annotated values should be public.")
        }
        ensureNoCompanion(value, name)
        ensureNoObject(value, name)
        ensureNoTypeParameters(value, name)
        ensureNoSuperTypes(value, name)

        return if (isDataClass) {
            AnnotatedDataClass(
                type = typeParser.parseFromDeclaration(value),
                properties = value.getAllProperties().map(::parseProperty).toList()
            )
        } else {
            parseEnumClass(value)
        }
    }

    private fun parseEnumClass(classDeclaration: KSClassDeclaration): AnnotatedEnumClass {
        val variants =
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .map { it.simpleName.asString() }
                .toList()
        return AnnotatedEnumClass(
            type = typeParser.parseFromDeclaration(classDeclaration),
            variants = variants
        )
    }

    private fun ensureNoCompanion(classDeclaration: KSClassDeclaration, name: String) {
        if (
            classDeclaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .any(KSClassDeclaration::isCompanionObject)
        ) {
            logger.error("Error in $name: annotated values cannot declare companion objects.")
        }
    }

    private fun ensureNoObject(classDeclaration: KSClassDeclaration, name: String) {
        if (
            classDeclaration.declarations
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
            logger.error("Error in $name: annotated values cannot declare objects or classes.")
        }
    }

    private fun ensureNoTypeParameters(classDeclaration: KSClassDeclaration, name: String) {
        if (classDeclaration.typeParameters.isNotEmpty()) {
            logger.error(
                "Error in $name: annotated values cannot declare type parameters (${
                    classDeclaration.typeParameters.map { it.simpleName.getShortName() }.sorted()
                        .joinToString(limit = 3)
                })."
            )
        }
    }

    private fun ensureNoSuperTypes(classDeclaration: KSClassDeclaration, name: String) {
        val supertypes =
            classDeclaration.superTypes.map {
                it.resolve().declaration.qualifiedName?.getFullName()
                    ?: it.resolve().declaration.simpleName.getShortName()
            }
        if (!supertypes.all { it in listOf("kotlin.Any", "kotlin.Enum") }) {
            logger.error(
                "Error in $name: values annotated with @PrivacySandboxValue may not " +
                    "inherit other types (${supertypes.joinToString(limit = 3)})"
            )
        }
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
