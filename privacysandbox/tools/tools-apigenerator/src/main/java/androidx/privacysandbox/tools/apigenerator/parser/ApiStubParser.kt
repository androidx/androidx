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

package androidx.privacysandbox.tools.apigenerator.parser

import androidx.privacysandbox.tools.core.PrivacySandboxParsingException
import androidx.privacysandbox.tools.core.model.AnnotatedDataClass
import androidx.privacysandbox.tools.core.model.AnnotatedEnumClass
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.core.model.ValueProperty
import androidx.privacysandbox.tools.core.validator.ModelValidator
import java.nio.file.Path
import kotlinx.metadata.ClassKind
import kotlinx.metadata.ClassName
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.KmType
import kotlinx.metadata.isData
import kotlinx.metadata.isNullable
import kotlinx.metadata.isSuspend
import kotlinx.metadata.isVar
import kotlinx.metadata.kind

internal object ApiStubParser {
    /**
     * Parses the API annotated by a Privacy Sandbox SDK from its compiled classes.
     *
     * @param sdkStubsClasspath root directory of SDK classpath.
     */
    internal fun parse(sdkStubsClasspath: Path): ParsedApi {
        val (services, values, callbacks, interfaces) =
            AnnotatedClassReader.readAnnotatedClasses(sdkStubsClasspath)
        if (services.isEmpty())
            throw PrivacySandboxParsingException(
                "Unable to find valid interfaces annotated with @PrivacySandboxService."
            )
        return ParsedApi(
                services.map { parseInterface(it, "PrivacySandboxService") }.toSet(),
                values.map(::parseValue).toSet(),
                callbacks.map { parseInterface(it, "PrivacySandboxCallback") }.toSet(),
                interfaces.map { parseInterface(it, "PrivacySandboxInterface") }.toSet(),
            )
            .also(::validate)
    }

    private fun parseInterface(service: KmClass, annotationName: String): AnnotatedInterface {
        val type = parseClassName(service.name)
        val superTypes = service.supertypes.map(this::parseType).filterNot { it == Types.any }

        if (service.kind != ClassKind.INTERFACE) {
            throw PrivacySandboxParsingException(
                "${type.qualifiedName} is not a Kotlin interface but it's annotated with " +
                    "@$annotationName."
            )
        }

        return AnnotatedInterface(
            type = type,
            superTypes = superTypes,
            methods = service.functions.map(this::parseMethod),
        )
    }

    private fun parseValue(value: KmClass): AnnotatedValue {
        val type = parseClassName(value.name)
        val isEnum = value.kind == ClassKind.ENUM_CLASS

        if (!value.isData && !isEnum) {
            throw PrivacySandboxParsingException(
                "${type.qualifiedName} is not a Kotlin data class or enum class but it's " +
                    "annotated with @PrivacySandboxValue."
            )
        }
        val superTypes =
            value.supertypes
                .asSequence()
                .map { it.classifier }
                .filterIsInstance<KmClassifier.Class>()
                .map { it.name }
                .filter { it !in listOf("kotlin/Enum", "kotlin/Any") }
                .map { parseClassName(it) }
                .toList()
        if (superTypes.isNotEmpty()) {
            throw PrivacySandboxParsingException(
                "Error in ${type.qualifiedName}: values annotated with @PrivacySandboxValue may " +
                    "not inherit other types (${
                        superTypes.joinToString(limit = 3) { it.simpleName }
                    })"
            )
        }

        return if (value.isData) {
            AnnotatedDataClass(type, parseProperties(type, value))
        } else {
            AnnotatedEnumClass(type, value.enumEntries.toList())
        }
    }

    /** Parses properties and sorts them based on the order of constructor parameters. */
    private fun parseProperties(type: Type, valueClass: KmClass): List<ValueProperty> {
        // TODO: handle multiple constructors.
        if (valueClass.constructors.size != 1) {
            throw PrivacySandboxParsingException("Multiple constructors for values not supported.")
        }
        val parsedProperties = valueClass.properties.map { parseProperty(type, it) }
        val propertiesByName = parsedProperties.associateBy { it.name }
        return valueClass.constructors[0].valueParameters.map { propertiesByName[it.name]!! }
    }

    private fun parseProperty(containerType: Type, property: KmProperty): ValueProperty {
        val qualifiedName = "${containerType.qualifiedName}.${property.name}"
        if (property.isVar) {
            throw PrivacySandboxParsingException(
                "Error in $qualifiedName: mutable properties are not allowed in data classes " +
                    "annotated with @PrivacySandboxValue."
            )
        }
        return ValueProperty(property.name, parseType(property.returnType))
    }

    private fun parseMethod(function: KmFunction): Method {
        return Method(
            function.name,
            function.valueParameters.map { Parameter(it.name, parseType(it.type)) },
            parseType(function.returnType),
            function.isSuspend
        )
    }

    private fun parseType(type: KmType): Type {
        val classifier = type.classifier
        val isNullable = type.isNullable
        if (classifier !is KmClassifier.Class) {
            throw PrivacySandboxParsingException("Unsupported type in API description: $type")
        }
        val typeArguments = type.arguments.map { parseType(it.type!!) }
        return parseClassName(classifier.name, typeArguments, isNullable)
    }

    private fun parseClassName(
        className: ClassName,
        typeArguments: List<Type> = emptyList(),
        isNullable: Boolean = false
    ): Type {
        // Package names are separated with slashes and nested classes are separated with dots.
        // (e.g com/example/OuterClass.InnerClass).
        val (packageName, simpleName) =
            className.split('/').run { dropLast(1).joinToString(separator = ".") to last() }

        if (simpleName.contains('.')) {
            throw PrivacySandboxParsingException(
                "Error in $packageName.$simpleName: Inner types are not supported in API " +
                    "definitions."
            )
        }

        return Type(packageName, simpleName, typeArguments, isNullable)
    }

    private fun validate(api: ParsedApi) {
        val validationResult = ModelValidator.validate(api)
        if (validationResult.isFailure) {
            throw PrivacySandboxParsingException(
                "Invalid API descriptors:\n" + validationResult.errors.joinToString("\n")
            )
        }
    }
}
