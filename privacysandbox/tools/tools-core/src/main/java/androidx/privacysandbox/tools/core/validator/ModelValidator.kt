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

package androidx.privacysandbox.tools.core.validator

import androidx.privacysandbox.tools.core.model.AnnotatedDataClass
import androidx.privacysandbox.tools.core.model.AnnotatedEnumClass
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.core.model.Types.asNonNull
import javax.lang.model.SourceVersion.isKeyword as isJavaKeyword

class ModelValidator private constructor(val api: ParsedApi) {
    private val values = api.values.map(AnnotatedValue::type)
    private val interfaces = api.interfaces.map(AnnotatedInterface::type)
    private val callbacks = api.callbacks.map(AnnotatedInterface::type)

    private val errors: MutableList<String> = mutableListOf()

    companion object {
        fun validate(api: ParsedApi) = ModelValidator(api).validate()
    }

    private fun validate(): ValidationResult {
        validateSingleService()
        validateServiceSupertypes()
        validateNonSuspendFunctionsReturnUnit()
        validateServiceAndInterfaceMethods()
        validateValuePropertyTypes()
        validateCallbackMethods()
        validateNames()
        return ValidationResult(errors)
    }

    private fun validateSingleService() {
        if (api.services.size > 1) {
            errors.add(
                "Multiple services are not supported. Found: " +
                    "${api.services.joinToString { it.type.qualifiedName }}."
            )
        }
    }

    private fun validateServiceSupertypes() {
        val superTypes = api.services.first().superTypes
        if (superTypes.isNotEmpty()) {
            if (superTypes.contains(Types.sandboxedUiAdapter)) {
                errors.add(
                    "Interfaces annotated with @PrivacySandboxService may not extend any other " +
                        "interface. To define a SandboxedUiAdapter, use @PrivacySandboxInterface " +
                        "and return it from this service."
                )
            } else {
                errors.add(
                    "Interfaces annotated with @PrivacySandboxService may not extend any other " +
                        "interface. Found: ${superTypes.joinToString { it.qualifiedName }}."
                )
            }
        }
    }

    private fun validateNonSuspendFunctionsReturnUnit() {
        val annotatedInterfaces = api.services + api.interfaces + api.callbacks
        for (annotatedInterface in annotatedInterfaces) {
            for (method in annotatedInterface.methods) {
                if (!method.isSuspend && method.returnType != Types.unit) {
                    errors.add(
                        "Error in ${annotatedInterface.type.qualifiedName}.${method.name}: " +
                            "functions with return values should be suspending functions."
                    )
                }
            }
        }
    }

    private fun validateServiceAndInterfaceMethods() {
        val annotatedInterfaces = api.services + api.interfaces + api.callbacks
        for (annotatedInterface in annotatedInterfaces) {
            for (method in annotatedInterface.methods) {
                if (method.parameters.any { !(isValidInterfaceParameterType(it.type)) }) {
                    errors.add(
                        "Error in ${annotatedInterface.type.qualifiedName}.${method.name}: " +
                            "only primitives, lists, data/enum classes annotated with " +
                            "@PrivacySandboxValue, interfaces annotated with " +
                            "@PrivacySandboxCallback or @PrivacySandboxInterface, and " +
                            "SdkActivityLaunchers are supported as parameter types."
                    )
                }
                if (!isValidInterfaceReturnType(method.returnType)) {
                    errors.add(
                        "Error in ${annotatedInterface.type.qualifiedName}.${method.name}: " +
                            "only primitives, lists, data/enum classes annotated with " +
                            "@PrivacySandboxValue, interfaces annotated with " +
                            "@PrivacySandboxInterface, and SdkActivityLaunchers are supported as " +
                            "return types."
                    )
                }
            }
        }
    }

    private fun validateValuePropertyTypes() {
        for (value in api.values) {
            if (value !is AnnotatedDataClass) {
                continue
            }
            for (property in value.properties) {
                if (!isValidValuePropertyType(property.type)) {
                    errors.add(
                        "Error in ${value.type.qualifiedName}.${property.name}: " +
                            "only primitives, lists, data/enum classes annotated with " +
                            "@PrivacySandboxValue, interfaces annotated with " +
                            "@PrivacySandboxInterface, and SdkActivityLaunchers are supported as " +
                            "properties."
                    )
                }
            }
        }
    }

