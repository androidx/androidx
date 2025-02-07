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

import com.squareup.kotlinpoet.ClassName

/** Helper class to introspect AppFunction symbols. */
object IntrospectionHelper {
    // Package names
    private const val APP_FUNCTIONS_PACKAGE_NAME = "androidx.appfunctions"
    private const val APP_FUNCTIONS_INTERNAL_PACKAGE_NAME = "androidx.appfunctions.internal"
    private const val APP_FUNCTIONS_METADATA_PACKAGE_NAME = "androidx.appfunctions.metadata"

    // Annotation classes
    object AppFunctionAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunction")
        const val PROPERTY_IS_ENABLED = "isEnabled"
    }

    object AppFunctionSchemaDefinitionAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionSchemaDefinition")
        const val PROPERTY_CATEGORY = "category"
        const val PROPERTY_NAME = "name"
        const val PROPERTY_VERSION = "version"
    }

    object AppFunctionSerializableAnnotation {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionSerializable")
    }

    // Classes
    val APP_FUNCTION_INVENTORY_CLASS =
        ClassName(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, "AppFunctionInventory")
    val APP_FUNCTION_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionMetadata")
    val APP_FUNCTION_FUNCTION_NOT_FOUND_EXCEPTION_CLASS =
        ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionFunctionNotFoundException")
    val APP_FUNCTION_SCHEMA_METADATA_CLASS =
        ClassName(APP_FUNCTIONS_METADATA_PACKAGE_NAME, "AppFunctionSchemaMetadata")

    object ConfigurableAppFunctionFactoryClass {
        val CLASS_NAME =
            ClassName(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, "ConfigurableAppFunctionFactory")

        object CreateEnclosingClassMethod {
            const val METHOD_NAME = "createEnclosingClass"
        }
    }

    object AppFunctionContextClass {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_PACKAGE_NAME, "AppFunctionContext")
        const val CONTEXT_PROPERTY_NAME = "context"
    }

    object AppFunctionInvokerClass {
        val CLASS_NAME = ClassName(APP_FUNCTIONS_INTERNAL_PACKAGE_NAME, "AppFunctionInvoker")
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

        object ToAppFunctionDataMethod {
            const val METHOD_NAME = "toAppFunctionData"
        }
    }
}
