/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName

/** Helper class to introspect AppFunction symbols. */
object IntrospectionHelper {
    // Package names
    const val APP_FUNCTIONS_AGGREGATED_DEPS_PACKAGE_NAME = "appfunctions_aggregated_deps"
    const val SERIALIZABLE_PROXY_PACKAGE_NAME = "androidx.appfunctions.internal.serializableproxies"
    const val APP_FUNCTIONS_INTERNAL_PACKAGE_NAME = "androidx.appfunctions.internal"
    const val APP_FUNCTIONS_SERVICE_INTERNAL_PACKAGE_NAME = "androidx.appfunctions.service.internal"
    private const val APP_FUNCTIONS_PACKAGE_NAME = "androidx.appfunctions"
    private const val APP_FUNCTIONS_SERVICE_PACKAGE_NAME = "androidx.appfunctions.service"
    private const val APP_FUNCTIONS_METADATA_PACKAGE_NAME = "androidx.appfunctions.metadata"

    // Annotation classes
    object DeprecatedAnnotation {
        val CLASS_NAME = ClassName("kotlin", "Deprecated")
        const val PROPERTY_MESSAGE = "message"
    }

    object AppFunctionAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_SERVICE_PACKAGE_NAME, "AppFunction")
        const val PROPERTY_IS_ENABLED = "isEnabled"
        const val PROPERTY_IS_DESCRIBED_BY_KDOC = "isDescribedByKDoc"
    }

    object AppFunctionSchemaDefinitionAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionSchemaDefinition")
        const val PROPERTY_CATEGORY = "category"
        const val PROPERTY_NAME = "name"
        const val PROPERTY_VERSION = "version"
    }

    object AppFunctionSerializableAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionSerializable")
        const val PROPERTY_IS_DESCRIBED_BY_KDOC = "isDescribedByKDoc"
    }

    object AppFunctionSerializableInterfaceAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionSerializableInterface")
    }

    object AppFunctionSerializableProxyAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionSerializableProxy")
        const val PROPERTY_TARGET_CLASS = "targetClass"
    }

    object AppFunctionSchemaCapability {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionSchemaCapability")
    }

    object AppFunctionComponentRegistryAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionComponentRegistry")
        const val PROPERTY_COMPONENT_CATEGORY = "componentCategory"
        const val PROPERTY_COMPONENT_NAMES = "componentNames"
        const val PROPERTY_COMPONENT_DOCSTRINGS = "componentDocStrings"

        object Category {
            const val INVENTORY = "INVENTORY"
            const val INVOKER = "INVOKER"
            const val FUNCTION = "FUNCTION"
            const val SCHEMA_DEFINITION = "SCHEMA_DEFINITION"
            const val SERIALIZABLE = "SERIALIZABLE"
        }
    }

    object AppFunctionIntValueConstraintAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionIntValueConstraint")

        const val PROPERTY_ENUM_VALUES = "enumValues"
    }

    object AppFunctionStringValueConstraintAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionStringValueConstraint")

        const val PROPERTY_ENUM_VALUES = "enumValues"
    }

    // Classes
    val APP_FUNCTION_INVENTORY_CLASS =
        ClassName(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, "AppFunctionInventory")
    val SCHEMA_APP_FUNCTION_INVENTORY_CLASS =
        ClassName(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, "SchemaAppFunctionInventory")
    val APP_FUNCTION_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "CompileTimeAppFunctionMetadata")
    val APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS =
        ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionFunctionNotFoundException")
    val APP_FUNCTION_SCHEMA_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionSchemaMetadata")
    val APP_FUNCTION_PARAMETER_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionParameterMetadata")
    val APP_FUNCTION_DATA_TYPE_METADATA =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionDataTypeMetadata")
    val APP_FUNCTION_DEPRECATION_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionDeprecationMetadata")

    // Primitive Types
    val APP_FUNCTION_UNIT_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionUnitTypeMetadata")
    val APP_FUNCTION_BOOLEAN_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionBooleanTypeMetadata")
    val APP_FUNCTION_BYTES_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionBytesTypeMetadata")
    val APP_FUNCTION_DOUBLE_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionDoubleTypeMetadata")
    val APP_FUNCTION_FLOAT_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionFloatTypeMetadata")
    val APP_FUNCTION_LONG_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionLongTypeMetadata")
    val APP_FUNCTION_INT_TYPE_METADATA_CLASS = // You already have this one
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionIntTypeMetadata")
    val APP_FUNCTION_STRING_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionStringTypeMetadata")
    val APP_FUNCTION_PENDING_INTENT_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionPendingIntentTypeMetadata")

    val APP_FUNCTION_OBJECT_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionObjectTypeMetadata")
    val APP_FUNCTION_ARRAY_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionArrayTypeMetadata")
    val APP_FUNCTION_REFERENCE_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionReferenceTypeMetadata")
    val APP_FUNCTION_ALL_OF_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionAllOfTypeMetadata")
    val APP_FUNCTION_ONE_OF_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionOneOfTypeMetadata")
    val APP_FUNCTION_PARCELABLE_TYPE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionParcelableTypeMetadata")
    val APP_FUNCTION_COMPONENTS_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionComponentsMetadata")
    val APP_FUNCTION_RESPONSE_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionResponseMetadata")

    object ConfigurableAppFunctionFactoryClass {
        val CLASS_NAME =
            ClassName(APP_FUNCTIONS_SERVICE_INTERNAL_PACKAGE_NAME, "ConfigurableAppFunctionFactory")

        object CreateEnclosingClassMethod {
            const val METHOD_NAME = "createEnclosingClass"
        }
    }

    object AppFunctionContextClass {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionContext")
        const val CONTEXT_PROPERTY_NAME = "context"
    }

    object AppFunctionInvokerClass {
        val CLASS_NAME =
            ClassName(APP_FUNCTIONS_SERVICE_INTERNAL_PACKAGE_NAME, "AppFunctionInvoker")
        const val SUPPORTED_FUNCTION_IDS_PROPERTY_NAME = "supportedFunctionIds"

        object UnsafeInvokeMethod {
            const val METHOD_NAME = "unsafeInvoke"
            const val APPLICATION_CONTEXT_PARAM_NAME = "appFunctionContext"
            const val FUNCTION_ID_PARAM_NAME = "functionIdentifier"
            const val PARAMETERS_PARAM_NAME = "parameters"
        }
    }

    object AppFunctionSerializableFactoryClass {
        val CLASS_NAME =
            ClassName(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, "AppFunctionSerializableFactory")

        object FromAppFunctionDataMethod {
            const val METHOD_NAME = "fromAppFunctionData"
            const val APP_FUNCTION_DATA_PARAM_NAME = "appFunctionData"
        }

        object GetAppFunctionDataBuilder {
            const val METHOD_NAME = "getAppFunctionDataBuilder"
            const val QUALIFIED_NAME_PARAM_NAME = "qualifiedName"
        }

        object GetAppFunctionDataWithSpec {
            const val METHOD_NAME = "getAppFunctionDataWithSpec"
            const val QUALIFIED_NAME_PARAM_NAME = "appFunctionData"
        }

        object ToAppFunctionDataMethod {
            const val METHOD_NAME = "toAppFunctionData"
            const val APP_FUNCTION_SERIALIZABLE_PARAM_NAME = "appFunctionSerializable"
        }

        object TypeParameterClass {
            val CLASS_NAME =
                ClassName(
                    APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                    "AppFunctionSerializableFactory",
                    "TypeParameter",
                )

            object PrimitiveTypeParameterClass {
                val CLASS_NAME =
                    ClassName(
                        APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                        "AppFunctionSerializableFactory",
                        "TypeParameter",
                        "PrimitiveTypeParameter",
                    )
            }

            object PrimitiveListTypeParameter {
                val CLASS_NAME =
                    ClassName(
                        APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                        "AppFunctionSerializableFactory",
                        "TypeParameter",
                        "PrimitiveListTypeParameter",
                    )
            }

            object SerializableTypeParameter {
                val CLASS_NAME =
                    ClassName(
                        APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                        "AppFunctionSerializableFactory",
                        "TypeParameter",
                        "SerializableTypeParameter",
                    )
            }

            object SerializableListTypeParameter {
                val CLASS_NAME =
                    ClassName(
                        APP_FUNCTIONS_INTERNAL_PACKAGE_NAME,
                        "AppFunctionSerializableFactory",
                        "TypeParameter",
                        "SerializableListTypeParameter",
                    )
            }
        }
    }

    object AggregatedAppFunctionInventoryClass {
        val CLASS_NAME =
            ClassName(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, "AggregatedAppFunctionInventory")

        const val PROPERTY_INVENTORIES_NAME = "inventories"
    }

    object AggregatedAppFunctionInvokerClass {
        val CLASS_NAME =
            ClassName(APP_FUNCTIONS_SERVICE_INTERNAL_PACKAGE_NAME, "AggregatedAppFunctionInvoker")

        const val PROPERTY_INVOKERS_NAME = "invokers"
    }

    object AppFunctionDataClass {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionData")

        val APP_FUNCTION_DATA_QUALIFIED_NAME_PROPERTY = "qualifiedName"

        object BuilderClass {
            val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionData", "Builder")
        }
    }

    /** [AnnotationSpec] for @RequiresApi(33) */
    val RESTRICT_API_TO_33_ANNOTATION =
        AnnotationSpec.builder(ClassName("androidx.annotation", "RequiresApi"))
            .addMember("%L", 33)
            .build()

    val PARCELABLE_CLASS_NAME = ClassName("android.os", "Parcelable")
}