    private fun validateCallbackMethods() {
        for (callback in api.callbacks) {
            for (method in callback.methods) {
                if (method.parameters.any { !isValidCallbackParameterType(it.type) }) {
                    errors.add(
                        "Error in ${callback.type.qualifiedName}.${method.name}: " +
                            "only primitives, lists, data/enum classes annotated with " +
                            "@PrivacySandboxValue, interfaces annotated with " +
                            "@PrivacySandboxInterface, and SdkActivityLaunchers are supported as " +
                            "callback parameter types."
                    )
                }
            }
        }
    }

    private fun validateNames() {
        for (value in api.values.filterIsInstance<AnnotatedDataClass>()) {
            for (property in value.properties) {
                if (isJavaKeyword(property.name)) {
                    errors.add(
                        "Error in ${value.type.qualifiedName}.${property.name}: property name " +
                            "must not be a Java keyword."
                    )
                }
            }
        }
        for (value in api.values.filterIsInstance<AnnotatedEnumClass>()) {
            for (variant in value.variants) {
                if (isJavaKeyword(variant)) {
                    errors.add(
                        "Error in ${value.type.qualifiedName}.$variant: enum constant " +
                            "name must not be a Java keyword."
                    )
                }
            }
        }
        for (iface in api.interfaces + api.callbacks + api.services) {
            for (method in iface.methods) {
                if (isJavaKeyword(method.name)) {
                    errors.add(
                        "Error in ${iface.type.qualifiedName}.${method.name}: method name " +
                            "must not be a Java keyword."
                    )
                }
            }
        }
    }

    private fun isValidInterfaceParameterType(type: Type) =
        isValue(type) ||
            isInterface(type) ||
            isPrimitive(type) ||
            isList(type) ||
            isCallback(type) ||
            isBundledType(type)

    private fun isValidInterfaceReturnType(type: Type) =
        isValue(type) ||
            isInterface(type) ||
            isPrimitive(type) ||
            isList(type) ||
            isBundledType(type)

    private fun isValidValuePropertyType(type: Type) =
        isValue(type) ||
            isInterface(type) ||
            isPrimitive(type) ||
            isList(type) ||
            isBundledType(type)

    private fun isValidCallbackParameterType(type: Type) =
        isValue(type) ||
            isInterface(type) ||
            isPrimitive(type) ||
            isList(type) ||
            isBundledType(type)

    private fun isValue(type: Type) = values.contains(type.asNonNull())

    private fun isInterface(type: Type) = interfaces.contains(type.asNonNull())

    private fun isCallback(type: Type) = callbacks.contains(type.asNonNull())

    private fun isPrimitive(type: Type) = Types.primitiveTypes.contains(type.asNonNull())

    private fun isList(type: Type): Boolean {
        if (type.qualifiedName == "kotlin.collections.List") {
            require(type.typeParameters.size == 1) {
                "List type should have one type parameter, found ${type.typeParameters}."
            }
            if (type.isNullable) {
                errors.add("Nullable lists are not supported")
            }
            val typeParameter = type.typeParameters.first()
            if (typeParameter.isNullable) {
                errors.add(
                    "Nullable type parameters are not supported in lists, found ${
                        typeParameter.qualifiedName
                    }"
                )
            }
            val holdsValidType =
                isValue(typeParameter) || isPrimitive(typeParameter) || isBundledType(typeParameter)
            if (!holdsValidType) {
                errors.add("Invalid type parameter in list, found ${typeParameter.qualifiedName}.")
            }
            return true
        }
        return false
    }

    private fun isBundledType(type: Type) =
        type == Types.sdkActivityLauncher || type.asNonNull() == Types.bundle
}

data class ValidationResult(val errors: List<String>) {
    val isSuccess = errors.isEmpty()
    val isFailure = !isSuccess
}
